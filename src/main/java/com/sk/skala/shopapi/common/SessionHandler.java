package com.sk.skala.shopapi.common;

import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ResponseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 서버 세션(HttpSession) 기반 로그인 상태 관리.
 * 로그인한 customerId를 세션에 보관하고, 주문·취소 시 꺼내어 사용자를 식별한다.
 */
@Component
public class SessionHandler {

    private static final String SESSION_KEY = "LOGIN_CUSTOMER_ID";

    // 로그인 성공 시 세션에 고객 ID 저장
    public void storeAccessToken(String customerId) {
        getRequest().getSession(true).setAttribute(SESSION_KEY, customerId);
    }

    // 현재 요청의 세션에서 로그인한 고객 ID를 꺼낸다
    public String getCurrentCustomerId() {
        HttpSession session = getRequest().getSession(false);   // 없으면 새로 만들지 않음

        if (session == null || session.getAttribute(SESSION_KEY) == null) {
            throw new ResponseException(Error.NOT_AUTHENTICATED, "로그인이 필요합니다.");
        }
        return (String) session.getAttribute(SESSION_KEY);
    }

    // 로그아웃 - 세션 폐기
    public void clearAccessToken() {
        HttpSession session = getRequest().getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }

    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
    }
}