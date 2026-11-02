package com.example.galgot.recommend.service;

import java.util.List;
import java.util.Optional;

/**
 * 관광지 목록의 장소 이름 검색 규칙(기획서 18장 B). DB와 네트워크 없이 테스트할 수 있게 서비스에서 분리했다.
 * <ul>
 *   <li>검색어가 있으면 서울·경기 전체에서 이름으로만 찾는다. 고른 지역, 실내/실외, 테마는 쓰지 않는다</li>
 *   <li>결과 순서: 이름이 검색어와 같은 장소 → 검색어로 시작하는 장소 → 검색어가 들어 있는 장소, 같은 묶음 안은 가나다순.
 *       화면은 첫 번째 결과로 지도를 옮기므로, 찾던 장소가 맨 앞에 올 가능성을 높인다</li>
 *   <li>LIKE의 특수문자(%, _)와 이스케이프 문자(\)는 글자 그대로 찾도록 이스케이프한다</li>
 * </ul>
 */
public final class KeywordSearch {

    /** 검색어 최대 길이. 넘으면 앞에서 자른다. */
    public static final int MAX_LENGTH = 50;

    /** 검색 범위: 서울(11), 경기(41). */
    public static final List<String> SERVICE_REGIONS = List.of("11", "41");

    /** LIKE 패턴의 이스케이프 문자. */
    public static final char ESCAPE = '\\';

    private KeywordSearch() {
    }

    /**
     * 검색어를 정리한다. 앞뒤 공백을 지우고 연속 공백은 하나로 줄이며 50자까지 자른다.
     * 비었거나 공백뿐이면 검색이 아니다(빈 값).
     */
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.strip().replaceAll("\\s+", " ");
        if (s.isEmpty()) {
            return Optional.empty();
        }
        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH).strip();
        }
        return Optional.of(s);
    }

    /** 검색어가 들어 있는 이름: %검색어% */
    public static String containsPattern(String keyword) {
        return "%" + escape(keyword) + "%";
    }

    /** 검색어로 시작하는 이름: 검색어% */
    public static String startsWithPattern(String keyword) {
        return escape(keyword) + "%";
    }

    /** \, %, _ 앞에 이스케이프 문자를 붙인다(\ 를 먼저 처리). */
    static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (char c : s.toCharArray()) {
            if (c == ESCAPE || c == '%' || c == '_') {
                sb.append(ESCAPE);
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
