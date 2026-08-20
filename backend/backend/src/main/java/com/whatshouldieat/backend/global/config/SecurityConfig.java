package com.whatshouldieat.backend.global.config;


import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {
        return http
                // csrf를 비활성화 하는 코드
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(
                                // "/api/meal-histories/**"에 대한 요청은 인증없이 허용 한다는 코드
                                "/api/meal-histories/**",
                                // swagger 연동을 위해 허용하는 사이트에 추가
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        )
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                // 기본 로그인 화면 비활성화
                .formLogin(form -> form.disable())
                // HTTP basic 비활성화
                .httpBasic(basic -> basic.disable())
                .build();
    }

}
