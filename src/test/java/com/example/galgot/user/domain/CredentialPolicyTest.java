package com.example.galgot.user.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialPolicyTest {

    @Test
    void 이메일은_공백을_지우고_소문자로_정리한다() {
        assertEquals("trip@galgot.com", CredentialPolicy.normalizeEmail("  Trip@GalGot.COM "));
        assertNull(CredentialPolicy.normalizeEmail(null));
    }

    @Test
    void 이메일_형식을_확인한다() {
        assertTrue(CredentialPolicy.isValidEmail("trip@galgot.com"));
        assertFalse(CredentialPolicy.isValidEmail("trip@galgot"));
        assertFalse(CredentialPolicy.isValidEmail("trip galgot@a.com"));
        assertFalse(CredentialPolicy.isValidEmail(""));
        assertFalse(CredentialPolicy.isValidEmail(null));
        assertFalse(CredentialPolicy.isValidEmail("a".repeat(250) + "@b.com"));
    }

    @Test
    void 비밀번호_길이는_8자부터_64자까지() {
        assertFalse(CredentialPolicy.isValidPassword("abcd123"));          // 7자
        assertTrue(CredentialPolicy.isValidPassword("abcd1234"));          // 8자
        assertTrue(CredentialPolicy.isValidPassword("a1" + "x".repeat(62))); // 64자
        assertFalse(CredentialPolicy.isValidPassword("a1" + "x".repeat(63))); // 65자
        assertFalse(CredentialPolicy.isValidPassword(null));
    }

    @Test
    void 비밀번호는_영문과_숫자를_모두_포함해야_한다() {
        assertFalse(CredentialPolicy.isValidPassword("abcdefgh"));
        assertFalse(CredentialPolicy.isValidPassword("12345678"));
        assertTrue(CredentialPolicy.isValidPassword("Abc!1234"));
    }

    @Test
    void 비밀번호에_공백이나_ASCII_밖_문자는_쓸_수_없다() {
        assertFalse(CredentialPolicy.isValidPassword("abcd 1234"));
        assertFalse(CredentialPolicy.isValidPassword("비밀번호abc123"));
    }
}
