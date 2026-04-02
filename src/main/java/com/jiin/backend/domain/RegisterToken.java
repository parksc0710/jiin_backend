package com.jiin.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterToken {

    private String token;
    private String provider;
    private String providerId;
    private String email;
    private String profileImage;
    private LocalDateTime expiresAt;
}
