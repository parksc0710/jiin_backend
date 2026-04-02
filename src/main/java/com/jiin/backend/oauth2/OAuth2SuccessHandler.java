package com.jiin.backend.oauth2;

import com.jiin.backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Value("${oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Value("${jwt.cookie-secure}")
    private boolean cookieSecure;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Long userId = (Long) oAuth2User.getAttribute("userId");
        String provider = (String) oAuth2User.getAttribute("provider");

        String accessToken = jwtProvider.createAccessToken(userId, provider);

        // httpOnly 쿠키로 JWT 발급 (JS에서 접근 불가 → XSS 방어)
        ResponseCookie cookie = ResponseCookie.from(cookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenExpiry))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.debug("OAuth2 로그인 성공 - userId={}, provider={}", userId, provider);

        getRedirectStrategy().sendRedirect(request, response, redirectUri + "/");
    }
}
