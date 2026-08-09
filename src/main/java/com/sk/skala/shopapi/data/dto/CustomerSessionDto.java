package com.sk.skala.shopapi.data.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

/**
 * 고객 인증·계정 관련 요청 공통 DTO (가입, 로그인, 비밀번호 변경, 탈퇴)
 * 네 기능 모두 필요한 입력이 아이디와 비밀번호로 동일하여 하나로 관리한다
 * 잔액·포인트는 서버 정책과 주문/취소로만 결정되므로 요청으로 받지 않는다
 */

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "customerPassword") 
public class CustomerSessionDto {
    private String customerId;
    private String customerPassword;
}
