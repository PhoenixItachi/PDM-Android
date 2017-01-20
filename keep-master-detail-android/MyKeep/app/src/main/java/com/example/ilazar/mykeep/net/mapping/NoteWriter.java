package com.example.ilazar.mykeep.net.mapping;

import android.util.JsonWriter;

import com.example.ilazar.mykeep.content.Anime;

import java.io.IOException;

import static com.example.ilazar.mykeep.net.mapping.Api.Note.STATUS;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.TEXT;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.UPDATED;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.USER_ID;
import static com.example.ilazar.mykeep.net.mapping.Api.Note.VERSION;
import static com.example.ilazar.mykeep.net.mapping.Api.Note._ID;

public class NoteWriter implements ResourceWriter<Anime, JsonWriter>{
  @Override
  public void write(Anime anime, JsonWriter writer) throws IOException {
    writer.beginObject();
    {
      if (anime.getId() != null) {
        writer.name(_ID).value(anime.getId());
      }
      writer.name(TEXT).value(anime.getText());
      writer.name(STATUS).value(anime.getStatus().name());
      if (anime.getUpdated() > 0) {
        writer.name(UPDATED).value(anime.getUpdated());
      }
      if (anime.getUserId() != null) {
        writer.name(USER_ID).value(anime.getUserId());
      }
      if (anime.getVersion() > 0) {
        writer.name(VERSION).value(anime.getVersion());
      }
    }
    writer.endObject();
  }
}
