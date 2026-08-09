package com.sk.skala.shopapi.data.table;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;

    private Double pointUsed;      // 이 주문에 사용한 포인트 (누적)
    private Double cashUsed;       // 이 주문에 지불한 현금 (누적)
    private Double pointEarned;    // 이 주문으로 적립된 포인트 (누적)

    public OrderItem(Customer customer, Product product) {
        this.customer = customer;
        this.product = product;
        this.quantity = 0;
        this.pointUsed = 0.0;
        this.cashUsed = 0.0;
        this.pointEarned = 0.0;
    }
}