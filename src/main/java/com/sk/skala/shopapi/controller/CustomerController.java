package com.sk.skala.shopapi.controller;

import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.data.dto.CustomerSessionDto;
import com.sk.skala.shopapi.data.dto.OrderRequestDto;
import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/list")
    @Operation(summary = "사용자 목록 조회", description = "모든 사용자의 목록을 조회합니다")
    public Response getAllCustomers(@RequestParam(defaultValue = "0") int offset,
                                     @RequestParam(defaultValue = "10") int count) {
        return customerService.getAllCustomers(offset, count);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "사용자 조회", description = "특정 사용자를 조회합니다")
    public Response getCustomerById(@PathVariable String customerId) {
        return customerService.getCustomerById(customerId);
    }

    @PostMapping
    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다")
    public Response createCustomer(@RequestBody CustomerSessionDto request) {
        return customerService.createCustomer(request);
    }

    @PostMapping("/login")
    @Operation(summary = "사용자 로그인", description = "사용자 로그인을 수행합니다")
    public Response loginCustomer(@RequestBody CustomerSessionDto customerSession) {
        return customerService.loginCustomer(customerSession);
    }

    @PutMapping
    @Operation(summary = "사용자 정보 수정", description = "기존 사용자의 정보를 수정합니다")
    public Response updateCustomer(@RequestBody Customer customer) {
        return customerService.updateCustomer(customer);
    }

    @DeleteMapping
    @Operation(summary = "사용자 삭제", description = "기존 사용자를 삭제합니다")
    public Response deleteCustomer(@RequestBody Customer customer) {
        return customerService.deleteCustomer(customer);
    }

    @PostMapping("/order")
    @Operation(summary = "주문하기", description = "사용자가 상품을 주문합니다")
    public Response placeOrder(@RequestBody OrderRequestDto order) {
        return customerService.placeOrder(order);
    }

    @PostMapping("/cancel")
    @Operation(summary = "주문 취소", description = "사용자가 주문을 취소합니다")
    public Response cancelOrder(@RequestBody OrderRequestDto order) {
        return customerService.cancelOrder(order);
    }

    @PostMapping("/logout")
    @Operation(summary = "사용자 로그아웃", description = "서버 세션을 폐기합니다")
    public Response logoutCustomer() {
        return customerService.logoutCustomer();
    }

}