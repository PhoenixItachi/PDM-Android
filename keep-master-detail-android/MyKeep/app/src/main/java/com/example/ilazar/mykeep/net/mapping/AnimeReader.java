package com.example.ilazar.mykeep.net.mapping;

import android.util.JsonReader;
import android.util.Log;

import com.example.ilazar.mykeep.content.Anime;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static com.example.ilazar.mykeep.net.mapping.Api.Note.STATUS;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.TEXT;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.UPDATED;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.USER_ID;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.VERSION;
import static com.example.ilazar.mykeep.net.mapping.Api.Note._ID;

public class AnimeReader implements ResourceReader<Anime, JSONObject> {
  private static final String TAG = AnimeReader.class.getSimpleName();

  @Override
  public Anime read(JSONObject json) throws IOException, JSONException {
    Anime anime = new Anime();
    anime.mId = json.getString("id");
    anime.Title = json.getString("title");
    anime.NoEpisodes = json.getString("noEpisodes");
    anime.Synopsis = json.getString("synopsis");
    anime.Synonyms= json.getString("synonyms");
    anime.Status = json.getString("status");
    anime.mUpdated = json.getString("lastTimeModified");
    return anime;
  }
}
