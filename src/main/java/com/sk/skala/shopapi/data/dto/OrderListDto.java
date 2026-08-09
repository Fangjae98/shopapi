package com.sk.skala.shopapi.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListDto {
    private String customerId;
    private Double customerPoint;
    private List<OrderItemDto> products;
    private Double customerBalance;
}