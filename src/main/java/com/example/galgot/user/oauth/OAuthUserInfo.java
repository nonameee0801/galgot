package com.example.galgot.user.oauth;

/**
 * 소셜 로그인 제공자(카카오, 네이버 등)마다 다른 사용자 정보 형식을
 * 하나의 형태로 맞추기 위한 인터페이스. 네이버를 추가할 때는 구현체만 하나 더 만든다.
 */
public interface OAuthUserInfo {

    /** 예: KAKAO */
    String provider();

    /** 제공자 쪽 사용자 고유 ID */
    String providerUserId();

    String nickname();

    /** 없을 수 있다(null) */
    String email();
}
