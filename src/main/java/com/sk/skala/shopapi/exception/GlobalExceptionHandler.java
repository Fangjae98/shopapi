package com.sk.skala.shopapi.exception;

import com.sk.skala.shopapi.common.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.dao.PessimisticLockingFailureException;


import java.util.Arrays;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 비즈니스 로직 예외
    @ExceptionHandler(ResponseException.class)
    public ResponseEntity<Response> handleResponseException(ResponseException e) {
        log.warn("[ResponseException] {} - {}", e.getError(), e.getMessage());

        Response response = Response.builder()
                .code(e.getError().getStatus().value())
                .message(e.getMessage())
                .body(e.getError().name())
                .build();

        return ResponseEntity.status(e.getError().getStatus()).body(response);
    }

    // 2. 파라미터 검증 예외
    @ExceptionHandler(ParameterException.class)
    public ResponseEntity<Response> handleParameterException(ParameterException e) {
        log.warn("[ParameterException] {}", e.getMessage());

        Response response = Response.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(e.getMessage())
                .body(Arrays.asList(e.getParameters()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 3. 그 외 모든 예외 (최후의 방어선)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleException(Exception e) {
        log.error("[Unexpected Exception]", e);

        Response response = Response.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("서버 내부 오류가 발생했습니다.")
                .body(null)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }


    // 404는 그대로 404로 내보내기 (Exception 핸들러보다 먼저 매칭됨)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Response> handleNoResource(NoResourceFoundException e) {
        Response response = Response.builder()
                .code(404)
                .message("요청한 경로를 찾을 수 없습니다: " + e.getResourcePath())
                .body(null)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 비관적 락 대기 시간 초과 / 데드락
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<Response> handleLockFailure(PessimisticLockingFailureException e) {
        log.warn("[PessimisticLock] 행 잠금 획득 실패 - {}", e.getMessage());

        Response response = Response.builder()
                .code(Error.LOCK_TIMEOUT.getStatus().value())
                .message(Error.LOCK_TIMEOUT.getDefaultMessage())
                .body(Error.LOCK_TIMEOUT.name())
                .build();

        return ResponseEntity.status(Error.LOCK_TIMEOUT.getStatus()).body(response);
    }
    
}