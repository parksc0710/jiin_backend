package com.jiin.backend.oauth2;

import com.jiin.backend.jwt.JwtProvider;
import com.jiin.backend.mapper.RegisterTokenMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RegisterTokenMapper registerTokenMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(jwtProvider, registerTokenMapper);
        ReflectionTestUtils.setField(handler, "redirectUri", "http://localhost:8088");
        ReflectionTestUtils.setField(handler, "cookieName", "access_token");
        ReflectionTestUtils.setField(handler, "cookieSecure", false);
        ReflectionTestUtils.setField(handler, "accessTokenExpiry", 3_600_000L);
    }

    @Test
    void 로그인_성공_시_httpOnly_쿠키_응답_헤더에_포함() throws Exception {
        OAuth2User oAuth2User = mockOAuth2User(1L, "KAKAO");
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.createAccessToken(1L, "KAKAO")).willReturn("mocked.jwt.token");

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(
                org.mockito.ArgumentMatchers.eq(HttpHeaders.SET_COOKIE),
                headerCaptor.capture()
        );

        String cookieHeader = headerCaptor.getValue();
        assertThat(cookieHeader).contains("HttpOnly");
        assertThat(cookieHeader).startsWith("access_token=mocked.jwt.token");
        assertThat(cookieHeader).contains("SameSite=Lax");
    }

    @Test
    void 로그인_성공_시_프론트엔드_URL로_리다이렉트() throws Exception {
        OAuth2User oAuth2User = mockOAuth2User(1L, "KAKAO");
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.createAccessToken(1L, "KAKAO")).willReturn("mocked.jwt.token");
        // DefaultRedirectStrategy가 내부적으로 encodeRedirectURL을 호출하므로 URL을 그대로 반환하도록 stubbing
        given(response.encodeRedirectURL(anyString())).willAnswer(inv -> inv.getArgument(0));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8088/");
    }

    @Test
    void 네이버_로그인도_쿠키_정상_발급() throws Exception {
        OAuth2User oAuth2User = mockOAuth2User(2L, "NAVER");
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.createAccessToken(2L, "NAVER")).willReturn("naver.jwt.token");

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(
                org.mockito.ArgumentMatchers.eq(HttpHeaders.SET_COOKIE),
                headerCaptor.capture()
        );

        assertThat(headerCaptor.getValue()).startsWith("access_token=naver.jwt.token");
    }

    @Test
    void Secure_false_일때_쿠키에_Secure_없음() throws Exception {
        OAuth2User oAuth2User = mockOAuth2User(1L, "KAKAO");
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.createAccessToken(1L, "KAKAO")).willReturn("mocked.jwt.token");

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(
                org.mockito.ArgumentMatchers.eq(HttpHeaders.SET_COOKIE),
                headerCaptor.capture()
        );

        assertThat(headerCaptor.getValue()).doesNotContain(";Secure").doesNotContain("; Secure");
    }

    private OAuth2User mockOAuth2User(Long userId, String provider) {
        Map<String, Object> attributes = Map.of("userId", userId, "provider", provider);
        return new DefaultOAuth2User(
                Set.of(new OAuth2UserAuthority(attributes)),
                attributes,
                "userId"
        );
    }
}
