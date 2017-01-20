package com.example.ilazar.mykeep;

import android.app.Application;
import android.util.Log;

import com.example.ilazar.mykeep.net.NoteRestClient;
import com.example.ilazar.mykeep.net.NoteSocketClient;
import com.example.ilazar.mykeep.service.NoteManager;

public class KeepApp extends Application {
  public static final String TAG = KeepApp.class.getSimpleName();
  private NoteManager mNoteManager;
  private NoteRestClient mNoteRestClient;
  private NoteSocketClient mNoteSocketClient;

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate");
    super.onCreate();
    mNoteManager = new NoteManager(this);
    mNoteRestClient = new NoteRestClient(this);
    mNoteSocketClient = new NoteSocketClient(this);
    mNoteManager.setNoteRestClient(mNoteRestClient);
    mNoteManager.setNoteSocketClient(mNoteSocketClient);
  }

  public NoteManager getNoteManager() {
    return mNoteManager;
  }
  public NoteRestClient getNoteRestClient() {
    return mNoteRestClient;
  }

  @Override
  public void onTerminate() {
    Log.d(TAG, "onTerminate");
    super.onTerminate();
  }
}
