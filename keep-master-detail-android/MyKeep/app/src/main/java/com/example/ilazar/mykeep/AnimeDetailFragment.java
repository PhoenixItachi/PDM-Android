package com.example.ilazar.mykeep;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.ilazar.mykeep.content.Anime;
import com.example.ilazar.mykeep.util.Cancellable;
import com.example.ilazar.mykeep.util.DialogUtils;
import com.example.ilazar.mykeep.util.OnErrorListener;
import com.example.ilazar.mykeep.util.OnSuccessListener;

/**
 * A fragment representing a single Anime detail screen.
 * This fragment is either contained in a {@link AnimeListActivity}
 * in two-pane mode (on tablets) or a {@link AnimeDetailActivity}
 * on handsets.
 */
public class AnimeDetailFragment extends Fragment {
    public static final String TAG = AnimeDetailFragment.class.getSimpleName();

    /**
     * The fragment argument representing the item ID that this fragment represents.
     */
    public static final String ANIME_ID = "anime_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private Anime mAnime;

    private KeepApp mApp;

    private Cancellable mFetchNoteAsync;
    private TextView mAnimeTextView;
    private CollapsingToolbarLayout mAppBarLayout;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AnimeDetailFragment() {
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach");
        super.onAttach(context);
        mApp = (KeepApp) context.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ANIME_ID)) {
            // In a real-world scenario, use a Loader
            // to load content from a content provider.
            Activity activity = this.getActivity();
            mAppBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.anime_detail, container, false);
        mAnimeTextView = (TextView) rootView.findViewById(R.id.anime_text);
        fillAnimeDetails();
        fetchAnimeAsync();
        return rootView;
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    private void fetchAnimeAsync() {
        mFetchNoteAsync = mApp.getAnimeManager().getAnimeAsync(
                getArguments().getString(ANIME_ID),
                new OnSuccessListener<Anime>() {

                    @Override
                    public void onSuccess(final Anime anime) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAnime = anime;
                                fillAnimeDetails();
                            }
                        });
                    }
                }, new OnErrorListener() {

                    @Override
                    public void onError(final Exception e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DialogUtils.showError(getActivity(), e);
                            }
                        });
                    }
                });
    }

    private void fillAnimeDetails() {
        if (mAnime != null) {
            if (mAppBarLayout != null) {
                mAppBarLayout.setTitle(mAnime.Title);
            }
            mAnimeTextView.setText(getAnimeText(mAnime));
        }
    }

    private String getAnimeText(Anime anime){
        return "Title: " + anime.Title + "\n" +
                "NoEpisodes:" + anime.NoEpisodes + "\n" +
                "Synonyms:" + anime.Synonyms + "\n" +
                "Synopsis:" + anime.Synopsis + "\n" +
                "Status: " + anime.Status;
    }
}
