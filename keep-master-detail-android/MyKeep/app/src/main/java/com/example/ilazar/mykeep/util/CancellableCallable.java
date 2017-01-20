package com.example.ilazar.mykeep.util;

import java.util.concurrent.Callable;

public interface CancellableCallable<E> extends Callable<E>, Cancellable {
}
