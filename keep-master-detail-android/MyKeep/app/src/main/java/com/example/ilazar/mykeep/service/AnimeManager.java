package com.example.ilazar.mykeep.service;

import android.content.Context;
import android.provider.MediaStore;
import android.util.Log;

import com.example.ilazar.mykeep.AnimeListActivity;
import com.example.ilazar.mykeep.content.Anime;
import com.example.ilazar.mykeep.content.User;
import com.example.ilazar.mykeep.content.database.KeepDatabase;
import com.example.ilazar.mykeep.net.AnimeRestClient;
import com.example.ilazar.mykeep.net.LastModifiedList;
import com.example.ilazar.mykeep.net.AnimeSocketClient;
import com.example.ilazar.mykeep.net.ResourceChangeListener;
import com.example.ilazar.mykeep.net.ResourceException;
import com.example.ilazar.mykeep.net.mapping.AnimeReader;
import com.example.ilazar.mykeep.util.Cancellable;
import com.example.ilazar.mykeep.util.CancellableCallable;
import com.example.ilazar.mykeep.util.DialogUtils;
import com.example.ilazar.mykeep.util.OnErrorListener;
import com.example.ilazar.mykeep.util.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AnimeManager extends Observable {
    private static final String TAG = AnimeManager.class.getSimpleName();
    private final KeepDatabase mKD;

    private final String LIST_FILENAME = "animeList.txt";

    private ConcurrentMap<String, Anime> mAnime = new ConcurrentHashMap<String, Anime>();
    private String mAnimeLastUpdate;

    private final Context mContext;
    private AnimeRestClient mAnimeRestClient;
    private AnimeSocketClient mAnimeSocketClient;
    private String mToken;
    private User mCurrentUser;
    private boolean isOnline;

    public AnimeManager(Context context) {
        mContext = context;
        mKD = new KeepDatabase(context);

    }

    public boolean getIsOnlineStatus(){
        return isOnline;
    }

    public CancellableCallable<LastModifiedList<Anime>> getAnimesCall() {
        Log.d(TAG, "getAnimesCall");
        return mAnimeRestClient.search(mAnimeLastUpdate);
    }

    public List<Anime> executeAnimesCall(CancellableCallable<LastModifiedList<Anime>> getAnimesCall) throws Exception {
        Log.d(TAG, "execute getNotes...");
        LastModifiedList<Anime> result = getAnimesCall.call();
        List<Anime> animes = result.getList();
        if (animes != null) {
            mAnimeLastUpdate = result.getLastModified();
            updateCachedAnime(animes);
            notifyObservers();
        }
        return cachedAnimeByUpdated();
    }

    public AnimeLoader getAnimeLoader() {
        Log.d(TAG, "getAnimeLoader...");
        return new AnimeLoader(mContext, this);
    }

    public void setAnimeRestClient(AnimeRestClient animeRestClient) {
        mAnimeRestClient = animeRestClient;
    }

    public Cancellable getAnimeAsync(final OnSuccessListener<List<Anime>> successListener, OnErrorListener errorListener) {
        Log.d(TAG, "getAnimeAsync...");
        return mAnimeRestClient.searchAsync(mAnimeLastUpdate, new OnSuccessListener<LastModifiedList<Anime>>() {
            @Override
            public void onSuccess(LastModifiedList<Anime> result) {
                Log.d(TAG, "getAnimeAsync succeeded");
                List<Anime> animes = result.getList();
                mAnimeLastUpdate = result.getLastModified();
                if (animes != null) {
                    updateCachedAnime(animes);
                }
                successListener.onSuccess(cachedAnimeByUpdated());
                notifyObservers();
            }
        }, errorListener);
    }

    public Cancellable addAnimeAsync(Anime anime, final OnSuccessListener<String> successListener, OnErrorListener errorListener) {
        Log.d(TAG, "getAnimeAsync...");
        return mAnimeRestClient.addAsync(anime, new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "addAnimeAsync succeeded");
                successListener.onSuccess(result);
            }
        }, errorListener);
    }

    public Cancellable ping(OnSuccessListener<Boolean> onSuccessListener, OnErrorListener onErrorListener){
        return mAnimeRestClient.ping(onSuccessListener, onErrorListener);
    }

    public Cancellable getAnimeAsync(
            final String animeId,
            final OnSuccessListener<Anime> successListener,
            final OnErrorListener errorListener) {
        Log.d(TAG, "getAnimeAsync...");
        return mAnimeRestClient.readAsync(animeId, new OnSuccessListener<Anime>() {

            @Override
            public void onSuccess(Anime anime) {
                Log.d(TAG, "getAnimeAsync succeeded");
                if (anime == null) {
                    setChanged();
                    mAnime.remove(animeId);
                } else {
                    if (!anime.equals(mAnime.get(anime.mId))) {
                        setChanged();
                        mAnime.put(animeId, anime);
                    }
                }
                successListener.onSuccess(anime);
                notifyObservers();
            }
        }, errorListener);
    }

    public Cancellable deleteAnimeAsync(
            final String animeId,
            final OnSuccessListener<String> successListener,
            final OnErrorListener errorListener) {
        Log.d(TAG, "getAnimeAsync...");
        return mAnimeRestClient.deleteAsync(animeId, new OnSuccessListener<String>() {

            @Override
            public void onSuccess(String status) {
                Log.d(TAG, "getAnimeAsync succeeded");
                if (status.equals("OK")) {
                    setChanged();
                    mAnime.remove(animeId);
                } else {
                    errorListener.onError(new Exception("error"));
                }
                successListener.onSuccess(status);
                notifyObservers();
            }
        }, errorListener);
    }

    public Cancellable saveAnimeAsync(
            final Anime anime,
            final OnSuccessListener<Anime> successListener,
            final OnErrorListener errorListener) {
        Log.d(TAG, "saveAnimeAsync...");
        return mAnimeRestClient.updateAsync(anime, new OnSuccessListener<Anime>() {

            @Override
            public void onSuccess(Anime anime) {
                Log.d(TAG, "saveAnimeAsync succeeded");
                mAnime.put(anime.getId(), anime);
                successListener.onSuccess(anime);
                setChanged();
                notifyObservers();
            }
        }, errorListener);
    }

    public void subscribeChangeListener() {
        mAnimeSocketClient.subscribe(new ResourceChangeListener<Anime>() {
            @Override
            public void onCreated(Anime anime) {
                Log.d(TAG, "changeListener, onCreated");
                ensureAnimeCached(anime);
            }

            @Override
            public void onUpdated(Anime anime) {
                Log.d(TAG, "changeListener, onUpdated");
                ensureAnimeCached(anime);
            }

            @Override
            public void onDeleted(String animeId) {
                Log.d(TAG, "changeListener, onDeleted");
                if (mAnime.remove(animeId) != null) {
                    setChanged();
                    notifyObservers();
                }
            }

            private void ensureAnimeCached(Anime anime) {
                if (!anime.equals(mAnime.get(anime.getId()))) {
                    Log.d(TAG, "changeListener, cache updated");
                    mAnime.put(anime.getId(), anime);
                    setChanged();
                    notifyObservers();
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "changeListener, error", t);
            }
        });
    }

    public void unsubscribeChangeListener() {
        mAnimeSocketClient.unsubscribe();
    }

    public void setNoteSocketClient(AnimeSocketClient animeSocketClient) {
        mAnimeSocketClient = animeSocketClient;
    }

    private void updateCachedAnime(List<Anime> animes) {
        Log.d(TAG, "updateCachedAnime");
        mAnime.clear();
        for (Anime anime : animes) {
            mAnime.put(anime.getId(), anime);
        }
        setChanged();
    }

    private List<Anime> cachedAnimeByUpdated() {
        ArrayList<Anime> animes = new ArrayList<>(mAnime.values());
        Collections.sort(animes, new AnimeByUpdatedComparator());
        return animes;
    }

    public List<Anime> getCachedAnime() {
        return cachedAnimeByUpdated();
    }

    public Cancellable loginAsync(
            String username, String password,
            final OnSuccessListener<String> successListener,
            final OnErrorListener errorListener) {
        final User user = new User(username, password);
        return mAnimeRestClient.getToken(
                user, new OnSuccessListener<String>() {

                    @Override
                    public void onSuccess(String token) {
                        mToken = token;
                        if (mToken != null) {
                            user.setToken(mToken);
                            setCurrentUser(user);
                            mKD.saveUser(user);
                            successListener.onSuccess(mToken);
                        } else {
                            errorListener.onError(new ResourceException(new IllegalArgumentException("Invalid credentials")));
                        }
                    }
                }, errorListener);
    }

    public void setCurrentUser(User currentUser) {
        mCurrentUser = currentUser;
        mAnimeRestClient.setUser(currentUser);
    }

    public User getCurrentUser() {
        return mKD.getCurrentUser();
    }

    private class AnimeByUpdatedComparator implements java.util.Comparator<Anime> {
        @Override
        public int compare(Anime n1, Anime n2) {
            return (int) (n1.mUpdated.compareTo(n2.mUpdated));
        }
    }
}
