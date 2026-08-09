package com.sk.skala.shopapi.tools;

public class StringUtil {

    // 인자로 받은 문자열 중 하나라도 null이거나 빈 값이면 true
    public static boolean isAnyEmpty(String... values) {
        for (String v : values) {
            if (v == null || v.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}