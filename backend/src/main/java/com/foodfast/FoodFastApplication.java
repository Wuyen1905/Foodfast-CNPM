package com.foodfast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Main Spring Boot Application
 * FoodFast Backend API Server
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FoodFastApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodFastApplication.class, args);
    }
}

