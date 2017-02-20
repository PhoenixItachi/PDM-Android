package com.example.ilazar.mykeep;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.ilazar.mykeep.content.Anime;
import com.example.ilazar.mykeep.util.Cancellable;
import com.example.ilazar.mykeep.util.DialogUtils;
import com.example.ilazar.mykeep.util.OnErrorListener;
import com.example.ilazar.mykeep.util.OnSuccessListener;

/**
 * An activity representing a single Anime detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link AnimeListActivity}.
 */
public class AnimeDetailActivity extends AppCompatActivity {

  private Cancellable mDeleteAnimeAsync;
  private KeepApp mApp;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_anime_detail);
    Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
    setSupportActionBar(toolbar);

    mApp = (KeepApp) getApplication();

    // Show the Up button in the action bar.
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
    // savedInstanceState is non-null when there is fragment state
    // saved from previous configurations of this activity
    // (e.g. when rotating the screen from portrait to landscape).
    // In this case, the fragment will automatically be re-added
    // to its container so we don't need to manually add it.
    // For more information, see the Fragments API guide at:
    //
    // http://developer.android.com/guide/components/fragments.html
    //
    if (savedInstanceState == null) {
      // Create the detail fragment and add it to the activity
      // using a fragment transaction.
      Bundle arguments = new Bundle();
      arguments.putString(AnimeDetailFragment.ANIME_ID,
          getIntent().getStringExtra(AnimeDetailFragment.ANIME_ID));
      AnimeDetailFragment fragment = new AnimeDetailFragment();
      fragment.setArguments(arguments);
      getSupportFragmentManager().beginTransaction()
          .add(R.id.anime_detail_container, fragment)
          .commit();
    }

    final String animeID = getIntent().getStringExtra(AnimeDetailFragment.ANIME_ID);
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mDeleteAnimeAsync = mApp.getAnimeManager().deleteAnimeAsync(
                animeID,
                new OnSuccessListener<String>() {
                  @Override
                  public void onSuccess(final String status) {
                    runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                        if(status != null && status.equals("OK"))
                        {
                          Toast.makeText(mApp, "Anime deleted successfully.", Toast.LENGTH_SHORT).show();
                          Intent intent = new Intent(AnimeDetailActivity.this ,AnimeListActivity.class);
                          startActivity(intent);
                        }
                      }
                    });
                  }
                }, new OnErrorListener() {

                  @Override
                  public void onError(final Exception e) {
                    runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                        DialogUtils.showError(AnimeDetailActivity.this, e);
                      }
                    });
                  }
                });
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      NavUtils.navigateUpTo(this, new Intent(this, AnimeListActivity.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
