package com.sk.skala.shopapi.controller;

import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.data.table.Product;
import com.sk.skala.shopapi.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 전체 상품 목록 조회 (페이징)
    @GetMapping("/list")
    @Operation(summary = "상품 목록 조회", description = "모든 상품의 목록을 조회합니다")
    public Response getAllProducts(@RequestParam(defaultValue = "0") Integer offset,
                                    @RequestParam(defaultValue = "10") Integer count) {
        return productService.getAllProducts(offset, count);
    }

    // 개별 상품 상세 조회
    @GetMapping("/{id}")
    @Operation(summary = "상품 조회", description = "특정 상품의 상세 정보를 조회합니다")
    public Response getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // 상품 등록
    @PostMapping
    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다")
    public Response createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    // 상품 수정
    @PutMapping
    @Operation(summary = "상품 수정", description = "기존 상품의 정보를 수정합니다")
    public Response updateProduct(@RequestBody Product product) {
        return productService.updateProduct(product);
    }

    // 상품 삭제
    @DeleteMapping
    @Operation(summary = "상품 삭제", description = "기존 상품을 삭제합니다")
    public Response deleteProduct(@RequestBody Product product) {
        return productService.deleteProduct(product);
    }
}