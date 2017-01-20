package com.example.ilazar.mykeep.net.mapping;

import android.util.JsonReader;
import android.util.Log;

import com.example.ilazar.mykeep.content.Anime;

import java.io.IOException;

import static com.example.ilazar.mykeep.net.mapping.Api.Note.STATUS;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.TEXT;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.UPDATED;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.USER_ID;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.VERSION;
import static com.example.ilazar.mykeep.net.mapping.Api.Note._ID;

public class NoteReader implements ResourceReader<Anime, JsonReader> {
  private static final String TAG = NoteReader.class.getSimpleName();

  @Override
  public Anime read(JsonReader reader) throws IOException {
    Anime anime = new Anime();
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName();
      if (name.equals(_ID)) {
        anime.setId(reader.nextString());
      } else if (name.equals(TEXT)) {
        anime.setText(reader.nextString());
      } else if (name.equals(STATUS)) {
        anime.setStatus(Anime.Status.valueOf(reader.nextString()));
      } else if (name.equals(UPDATED)) {
        anime.setUpdated(reader.nextLong());
      } else if (name.equals(USER_ID)) {
        anime.setUserId(reader.nextString());
      } else if (name.equals(VERSION)) {
        anime.setVersion(reader.nextInt());
      } else {
        reader.skipValue();
        Log.w(TAG, String.format("Anime property '%s' ignored", name));
      }
    }
    reader.endObject();
    return anime;
  }
}
