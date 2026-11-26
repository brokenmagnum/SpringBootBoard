package com.board;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


// 초기 SpringBoot 시큐리티와 H2 콘솔을 함께 사용할 때 발생하는 문제를 해결하기 위한 설정 클래스입니다.
// 지금은 SecurityConfig 클래스에서 H2 콘솔에 대한 설정을 처리하고 있으므로, 이 클래스는 주석 처리되어 있습니다.

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