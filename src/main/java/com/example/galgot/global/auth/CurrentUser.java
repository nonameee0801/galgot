package com.example.galgot.global.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

/** 로그인한 사용자의 우리 서비스 회원 ID를 꺼내는 도우미. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Object userId = principal.getAttribute("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인할 수 없습니다.");
    }
}
