package com.example.galgot.infra.tourapi;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 2026-11-02에 실제로 받은 TourAPI 상세 응답 12곳의 모양을 바탕으로 만든 테스트. */
class TourDetailParserTest {

    private static Map<String, Object> map(String... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void 관광지_항목을_정리한다() {
        // 가금철교 문화공원(관광지) 응답 모양
        TourDetailItem item = TourDetailParser.parse(
                map("overview", "가금철교 문화공원은 ...", "tel", "", "homepage", ""),
                map("infocenter", "의정부시청 도시정원과 031-828-2415", "restdate", "연중무휴",
                        "usetime", "상시 개방", "parking", "불가능", "chkpet", ""));

        assertEquals("가금철교 문화공원은 ...", item.overview());
        assertEquals("상시 개방", item.useTime());
        assertEquals("연중무휴", item.restDate());
        assertEquals("불가능", item.parking());
        assertEquals("의정부시청 도시정원과 031-828-2415", item.contact());
        assertNull(item.useFee());
        assertNull(item.parkingFee());
        assertEquals(List.of(), item.homepages());
    }

    @Test
    void 문화시설은_culture가_붙은_항목_이름을_쓴다() {
        // 가평문화예술회관(문화시설) 응답 모양
        TourDetailItem item = TourDetailParser.parse(
                map("homepage", "가평군시설관리공단 http://www.gpfmc.or.kr"),
                map("usefee", "공연/전시별로 상이함", "parkingfee", "무료", "infocenterculture", "031-8078-8076~7",
                        "usetimeculture", "공연/전시별로 상이함", "restdateculture", "", "parkingculture", ""));

        assertEquals("공연/전시별로 상이함", item.useTime());
        assertEquals("공연/전시별로 상이함", item.useFee());
        assertEquals("무료", item.parkingFee());
        assertEquals("031-8078-8076~7", item.contact());
        assertNull(item.restDate());
        assertNull(item.parking());
        assertEquals(List.of("http://www.gpfmc.or.kr"), item.homepages());
    }

    @Test
    void 레포츠는_leports가_붙은_항목_이름을_쓴다() {
        TourDetailItem item = TourDetailParser.parse(
                map("homepage", "http://www.knps.or.kr"),
                map("infocenterleports", "우이탐방지원센터 02-998-8365\n교현탐방지원센터 031-855-6559",
                        "restdateleports", "연중무휴", "usetimeleports", "09:00~16:00", "usefeeleports", "무료",
                        "parkingleports", "가능", "parkingfeeleports", "유료", "openperiod", "연중", "reservation", "사전 예약"));

        assertEquals("우이탐방지원센터 02-998-8365\n교현탐방지원센터 031-855-6559", item.contact());
        assertEquals("09:00~16:00", item.useTime());
        assertEquals("무료", item.useFee());
        assertEquals("가능", item.parking());
        assertEquals("유료", item.parkingFee());
        assertEquals("연중", item.openPeriod());
        assertEquals("사전 예약", item.reservation());
    }

    @Test
    void 유형별_소개가_없는_장소는_공통_정보만_남는다() {
        // [경기옛길 경흥길 제2길] 천보산길: detailIntro2에 항목이 없었다
        TourDetailItem item = TourDetailParser.parse(map("overview", "천보산길은 ...", "tel", ""), Map.of());

        assertEquals("천보산길은 ...", item.overview());
        assertNull(item.useTime());
        assertNull(item.contact());
    }

    @Test
    void 문의처가_없으면_tel을_쓴다() {
        TourDetailItem item = TourDetailParser.parse(map("tel", "02-123-4567"), map("infocenter", ""));

        assertEquals("02-123-4567", item.contact());
    }

    @Test
    void a_태그로_온_홈페이지는_href의_자세한_주소만_남긴다() {
        String raw = "<a href=\"https://ggcr.kr/?pg=10002&pgView=gyeongheung&lNumber=6\" target=\"_blank\" "
                + "title=\"새창 : 경기옛길센터로 이동\">https://ggcr.kr</a>";

        assertEquals(List.of("https://ggcr.kr/?pg=10002&pgView=gyeongheung&lNumber=6"), TourDetailParser.homepages(raw));
    }

    @Test
    void 주소_뒤에_한글이_붙어_와도_두_주소로_나눈다() {
        String raw = "공식 홈페이지 http://www.jeijcc.org공식 인스타그램 https://www.instagram.com/jcc_arts_center/";

        assertEquals(List.of("http://www.jeijcc.org", "https://www.instagram.com/jcc_arts_center/"),
                TourDetailParser.homepages(raw));
    }

    @Test
    void 이름과_주소가_같이_오면_주소만_뽑는다() {
        assertEquals(List.of("https://www.visitsuwon.or.kr"), TourDetailParser.homepages("비짓수원 https://www.visitsuwon.or.kr"));
        assertEquals(List.of("https://gil.seoul.go.kr/gil"), TourDetailParser.homepages("https://gil.seoul.go.kr/gil"));
    }

    @Test
    void 홈페이지에_주소가_없거나_같은_주소가_반복되면_정리한다() {
        assertEquals(List.of(), TourDetailParser.homepages("홈페이지 없음"));
        assertEquals(List.of(), TourDetailParser.homepages(null));
        assertEquals(List.of("https://a.kr/x"), TourDetailParser.homepages("https://a.kr/x, https://a.kr/x."));
        assertEquals(List.of("http://b.kr"), TourDetailParser.homepages("http://b.kr&amp;"));
    }

    @Test
    void br은_줄바꿈으로_태그는_지우고_기호는_되돌린다() {
        assertEquals("09:00~18:00\n(입장 마감 17:00)",
                TourDetailParser.text("09:00~18:00<br />(입장 마감 17:00)", 1000));
        assertEquals("월요일 휴관 & 1월 1일", TourDetailParser.text("<b>월요일 휴관</b> &amp; 1월 1일", 1000));
        assertEquals("가\n\n나", TourDetailParser.text("  가  \n\n\n\n  나  ", 1000));
    }

    @Test
    void 빈_값은_null() {
        assertNull(TourDetailParser.text("", 1000));
        assertNull(TourDetailParser.text("   ", 1000));
        assertNull(TourDetailParser.text("<br>", 1000));
        assertNull(TourDetailParser.text(null, 1000));
    }

    @Test
    void 긴_값은_잘라서_말줄임표를_붙인다() {
        String cut = TourDetailParser.text("가".repeat(1200), TourDetailParser.MAX_LENGTH);

        assertEquals(TourDetailParser.MAX_LENGTH, cut.length());
        assertTrue(cut.endsWith("…"));
    }

    @Test
    void 홈페이지는_이어_붙여도_컬럼_길이를_넘지_않는다() {
        String a = "https://a.kr/" + "x".repeat(600);
        String b = "https://b.kr/" + "y".repeat(600);

        assertEquals(List.of(a), TourDetailParser.homepages(a + " " + b));
    }
}
