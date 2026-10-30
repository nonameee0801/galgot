package com.example.galgot.user.controller;

import com.example.galgot.user.dto.LoginRequest;
import com.example.galgot.user.dto.SignupRequest;
import com.example.galgot.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 이메일 회원가입과 로그인. 로그아웃은 SecurityConfig의 POST /api/auth/logout이 맡는다.
 *
 * 로그인한 사람을 나타내는 객체는 카카오 로그인과 같은 형태(OAuth2User, 속성 userId)로 만든다.
 * 그래서 기존 컨트롤러의 @AuthenticationPrincipal OAuth2User와 CurrentUser를 그대로 쓸 수 있다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public AuthController(UserService userService, SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
    }

    /** 가입하면 바로 로그인 상태가 된다. */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserController.MeResponse signup(@Valid @RequestBody SignupRequest body,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        UserService.LoginUser user = userService.signup(body.email(), body.password(), body.nickname());
        startSession(user, request, response);
        return new UserController.MeResponse(user.id(), user.nickname());
    }

    /** 실패하면 이유와 상관없이 401. */
    @PostMapping("/login")
    public UserController.MeResponse login(@RequestBody LoginRequest body,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        UserService.LoginUser user = userService.authenticate(body.email(), body.password());
        startSession(user, request, response);
        return new UserController.MeResponse(user.id(), user.nickname());
    }

    private void startSession(UserService.LoginUser user, HttpServletRequest request, HttpServletResponse response) {
        // 세션 고정 공격 방지: 로그인 전부터 있던 세션이면 ID를 새로 바꾼다
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("userId", user.id(), "nickname", user.nickname()),
                "userId");
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
