package com.example.galgot.infra.kma;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KmaBaseTimeTest {

    private static KmaBaseTime at(int month, int day, int hour, int minute) {
        return KmaBaseTime.latestAvailable(LocalDateTime.of(2026, month, day, hour, minute));
    }

    @Test
    void 발표_직후_10분_전에는_이전_발표분을_쓴다() {
        assertEquals(new KmaBaseTime(LocalDate.of(2026, 10, 30), "0500"), at(10, 30, 8, 5));
    }

    @Test
    void 발표_10분_뒤부터_새_발표분을_쓴다() {
        assertEquals(new KmaBaseTime(LocalDate.of(2026, 10, 30), "0800"), at(10, 30, 8, 10));
    }

    @Test
    void 새벽_두시_십분_전에는_전날_23시_발표분이다() {
        assertEquals(new KmaBaseTime(LocalDate.of(2026, 10, 29), "2300"), at(10, 30, 1, 30));
        assertEquals(new KmaBaseTime(LocalDate.of(2026, 10, 29), "2300"), at(10, 30, 2, 5));
    }

    @Test
    void 자정_직후에도_전날_23시_발표분이다() {
        assertEquals(new KmaBaseTime(LocalDate.of(2026, 10, 29), "2300"), at(10, 30, 0, 5));
    }

    @Test
    void 월초_새벽에는_전달_말일로_넘어간다() {
        assertEquals(new KmaBaseTime(LocalDate.of(2026, 10, 31), "2300"), at(11, 1, 1, 0));
    }

    @Test
    void 밤_11시_십분_뒤에는_당일_23시_발표분이다() {
        assertEquals(new KmaBaseTime(LocalDate.of(2026, 10, 30), "2300"), at(10, 30, 23, 50));
    }
}
