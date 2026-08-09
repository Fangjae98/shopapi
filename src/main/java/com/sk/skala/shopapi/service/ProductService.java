package com.sk.skala.shopapi.service;

import com.sk.skala.shopapi.common.PagedList;
import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.data.table.Product;
import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

        // 1. 전체 상품 목록 조회
        public Response getAllProducts(int offset, int count) {
                
        // 페이징 조건 생성
        Pageable pageable = PageRequest.of(offset, count);

        // 페이지 단위로 조회
        Page<Product> page = productRepository.findAll(pageable);

        // 공통 응답 형식(PagedList)으로 가공
        PagedList<Product> pagedList = PagedList.<Product>builder()
                .items(page.getContent())
                .offset(offset)
                .count(count)
                .totalElements(page.getTotalElements())
                .build();

        // Response에 담아 반환
        return Response.builder()
                .code(200)
                .message("상품 목록 조회 성공")
                .body(pagedList)
                .build();                                           
        }


        // 2. 개별 상품 상세 조회
        public Response getProductById(Long id) {
                Product product = productRepository.findById(id)
                        .orElseThrow(() -> new ResponseException(
                                Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다: " + id));

                return Response.builder()
                        .code(200)
                        .message("상품 조회 성공")
                        .body(product)
                        .build();
        }


        // 3. 상품 등록
        public Response createProduct(Product product) {
                // 입력값 검증
                if (product.getProductName() == null || product.getProductName().isEmpty()) {
                        throw new ParameterException("productName");
                }
                if (product.getProductPrice() == null || product.getProductPrice() <= 0) {
                        throw new ParameterException("productPrice");
                }

                // 상품명 중복 체크
                productRepository.findByProductName(product.getProductName())
                        .ifPresent(p -> {
                                throw new ResponseException(Error.DATA_DUPLICATED,
                                        "이미 존재하는 상품명입니다: " + product.getProductName());
                        });

                // 신규 등록이므로 id 초기화 (JPA가 자동 생성)
                product.setId(null);

                // 저장
                Product saved = productRepository.save(product);

                return Response.builder()
                        .code(200)
                        .message("상품 등록 성공")
                        .body(saved)
                        .build();
        }


        // 4. 상품 수정
        public Response updateProduct(Product product) {
                // 입력값 검증
                if (product.getProductName() == null || product.getProductName().isEmpty()) {
                        throw new ParameterException("productName");
                }
                if (product.getProductPrice() == null || product.getProductPrice() <= 0) {
                        throw new ParameterException("productPrice");
                }

                // 존재 확인
                Product existing = productRepository.findById(product.getId())
                        .orElseThrow(() -> new ResponseException(
                                Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다: " + product.getId()));

                // 값 수정
                existing.setProductName(product.getProductName());
                existing.setProductPrice(product.getProductPrice());

                // 저장
                Product saved = productRepository.save(existing);

                return Response.builder()
                        .code(200)
                        .message("상품 수정 성공")
                        .body(saved)
                        .build();
        }


        // 5. 상품 삭제
        public Response deleteProduct(Product product) {
                Product existing = productRepository.findById(product.getId())
                        .orElseThrow(() -> new ResponseException(
                                Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다: " + product.getId()));

                productRepository.delete(existing);

                return Response.builder()
                        .code(200)
                        .message("상품 삭제 성공")
                        .body(null)
                        .build();
                }
}