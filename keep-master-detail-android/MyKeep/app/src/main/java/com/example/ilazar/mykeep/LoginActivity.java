package com.example.ilazar.mykeep;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import com.example.ilazar.mykeep.content.User;
import com.example.ilazar.mykeep.service.NoteManager;
import com.example.ilazar.mykeep.util.Cancellable;
import com.example.ilazar.mykeep.util.DialogUtils;
import com.example.ilazar.mykeep.util.OnErrorListener;
import com.example.ilazar.mykeep.util.OnSuccessListener;

public class LoginActivity extends AppCompatActivity {

  private Cancellable mCancellable;
  private NoteManager mNoteManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    mNoteManager = ((KeepApp) getApplication()).getNoteManager();
    User user = mNoteManager.getCurrentUser();
    if (user != null) {
      startNoteListActivity();
      finish();
    }
    setupToolbar();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mCancellable != null) {
      mCancellable.cancel();
    }
  }

  private void setupToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        login();
        Snackbar.make(view, "Authenticating, please wait", Snackbar.LENGTH_INDEFINITE)
            .setAction("Action", null).show();
      }
    });
  }

  private void login() {
    EditText usernameEditText = (EditText) findViewById(R.id.username);
    EditText passwordEditText = (EditText) findViewById(R.id.password);
    mCancellable = mNoteManager
        .loginAsync(
            usernameEditText.getText().toString(), passwordEditText.getText().toString(),
            new OnSuccessListener<String>() {
              @Override
              public void onSuccess(String s) {
                runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    startNoteListActivity();
                  }
                });
              }
            }, new OnErrorListener() {
              @Override
              public void onError(final Exception e) {
                runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    DialogUtils.showError(LoginActivity.this, e);
                  }
                });
              }
            });
  }

  private void startNoteListActivity() {
    startActivity(new Intent(this, NoteListActivity.class));
  }
}
