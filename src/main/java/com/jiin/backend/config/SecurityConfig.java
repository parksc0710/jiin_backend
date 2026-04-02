package com.jiin.backend.config;

import com.jiin.backend.jwt.JwtAuthFilter;
import com.jiin.backend.jwt.JwtProvider;
import com.jiin.backend.oauth2.CustomOAuth2UserService;
import com.jiin.backend.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/oauth2/**",
                    "/api/login/**",
                    "/api/auth/logout",
                    "/api/health",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint ->
                    endpoint.baseUri("/api/oauth2/authorization")
                )
                .redirectionEndpoint(endpoint ->
                    endpoint.baseUri("/api/login/oauth2/code/*")
                )
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
            )

            // 인증 실패 시 OAuth2 로그인 리다이렉트(302) 대신 401 반환
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
            )

            // 로그아웃: access_token 쿠키를 만료시켜 삭제
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .addLogoutHandler((request, response, auth) -> {
                    ResponseCookie expiredCookie = ResponseCookie.from(cookieName, "")
                            .httpOnly(true)
                            .path("/")
                            .maxAge(0)
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
                })
                .logoutSuccessHandler((request, response, auth) ->
                    response.setStatus(HttpServletResponse.SC_OK)
                )
            )

            .addFilterBefore(new JwtAuthFilter(jwtProvider, cookieName), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
