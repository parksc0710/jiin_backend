package com.jiin.backend.controller;

import com.jiin.backend.domain.User;
import com.jiin.backend.dto.NicknameAvailableResponse;
import com.jiin.backend.dto.UpdateNicknameRequest;
import com.jiin.backend.dto.UserMeResponse;
import com.jiin.backend.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


@Tag(name = "회원", description = "회원 정보 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @Operation(summary = "닉네임 중복 확인", description = "닉네임 사용 가능 여부를 반환합니다. 인증 불필요.")
    @GetMapping("/check-nickname")
    public ResponseEntity<NicknameAvailableResponse> checkNickname(
            @RequestParam(required = false) String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return ResponseEntity.badRequest().build();
        }
        boolean available = userMapper.countByNickname(nickname) == 0;
        return ResponseEntity.ok(NicknameAvailableResponse.of(available));
    }

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe() {
        Long userId = resolveUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(UserMeResponse.from(user));
    }

    @Operation(summary = "닉네임 수정", description = "로그인한 사용자의 닉네임을 수정합니다.")
    @PatchMapping("/me")
    public ResponseEntity<Void> updateMe(@Valid @RequestBody UpdateNicknameRequest request) {
        Long userId = resolveUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        userMapper.updateNickname(userId, request.nickname());
        return ResponseEntity.ok().build();
    }

    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }
}
