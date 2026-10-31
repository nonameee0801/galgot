package com.example.galgot.infra.kma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 기대값은 기상청 단기예보 격자 엑셀(2026-09-30)의 시군구 행이다. */
class KmaGridConverterTest {

    private static void assertGrid(double lat, double lng, int nx, int ny) {
        assertEquals(new KmaGridConverter.Grid(nx, ny), KmaGridConverter.toGrid(lat, lng));
    }

    @Test
    void 서울_종로구() {
        assertGrid(37.5703777778, 126.9816416667, 60, 127);
    }

    @Test
    void 경기_가평군() {
        assertGrid(37.8288305556, 127.5117777778, 69, 133);
    }

    @Test
    void 경기_최북단_연천군() {
        assertGrid(38.0933638889, 127.0770666667, 61, 138);
    }

    @Test
    void 경기_최남단_평택시() {
        assertGrid(36.9894388889, 127.1146555556, 62, 114);
    }

    @Test
    void 경기_동쪽_여주시() {
        assertGrid(37.2953583333, 127.6396222222, 71, 121);
    }

    @Test
    void 경기_화성시_만세구() {
        assertGrid(37.1377014266, 126.9256619851, 59, 117);
    }
}
