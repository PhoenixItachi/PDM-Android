package com.example.ilazar.mykeep.net.mapping;

import java.io.IOException;

public interface ResourceWriter<E, Writer> {
    void write(E e, Writer writer) throws IOException;
}
