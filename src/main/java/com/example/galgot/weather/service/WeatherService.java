package com.example.galgot.weather.service;

import com.example.galgot.area.domain.Area;
import com.example.galgot.infra.kma.KmaBaseTime;
import com.example.galgot.infra.kma.KmaClient;
import com.example.galgot.infra.kma.KmaException;
import com.example.galgot.weather.domain.WeatherForecast;
import com.example.galgot.weather.repository.WeatherForecastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 날씨 조회. 지역(areas)마다 DB에 저장해 둔 예보를 먼저 쓰고, 오래됐을 때만(기본 3시간) 기상청을 다시 호출한다.
 * 요청이 몰려도 외부 호출은 지역당 3시간에 한 번 정도로 줄어든다.
 * 외부 호출 중에는 DB 트랜잭션을 잡지 않는다(저장은 repository 메서드 단위로 처리).
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    /** 단기예보는 오늘부터 약 3일 뒤까지 제공된다. 일차별 추천(TripRecommendationService)의 출발일 범위도 이 값으로 정한다. */
    public static final int MAX_DAYS_AHEAD = 3;

    private final KmaClient kmaClient;
    private final WeatherForecastRepository repository;
    private final Duration cacheTtl;
    /** 지역별 갱신 잠금. 같은 지역은 한 번만 갱신하고, 다른 지역끼리는 서로 기다리지 않는다. */
    private final Map<String, Object> refreshLocks = new ConcurrentHashMap<>();

    public WeatherService(KmaClient kmaClient,
                          WeatherForecastRepository repository,
                          @Value("${weather.cache-minutes:180}") long cacheMinutes) {
        this.kmaClient = kmaClient;
        this.repository = repository;
        this.cacheTtl = Duration.ofMinutes(cacheMinutes);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public WeatherForecast getForecast(Area area, LocalDate date) {
        LocalDate today = today();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "예보는 오늘부터 " + MAX_DAYS_AHEAD + "일 뒤까지만 조회할 수 있습니다.");
        }

        Optional<WeatherForecast> cached = repository.findByAreaCodeAndForecastDate(area.getCode(), date);
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (cached.isPresent() && !isStale(cached.get(), now)) {
            return cached.get();
        }

        try {
            refresh(area, now);
        } catch (KmaException e) {
            log.warn("기상청 예보 갱신 실패: area={}, {}", area.getCode(), e.getMessage());
            if (cached.isPresent()) {
                return cached.get(); // 오래됐어도 없는 것보다 낫다
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "날씨 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }

        return repository.findByAreaCodeAndForecastDate(area.getCode(), date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "해당 날짜의 예보가 아직 없습니다."));
    }

    boolean isStale(WeatherForecast f, LocalDateTime now) {
        return f.getFetchedAt() == null || Duration.between(f.getFetchedAt(), now).compareTo(cacheTtl) > 0;
    }

    /** 같은 지역에 동시에 여러 요청이 와도 한 번만 갱신하도록 막는다(유니크 키 충돌 방지). */
    private void refresh(Area area, LocalDateTime now) {
        Object lock = refreshLocks.computeIfAbsent(area.getCode(), k -> new Object());
        synchronized (lock) {
            // 기다리는 사이에 다른 요청이 이미 갱신했을 수 있다
            Optional<WeatherForecast> again = repository.findByAreaCodeAndForecastDate(area.getCode(), now.toLocalDate());
            if (again.isPresent() && !isStale(again.get(), now)) {
                return;
            }

            KmaBaseTime base = KmaBaseTime.latestAvailable(now);
            var items = kmaClient.fetchShortTermForecast(area.getNx(), area.getNy(), base);
            Map<LocalDate, DailySummary> summaries = ForecastAggregator.aggregate(items);
            if (summaries.isEmpty()) {
                throw new KmaException("기상청 응답에 예보 데이터가 없습니다.");
            }

            Map<LocalDate, WeatherForecast> existing = repository
                    .findAllByAreaCodeAndForecastDateIn(area.getCode(), summaries.keySet()).stream()
                    .collect(Collectors.toMap(WeatherForecast::getForecastDate, f -> f));

            List<WeatherForecast> toSave = summaries.values().stream().map(s -> {
                WeatherForecast f = existing.getOrDefault(s.date(), WeatherForecast.create(
                        area.getCode(), area.getLdongRegnCd(), area.getNx(), area.getNy(), s.date()));
                f.apply(s.sky(), s.precipType(), s.pop(), s.tempMax(), s.tempMin(), now);
                return f;
            }).toList();
            repository.saveAll(toSave);
            log.info("날씨 갱신: area={}, base={} {}, {}일치", area.getCode(), base.baseDate(), base.baseTime(), toSave.size());
        }
    }
}
