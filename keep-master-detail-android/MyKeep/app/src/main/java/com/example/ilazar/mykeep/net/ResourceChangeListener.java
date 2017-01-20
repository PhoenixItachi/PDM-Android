package com.example.ilazar.mykeep.net;

public interface ResourceChangeListener<E> {
    void onCreated(E e);
    void onUpdated(E e);
    void onDeleted(String id);
    void onError(Throwable t);
}
