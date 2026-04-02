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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;

    // 테스트에서 super.loadUser() 호출을 spy로 대체할 수 있도록 protected로 분리
    protected OAuth2User fetchOAuth2User(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = fetchOAuth2User(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthUserInfo userInfo = extractUserInfo(registrationId, oAuth2User.getAttributes());

        // 기존 유저 여부 조회 (upsert 없이 조회만)
        User existingUser = userMapper.findByProviderAndProviderId(userInfo.provider(), userInfo.providerId());

        Map<String, Object> attributes = new HashMap<>();

        if (existingUser != null) {
            // 기존 유저: userId, provider 전달
            attributes.put("isNewUser", false);
            attributes.put("userId",    existingUser.getUserId());
            attributes.put("provider",  existingUser.getProvider());
            log.debug("기존 유저 로그인 - userId={}, provider={}", existingUser.getUserId(), userInfo.provider());
        } else {
            // 신규 유저: 소셜 정보만 전달 (DB 저장은 /register 에서 처리)
            attributes.put("isNewUser",     true);
            attributes.put("provider",      userInfo.provider());
            attributes.put("providerId",    userInfo.providerId());
            attributes.put("email",         userInfo.email());
            attributes.put("profileImage",  userInfo.profileImage());
            log.debug("신규 유저 감지 - provider={}, providerId={}", userInfo.provider(), userInfo.providerId());
        }

        return new DefaultOAuth2User(
                Set.of(new OAuth2UserAuthority(attributes)),
                attributes,
                "provider"
        );
    }

    private OAuthUserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> parseKakao(attributes);
            case "naver" -> parseNaver(attributes);
            default -> throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                            "unsupported_provider",
                            "지원하지 않는 소셜 로그인: " + registrationId,
                            null));
        };
    }

    @SuppressWarnings("unchecked")
    OAuthUserInfo parseKakao(Map<String, Object> attributes) {
        String providerId = String.valueOf(attributes.get("id"));

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

        String nickname     = (String) profile.getOrDefault("nickname", "");
        String profileImage = (String) profile.getOrDefault("profile_image_url", null);
        String email        = (String) kakaoAccount.getOrDefault("email", null);

        return new OAuthUserInfo("KAKAO", providerId, email, nickname, profileImage);
    }

    @SuppressWarnings("unchecked")
    OAuthUserInfo parseNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        String providerId   = (String) response.get("id");
        String nickname     = (String) response.getOrDefault("name", "");
        String profileImage = (String) response.getOrDefault("profile_image", null);
        String email        = (String) response.getOrDefault("email", null);

        return new OAuthUserInfo("NAVER", providerId, email, nickname, profileImage);
    }

    record OAuthUserInfo(String provider, String providerId, String email,
                         String nickname, String profileImage) {}
}
