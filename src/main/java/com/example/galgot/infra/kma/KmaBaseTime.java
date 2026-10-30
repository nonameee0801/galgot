package com.example.galgot.infra.kma;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 단기예보 조회 기준 시각(base_date, base_time).
 * 예보는 02, 05, 08, 11, 14, 17, 20, 23시에 발표되고, 발표 후 약 10분이 지나야 조회된다.
 */
public record KmaBaseTime(LocalDate baseDate, String baseTime) {

    private static final int[] ANNOUNCE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final int AVAILABLE_DELAY_MINUTES = 10;

    /** now 시점에 조회 가능한 가장 최근 발표 시각. */
    public static KmaBaseTime latestAvailable(LocalDateTime now) {
        LocalDateTime t = now.minusMinutes(AVAILABLE_DELAY_MINUTES);
        for (int i = ANNOUNCE_HOURS.length - 1; i >= 0; i--) {
            if (t.getHour() >= ANNOUNCE_HOURS[i]) {
                return new KmaBaseTime(t.toLocalDate(), String.format("%02d00", ANNOUNCE_HOURS[i]));
            }
        }
        // 02:10 이전에는 전날 23시 발표분이 가장 최근이다
        return new KmaBaseTime(t.toLocalDate().minusDays(1), "2300");
    }
}
