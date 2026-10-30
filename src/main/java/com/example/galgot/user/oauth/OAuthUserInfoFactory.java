package com.example.galgot.user.oauth;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

/** registrationId(application.yml의 registration 이름)로 알맞은 해석기를 고른다. */
public final class OAuthUserInfoFactory {

    private OAuthUserInfoFactory() {
    }

    public static OAuthUserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoUserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider", "지원하지 않는 로그인 방식입니다: " + registrationId, null));
        };
    }
}
