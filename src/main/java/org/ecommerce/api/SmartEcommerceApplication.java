package org.ecommerce.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.ecommerce.api")
public class SmartEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartEcommerceApplication.class, args);
    }
}
