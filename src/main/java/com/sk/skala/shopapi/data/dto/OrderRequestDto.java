package com.sk.skala.shopapi.data.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class OrderRequestDto {
    private Long productId;
    private Integer quantity;
    private Boolean usePoint;      // 포인트 사용 여부 (기본 false)
    private Double pointToUse;     // 사용할 포인트 (null이면 가능한 최대)
}