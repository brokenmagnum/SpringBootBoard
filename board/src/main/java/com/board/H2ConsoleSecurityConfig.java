package com.board;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Profile("dev")
@Configuration(proxyBeanMethods = false)
class H2ConsoleSecurityConfig {

    // @Bean
    // @Order(1)
    // SecurityFilterChain configureH2ConsoleSecurity(HttpSecurity http) throws Exception {
    //     return http
    //             .securityMatcher(PathRequest.toH2Console())
    //             .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
    //             .httpBasic(Customizer.withDefaults())
    //             .csrf(csrf -> csrf.disable())
    //             .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
    //             .build();
    // }

    // @Bean
    // SecurityFilterChain configureApplicationSecurity(HttpSecurity http) throws Exception {
    //     return http
    //             .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
    //             .formLogin(Customizer.withDefaults())
    //             .httpBasic(Customizer.withDefaults())
    //             .build();
    // }
}