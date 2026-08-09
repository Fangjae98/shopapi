package com.sk.skala.shopapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum Error {

    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 데이터를 찾을 수 없습니다."),
    DATA_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 데이터입니다."),
    INSUFFICIENT_FUNDS(HttpStatus.BAD_REQUEST, "보유 잔액이 부족합니다."),
    INSUFFICIENT_QUANTITY(HttpStatus.BAD_REQUEST, "보유 수량이 부족합니다."),
    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    PARAMETER_EXCEPTION(HttpStatus.BAD_REQUEST, "잘못된 파라미터입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "보유 포인트가 부족합니다."),
    LOCK_TIMEOUT(HttpStatus.CONFLICT, "다른 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요.");
    private final HttpStatus status;
    private final String defaultMessage;

    Error(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}