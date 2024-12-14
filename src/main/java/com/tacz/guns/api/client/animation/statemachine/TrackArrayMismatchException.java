package com.tacz.guns.api.client.animation.statemachine;

public class TrackArrayMismatchException extends RuntimeException {
    public TrackArrayMismatchException(String msg) {
        super(msg);
    }

    public TrackArrayMismatchException(){
        super();
    }
}
