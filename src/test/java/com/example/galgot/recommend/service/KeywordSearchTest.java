package com.example.galgot.recommend.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordSearchTest {

    @Test
    void 비었거나_공백뿐이면_검색이_아니다() {
        assertEquals(Optional.empty(), KeywordSearch.normalize(null));
        assertEquals(Optional.empty(), KeywordSearch.normalize(""));
        assertEquals(Optional.empty(), KeywordSearch.normalize("   \t "));
    }

    @Test
    void 앞뒤_공백을_지우고_연속_공백은_하나로_줄인다() {
        assertEquals(Optional.of("국립 중앙 박물관"), KeywordSearch.normalize("  국립   중앙\t박물관 "));
    }

    @Test
    void 오십_자를_넘으면_앞에서_자른다() {
        String longText = "가".repeat(60);
        assertEquals(KeywordSearch.MAX_LENGTH, KeywordSearch.normalize(longText).orElseThrow().length());
    }

    @Test
    void 자른_끝이_공백이면_지운다() {
        String text = "가".repeat(49) + " 나다";
        assertEquals("가".repeat(49), KeywordSearch.normalize(text).orElseThrow());
    }

    @Test
    void 포함_패턴은_앞뒤에_퍼센트를_붙인다() {
        assertEquals("%박물관%", KeywordSearch.containsPattern("박물관"));
        assertEquals("박물관%", KeywordSearch.startsWithPattern("박물관"));
    }

    @Test
    void LIKE_특수문자는_글자_그대로_찾도록_이스케이프한다() {
        assertEquals("%100\\%%", KeywordSearch.containsPattern("100%"));
        assertEquals("%a\\_b%", KeywordSearch.containsPattern("a_b"));
        assertEquals("%c\\\\d%", KeywordSearch.containsPattern("c\\d"));
    }

    @Test
    void 이스케이프_문자를_먼저_처리해_두_번_이스케이프하지_않는다() {
        assertEquals("\\\\\\%", KeywordSearch.escape("\\%"));
    }
}
