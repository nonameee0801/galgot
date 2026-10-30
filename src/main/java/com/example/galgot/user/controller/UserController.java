package com.example.galgot.user.controller;

import com.example.galgot.global.auth.CurrentUser;
import com.example.galgot.user.domain.User;
import com.example.galgot.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public record MeResponse(Long id, String nickname) {
    }

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 내 정보. 로그인하지 않았으면 401. */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal OAuth2User principal) {
        Long userId = CurrentUser.id(principal);
        User user = userService.find(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다."));
        return new MeResponse(user.getId(), user.getNickname());
    }
}
