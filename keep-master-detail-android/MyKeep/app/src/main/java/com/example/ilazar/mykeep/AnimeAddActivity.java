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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
public class AnimeAddActivity extends AppCompatActivity {

    private KeepApp mApp;
    private EditText mTitle;
    private EditText mNoEpisodes;
    private Spinner mStatus;
    private EditText mSynopsis;
    private EditText mSynonims;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_anime);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        mTitle = (EditText) findViewById(R.id.animeTitle);
        mNoEpisodes = (EditText) findViewById(R.id.animeNoEpisodes);
        mStatus = (Spinner) findViewById(R.id.animeStatus);
        mSynonims = (EditText) findViewById(R.id.animeSynonyms);
        mSynopsis = (EditText) findViewById(R.id.animeSynopsis);
        mApp = (KeepApp) getApplication();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[] { "Finished Airing", "Not yet aired" , "Currently Airing"});
        mStatus.setAdapter(adapter);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }



        final String animeID = getIntent().getStringExtra(AnimeDetailFragment.ANIME_ID);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addBtn);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Anime anime = new Anime();
                anime.Title = mTitle.getText().toString();
                anime.Synopsis = mSynopsis.getText().toString();
                anime.Synonyms = mSynonims.getText().toString();
                anime.NoEpisodes = mNoEpisodes.getText().toString();
                anime.Status = mStatus.getSelectedItem().toString();

                 mApp.getAnimeManager().addAnimeAsync(
                        anime,
                        new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(final String status) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(status != null && status.equals("Added"))
                                        {
                                            Toast.makeText(mApp, "Anime added successfully.", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(AnimeAddActivity.this ,AnimeListActivity.class);
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
                                        DialogUtils.showError(AnimeAddActivity.this, e);
                                    }
                                });
                            }
                        });
            }
        });
    }

}
