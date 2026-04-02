package com.jiin.backend.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String COOKIE_NAME = "access_token";
    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtProvider jwtProvider;
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiry", 3_600_000L);
        jwtProvider.init();

        jwtAuthFilter = new JwtAuthFilter(jwtProvider, COOKIE_NAME);
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_쿠키_토큰_SecurityContext_인증_설정() throws Exception {
        String token = jwtProvider.createAccessToken(1L, "KAKAO");
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie(COOKIE_NAME, token)});

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 유효하지_않은_토큰_쿠키_인증_미설정() throws Exception {
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie(COOKIE_NAME, "invalid.token.value")});

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 쿠키_없으면_인증_미설정() throws Exception {
        given(request.getCookies()).willReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 다른_이름의_쿠키만_있으면_인증_미설정() throws Exception {
        String token = jwtProvider.createAccessToken(1L, "KAKAO");
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie("other_cookie", token)});

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 빈_쿠키_배열이면_인증_미설정() throws Exception {
        given(request.getCookies()).willReturn(new Cookie[]{});

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
