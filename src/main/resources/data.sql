INSERT INTO product (product_name, product_price) VALUES
('캔커피', 1500),
('컵라면', 1900),
('과자', 2900),
('탄산음료', 2900),
('아이스크림', 2500),
('빵', 3900),
('맥주', 3500),
('우유', 2200),
('에너지드링크', 2800),
('가공식품(햄)', 4200),
('초콜렛', 2200),
('샌드위치', 4500),
('즉석밥', 2500),
('봉지라면', 2000),
('즉석카레', 4000),
('Test', 10000),
('Test_delet', 10000);

-- customer_balance(자본), customer_point(자본의 10%), version(낙관적 락 초기값)
INSERT INTO customer (customer_id, customer_password, customer_balance, customer_point) VALUES
('skala01', 'pw1234', 1000000, 100000),
('skala02', 'pw1234', 500000, 50000),
('test', '1234', 10000000, 10000);