package com.jiin.backend.oauth2;

import com.jiin.backend.domain.User;
import com.jiin.backend.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserMapper userMapper;

    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOAuth2UserService(userMapper);
    }

    // ──────────────────────────────────────────────
    // parseKakao 파싱 테스트
    // ──────────────────────────────────────────────

    @Test
    void 카카오_응답_정상_파싱() {
        Map<String, Object> attributes = kakaoAttributes("12345", "지인", "jiin@kakao.com", "https://img.kakao.com/profile.jpg");

        CustomOAuth2UserService.OAuthUserInfo info = service.parseKakao(attributes);

        assertThat(info.provider()).isEqualTo("KAKAO");
        assertThat(info.providerId()).isEqualTo("12345");
        assertThat(info.nickname()).isEqualTo("지인");
        assertThat(info.email()).isEqualTo("jiin@kakao.com");
        assertThat(info.profileImage()).isEqualTo("https://img.kakao.com/profile.jpg");
    }

    @Test
    void 카카오_이메일_미제공시_null() {
        Map<String, Object> attributes = kakaoAttributes("12345", "지인", null, null);

        CustomOAuth2UserService.OAuthUserInfo info = service.parseKakao(attributes);

        assertThat(info.email()).isNull();
        assertThat(info.profileImage()).isNull();
    }

    @Test
    void 카카오_kakao_account_없어도_빈값_처리() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "99999");

        CustomOAuth2UserService.OAuthUserInfo info = service.parseKakao(attributes);

        assertThat(info.provider()).isEqualTo("KAKAO");
        assertThat(info.providerId()).isEqualTo("99999");
        assertThat(info.nickname()).isEqualTo("");
    }

    // ──────────────────────────────────────────────
    // parseNaver 파싱 테스트
    // ──────────────────────────────────────────────

    @Test
    void 네이버_응답_정상_파싱() {
        Map<String, Object> attributes = naverAttributes("naver-id-001", "지인", "jiin@naver.com", "https://img.naver.com/profile.jpg");

        CustomOAuth2UserService.OAuthUserInfo info = service.parseNaver(attributes);

        assertThat(info.provider()).isEqualTo("NAVER");
        assertThat(info.providerId()).isEqualTo("naver-id-001");
        assertThat(info.nickname()).isEqualTo("지인");
        assertThat(info.email()).isEqualTo("jiin@naver.com");
        assertThat(info.profileImage()).isEqualTo("https://img.naver.com/profile.jpg");
    }

    @Test
    void 네이버_이메일_미제공시_null() {
        Map<String, Object> attributes = naverAttributes("naver-id-001", "지인", null, null);

        CustomOAuth2UserService.OAuthUserInfo info = service.parseNaver(attributes);

        assertThat(info.email()).isNull();
        assertThat(info.profileImage()).isNull();
    }

    // ──────────────────────────────────────────────
    // loadUser 전체 흐름 테스트
    // ──────────────────────────────────────────────

    @Test
    void loadUser_카카오_DB_upsert_후_OAuth2User_반환() {
        CustomOAuth2UserService spyService = spy(service);
        OAuth2UserRequest userRequest = kakaoUserRequest();
        OAuth2User mockOAuth2User = mockOAuth2User(kakaoAttributes("12345", "지인", "jiin@kakao.com", null));
        // fetchOAuth2User를 spy로 대체해 super.loadUser() HTTP 호출 방지
        doReturn(mockOAuth2User).when(spyService).fetchOAuth2User(userRequest);

        User savedUser = User.builder().userId(1L).provider("KAKAO").providerId("12345")
                .nickname("지인").build();
        given(userMapper.findByProviderAndProviderId(eq("KAKAO"), eq("12345"))).willReturn(savedUser);

        OAuth2User result = spyService.loadUser(userRequest);

        verify(userMapper).upsertUser(any(User.class));
        verify(userMapper).findByProviderAndProviderId("KAKAO", "12345");
        assertThat((Long) result.getAttribute("userId")).isEqualTo(1L);
        assertThat((String) result.getAttribute("provider")).isEqualTo("KAKAO");
    }

    @Test
    void loadUser_지원하지_않는_플랫폼_예외_발생() {
        CustomOAuth2UserService spyService = spy(service);
        OAuth2UserRequest userRequest = googleUserRequest();
        OAuth2User mockOAuth2User = mockOAuth2User(Map.of("sub", "google-id"));
        // fetchOAuth2User를 spy로 대체해 super.loadUser() HTTP 호출 방지
        doReturn(mockOAuth2User).when(spyService).fetchOAuth2User(userRequest);

        assertThatThrownBy(() -> spyService.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("지원하지 않는 소셜 로그인");
    }

    // ──────────────────────────────────────────────
    // 헬퍼 메서드
    // ──────────────────────────────────────────────

    private Map<String, Object> kakaoAttributes(String id, String nickname, String email, String profileImage) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", nickname);
        if (profileImage != null) profile.put("profile_image_url", profileImage);

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("profile", profile);
        if (email != null) kakaoAccount.put("email", email);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", id);
        attributes.put("kakao_account", kakaoAccount);
        return attributes;
    }

    private Map<String, Object> naverAttributes(String id, String name, String email, String profileImage) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("name", name);
        if (email != null) response.put("email", email);
        if (profileImage != null) response.put("profile_image", profileImage);

        return Map.of("response", response);
    }

    private OAuth2User mockOAuth2User(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                java.util.Set.of(new org.springframework.security.oauth2.core.user.OAuth2UserAuthority(attributes)),
                attributes,
                attributes.containsKey("id") ? "id" : attributes.keySet().iterator().next()
        );
    }

    private OAuth2UserRequest kakaoUserRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("kakao")
                .clientId("kakao-client-id")
                .clientSecret("kakao-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/api/login/oauth2/code/{registrationId}")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .build();
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "test-token",
                Instant.now(), Instant.now().plusSeconds(3600));
        return new OAuth2UserRequest(registration, token);
    }

    private OAuth2UserRequest googleUserRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("google-client-id")
                .clientSecret("google-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .build();
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "test-token",
                Instant.now(), Instant.now().plusSeconds(3600));
        return new OAuth2UserRequest(registration, token);
    }
}
