package com.sk.skala.shopapi.data.dto;

import com.sk.skala.shopapi.data.table.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDto {

    private String customerId;
    private Double customerBalance;
    private Double customerPoint;

    public static CustomerDto of(Customer customer) {
        return CustomerDto.builder()
                .customerId(customer.getCustomerId())
                .customerBalance(customer.getCustomerBalance())
                .customerPoint(customer.getCustomerPoint())
                .build();
    }
}