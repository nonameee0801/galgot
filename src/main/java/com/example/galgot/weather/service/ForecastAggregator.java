package com.example.galgot.weather.service;

import com.example.galgot.infra.kma.KmaItem;
import com.example.galgot.weather.domain.PrecipType;
import com.example.galgot.weather.domain.Sky;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 시간별 예보(KmaItem 목록)를 날짜별 요약으로 줄이는 순수 로직(DB, 네트워크 없음).
 * - 최고/최저기온: TMX/TMN이 있으면 그 값, 없으면 시간별 기온(TMP)의 최대/최소
 *   (TMX/TMN은 발표 시각에 따라 오늘 분이 빠질 수 있다)
 * - 강수확률: 하루 중 최대값
 * - 강수 형태: 하루 중 가장 나쁜 값
 * - 하늘 상태: 가장 많이 나온 값(같으면 더 흐린 쪽)
 */
public final class ForecastAggregator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ForecastAggregator() {
    }

    public static Map<LocalDate, DailySummary> aggregate(List<KmaItem> items) {
        Map<LocalDate, List<KmaItem>> byDate = new TreeMap<>();
        for (KmaItem item : items) {
            LocalDate date = parseDate(item.fcstDate());
            if (date != null) {
                byDate.computeIfAbsent(date, d -> new ArrayList<>()).add(item);
            }
        }

        Map<LocalDate, DailySummary> result = new TreeMap<>();
        byDate.forEach((date, rows) -> result.put(date, summarize(date, rows)));
        return result;
    }

    private static DailySummary summarize(LocalDate date, List<KmaItem> rows) {
        Integer tmx = null, tmn = null, tmpMax = null, tmpMin = null, pop = null;
        PrecipType worst = null;
        Map<Sky, Integer> skyCount = new EnumMap<>(Sky.class);

        for (KmaItem row : rows) {
            switch (row.category()) {
                case "TMX" -> tmx = pick(tmx, toTemp(row.fcstValue()), true);
                case "TMN" -> tmn = pick(tmn, toTemp(row.fcstValue()), false);
                case "TMP" -> {
                    Integer t = toTemp(row.fcstValue());
                    tmpMax = pick(tmpMax, t, true);
                    tmpMin = pick(tmpMin, t, false);
                }
                case "POP" -> pop = pick(pop, toInt(row.fcstValue()), true);
                case "PTY" -> {
                    PrecipType p = PrecipType.fromCode(row.fcstValue());
                    if (p != null && (worst == null || p.ordinal() > worst.ordinal())) {
                        worst = p;
                    }
                }
                case "SKY" -> {
                    Sky s = Sky.fromCode(row.fcstValue());
                    if (s != null) {
                        skyCount.merge(s, 1, Integer::sum);
                    }
                }
                default -> { }
            }
        }

        return new DailySummary(date, dominantSky(skyCount), worst, pop,
                tmx != null ? tmx : tmpMax,
                tmn != null ? tmn : tmpMin);
    }

    private static Sky dominantSky(Map<Sky, Integer> counts) {
        Sky best = null;
        int bestCount = 0;
        for (Map.Entry<Sky, Integer> e : counts.entrySet()) {
            // EnumMap은 선언 순서(맑음 → 흐림)로 돌기 때문에, >= 로 비교하면 동률일 때 더 흐린 쪽이 남는다
            if (e.getValue() >= bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return best;
    }

    private static Integer pick(Integer current, Integer candidate, boolean max) {
        if (candidate == null) return current;
        if (current == null) return candidate;
        return max ? Math.max(current, candidate) : Math.min(current, candidate);
    }

    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s, DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    /** "27.0", "-3.0" 같은 값을 반올림한 정수로. */
    private static Integer toTemp(String v) {
        try {
            return (int) Math.round(Double.parseDouble(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer toInt(String v) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
