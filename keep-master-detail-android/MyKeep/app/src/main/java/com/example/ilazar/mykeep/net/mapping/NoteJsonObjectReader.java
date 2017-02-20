package com.example.ilazar.mykeep.net.mapping;

import com.example.ilazar.mykeep.content.Anime;

import org.json.JSONObject;

import static com.example.ilazar.mykeep.net.mapping.Api.Note.STATUS;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.TEXT;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.UPDATED;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.USER_ID;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.VERSION;
import static com.example.ilazar.mykeep.net.mapping.Api.Note._ID;

public class NoteJsonObjectReader implements ResourceReader<Anime, JSONObject> {
  private static final String TAG = NoteJsonObjectReader.class.getSimpleName();

  @Override
  public Anime read(JSONObject obj) throws Exception {
    Anime anime = new Anime();
    anime.setId(obj.getString(_ID));
    anime.mId = obj.getString("id");
    return anime;
  }
}
