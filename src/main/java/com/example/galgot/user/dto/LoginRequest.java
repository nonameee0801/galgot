package com.example.galgot.user.dto;

/**
 * 이메일 로그인 요청.
 * 값이 비어 있어도 400으로 따로 알려 주지 않고, 다른 실패와 똑같이 401로 돌려준다(UserService.authenticate).
 */
public record LoginRequest(String email, String password) {
}
