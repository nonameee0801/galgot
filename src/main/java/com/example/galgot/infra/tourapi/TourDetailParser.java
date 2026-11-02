package com.example.galgot.infra.tourapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TourAPI 상세 응답을 정리하는 순수 로직(기획서 17장). DB와 네트워크 없이 테스트할 수 있게 분리했다.
 * <ul>
 *   <li>같은 내용의 응답 항목 이름이 유형마다 다르다(관광지 usetime, 문화시설 usetimeculture, 레포츠 usetimeleports).
 *       있는 이름을 찾아 하나로 모은다</li>
 *   <li>빈 문자열은 null. br은 줄바꿈, 나머지 HTML 태그는 지우고 &amp;amp; 같은 기호는 원래 글자로 되돌린다</li>
 *   <li>문의처는 infocenter 계열을 쓰고, 없으면 tel(2026-11-02 확인: 12곳 모두 tel이 비어 있었다)</li>
 *   <li>홈페이지는 글에서 http, https 주소만 뽑는다. 주소 바로 뒤에 한글이 붙어 와도 나뉘게 주소에 쓰는 글자만 받는다.
 *       a 태그의 href와 화면 글자가 겹치면(https://a.kr/길게 와 https://a.kr) 더 자세한 주소만 남긴다</li>
 *   <li>긴 값은 저장할 컬럼 길이에 맞춰 자르고 "…"을 붙인다</li>
 * </ul>
 */
public final class TourDetailParser {

    /** VARCHAR(1000) 컬럼에 넣는 값의 최대 길이 */
    public static final int MAX_LENGTH = 1000;
    /** 소개글(TEXT)의 최대 길이. utf8mb4 TEXT(65,535바이트) 안에 넉넉히 들어가는 값 */
    public static final int MAX_OVERVIEW_LENGTH = 10_000;

    private static final Pattern BR = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern URL = Pattern.compile("https?://[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
    /** 주소 끝에 붙어 온 문장 부호(쉼표, 마침표, 닫는 괄호, 끝에 남은 &amp; 등)는 주소에서 뺀다. */
    private static final String TRAILING = ".,;:!?)'&";

    private TourDetailParser() {
    }

    /**
     * @param common detailCommon2의 item 하나(없으면 빈 Map)
     * @param intro  detailIntro2의 item 하나(유형별 소개가 없는 장소도 있어 빈 Map일 수 있다)
     */
    public static TourDetailItem parse(Map<String, Object> common, Map<String, Object> intro) {
        String contact = text(first(intro, "infocenter", "infocenterculture", "infocenterleports"), MAX_LENGTH);
        if (contact == null) {
            contact = text(common.get("tel"), MAX_LENGTH);
        }
        return new TourDetailItem(
                text(common.get("overview"), MAX_OVERVIEW_LENGTH),
                text(first(intro, "usetime", "usetimeculture", "usetimeleports"), MAX_LENGTH),
                text(first(intro, "restdate", "restdateculture", "restdateleports"), MAX_LENGTH),
                text(first(intro, "usefee", "usefeeleports"), MAX_LENGTH),
                text(first(intro, "parking", "parkingculture", "parkingleports"), MAX_LENGTH),
                text(first(intro, "parkingfee", "parkingfeeleports"), MAX_LENGTH),
                contact,
                homepages(common.get("homepage")),
                text(intro.get("openperiod"), MAX_LENGTH),
                text(intro.get("reservation"), MAX_LENGTH));
    }

    /** 이름 후보 중 값이 있는 첫 항목. */
    private static Object first(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value;
            }
        }
        return null;
    }

    /** HTML을 글자로 정리하고, 빈 값은 null, 길면 자른다. */
    static String text(Object raw, int max) {
        if (raw == null) {
            return null;
        }
        String s = BR.matcher(raw.toString()).replaceAll("\n");
        s = TAG.matcher(s).replaceAll("");
        s = decodeEntities(s).replace("\r\n", "\n").replace('\r', '\n');

        // 줄마다 앞뒤 공백을 지우고, 빈 줄은 하나까지만 남긴다
        StringBuilder sb = new StringBuilder();
        int blank = 0;
        for (String line : s.split("\n", -1)) {
            String t = line.strip();
            if (t.isEmpty()) {
                blank++;
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(blank > 0 ? "\n\n" : "\n");
            }
            sb.append(t);
            blank = 0;
        }
        String result = sb.toString();
        if (result.isEmpty()) {
            return null;
        }
        return result.length() > max ? result.substring(0, max - 1) + "…" : result;
    }

    /** 홈페이지 글에서 http, https 주소만 뽑는다. 이어 붙여 저장할 때 MAX_LENGTH를 넘지 않는 만큼만. */
    static List<String> homepages(Object raw) {
        List<String> found = new ArrayList<>();
        if (raw == null) {
            return found;
        }
        Matcher m = URL.matcher(decodeEntities(raw.toString()));
        while (m.find()) {
            String url = stripTrailing(m.group());
            if (url.length() > "https://".length() && !found.contains(url)) {
                found.add(url);
            }
        }
        // 더 자세한 주소가 있으면 그 앞부분만인 주소는 뺀다(a 태그 href와 화면 글자가 겹치는 경우)
        List<String> result = new ArrayList<>();
        int length = 0;
        for (String url : found) {
            boolean shorterCopy = found.stream().anyMatch(o -> !o.equals(url) && o.startsWith(url));
            if (shorterCopy) {
                continue;
            }
            int add = url.length() + (result.isEmpty() ? 0 : 1);
            if (length + add > MAX_LENGTH) {
                break;
            }
            result.add(url);
            length += add;
        }
        return List.copyOf(result);
    }

    private static String stripTrailing(String url) {
        int end = url.length();
        while (end > 0 && TRAILING.indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }

    private static String decodeEntities(String s) {
        return s.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }
}
