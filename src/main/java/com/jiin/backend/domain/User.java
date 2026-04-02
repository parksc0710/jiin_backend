package com.jiin.backend.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class User {

    private Long userId;
    private String provider;
    private String providerId;
    private String providerEmail;
    private String nickname;
    private String profileImage;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
