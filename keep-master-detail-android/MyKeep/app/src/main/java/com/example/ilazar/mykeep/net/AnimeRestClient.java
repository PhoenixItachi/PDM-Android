package com.example.ilazar.mykeep.net;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.example.ilazar.mykeep.R;
import com.example.ilazar.mykeep.content.Anime;
import com.example.ilazar.mykeep.content.User;
import com.example.ilazar.mykeep.content.database.KeepDatabase;
import com.example.ilazar.mykeep.net.mapping.CredentialsWriter;
import com.example.ilazar.mykeep.net.mapping.IssueReader;
import com.example.ilazar.mykeep.net.mapping.AnimeReader;
import com.example.ilazar.mykeep.net.mapping.AnimeWriter;
import com.example.ilazar.mykeep.net.mapping.ResourceListReader;
import com.example.ilazar.mykeep.util.Cancellable;
import com.example.ilazar.mykeep.util.CancellableCallable;
import com.example.ilazar.mykeep.util.OnErrorListener;
import com.example.ilazar.mykeep.util.OnSuccessListener;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AnimeRestClient {
    private static final String TAG = AnimeRestClient.class.getSimpleName();
    public static final String APPLICATION_JSON = "application/json";
    public static final String UTF_8 = "UTF-8";
    public static final String LAST_MODIFIED = "Last-Modified";
    private final KeepDatabase mKD;

    private final OkHttpClient mOkHttpClient;
    private final String mApiUrl;
    private final String mAnimeUrl;
    private final Context mContext;
    private final String mAuthUrl;
    private final String mPingUrl;
    private Socket mSocket;
    private User mUser;

    public AnimeRestClient(Context context) {
        mContext = context;
        mKD = new KeepDatabase(context);
        mOkHttpClient = new OkHttpClient();
        mApiUrl = context.getString(R.string.api_url);
        mAnimeUrl = mApiUrl.concat("/api/anime");
        mAuthUrl = mApiUrl.concat("/api/auth");
        mPingUrl = mApiUrl.concat("/api/anime/ping");
        Log.d(TAG, "AnimeRestClient created");
    }

    public CancellableOkHttpAsync<String> getToken(User user, OnSuccessListener<String> successListener, OnErrorListener errorListener) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonWriter writer = null;
        try {
            writer = new JsonWriter(new OutputStreamWriter(baos, UTF_8));
            new CredentialsWriter().write(user, writer);
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "getToken failed", e);
            throw new ResourceException(e);
        }
        return new CancellableOkHttpAsync<String>(
                new Request.Builder()
                        .url(String.format("%s/session?username=%s&password=%s", mAuthUrl, user.getUsername(), user.getPassword()))
                        .get()
                        .build(),
                new ResponseReader<String>() {
                    @Override
                    public String read(Response response) throws Exception {
                        String jsonData = response.body().string();
                        JSONObject Jobject = new JSONObject(jsonData);
                        if (response.code() == 201) {
                            return Jobject.getString("token");
                        } else {
                            return null;
                        }
                    }
                },
                successListener,
                errorListener
        );
    }

    public OkHttpCancellableCallable<LastModifiedList<Anime>> search(String mAnimeLastUpdate) {
        Request.Builder requestBuilder = new Request.Builder().url(mAnimeUrl);
        if (mAnimeLastUpdate != null) {
            requestBuilder.header(LAST_MODIFIED, mAnimeLastUpdate);
        }
        addAuthToken(requestBuilder);
        return new OkHttpCancellableCallable<LastModifiedList<Anime>>(
                requestBuilder.build(),
                new ResponseReader<LastModifiedList<Anime>>() {
                    @Override
                    public LastModifiedList<Anime> read(Response response) throws Exception {
                        List<Anime> anime = new ArrayList<Anime>();
                        String jsonData = response.body().string();
                        JSONObject Jobject = new JSONObject(jsonData);
                        String lastModified = Jobject.getString("lastModified");
                        if (Jobject.getString("success") == "true") { //not modified
                            JSONArray jsonList = Jobject.getJSONArray("items");

                            for (int i = 0; i < jsonList.length(); i++) {
                                JSONObject el = jsonList.getJSONObject(i);
                                Anime a = new AnimeReader().read(el);
                                anime.add(a);
                            }
                            return new LastModifiedList<>(lastModified,anime);
                        } else {
                            return new LastModifiedList<>(lastModified,anime);
                        }
                    }
                }
        );
    }

    private void addAuthToken(Request.Builder requestBuilder) {
        User user = mKD.getCurrentUser();
        if (user != null) {
            requestBuilder.header("Authorization", String.format("%s", user.getToken()));
        }
    }

    public Cancellable searchAsync(
            final String mAnimeLastUpdate,
            final OnSuccessListener<LastModifiedList<Anime>> successListener,
            final OnErrorListener errorListener) {

        Request.Builder requestBuilder = new Request.Builder().url(mAnimeUrl);
        addAuthToken(requestBuilder);

        if(mAnimeLastUpdate != null)
            requestBuilder.header(LAST_MODIFIED, mAnimeLastUpdate);

        return new CancellableOkHttpAsync<LastModifiedList<Anime>>(
                requestBuilder.build(),
                new ResponseReader<LastModifiedList<Anime>>() {
                    @Override
                    public LastModifiedList<Anime> read(Response response) throws Exception {
                        if(response.code() == 200){
                            List<Anime> anime = new ArrayList<Anime>();
                            String jsonData = response.body().string();
                            JSONObject jsonObject = new JSONObject(jsonData);
                            JSONArray jsonList = jsonObject.getJSONArray("items");
                            for (int i = 0; i < jsonList.length(); i++) {
                                JSONObject el = jsonList.getJSONObject(i);
                                Anime a = new AnimeReader().read(el);
                                anime.add(a);
                            }
                            return new LastModifiedList<>(jsonObject.getString("lastModified"), anime);
                        }

                        if(response.code() == 401)
                            errorListener.onError(new Exception("401"));


                        return new LastModifiedList<>(mAnimeLastUpdate, null);
                    }
                },
                successListener,
                errorListener
        );
    }

    public Cancellable readAsync(String animeId,
                                 final OnSuccessListener<Anime> successListener,
                                 final OnErrorListener errorListener) {
        Request.Builder builder = new Request.Builder().url(String.format("%s/%s", mAnimeUrl, animeId));
        addAuthToken(builder);
        return new CancellableOkHttpAsync<Anime>(
                builder.build(),
                new ResponseReader<Anime>() {
                    @Override
                    public Anime read(Response response) throws Exception {
                        if (response.code() == 200) {
                            String jsonData = response.body().string();
                            JSONObject jsonObject = new JSONObject(jsonData);
                            return new AnimeReader().read(jsonObject);
                        } else { //404 not found
                            return null;
                        }
                    }
                },
                successListener,
                errorListener
        );
    }

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public Cancellable addAsync(Anime anime,
                                 final OnSuccessListener<String> successListener,
                                 final OnErrorListener errorListener) {

        Gson gson = new Gson();
        RequestBody body = RequestBody.create(JSON, gson.toJson(anime));
        Request.Builder builder = new Request.Builder().post(body).url(mAnimeUrl);
        addAuthToken(builder);
        return new CancellableOkHttpAsync<String>(
                builder.build(),
                new ResponseReader<String>() {
                    @Override
                    public String read(Response response) throws Exception {
                        if (response.code() == 200) {
                            return "Added";
                        } else { //404 not found
                            return "Error";
                        }
                    }
                },
                successListener,
                errorListener
        );
    }


    public Cancellable deleteAsync(String animeId,
                                 final OnSuccessListener successListener,
                                 final OnErrorListener errorListener) {
        Request.Builder builder = new Request.Builder().delete().url(String.format("%s/%s", mAnimeUrl, animeId));
        addAuthToken(builder);
        return new CancellableOkHttpAsync<String>(
                builder.build(),
                new ResponseReader<String>() {
                    @Override
                    public String read(Response response) throws Exception {
                        if (response.code() == 200) {
                            return "OK";
                        } else { //404 not found
                            return "NOT Deleted";
                        }
                    }
                },
                successListener,
                errorListener
        );
    }

    public Cancellable updateAsync(Anime anime,
                                   final OnSuccessListener<Anime> successListener,
                                   final OnErrorListener errorListener) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(baos, UTF_8));
            new AnimeWriter().write(anime, writer);
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "updateAsync failed", e);
            errorListener.onError(new ResourceException(e));
        } finally {
            Request.Builder builder = new Request.Builder()
                    .url(String.format("%s/%s", mAnimeUrl, anime.getId()))
                    .put(RequestBody.create(MediaType.parse(APPLICATION_JSON), baos.toByteArray()));
            addAuthToken(builder);
            return new CancellableOkHttpAsync<Anime>(
                    builder.build(),
                    new ResponseReader<Anime>() {
                        @Override
                        public Anime read(Response response) throws Exception {
                            int code = response.code();
                            String jsonData = response.body().string();
                            JSONObject jsonObject = new JSONObject(jsonData);
                            JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), UTF_8));
                            if (code == 400 || code == 409 || code == 405) { //bad request, conflict, method not allowed
                                throw new ResourceException(new ResourceListReader<Issue>(new IssueReader()).read(reader));
                            }
                            return new AnimeReader().read(jsonObject);
                        }
                    },
                    successListener,
                    errorListener
            );
        }
    }

    public void setUser(User user) {
        mUser = user;
    }

    private class OkHttpCancellableCallable<E> implements CancellableCallable<E> {
        private final Call mCall;
        private final Request mRequest;
        private final ResponseReader<E> mResponseReader;

        public OkHttpCancellableCallable(Request request, ResponseReader<E> responseReader) {
            mRequest = request;
            mResponseReader = responseReader;
            mCall = mOkHttpClient.newCall(request);
        }

        @Override
        public E call() throws Exception {
            try {
                Log.d(TAG, String.format("started %s %s", mRequest.method(), mRequest.url()));
                Response response = mCall.execute();
                Log.d(TAG, String.format("succeeded %s %s", mRequest.method(), mRequest.url()));
                if (mCall.isCanceled()) {
                    return null;
                }
                return mResponseReader.read(response);
            } catch (Exception e) {
                Log.e(TAG, String.format("failed %s %s", mRequest.method(), mRequest.url()), e);
                throw e instanceof ResourceException ? e : new ResourceException(e);
            }
        }

        @Override
        public void cancel() {
            if (mCall != null) {
                mCall.cancel();
            }
        }
    }

    public Cancellable ping(final OnSuccessListener<Boolean> successListener,
                            final OnErrorListener errorListener){
        Request.Builder builder = new Request.Builder().get().url(mPingUrl);
        return new CancellableOkHttpAsync<Boolean>(
                builder.build(),
                new ResponseReader<Boolean>() {
                    @Override
                    public Boolean read(Response response) throws Exception {
                        if (response.code() == 200) {
                            return true;
                        } else { //404 not found
                            return false;
                        }
                    }
                },
                successListener,
                errorListener
        );
    }

    private interface ResponseReader<E> {
        E read(Response response) throws Exception;
    }

    private class CancellableOkHttpAsync<E> implements Cancellable {
        private Call mCall;

        public CancellableOkHttpAsync(
                final Request request,
                final ResponseReader<E> responseReader,
                final OnSuccessListener<E> successListener,
                final OnErrorListener errorListener) {
            try {
                mCall = mOkHttpClient.newCall(request);
                Log.d(TAG, String.format("started %s %s", request.method(), request.url()));
                //retry 3x, renew token
                mCall.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        notifyFailure(e, request, errorListener);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            notifySuccess(response, request, successListener, responseReader);
                        } catch (Exception e) {
                            notifyFailure(e, request, errorListener);
                        }
                    }
                });
            } catch (Exception e) {
                notifyFailure(e, request, errorListener);
            }
        }

        @Override
        public void cancel() {
            if (mCall != null) {
                mCall.cancel();
            }
        }

        private void notifySuccess(Response response, Request request,
                                   OnSuccessListener<E> successListener, ResponseReader<E> responseReader) throws Exception {
            if (mCall.isCanceled()) {
                Log.d(TAG, String.format("completed, but cancelled %s %s", request.method(), request.url()));
            } else {
                Log.d(TAG, String.format("completed %s %s", request.method(), request.url()));
                successListener.onSuccess(responseReader.read(response));
            }
        }

        private void notifyFailure(Exception e, Request request, OnErrorListener errorListener) {
            if (mCall.isCanceled()) {
                Log.d(TAG, String.format("failed, but cancelled %s %s", request.method(), request.url()));
            } else {
                Log.e(TAG, String.format("failed %s %s", request.method(), request.url()), e);
                errorListener.onError(e instanceof ResourceException ? e : new ResourceException(e));
            }
        }
    }
}
