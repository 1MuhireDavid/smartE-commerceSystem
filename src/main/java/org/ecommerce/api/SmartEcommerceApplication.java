package org.ecommerce.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "org.ecommerce.api")
@EnableCaching
public class SmartEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartEcommerceApplication.class, args);
    }
}
