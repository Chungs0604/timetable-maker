package com.timetable.config;

import com.timetable.common.jwt.JwtAuthFilter;
import com.timetable.common.jwt.JwtService;
import com.timetable.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;           // ⬅ JwtService 주입
    private final UserRepository userRepository;   // ⬅ 토큰의 사용자 존재 확인용(선택이지만 권장)

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable()) // ⬅ 기본 인증 비활성화 (JWT만 사용)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/error").permitAll() // 로그인/회원가입 등 오픈
                        .anyRequest().authenticated()                      // 나머지는 보호
                )
                // ⬇ JwtAuthFilter를 UsernamePasswordAuthenticationFilter 앞에 삽입
                .addFilterBefore(new JwtAuthFilter(jwtService, userRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
