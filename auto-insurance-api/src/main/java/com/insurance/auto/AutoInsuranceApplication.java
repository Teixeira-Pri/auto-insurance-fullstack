package com.insurance.auto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AutoInsuranceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoInsuranceApplication.class, args);
    }
}
