package com.jiin.backend.controller;

import com.jiin.backend.domain.RegisterToken;
import com.jiin.backend.domain.User;
import com.jiin.backend.dto.RegisterRequest;
import com.jiin.backend.jwt.JwtProvider;
import com.jiin.backend.mapper.RegisterTokenMapper;
import com.jiin.backend.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "인증", description = "회원가입 / 인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterTokenMapper registerTokenMapper;
    private final UserMapper userMapper;
    private final JwtProvider jwtProvider;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Value("${jwt.cookie-secure}")
    private boolean cookieSecure;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Operation(summary = "회원가입 완료", description = "임시 토큰과 닉네임으로 최종 회원가입 처리 후 세션 쿠키를 발급합니다.")
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        // 임시 토큰 유효성 검증 및 소비 (1회성)
        RegisterToken registerToken = registerTokenMapper.consumeToken(request.token());
        if (registerToken == null) {
            return ResponseEntity.status(401).build();
        }

        // 닉네임 중복 검사
        if (userMapper.countByNickname(request.nickname()) > 0) {
            return ResponseEntity.status(409).build();
        }

        // 신규 유저 INSERT
        User newUser = User.builder()
                .provider(registerToken.getProvider())
                .providerId(registerToken.getProviderId())
                .providerEmail(registerToken.getEmail())
                .nickname(request.nickname())
                .profileImage(registerToken.getProfileImage())
                .build();
        userMapper.insertUser(newUser);

        // DB에서 생성된 userId 조회 후 JWT 발급
        User savedUser = userMapper.findByProviderAndProviderId(
                registerToken.getProvider(), registerToken.getProviderId());

        String accessToken = jwtProvider.createAccessToken(savedUser.getUserId(), savedUser.getProvider());

        ResponseCookie cookie = ResponseCookie.from(cookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenExpiry))
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
