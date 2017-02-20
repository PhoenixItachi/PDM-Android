package com.example.ilazar.mykeep;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ilazar.mykeep.content.Anime;
import com.example.ilazar.mykeep.util.Cancellable;
import com.example.ilazar.mykeep.util.DialogUtils;
import com.example.ilazar.mykeep.util.OnErrorListener;
import com.example.ilazar.mykeep.util.OnSuccessListener;

import java.util.List;

/**
 * An activity representing a list of Notes. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link AnimeDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class AnimeListActivity extends AppCompatActivity {

    public static final String TAG = AnimeListActivity.class.getSimpleName();

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean mTwoPane;

    /**
     * Whether or not the the notes were loaded.
     */
    private boolean mAnimeLoaded;


    private Boolean isOnline;

    /**
     * Reference to the singleton app used to access the app state and logic.
     */
    private KeepApp mApp;

    /**
     * Reference to the last async call used for cancellation.
     */
    private Cancellable mGetAnimeAsyncCall;
    private View mContentLoadingView;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mApp = (KeepApp) getApplication();
        setContentView(R.layout.activity_note_list);
        setupToolbar();
        setupFloatingActionBar();
        setupRecyclerView();
        checkTwoPaneMode();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        try {
            startGetAnimeAsyncCall();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        mApp.getAnimeManager().subscribeChangeListener();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        //ensureGetNotesAsyncTaskCancelled();
        ensureGetAnimeAsyncCallCancelled();
//        mApp.getAnimeManager().unsubscribeChangeListener();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
    }

    private void setupFloatingActionBar() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.floatingAddButton);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(AnimeListActivity.this, AnimeAddActivity.class);
                startActivity(i);
            }
        });


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
//                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"uchiha09deva@yahoo.com"});
//                i.putExtra(Intent.EXTRA_SUBJECT, "Anime local database updated.");
//                i.putExtra(Intent.EXTRA_TEXT   , "New anime have been added to the cache. Go and check them.");
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(AnimeListActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupRecyclerView() {
        mContentLoadingView = findViewById(R.id.content_loading);
        mRecyclerView = (RecyclerView) findViewById(R.id.note_list);
    }

    private void checkTwoPaneMode() {
        if (findViewById(R.id.anime_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    private void startGetAnimeAsyncCall() throws InterruptedException {
        if (mAnimeLoaded) {
            Log.d(TAG, "start getNotesAsyncCall - content already loaded, return");
            return;
        }

        showLoadingIndicator();
        mGetAnimeAsyncCall = mApp.getAnimeManager().getAnimeAsync(
                new OnSuccessListener<List<Anime>>() {
                    @Override
                    public void onSuccess(final List<Anime> animes) {
                        Log.d(TAG, "getAnimesAsyncCall - success");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showContent(animes);
                            }
                        });
                    }
                }, new OnErrorListener() {
                    @Override
                    public void onError(final Exception e) {
                        Log.d(TAG, "getAnimesAsyncCall - error");
                        ensureGetAnimeAsyncCallCancelled();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showError(e);
                                if(e.getMessage() == "401"){
                                    Intent intent = new Intent(AnimeListActivity.this, LoginActivity.class);
                                    intent.putExtra("status", "401 - Unauthorized");
                                    mApp.getAnimeManager().setCurrentUser(null);
                                    startActivity(intent);
                                }else{
                                    if(mApp.getAnimeManager().getCachedAnime() != null && mApp.getAnimeManager().getCachedAnime().size() > 0)
                                        showContent(mApp.getAnimeManager().getCachedAnime());
                                    else{
                                        Intent intent = new Intent(AnimeListActivity.this, LoginActivity.class);
                                        intent.putExtra("status", "401 - Unauthorized");
                                        intent.putExtra("error", "No connection to the server and no data chached.");
                                        mApp.getAnimeManager().setCurrentUser(null);
                                        startActivity(intent);
                                    }
                                }
                            }
                        });
                    }
                }
        );
    }


    private void ensureGetAnimeAsyncCallCancelled() {
        if (mGetAnimeAsyncCall != null) {
            Log.d(TAG, "ensureGetAnimeAsyncCallCancelled - cancelling the task");
            mGetAnimeAsyncCall.cancel();
        }
    }

    private void showError(Exception e) {
        Log.e(TAG, "showError", e);
        if (mContentLoadingView.getVisibility() == View.VISIBLE) {
            mContentLoadingView.setVisibility(View.GONE);
        }
        DialogUtils.showError(this, e);
    }

    private void showLoadingIndicator() {
        Log.d(TAG, "showLoadingIndicator");
        mRecyclerView.setVisibility(View.GONE);
        mContentLoadingView.setVisibility(View.VISIBLE);
    }

    private void showContent(final List<Anime> animes) {
        Log.d(TAG, "showContent");
        mRecyclerView.setAdapter(new NoteRecyclerViewAdapter(animes));
        mContentLoadingView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    public class NoteRecyclerViewAdapter
            extends RecyclerView.Adapter<NoteRecyclerViewAdapter.ViewHolder> {

        private final List<Anime> mValues;

        public NoteRecyclerViewAdapter(List<Anime> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.anime_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mIdView.setText(mValues.get(position).getId());
            holder.mStatusView.setText(mValues.get(position).Status);
            holder.mTitleView.setText(mValues.get(position).Title);
            holder.mView.setBackgroundColor(Color.parseColor("#8800FC26"));

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(AnimeDetailFragment.ANIME_ID, holder.mItem.getId());
                        AnimeDetailFragment fragment = new AnimeDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.anime_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, AnimeDetailActivity.class);
                        intent.putExtra(AnimeDetailFragment.ANIME_ID, holder.mItem.getId());
                        context.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mTitleView;
            public final TextView mStatusView;
            public Anime mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mTitleView = (TextView) view.findViewById(R.id.title);
                mStatusView = (TextView) view.findViewById(R.id.status);
                Animation slideLeft = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.slideleft);
                mView.startAnimation(slideLeft);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mTitleView.getText();
            }
        }
    }
}
