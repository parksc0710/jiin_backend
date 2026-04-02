package com.jiin.backend.oauth2;

import com.jiin.backend.domain.User;
import com.jiin.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthUserInfo userInfo = extractUserInfo(registrationId, oAuth2User.getAttributes());

        // DB에 사용자 저장 또는 갱신
        User user = User.builder()
                .provider(userInfo.provider())
                .providerId(userInfo.providerId())
                .providerEmail(userInfo.email())
                .nickname(userInfo.nickname())
                .profileImage(userInfo.profileImage())
                .build();
        userMapper.upsertUser(user);

        // 이후 SuccessHandler에서 userId를 꺼낼 수 있도록 DB 조회
        User savedUser = userMapper.findByProviderAndProviderId(userInfo.provider(), userInfo.providerId());

        Map<String, Object> attributes = Map.of(
                "userId",   savedUser.getUserId(),
                "provider", savedUser.getProvider()
        );

        return new DefaultOAuth2User(
                Set.of(new OAuth2UserAuthority(attributes)),
                attributes,
                "userId"
        );
    }

    private OAuthUserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> parseKakao(attributes);
            case "naver" -> parseNaver(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + registrationId);
        };
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo parseKakao(Map<String, Object> attributes) {
        String providerId = String.valueOf(attributes.get("id"));

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

        String nickname = (String) profile.getOrDefault("nickname", "");
        String profileImage = (String) profile.getOrDefault("profile_image_url", null);
        String email = (String) kakaoAccount.getOrDefault("email", null);

        return new OAuthUserInfo("KAKAO", providerId, email, nickname, profileImage);
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo parseNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        String providerId = (String) response.get("id");
        String nickname = (String) response.getOrDefault("name", "");
        String profileImage = (String) response.getOrDefault("profile_image", null);
        String email = (String) response.getOrDefault("email", null);

        return new OAuthUserInfo("NAVER", providerId, email, nickname, profileImage);
    }

    record OAuthUserInfo(String provider, String providerId, String email,
                         String nickname, String profileImage) {}
}
