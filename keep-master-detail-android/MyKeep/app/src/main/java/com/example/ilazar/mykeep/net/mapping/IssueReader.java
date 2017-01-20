package com.example.ilazar.mykeep.net.mapping;

import android.util.JsonReader;

import com.example.ilazar.mykeep.net.Issue;

import java.io.IOException;

public class IssueReader implements ResourceReader<Issue, JsonReader> {
  @Override
  public Issue read(JsonReader reader) throws IOException {
    Issue issue = new Issue();
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName();
      issue.add(name, reader.nextString());
    }
    reader.endObject();
    return issue;
  }
}
