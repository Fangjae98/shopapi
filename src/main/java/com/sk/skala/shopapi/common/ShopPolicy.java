package com.sk.skala.shopapi.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "shop")
public class ShopPolicy {
    private Double initialBalance;
    private Double signupPointRate;
    private Double earnPointRate;
}