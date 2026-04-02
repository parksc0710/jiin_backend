package com.jiin.backend.dto;

import com.jiin.backend.domain.User;

public record UserMeResponse(
        Long userId,
        String nickname,
        String provider,
        String profileImage
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getUserId(),
                user.getNickname(),
                user.getProvider(),
                user.getProfileImage()
        );
    }
}
