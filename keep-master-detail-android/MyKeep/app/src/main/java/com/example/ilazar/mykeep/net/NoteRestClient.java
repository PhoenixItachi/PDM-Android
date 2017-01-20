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
import com.example.ilazar.mykeep.net.mapping.NoteReader;
import com.example.ilazar.mykeep.net.mapping.NoteWriter;
import com.example.ilazar.mykeep.net.mapping.ResourceListReader;
import com.example.ilazar.mykeep.util.Cancellable;
import com.example.ilazar.mykeep.util.CancellableCallable;
import com.example.ilazar.mykeep.util.OnErrorListener;
import com.example.ilazar.mykeep.util.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

public class NoteRestClient {
  private static final String TAG = NoteRestClient.class.getSimpleName();
  public static final String APPLICATION_JSON = "application/json";
  public static final String UTF_8 = "UTF-8";
  public static final String LAST_MODIFIED = "Last-Modified";
  private final KeepDatabase mKD;

  private final OkHttpClient mOkHttpClient;
  private final String mApiUrl;
  private final String mAnimeUrl;
  private final Context mContext;
  private final String mAuthUrl;
  private Socket mSocket;
  private User mUser;

  public NoteRestClient(Context context) {
    mContext = context;
    mKD = new KeepDatabase(context);
    mOkHttpClient = new OkHttpClient();
    mApiUrl = context.getString(R.string.api_url);
    mAnimeUrl = mApiUrl.concat("/api/anime");
    mAuthUrl = mApiUrl.concat("/api/auth");
    Log.d(TAG, "NoteRestClient created");
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
//            JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), UTF_8));
            String jsonData = response.body().string();
            JSONObject Jobject = new JSONObject(jsonData);
            if (response.code() == 201) { //created
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

  public OkHttpCancellableCallable<List<Anime>> search(String mNotesLastUpdate) {
    Request.Builder requestBuilder = new Request.Builder().url(mAnimeUrl);
    if (mNotesLastUpdate != null) {
      requestBuilder.header(LAST_MODIFIED, mNotesLastUpdate);
    }
    addAuthToken(requestBuilder);
    return new OkHttpCancellableCallable<List<Anime>>(
        requestBuilder.build(),
        new ResponseReader<List<Anime>>() {
          @Override
          public List<Anime> read(Response response) throws Exception {
            List<Anime> anime = new ArrayList<Anime>();
            String jsonData = response.body().string();
            JSONObject Jobject = new JSONObject(jsonData);
            if (Jobject.getString("success") == "true") { //not modified
              JSONArray jsonList = Jobject.getJSONArray("items");
              for(int i = 0; i<jsonList.length(); i++){
                JSONObject el = jsonList.getJSONObject(i);
                Anime a = new Anime(el.getString("title"), el.getString("score"), el.getString("noEpisodes"), el.getString("synopsis"));
                anime.add(a);
              }
              return anime;
            } else {
              return new ArrayList<Anime>();
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
      String mNotesLastUpdate,
      final OnSuccessListener<List<Anime>> successListener,
      final OnErrorListener errorListener) {
    Request.Builder requestBuilder = new Request.Builder().url(mAnimeUrl);
    addAuthToken(requestBuilder);

    return new CancellableOkHttpAsync<List<Anime>>(
        requestBuilder.build(),
        new ResponseReader<List<Anime>>() {
          @Override
          public List<Anime> read(Response response) throws Exception {
            List<Anime> anime = new ArrayList<Anime>();
            String jsonData = response.body().string();
              JSONArray jsonList = new JSONArray(jsonData);
              for(int i = 0; i<jsonList.length(); i++){
                JSONObject el = jsonList.getJSONObject(i);
                Anime a = new Anime(el.getString("title"), el.getString("score"), el.getString("noEpisodes"), el.getString("synopsis"));
                anime.add(a);
              }
                return anime;
          }
        },
        successListener,
        errorListener
    );
  }

  public Cancellable readAsync(String noteId,
                               final OnSuccessListener<Anime> successListener,
                               final OnErrorListener errorListener) {
    Request.Builder builder = new Request.Builder().url(String.format("%s/%s", mAnimeUrl, noteId));
    addAuthToken(builder);
    return new CancellableOkHttpAsync<Anime>(
        builder.build(),
        new ResponseReader<Anime>() {
          @Override
          public Anime read(Response response) throws Exception {
            if (response.code() == 200) {
              JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), UTF_8));
              return new NoteReader().read(reader);
            } else { //404 not found
              return null;
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
      new NoteWriter().write(anime, writer);
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
              JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), UTF_8));
              if (code == 400 || code == 409 || code == 405) { //bad request, conflict, method not allowed
                throw new ResourceException(new ResourceListReader<Issue>(new IssueReader()).read(reader));
              }
              return new NoteReader().read(reader);
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

  private static interface ResponseReader<E> {
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
