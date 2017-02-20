package com.example.ilazar.mykeep;

import android.app.Application;
import android.util.Log;

import com.example.ilazar.mykeep.net.AnimeRestClient;
import com.example.ilazar.mykeep.net.AnimeSocketClient;
import com.example.ilazar.mykeep.service.AnimeManager;

public class KeepApp extends Application {
  public static final String TAG = KeepApp.class.getSimpleName();
  private AnimeManager mAnimeManager;
  private AnimeRestClient mAnimeRestClient;
  private AnimeSocketClient mAnimeSocketClient;

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate");
    super.onCreate();
    mAnimeManager = new AnimeManager(this);
    mAnimeRestClient = new AnimeRestClient(this);
    mAnimeSocketClient = new AnimeSocketClient(this);
    mAnimeManager.setAnimeRestClient(mAnimeRestClient);
//    mAnimeManager.setNoteSocketClient(mAnimeSocketClient);
  }

  public AnimeManager getAnimeManager() {
    return mAnimeManager;
  }
  public AnimeRestClient getAnimeRestClient() {
    return mAnimeRestClient;
  }

  @Override
  public void onTerminate() {
    Log.d(TAG, "onTerminate");
    super.onTerminate();
  }
}
