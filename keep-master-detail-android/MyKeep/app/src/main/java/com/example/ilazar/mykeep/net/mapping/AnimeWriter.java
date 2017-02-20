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

public class AnimeWriter implements ResourceWriter<Anime, JsonWriter>{
  @Override
  public void write(Anime anime, JsonWriter writer) throws IOException {
    writer.beginObject();
        writer.name("Title").value(anime.Title);
        writer.name("NoEpisodes").value(anime.NoEpisodes);
        writer.name("Synonyms").value(anime.Synonyms);
        writer.name("Status").value(anime.Status);
        writer.name("Synopsis").value(anime.Synopsis);
    writer.endObject();
  }
}
