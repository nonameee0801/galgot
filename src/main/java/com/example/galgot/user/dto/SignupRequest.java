package com.example.galgot.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 이메일 회원가입 요청. 닉네임은 비우면 "여행자"로 저장된다. 세부 규칙은 CredentialPolicy에서 확인한다. */
public record SignupRequest(
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(max = 64) String password,
        @Size(max = 50) String nickname) {
}
