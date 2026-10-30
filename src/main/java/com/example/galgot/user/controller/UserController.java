package com.example.galgot.user.controller;

import com.example.galgot.global.auth.CurrentUser;
import com.example.galgot.user.domain.User;
import com.example.galgot.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    /** 회원 탈퇴. 회원과 관련 데이터를 지운 뒤 세션을 끊고 204를 돌려준다. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal OAuth2User principal,
                         Authentication authentication,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        userService.withdraw(CurrentUser.id(principal));
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        new CookieClearingLogoutHandler("JSESSIONID").logout(request, response, authentication);
    }
}
