package com.tacz.guns.util;

public interface ThrowingRunnable<T extends Throwable> {
    void run() throws T;
}
