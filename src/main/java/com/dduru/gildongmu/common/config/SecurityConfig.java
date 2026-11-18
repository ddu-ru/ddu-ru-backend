package com.dduru.gildongmu.common.config;

import com.dduru.gildongmu.common.jwt.JwtAuthenticationEntryPoint;
import com.dduru.gildongmu.common.jwt.JwtAuthenticationFilter;
import com.dduru.gildongmu.common.jwt.JwtAccessDeniedHandler;
import com.dduru.gildongmu.common.jwt.JwtTokenProvider;
import com.dduru.gildongmu.home.controller.HomeEndpoints;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        /* API 권한 설정 */
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/ws/chat").permitAll()
                        .requestMatchers(HttpMethod.GET, HomeEndpoints.HOME).permitAll()
                        .requestMatchers(HttpMethod.GET, HomeEndpoints.POPULAR_DESTINATIONS).permitAll()
                        .requestMatchers(HttpMethod.GET, HomeEndpoints.SUPER_HOSTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/destinations/popular").permitAll()
                        // 게시글 목록 / 상세 조회만 비로그인 허용
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/*").permitAll()
                        // 게시글 공유 웹 페이지 및 Android App Link 검증 파일 비로그인 허용
                        .requestMatchers(HttpMethod.GET, "/share/posts/*", "/.well-known/assetlinks.json").permitAll()
                        .requestMatchers("/api/v1/verifications/**").authenticated()
                        .requestMatchers("/api/v1/surveys/questions").permitAll()
                        .requestMatchers("/login/page", "/test/login/oauth2/code/**").permitAll()

                        /* Swagger, 정적 리소스, Actuator 권한 설정 */
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/", "/error", "/favicon.ico", "/webjars/**", "/*.png", "/static/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
