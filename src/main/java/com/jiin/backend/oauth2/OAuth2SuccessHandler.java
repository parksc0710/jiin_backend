package com.jiin.backend.oauth2;

import com.jiin.backend.domain.RegisterToken;
import com.jiin.backend.jwt.JwtProvider;
import com.jiin.backend.mapper.RegisterTokenMapper;
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
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final int REGISTER_TOKEN_EXPIRY_MINUTES = 3;

    private final JwtProvider jwtProvider;
    private final RegisterTokenMapper registerTokenMapper;

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
        boolean isNewUser = Boolean.TRUE.equals(oAuth2User.getAttribute("isNewUser"));

        if (!isNewUser) {
            handleExistingUser(request, response, oAuth2User);
        } else {
            handleNewUser(request, response, oAuth2User);
        }
    }

    private void handleExistingUser(HttpServletRequest request,
                                    HttpServletResponse response,
                                    OAuth2User oAuth2User) throws IOException {
        Long userId   = (Long) oAuth2User.getAttribute("userId");
        String provider = (String) oAuth2User.getAttribute("provider");

        String accessToken = jwtProvider.createAccessToken(userId, provider);

        ResponseCookie cookie = ResponseCookie.from(cookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenExpiry))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.debug("기존 유저 로그인 성공 - userId={}, provider={}", userId, provider);
        getRedirectStrategy().sendRedirect(request, response, redirectUri + "/");
    }

    private void handleNewUser(HttpServletRequest request,
                               HttpServletResponse response,
                               OAuth2User oAuth2User) throws IOException {
        String provider     = (String) oAuth2User.getAttribute("provider");
        String providerId   = (String) oAuth2User.getAttribute("providerId");
        String email        = (String) oAuth2User.getAttribute("email");
        String profileImage = (String) oAuth2User.getAttribute("profileImage");

        String token = UUID.randomUUID().toString();

        registerTokenMapper.insertToken(RegisterToken.builder()
                .token(token)
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .profileImage(profileImage)
                .expiresAt(LocalDateTime.now().plusMinutes(REGISTER_TOKEN_EXPIRY_MINUTES))
                .build());

        log.debug("신규 유저 임시 토큰 발급 - provider={}, providerId={}", provider, providerId);
        getRedirectStrategy().sendRedirect(request, response, redirectUri + "/register?token=" + token);
    }
}
