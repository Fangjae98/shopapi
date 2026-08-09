package com.sk.skala.shopapi.data.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "customerPassword") 
public class CustomerSessionDto {
    private String customerId;
    private String customerPassword;
}