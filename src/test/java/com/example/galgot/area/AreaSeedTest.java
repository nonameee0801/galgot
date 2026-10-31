package com.example.galgot.area;

import com.example.galgot.infra.kma.KmaGridConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V6 시드를 DB 없이 SQL 파일에서 직접 읽어 검사한다.
 * 한 번 적용한 마이그레이션은 고칠 수 없으므로, 오타는 적용하기 전에 여기서 잡는다.
 */
class AreaSeedTest {

    private static final String V6 = "/db/migration/V6__add_areas.sql";
    private static final Pattern AREA_ROW = Pattern.compile(
            "\\('(\\d+)', '(\\d+)', '(SIDO|SIGUNGU)', '([^']+)', (\\d+), (\\d+), ([\\d.]+), ([\\d.]+)\\)");
    private static final Pattern SIGNGU_ROW = Pattern.compile("^\\s*\\('(\\d{2})', '(\\d{3})', '(\\d{5})'\\)[,;]");

    record AreaRow(String code, String regn, String level, String name, int nx, int ny, double lat, double lng) {
    }

    record SignguRow(String regn, String signgu, String area) {
    }

    static List<AreaRow> areas = new ArrayList<>();
    static List<SignguRow> signgus = new ArrayList<>();

    @BeforeAll
    static void readSeed() throws IOException {
        String sql;
        try (InputStream in = AreaSeedTest.class.getResourceAsStream(V6)) {
            assertNotNull(in, "V6 파일을 찾지 못했습니다");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = AREA_ROW.matcher(sql);
        while (m.find()) {
            areas.add(new AreaRow(m.group(1), m.group(2), m.group(3), m.group(4),
                    Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)),
                    Double.parseDouble(m.group(7)), Double.parseDouble(m.group(8))));
        }
        for (String line : sql.split("\\R")) {
            Matcher s = SIGNGU_ROW.matcher(line);
            if (s.find()) {
                signgus.add(new SignguRow(s.group(1), s.group(2), s.group(3)));
            }
        }
    }

    private static long count(String regn, String level) {
        return areas.stream().filter(a -> a.regn().equals(regn) && a.level().equals(level)).count();
    }

    @Test
    void 지역은_시도_전체_2개와_서울_구_25개_경기_시군_31개() {
        assertEquals(58, areas.size());
        assertEquals(1, count("11", "SIDO"));
        assertEquals(1, count("41", "SIDO"));
        assertEquals(25, count("11", "SIGUNGU"));
        assertEquals(31, count("41", "SIGUNGU"));
        assertEquals(58, areas.stream().map(AreaRow::code).distinct().count(), "지역 코드가 겹칩니다");
    }

    @Test
    void 모든_지역의_격자가_대표_위경도에서_계산한_값과_같다() {
        for (AreaRow a : areas) {
            KmaGridConverter.Grid expected = KmaGridConverter.toGrid(a.lat(), a.lng());
            assertEquals(expected, new KmaGridConverter.Grid(a.nx(), a.ny()), a.name() + " 격자가 위경도와 다릅니다");
        }
    }

    @Test
    void 장소의_시군구_코드_73개가_모두_같은_시도의_시군구_지역으로_이어진다() {
        assertEquals(73, signgus.size());
        Map<String, AreaRow> byCode = new HashMap<>();
        areas.forEach(a -> byCode.put(a.code(), a));

        Set<String> keys = new HashSet<>();
        for (SignguRow s : signgus) {
            assertTrue(keys.add(s.regn() + s.signgu()), "시군구 코드가 겹칩니다: " + s);
            AreaRow area = byCode.get(s.area());
            assertNotNull(area, "없는 지역으로 이어집니다: " + s);
            assertEquals("SIGUNGU", area.level(), s.toString());
            assertEquals(s.regn(), area.regn(), s.toString());
        }
    }

    @Test
    void 시군구_지역마다_이어진_코드가_하나_이상_있다() {
        Set<String> linked = new HashSet<>();
        signgus.forEach(s -> linked.add(s.area()));
        areas.stream()
                .filter(a -> a.level().equals("SIGUNGU"))
                .forEach(a -> assertTrue(linked.contains(a.code()), a.name() + "에 이어진 장소 코드가 없습니다"));
    }

    @Test
    void 구가_있는_시는_여러_코드를_한_지역으로_묶는다() {
        assertEquals(4, signgus.stream().filter(s -> s.area().equals("41110")).count()); // 수원시
        assertEquals(5, signgus.stream().filter(s -> s.area().equals("41590")).count()); // 화성시(옛 코드 590 포함)
    }
}
