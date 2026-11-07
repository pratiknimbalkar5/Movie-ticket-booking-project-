package com.moviebooking.exception;

public class TmdbAuthException extends RuntimeException {
    public TmdbAuthException(String message) {
        super(message);
    }
}
