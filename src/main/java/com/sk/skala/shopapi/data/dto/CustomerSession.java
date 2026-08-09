package com.sk.skala.shopapi.data.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class CustomerSession {
    private String customerId;
    private String customerPassword;
}