package com.example.galgot.user.dto;

/** 닉네임 바꾸기 요청. 규칙(공백 정리, 1~50자)은 User.changeNickname에서 확인한다. */
public record NicknameRequest(String nickname) {
}
