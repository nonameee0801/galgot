package com.example.galgot.weather.service;

import com.example.galgot.infra.kma.KmaItem;
import com.example.galgot.weather.domain.PrecipType;
import com.example.galgot.weather.domain.Sky;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastAggregatorTest {

    private static KmaItem item(String date, String time, String category, String value) {
        return new KmaItem(date, time, category, value);
    }

    private static final LocalDate D1 = LocalDate.of(2026, 10, 30);
    private static final LocalDate D2 = LocalDate.of(2026, 10, 31);

    @Test
    void 날짜별로_나누어_요약한다() {
        Map<LocalDate, DailySummary> result = ForecastAggregator.aggregate(List.of(
                item("20261030", "0300", "TMP", "9"),
                item("20261031", "0300", "TMP", "11")));

        assertEquals(2, result.size());
        assertTrue(result.containsKey(D1));
        assertTrue(result.containsKey(D2));
    }

    @Test
    void 최고_최저기온은_TMX_TMN을_우선한다() {
        DailySummary s = ForecastAggregator.aggregate(List.of(
                item("20261030", "0600", "TMN", "7.0"),
                item("20261030", "1500", "TMX", "18.0"),
                item("20261030", "0900", "TMP", "5"),
                item("20261030", "1200", "TMP", "25"))).get(D1);

        assertEquals(18, s.tempMax());
        assertEquals(7, s.tempMin());
    }

    @Test
    void TMX_TMN이_없으면_시간별_기온에서_구한다() {
        DailySummary s = ForecastAggregator.aggregate(List.of(
                item("20261030", "0900", "TMP", "8"),
                item("20261030", "1200", "TMP", "16"),
                item("20261030", "1800", "TMP", "-2"))).get(D1);

        assertEquals(16, s.tempMax());
        assertEquals(-2, s.tempMin());
    }

    @Test
    void 영하_소수점_기온은_반올림한다() {
        DailySummary s = ForecastAggregator.aggregate(List.of(
                item("20261030", "0600", "TMN", "-3.4"))).get(D1);

        assertEquals(-3, s.tempMin());
    }

    @Test
    void 강수확률은_하루_중_최대값이다() {
        DailySummary s = ForecastAggregator.aggregate(List.of(
                item("20261030", "0300", "POP", "10"),
                item("20261030", "1500", "POP", "70"),
                item("20261030", "2100", "POP", "30"))).get(D1);

        assertEquals(70, s.pop());
    }

    @Test
    void 강수_형태는_하루_중_가장_나쁜_값이다() {
        DailySummary s = ForecastAggregator.aggregate(List.of(
                item("20261030", "0300", "PTY", "0"),
                item("20261030", "1200", "PTY", "4"),
                item("20261030", "1500", "PTY", "1"),
                item("20261030", "1800", "PTY", "0"))).get(D1);

        assertEquals(PrecipType.RAIN, s.precipType());
    }

    @Test
    void 하늘_상태는_가장_많은_값이고_같으면_더_흐린_쪽이다() {
        DailySummary many = ForecastAggregator.aggregate(List.of(
                item("20261030", "0300", "SKY", "1"),
                item("20261030", "0600", "SKY", "1"),
                item("20261030", "0900", "SKY", "4"))).get(D1);
        DailySummary tie = ForecastAggregator.aggregate(List.of(
                item("20261030", "0300", "SKY", "1"),
                item("20261030", "0600", "SKY", "4"))).get(D1);

        assertEquals(Sky.CLEAR, many.sky());
        assertEquals(Sky.OVERCAST, tie.sky());
    }

    @Test
    void 강수량_적설_같은_문자열_항목은_무시한다() {
        DailySummary s = ForecastAggregator.aggregate(List.of(
                item("20261030", "0300", "PCP", "강수없음"),
                item("20261030", "0300", "SNO", "적설없음"),
                item("20261030", "0300", "TMP", "9"))).get(D1);

        assertEquals(9, s.tempMax());
        assertNull(s.pop());
        assertNull(s.precipType());
    }

    @Test
    void 날짜_형식이_이상한_항목은_건너뛴다() {
        assertTrue(ForecastAggregator.aggregate(List.of(item("abc", "0300", "TMP", "9"))).isEmpty());
    }
}
