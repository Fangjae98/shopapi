package com.sk.skala.shopapi.repository;

import com.sk.skala.shopapi.data.table.Customer;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);

    /* 비관적 락(PESSIMISTIC_WRITE)으로 고객 행을 잠근 채 조회한다. 잔액·포인트를 변경하는 주문/취소에서만 사용한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Customer> findWithLockByCustomerId(String customerId);
}