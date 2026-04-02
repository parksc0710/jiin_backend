package com.jiin.backend.dto;

public record NicknameAvailableResponse(boolean available) {

    public static NicknameAvailableResponse of(boolean available) {
        return new NicknameAvailableResponse(available);
    }
}
