package com.example.galgot.user.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 닉네임 바꾸기 규칙(기획서 18장 F). */
class UserTest {

    @Test
    void 닉네임은_앞뒤_공백을_지우고_바꾼다() {
        User user = User.create("여행자", null);

        user.changeNickname("  길동  ");

        assertEquals("길동", user.getNickname());
    }

    @Test
    void 빈_닉네임은_거부하고_그대로_둔다() {
        User user = User.create("여행자", null);

        assertThrows(IllegalArgumentException.class, () -> user.changeNickname("   "));
        assertThrows(IllegalArgumentException.class, () -> user.changeNickname(null));
        assertEquals("여행자", user.getNickname());
    }

    @Test
    void 닉네임은_50자까지_된다() {
        User user = User.create("여행자", null);

        user.changeNickname("가".repeat(User.NICKNAME_MAX));

        assertEquals(User.NICKNAME_MAX, user.getNickname().length());
    }

    @Test
    void 닉네임이_51자면_거부한다() {
        User user = User.create("여행자", null);

        assertThrows(IllegalArgumentException.class, () -> user.changeNickname("가".repeat(User.NICKNAME_MAX + 1)));
        assertEquals("여행자", user.getNickname());
    }
}
