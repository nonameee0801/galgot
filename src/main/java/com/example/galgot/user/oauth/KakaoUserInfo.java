package com.example.galgot.user.oauth;

import java.util.Collections;
import java.util.Map;

/**
 * 카카오 사용자 정보 응답을 해석한다.
 * 응답 예: { "id": 123, "kakao_account": { "profile": { "nickname": "..." }, "email": "..." },
 *           "properties": { "nickname": "..." } }
 */
public class KakaoUserInfo implements OAuthUserInfo {

    private static final String DEFAULT_NICKNAME = "카카오사용자";

    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String provider() {
        return "KAKAO";
    }

    @Override
    public String providerUserId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String nickname() {
        Map<String, Object> profile = asMap(asMap(attributes.get("kakao_account")).get("profile"));
        String nickname = text(profile.get("nickname"));
        if (nickname == null) {
            nickname = text(asMap(attributes.get("properties")).get("nickname"));
        }
        return nickname != null ? nickname : DEFAULT_NICKNAME;
    }

    @Override
    public String email() {
        return text(asMap(attributes.get("kakao_account")).get("email"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Collections.emptyMap();
    }

    private static String text(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
