package com.foodfast.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodfast.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "code", "AUTHENTICATION_REQUIRED",
                                    "message", "Vui lòng đăng nhập hoặc đăng nhập lại"
                            ));
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "code", "ACCESS_DENIED",
                                    "message", "Bạn không có quyền thực hiện thao tác này"
                            ));
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/login", "/api/auth/register",
                                "/api/health", "/api/_host_info", "/_host_info",
                                "/api/payment/vnpay/status",
                                "/api/payment/vnpay/return", "/api/payment/vnpay/browser-return",
                                "/api/payment/vnpay/ipn",
                                "/h2-console/**",
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/restaurants", "/api/restaurants/*",
                                "/api/restaurants/*/reviews").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/users").hasRole("ADMIN")
                        .requestMatchers("/api/customer-notifications/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/analytics/**", "/api/notifications/**")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers("/api/restaurants/owner/**", "/api/restaurants/*/operations")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/orders",
                                "/api/orders/quote", "/orders/quote")
                                .hasRole("CUSTOMER")
                        .requestMatchers("/api/cart/**", "/api/payment/vnpay/create")
                                .hasRole("CUSTOMER")
                        .requestMatchers("/api/favorites/**", "/api/addresses/**")
                                .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/complaints", "/api/complaints/evidence")
                                .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/complaints/*/refund/complete")
                                .hasRole("ADMIN")
                        .requestMatchers("/api/complaints/**")
                                .hasAnyRole("CUSTOMER", "RESTAURANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/review").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/substitutions/*/respond")
                                .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/substitutions")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/*/substitutions")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/deliveries/*/customer-confirm")
                                .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/deliveries/order/*")
                                .authenticated()
                        .requestMatchers("/api/deliveries/**", "/api/couriers/**",
                                "/api/restaurants/*/couriers", "/api/restaurants/*/deliveries")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/**")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/*/option-groups")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers("/api/support/tickets/mine", "/api/support/tickets/contact")
                                .hasRole("CUSTOMER")
                        .requestMatchers("/api/support/tickets/*/messages")
                                .authenticated()
                        .requestMatchers("/api/support/tickets/**")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/vouchers/validate")
                                .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/vouchers/available")
                                .hasRole("CUSTOMER")
                        .requestMatchers("/api/vouchers/manage", "/api/vouchers/manage/**")
                                .hasAnyRole("RESTAURANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/support/chat")
                                .hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
