package com.example.galgot.place.service;

import com.example.galgot.infra.tourapi.TourApiClient;
import com.example.galgot.infra.tourapi.TourApiException;
import com.example.galgot.infra.tourapi.TourDetailItem;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.domain.PlaceDetail;
import com.example.galgot.place.dto.PlaceDetailResponse;
import com.example.galgot.place.repository.PlaceDetailRepository;
import com.example.galgot.place.repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 장소 상세 정보(기획서 17장). DB에 저장해 둔 값을 먼저 쓰고, 없거나 오래됐을 때만(기본 30일) TourAPI를 부른다(장소당 2번).
 * 호출이 실패하면 이전 값을 쓰고, 이전 값도 없으면 503. TourAPI가 정보를 하나도 주지 않아도 빈 행으로 저장해 다시 부르지 않는다.
 * 날씨(WeatherService)와 같이 외부 호출 중에는 DB 트랜잭션을 잡지 않고, 같은 장소는 동시에 한 번만 받는다.
 */
@Service
public class PlaceDetailService {

    private static final Logger log = LoggerFactory.getLogger(PlaceDetailService.class);

    private final PlaceRepository placeRepository;
    private final PlaceDetailRepository detailRepository;
    private final TourApiClient tourApiClient;
    private final Duration cacheTtl;
    private final Clock clock;
    /** 장소별 갱신 잠금. 같은 장소는 한 번만 받고, 다른 장소끼리는 서로 기다리지 않는다. */
    private final Map<Long, Object> refreshLocks = new ConcurrentHashMap<>();

    @Autowired
    public PlaceDetailService(PlaceRepository placeRepository, PlaceDetailRepository detailRepository,
                              TourApiClient tourApiClient,
                              @Value("${place.detail.cache-days:30}") long cacheDays) {
        this(placeRepository, detailRepository, tourApiClient, cacheDays, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    /** 테스트에서 시각을 고정할 때 쓴다. */
    PlaceDetailService(PlaceRepository placeRepository, PlaceDetailRepository detailRepository,
                       TourApiClient tourApiClient, long cacheDays, Clock clock) {
        this.placeRepository = placeRepository;
        this.detailRepository = detailRepository;
        this.tourApiClient = tourApiClient;
        this.cacheTtl = Duration.ofDays(cacheDays);
        this.clock = clock;
    }

    public PlaceDetailResponse get(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now(clock);
        Optional<PlaceDetail> cached = detailRepository.findById(placeId);
        if (cached.isPresent() && !isStale(cached.get(), now)) {
            return PlaceDetailResponse.from(cached.get());
        }

        try {
            return PlaceDetailResponse.from(refresh(place, now));
        } catch (TourApiException e) {
            log.warn("장소 상세 갱신 실패: placeId={}, {}", placeId, e.getMessage());
            if (cached.isPresent()) {
                return PlaceDetailResponse.from(cached.get()); // 오래됐어도 없는 것보다 낫다
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "장소 상세 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    boolean isStale(PlaceDetail detail, LocalDateTime now) {
        return detail.getFetchedAt() == null || Duration.between(detail.getFetchedAt(), now).compareTo(cacheTtl) > 0;
    }

    private PlaceDetail refresh(Place place, LocalDateTime now) {
        Object lock = refreshLocks.computeIfAbsent(place.getId(), k -> new Object());
        synchronized (lock) {
            // 기다리는 사이에 다른 요청이 이미 받아 저장했을 수 있다
            Optional<PlaceDetail> again = detailRepository.findById(place.getId());
            if (again.isPresent() && !isStale(again.get(), now)) {
                return again.get();
            }
            TourDetailItem item = tourApiClient.fetchDetail(place.getContentId(), place.getContentTypeId());
            PlaceDetail detail = again.orElseGet(() -> PlaceDetail.create(place.getId()));
            detail.apply(item, now);
            PlaceDetail saved = detailRepository.save(detail);
            log.info("장소 상세 갱신: placeId={}, contentId={}", place.getId(), place.getContentId());
            return saved;
        }
    }
}
