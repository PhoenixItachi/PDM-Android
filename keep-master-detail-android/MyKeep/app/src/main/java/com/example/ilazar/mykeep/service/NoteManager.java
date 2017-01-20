package com.example.ilazar.mykeep.service;

import android.content.Context;
import android.util.Log;

import com.example.ilazar.mykeep.content.Anime;
import com.example.ilazar.mykeep.content.User;
import com.example.ilazar.mykeep.content.database.KeepDatabase;
import com.example.ilazar.mykeep.net.LastModifiedList;
import com.example.ilazar.mykeep.net.NoteRestClient;
import com.example.ilazar.mykeep.net.NoteSocketClient;
import com.example.ilazar.mykeep.net.ResourceChangeListener;
import com.example.ilazar.mykeep.net.ResourceException;
import com.example.ilazar.mykeep.util.Cancellable;
import com.example.ilazar.mykeep.util.CancellableCallable;
import com.example.ilazar.mykeep.util.OnErrorListener;
import com.example.ilazar.mykeep.util.OnSuccessListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NoteManager extends Observable {
  private static final String TAG = NoteManager.class.getSimpleName();
  private final KeepDatabase mKD;

  private ConcurrentMap<String, Anime> mNotes = new ConcurrentHashMap<String, Anime>();
  private String mNotesLastUpdate;

  private final Context mContext;
  private NoteRestClient mNoteRestClient;
  private NoteSocketClient mNoteSocketClient;
  private String mToken;
  private User mCurrentUser;

  public NoteManager(Context context) {
    mContext = context;
    mKD = new KeepDatabase(context);
  }

  public CancellableCallable<List<Anime>> getNotesCall() {
    Log.d(TAG, "getNotesCall");
    return mNoteRestClient.search(mNotesLastUpdate);
  }

  public List<Anime> executeNotesCall(CancellableCallable<LastModifiedList<Anime>> getNotesCall) throws Exception {
    Log.d(TAG, "execute getNotes...");
    LastModifiedList<Anime> result = getNotesCall.call();
    List<Anime> animes = result.getList();
    if (animes != null) {
      mNotesLastUpdate = result.getLastModified();
      updateCachedNotes(animes);
      notifyObservers();
    }
    return cachedNotesByUpdated();
  }

  public NoteLoader getNoteLoader() {
    Log.d(TAG, "getNoteLoader...");
    return new NoteLoader(mContext, this);
  }

  public void setNoteRestClient(NoteRestClient noteRestClient) {
    mNoteRestClient = noteRestClient;
  }

  public Cancellable getNotesAsync(final OnSuccessListener<List<Anime>> successListener, OnErrorListener errorListener) {
    Log.d(TAG, "getNotesAsync...");
    return mNoteRestClient.searchAsync(mNotesLastUpdate, new OnSuccessListener<List<Anime>>() {

      @Override
      public void onSuccess(List<Anime> result) {
        Log.d(TAG, "getNotesAsync succeeded");
        List<Anime> animes = result;
        if (animes != null) {
          updateCachedNotes(animes);
        }
        successListener.onSuccess(cachedNotesByUpdated());
        notifyObservers();
      }
    }, errorListener);
  }

  public Cancellable getNoteAsync(
      final String noteId,
      final OnSuccessListener<Anime> successListener,
      final OnErrorListener errorListener) {
    Log.d(TAG, "getNoteAsync...");
    return mNoteRestClient.readAsync(noteId, new OnSuccessListener<Anime>() {

      @Override
      public void onSuccess(Anime anime) {
        Log.d(TAG, "getNoteAsync succeeded");
        if (anime == null) {
          setChanged();
          mNotes.remove(noteId);
        } else {
          if (!anime.equals(mNotes.get(anime.getId()))) {
            setChanged();
            mNotes.put(noteId, anime);
          }
        }
        successListener.onSuccess(anime);
        notifyObservers();
      }
    }, errorListener);
  }

  public Cancellable saveNoteAsync(
      final Anime anime,
      final OnSuccessListener<Anime> successListener,
      final OnErrorListener errorListener) {
    Log.d(TAG, "saveNoteAsync...");
    return mNoteRestClient.updateAsync(anime, new OnSuccessListener<Anime>() {

      @Override
      public void onSuccess(Anime anime) {
        Log.d(TAG, "saveNoteAsync succeeded");
        mNotes.put(anime.getId(), anime);
        successListener.onSuccess(anime);
        setChanged();
        notifyObservers();
      }
    }, errorListener);
  }

  public void subscribeChangeListener() {
    mNoteSocketClient.subscribe(new ResourceChangeListener<Anime>() {
      @Override
      public void onCreated(Anime anime) {
        Log.d(TAG, "changeListener, onCreated");
        ensureNoteCached(anime);
      }

      @Override
      public void onUpdated(Anime anime) {
        Log.d(TAG, "changeListener, onUpdated");
        ensureNoteCached(anime);
      }

      @Override
      public void onDeleted(String noteId) {
        Log.d(TAG, "changeListener, onDeleted");
        if (mNotes.remove(noteId) != null) {
          setChanged();
          notifyObservers();
        }
      }

      private void ensureNoteCached(Anime anime) {
        if (!anime.equals(mNotes.get(anime.getId()))) {
          Log.d(TAG, "changeListener, cache updated");
          mNotes.put(anime.getId(), anime);
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
    mNoteSocketClient.unsubscribe();
  }

  public void setNoteSocketClient(NoteSocketClient noteSocketClient) {
    mNoteSocketClient = noteSocketClient;
  }

  private void updateCachedNotes(List<Anime> animes) {
    Log.d(TAG, "updateCachedNotes");
    for (Anime anime : animes) {
      mNotes.put(anime.getId(), anime);
    }
    setChanged();
  }

  private List<Anime> cachedNotesByUpdated() {
    ArrayList<Anime> animes = new ArrayList<>(mNotes.values());
    Collections.sort(animes, new NoteByUpdatedComparator());
    return animes;
  }

  public List<Anime> getCachedNotes() {
    return cachedNotesByUpdated();
  }

  public Cancellable loginAsync(
      String username, String password,
      final OnSuccessListener<String> successListener,
      final OnErrorListener errorListener) {
    final User user = new User(username, password);
    return mNoteRestClient.getToken(
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
    mNoteRestClient.setUser(currentUser);
  }

  public User getCurrentUser() {
    return mKD.getCurrentUser();
  }

  private class NoteByUpdatedComparator implements java.util.Comparator<Anime> {
    @Override
    public int compare(Anime n1, Anime n2) {
      return (int) (n1.getUpdated() - n2.getUpdated());
    }
  }
}
