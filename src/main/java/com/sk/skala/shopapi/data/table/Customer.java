package com.sk.skala.shopapi.data.table;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String customerPassword;

    @Column(nullable = false)
    private Double customerBalance;      // 보유 현금(자본)

    @Column(nullable = false)
    private Double customerPoint;        // 적립 포인트

    public Customer(String customerId, String customerPassword,
                    Double customerBalance, Double customerPoint) {
        this.customerId = customerId;
        this.customerPassword = customerPassword;
        this.customerBalance = customerBalance;
        this.customerPoint = customerPoint;
    }

    
}