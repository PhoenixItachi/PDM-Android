package com.example.ilazar.mykeep.net.mapping;

import android.util.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResourceListReader<E> implements ResourceReader<List<E>, JsonReader> {

    private final ResourceReader<E, JsonReader> mResourceReader;

    public ResourceListReader(ResourceReader<E, JsonReader> resourceReader) {
        mResourceReader = resourceReader;
    }

    @Override
    public List<E> read(JsonReader reader) throws Exception {
        List<E> entityList = new ArrayList<E>();
        reader.beginArray();
        while (reader.hasNext()) {
            entityList.add(mResourceReader.read(reader));
        }
        reader.endArray();
        return entityList;
    }
}
