package com.example.ilazar.mykeep.net.mapping;

import org.json.JSONException;

import java.io.IOException;

public interface ResourceReader<E, Reader> {
    E read(Reader reader) throws IOException, JSONException, Exception;
}
