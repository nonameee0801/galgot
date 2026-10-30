package com.example.galgot.user.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 이메일과 비밀번호 규칙. DB와 네트워크 없이 판단만 한다(단위 테스트 대상).
 * - 이메일: 앞뒤 공백을 지우고 소문자로 맞춘 뒤 형식을 확인한다.
 * - 비밀번호: 8~64자, 영문과 숫자를 각각 하나 이상 포함, 공백을 뺀 ASCII 문자만 허용.
 *   (BCrypt는 72바이트까지만 쓰므로 64자 상한이면 잘리는 일이 없다)
 */
public final class CredentialPolicy {

    public static final int EMAIL_MAX = 255;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private CredentialPolicy() {
    }

    /** null이면 null, 아니면 공백 제거 후 소문자. */
    public static String normalizeEmail(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    /** 정리된 이메일이 형식에 맞는지. */
    public static boolean isValidEmail(String normalized) {
        return normalized != null
                && !normalized.isEmpty()
                && normalized.length() <= EMAIL_MAX
                && EMAIL.matcher(normalized).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c < 0x21 || c > 0x7E) {
                return false; // 공백, 한글 등 ASCII 밖 문자
            }
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                hasLetter = true;
            } else if (c >= '0' && c <= '9') {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }
}
