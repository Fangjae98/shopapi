package com.sk.skala.shopapi.exception;

import lombok.Getter;

@Getter
public class ResponseException extends RuntimeException {

    private final Error error;

    public ResponseException(Error error, String message) {
        super(message);
        this.error = error;
    }

    public ResponseException(Error error) {
        super(error.getDefaultMessage());
        this.error = error;
    }
}