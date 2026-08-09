package com.sk.skala.shopapi.exception;

import lombok.Getter;

@Getter
public class ParameterException extends RuntimeException {

    private final String[] parameters;

    public ParameterException(String... parameters) {
        super("잘못된 파라미터입니다: " + String.join(", ", parameters));
        this.parameters = parameters;
    }
}