package com.jiin.backend.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";
    private static final long EXPIRY = 3_600_000L; // 1시간

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiry", EXPIRY);
        jwtProvider.init();
    }

    @Test
    void 토큰_생성_후_유효성_검증_성공() {
        String token = jwtProvider.createAccessToken(1L, "KAKAO");

        assertThat(jwtProvider.isValidToken(token)).isTrue();
    }

    @Test
    void 토큰에서_userId_추출() {
        Long userId = 42L;
        String token = jwtProvider.createAccessToken(userId, "NAVER");

        assertThat(jwtProvider.getUserId(token)).isEqualTo(userId);
    }

    @Test
    void 토큰에서_provider_클레임_확인() {
        String token = jwtProvider.createAccessToken(1L, "KAKAO");

        String provider = (String) jwtProvider.parseClaims(token).get("provider");
        assertThat(provider).isEqualTo("KAKAO");
    }

    @Test
    void 만료된_토큰_검증_실패() throws InterruptedException {
        JwtProvider shortExpiry = new JwtProvider();
        ReflectionTestUtils.setField(shortExpiry, "secret", SECRET);
        ReflectionTestUtils.setField(shortExpiry, "accessTokenExpiry", 1L); // 1ms
        shortExpiry.init();

        String token = shortExpiry.createAccessToken(1L, "KAKAO");
        Thread.sleep(10);

        assertThat(shortExpiry.isValidToken(token)).isFalse();
    }

    @Test
    void 다른_시크릿으로_서명된_토큰_검증_실패() {
        JwtProvider other = new JwtProvider();
        ReflectionTestUtils.setField(other, "secret", "other-secret-key-must-be-at-least-32bytes!!");
        ReflectionTestUtils.setField(other, "accessTokenExpiry", EXPIRY);
        other.init();

        String token = other.createAccessToken(1L, "KAKAO");

        assertThat(jwtProvider.isValidToken(token)).isFalse();
    }

    @Test
    void 빈_문자열_토큰_검증_실패() {
        assertThat(jwtProvider.isValidToken("")).isFalse();
    }

    @Test
    void 임의_문자열_토큰_검증_실패() {
        assertThat(jwtProvider.isValidToken("not.a.valid.jwt")).isFalse();
    }
}
