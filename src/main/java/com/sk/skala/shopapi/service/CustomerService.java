package com.sk.skala.shopapi.service;

import com.sk.skala.shopapi.common.PagedList;
import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.common.SessionHandler;
import com.sk.skala.shopapi.common.ShopPolicy;
import com.sk.skala.shopapi.data.dto.CustomerDto;
import com.sk.skala.shopapi.data.dto.CustomerSessionDto;
import com.sk.skala.shopapi.data.dto.OrderItemDto;
import com.sk.skala.shopapi.data.dto.OrderListDto;
import com.sk.skala.shopapi.data.dto.OrderRequestDto;
import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.data.table.OrderItem;
import com.sk.skala.shopapi.data.table.Product;
import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.CustomerRepository;
import com.sk.skala.shopapi.repository.OrderItemRepository;
import com.sk.skala.shopapi.repository.ProductRepository;
import com.sk.skala.shopapi.tools.StringUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;
    private final SessionHandler sessionHandler;
    private final ShopPolicy shopPolicy;


    // ==========================================================
    // 1. 전체 고객 목록 조회
    // ==========================================================
    public Response getAllCustomers(int offset, int count) {
        Pageable pageable = PageRequest.of(offset, count);
        Page<Customer> page = customerRepository.findAll(pageable);

        List<CustomerDto> items = page.getContent().stream()
                .map(CustomerDto::of)
                .toList();

        PagedList<CustomerDto> pagedList = PagedList.<CustomerDto>builder()
                .items(items)
                .offset(offset)
                .count(count)
                .totalElements(page.getTotalElements())
                .build();

        return Response.builder()
                .code(200)
                .message("고객 목록 조회 성공")
                .body(pagedList)
                .build();
    }


    // ==========================================================
    // 2. 단일 고객 조회 (+ 주문 목록)
    // ==========================================================
    @Transactional(readOnly = true)
    public Response getCustomerById(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResponseException(
                        Error.DATA_NOT_FOUND, "고객을 찾을 수 없습니다: " + customerId));

        List<OrderItem> orderItems = orderItemRepository.findByCustomerCustomerId(customerId);

        List<OrderItemDto> products = orderItems.stream()
                .map(item -> OrderItemDto.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getProductName())
                        .productPrice(item.getProduct().getProductPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        OrderListDto orderListDto = OrderListDto.builder()
                .customerId(customer.getCustomerId()).customerBalance(customer.getCustomerBalance())
                .customerPoint(customer.getCustomerPoint())
                .products(products)
                .build();

        return Response.builder()
                .code(200)
                .message("고객 조회 성공")
                .body(orderListDto)
                .build();
    }


    // ==========================================================
    // 3. 회원가입 — 초기 자본 지급 + 자본의 10%를 포인트로 지급
    // ==========================================================
    public Response createCustomer(Customer customer) {
        // 입력값 검증
        if (StringUtil.isAnyEmpty(customer.getCustomerId(), customer.getCustomerPassword())) {
            throw new ParameterException("customerId", "customerPassword");
        }

        // 아이디 중복 체크
        if (customerRepository.existsByCustomerId(customer.getCustomerId())) {
            throw new ResponseException(Error.DATA_DUPLICATED,
                    "이미 존재하는 고객 ID입니다: " + customer.getCustomerId());
        }

        // 정책에 따른 초기 자본 · 초기 포인트 계산
        double balance = shopPolicy.getInitialBalance();
        double point = Math.floor(balance * shopPolicy.getSignupPointRate());

        // 클라이언트가 보낸 값을 그대로 쓰지 않고, 필요한 필드만 뽑아 새 객체 생성
        Customer created = new Customer(
                customer.getCustomerId(),
                customer.getCustomerPassword(),
                balance,
                point
        );

        Customer saved = customerRepository.save(created);

        return Response.builder()
                .code(200)
                .message(String.format("가입 완료 — 초기 자본 %,.0f원, 포인트 %,.0fP 지급", balance, point))
                .body(CustomerDto.of(saved))
                .build();
    }


    // ==========================================================
    // 4. 로그인
    // ==========================================================
    public Response loginCustomer(CustomerSessionDto customerSession) {
        if (StringUtil.isAnyEmpty(customerSession.getCustomerId(),
                                  customerSession.getCustomerPassword())) {
            throw new ParameterException("customerId", "customerPassword");
        }

        Customer customer = customerRepository.findByCustomerId(customerSession.getCustomerId())
                .orElseThrow(() -> new ResponseException(Error.NOT_AUTHENTICATED,
                        "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!customer.getCustomerPassword().equals(customerSession.getCustomerPassword())) {
            throw new ResponseException(Error.NOT_AUTHENTICATED,
                    "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 인증 성공 → JWT 발급 후 쿠키에 저장
        sessionHandler.storeAccessToken(customer.getCustomerId());

        return Response.builder()
                .code(200)
                .message("로그인 성공")
                .body(CustomerDto.of(customer))
                .build();
    }


    // ==========================================================
    // 5. 로그아웃 — 쿠키의 토큰 폐기
    // ==========================================================
    public Response logoutCustomer() {
        sessionHandler.clearAccessToken();

        return Response.builder()
                .code(200)
                .message("로그아웃 성공")
                .body(null)
                .build();
    }


    // ==========================================================
    // 6. 고객 정보 수정 — 값이 들어온 필드만 반영
    // ==========================================================
    public Response updateCustomer(Customer customer) {
        if (StringUtil.isAnyEmpty(customer.getCustomerId())) {
            throw new ParameterException("customerId");
        }

        Customer existing = customerRepository.findByCustomerId(customer.getCustomerId())
                .orElseThrow(() -> new ResponseException(
                        Error.DATA_NOT_FOUND, "고객을 찾을 수 없습니다: " + customer.getCustomerId()));

        if (customer.getCustomerPassword() != null && !customer.getCustomerPassword().isEmpty()) {
            existing.setCustomerPassword(customer.getCustomerPassword());
        }
        if (customer.getCustomerBalance() != null) {
            if (customer.getCustomerBalance() < 0) {
                throw new ParameterException("customerBalance");
            }
            existing.setCustomerBalance(customer.getCustomerBalance());
        }
        if (customer.getCustomerPoint() != null) {
            if (customer.getCustomerPoint() < 0) {
                throw new ParameterException("customerPoint");
            }
            existing.setCustomerPoint(customer.getCustomerPoint());
        }

        Customer saved = customerRepository.save(existing);

        return Response.builder()
                .code(200)
                .message("고객 수정 성공")
                .body(CustomerDto.of(saved))
                .build();
    }


    // ==========================================================
    // 7. 고객 삭제 — 주문 내역을 먼저 지우고 고객 삭제
    // ==========================================================
    @Transactional
    public Response deleteCustomer(Customer customer) {
        Customer existing = customerRepository.findByCustomerId(customer.getCustomerId())
                .orElseThrow(() -> new ResponseException(
                        Error.DATA_NOT_FOUND, "고객을 찾을 수 없습니다: " + customer.getCustomerId()));

        List<OrderItem> orderItems =
                orderItemRepository.findByCustomerCustomerId(existing.getCustomerId());
        orderItemRepository.deleteAll(orderItems);

        customerRepository.delete(existing);

        return Response.builder()
                .code(200)
                .message("고객 삭제 성공")
                .body(null)
                .build();
    }


    // ==========================================================
    // 8. 상품 주문 — 포인트 결제 + 현금 결제 + 적립
    // ==========================================================
    @Transactional
    public Response placeOrder(OrderRequestDto order) {
        // 1) 입력값 검증
        if (order.getProductId() == null || order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new ParameterException("productId", "quantity");
        }

        // 2) 로그인한 고객 · 상품 조회
        String customerId = sessionHandler.getCurrentCustomerId();
        // 비관적 lock 걸기(동시 주문 발생 시 사고 방지)
        Customer customer = customerRepository.findWithLockByCustomerId(customerId)
                .orElseThrow(() -> new ResponseException(
                        Error.DATA_NOT_FOUND, "고객을 찾을 수 없습니다: " + customerId));

        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new ResponseException(
                        Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다: " + order.getProductId()));

        // 3) 총 주문 금액
        double totalPrice = product.getProductPrice() * order.getQuantity();

        // 4) 사용할 포인트 결정
        double pointUsed = 0.0;
        if (Boolean.TRUE.equals(order.getUsePoint())) {
            double wanted = (order.getPointToUse() != null)
                    ? order.getPointToUse()
                    : customer.getCustomerPoint();      // 미지정이면 가능한 최대

            if (wanted < 0) {
                throw new ParameterException("pointToUse");
            }
            if (wanted > customer.getCustomerPoint()) {
                throw new ResponseException(Error.INSUFFICIENT_POINT,
                        String.format("보유 포인트가 부족합니다. 보유: %,.0fP, 요청: %,.0fP",
                                customer.getCustomerPoint(), wanted));
            }
            // 결제 금액을 넘겨서 사용할 수는 없음
            pointUsed = Math.floor(Math.min(wanted, totalPrice));
        }

        // 5) 현금 결제액 및 잔액 검증
        double cashUsed = totalPrice - pointUsed;
        if (customer.getCustomerBalance() < cashUsed) {
            throw new ResponseException(Error.INSUFFICIENT_FUNDS,
                    String.format("잔액이 부족합니다. 필요: %,.0f원, 보유: %,.0f원",
                            cashUsed, customer.getCustomerBalance()));
        }

        // 6) 적립 포인트 — 현금 결제분 기준 (포인트 무한 증식 방지)
        double pointEarned = Math.floor(cashUsed * shopPolicy.getEarnPointRate());

        // 7) 잔액 · 포인트 반영
        customer.setCustomerBalance(customer.getCustomerBalance() - cashUsed);
        customer.setCustomerPoint(customer.getCustomerPoint() - pointUsed + pointEarned);

        // 8) 주문 저장 — 기존 주문이면 수량과 결제 내역을 누적
        OrderItem orderItem = orderItemRepository.findByCustomerAndProduct(customer, product)
                .orElse(new OrderItem(customer, product));

        orderItem.setQuantity(orderItem.getQuantity() + order.getQuantity());
        orderItem.setPointUsed(orderItem.getPointUsed() + pointUsed);
        orderItem.setCashUsed(orderItem.getCashUsed() + cashUsed);
        orderItem.setPointEarned(orderItem.getPointEarned() + pointEarned);

        orderItemRepository.save(orderItem);
        customerRepository.save(customer);

        return Response.builder()
                .code(200)
                .message(String.format("주문 완료 — 포인트 %,.0fP + 현금 %,.0f원 결제, %,.0fP 적립",
                        pointUsed, cashUsed, pointEarned))
                .body(CustomerDto.of(customer))
                .build();
    }


    // ==========================================================
    // 9. 주문 취소 — 비율만큼 환급하고 적립분은 회수
    // ==========================================================
    @Transactional
    public Response cancelOrder(OrderRequestDto order) {
        if (order.getProductId() == null || order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new ParameterException("productId", "quantity");
        }

        String customerId = sessionHandler.getCurrentCustomerId();
        // 비관적 락 : 잔액·포인트를 바꾸는 동안 같은 고객의 다른 주문/취소를 대기시킨다.
        Customer customer = customerRepository.findWithLockByCustomerId(customerId)
                .orElseThrow(() -> new ResponseException(
                        Error.DATA_NOT_FOUND, "고객을 찾을 수 없습니다: " + customerId));

        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new ResponseException(
                        Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다: " + order.getProductId()));

        OrderItem orderItem = orderItemRepository.findByCustomerAndProduct(customer, product)
                .orElseThrow(() -> new ResponseException(
                        Error.DATA_NOT_FOUND, "주문 내역이 없습니다."));

        if (orderItem.getQuantity() < order.getQuantity()) {
            throw new ResponseException(Error.INSUFFICIENT_QUANTITY,
                    "취소 수량이 주문 수량보다 많습니다. 주문: " + orderItem.getQuantity()
                            + ", 취소 요청: " + order.getQuantity());
        }

        // 1) 취소 비율 — 부분 취소를 지원하기 위해 결제 내역을 비율로 나눔
        double ratio = (double) order.getQuantity() / orderItem.getQuantity();

        double refundPoint   = Math.floor(orderItem.getPointUsed()   * ratio);  // 사용했던 포인트 반환
        double refundCash    = Math.floor(orderItem.getCashUsed()    * ratio);  // 지불했던 현금 반환
        double clawbackPoint = Math.floor(orderItem.getPointEarned() * ratio);  // 적립됐던 포인트 회수

        // 2) 회수할 포인트가 보유 포인트보다 많으면 부족분을 현금 환급액에서 차감
        //    (적립 포인트를 이미 써버린 뒤 취소하는 어뷰징 방지)
        double pointAfter = customer.getCustomerPoint() + refundPoint - clawbackPoint;
        double cashAdjust = 0.0;
        if (pointAfter < 0) {
            cashAdjust = -pointAfter;
            pointAfter = 0.0;
        }

        // 3) 잔액 · 포인트 반영
        customer.setCustomerPoint(pointAfter);
        customer.setCustomerBalance(customer.getCustomerBalance() + refundCash - cashAdjust);

        // 4) 주문 내역 갱신 또는 삭제
        int remaining = orderItem.getQuantity() - order.getQuantity();
        if (remaining == 0) {
            orderItemRepository.delete(orderItem);
        } else {
            orderItem.setQuantity(remaining);
            orderItem.setPointUsed(orderItem.getPointUsed() - refundPoint);
            orderItem.setCashUsed(orderItem.getCashUsed() - refundCash);
            orderItem.setPointEarned(orderItem.getPointEarned() - clawbackPoint);
            orderItemRepository.save(orderItem);
        }

        customerRepository.save(customer);

        return Response.builder()
                .code(200)
                .message(String.format("취소 완료 — 현금 %,.0f원 · 포인트 %,.0fP 환급, 적립 %,.0fP 회수",
                        refundCash - cashAdjust, refundPoint, clawbackPoint))
                .body(CustomerDto.of(customer))
                .build();
    }
}