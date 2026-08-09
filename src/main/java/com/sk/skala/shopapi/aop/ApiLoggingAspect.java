package com.sk.skala.shopapi.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    @Around("execution(public * com.sk.skala.shopapi.controller..*Controller.*(..))")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("[API REQUEST ] {} | Params: {}", methodName, Arrays.toString(args));

        try {
            Object result = joinPoint.proceed();     // 실제 컨트롤러 메서드 실행

            long duration = System.currentTimeMillis() - start;
            log.info("[API RESPONSE] {} | Duration: {}ms", methodName, duration);

            return result;

        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("[API ERROR   ] {} | {} | Duration: {}ms",
                    methodName, e.getClass().getSimpleName(), duration);
            throw e;    // 예외는 그대로 다시 던져야 GlobalExceptionHandler가 잡음
        }
    }
}