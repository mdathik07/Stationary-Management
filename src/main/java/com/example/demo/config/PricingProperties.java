package com.example.demo.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/** Per-page print rates, configurable without a code change. */
@ConfigurationProperties(prefix = "pricing")
@Getter
@Setter
public class PricingProperties {

    private BigDecimal bwPerPage = new BigDecimal("2.00");

    private BigDecimal colorPerPage = new BigDecimal("10.00");
}
