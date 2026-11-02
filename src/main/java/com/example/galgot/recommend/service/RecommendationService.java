package com.example.galgot.recommend.service;

import com.example.galgot.area.domain.Area;
import com.example.galgot.area.service.AreaService;
import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.dto.PageResponse;
import com.example.galgot.place.dto.PlaceResponse;
import com.example.galgot.place.repository.PlaceRepository;
import com.example.galgot.recommend.dto.MarkerResponse;
import com.example.galgot.recommend.dto.RecommendationResponse;
import com.example.galgot.weather.domain.WeatherForecast;
import com.example.galgot.weather.dto.WeatherResponse;
import com.example.galgot.weather.service.WeatherService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 날씨 + 장소 추천. 지역(시도 전체 또는 구·시군)의 날씨로 판정하고, 그 지역의 장소만 보여 준다.
 * 어떤 실내외 종류를 어떤 순서로 보여 줄지는 EnvPreference(순수 로직)가 정한다.
 * env를 고르지 않으면 그날 추천을 고른 것과 같다(부적합하면 INDOOR → BOTH, 적합하면 OUTDOOR → BOTH).
 * WeatherService(외부 호출 포함)를 트랜잭션 없이 부르고, 장소 조회는 repository 호출 한 번으로 끝난다.
 */
@Service
public class RecommendationService {

    static final int MAX_PAGE_SIZE = 100;
    /** 마커는 페이지 없이 한 번에 준다. 경기 전체가 약 2,900곳이라 넉넉한 상한을 둔다. */
    static final int MAX_MARKERS = 5000;

    /**
     * 서울·경기 좌표 범위. TourAPI에는 좌표가 엉뚱하게 들어간 장소가 있다(예: 위도 19.69, 경도 117.99로 남중국해, 2026-11-02 기준 11곳).
     * 이런 마커가 섞이면 "모든 마커가 보이게" 맞춘 지도가 동아시아 전체로 축소되므로 마커에서 뺀다. DB의 원본은 고치지 않는다.
     * 경기 동쪽 끝 장소(경도 128.00002)가 있어 경도 상한에 여유를 둔다.
     */
    static final double MIN_LAT = 36.8;
    static final double MAX_LAT = 38.4;
    static final double MIN_LNG = 126.0;
    static final double MAX_LNG = 128.2;

    private final WeatherService weatherService;
    private final OutdoorJudge outdoorJudge;
    private final PlaceRepository placeRepository;
    private final AreaService areaService;

    public RecommendationService(WeatherService weatherService, OutdoorJudge outdoorJudge,
                                 PlaceRepository placeRepository, AreaService areaService) {
        this.weatherService = weatherService;
        this.outdoorJudge = outdoorJudge;
        this.placeRepository = placeRepository;
        this.areaService = areaService;
    }

    public RecommendationResponse recommend(Area area, LocalDate date, Integer contentTypeId,
                                            EnvPreference.Choice choice, int page, int size) {
        WeatherForecast forecast = weatherService.getForecast(area, date);
        OutdoorJudge.Result judged = judge(forecast);
        EnvPreference.Plan plan = EnvPreference.plan(choice, judged);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<Place> places = findPlaces(area.getLdongRegnCd(), areaService.signguCodes(area),
                plan, contentTypeId, safePage, safeSize);

        return new RecommendationResponse(
                WeatherResponse.from(forecast, area),
                judged.outdoorSuitable(),
                plan.recommended(),
                judged.reasons(),
                plan.message(),
                PageResponse.of(places, PlaceResponse::from));
    }

    /**
     * 지도 마커. 목록(recommend)과 같은 조건(지역, 그날 날씨, 실내/실외 선택, 종류)의 장소를 페이지 없이 돌려준다.
     * 날씨는 DB에 저장된 값을 쓰므로 목록과 함께 불러도 기상청 호출이 늘지 않는다.
     * 좌표가 없거나 서울·경기 범위 밖인 장소는 뺀다(목록에는 그대로 나온다).
     */
    public List<MarkerResponse> markers(Area area, LocalDate date, Integer contentTypeId, EnvPreference.Choice choice) {
        WeatherForecast forecast = weatherService.getForecast(area, date);
        EnvPreference.Plan plan = EnvPreference.plan(choice, judge(forecast));

        return findPlaces(area.getLdongRegnCd(), areaService.signguCodes(area), plan, contentTypeId, 0, MAX_MARKERS)
                .getContent().stream()
                .filter(p -> inServiceArea(p.getLat(), p.getLng()))
                .map(MarkerResponse::from)
                .toList();
    }

    /** 좌표가 있고 서울·경기 범위 안이면 true. 경계값은 포함한다. */
    static boolean inServiceArea(Double lat, Double lng) {
        return lat != null && lng != null
                && lat >= MIN_LAT && lat <= MAX_LAT
                && lng >= MIN_LNG && lng <= MAX_LNG;
    }

    private OutdoorJudge.Result judge(WeatherForecast f) {
        return outdoorJudge.judge(f.getPrecipType(), f.getPop(), f.getTempMax(), f.getTempMin());
    }

    /**
     * signguCodes가 비어 있으면 시도 전체, 아니면 그 시군구 코드의 장소만.
     * plan.order()가 있으면 그 순서(CASE)로, 같은 그룹 안은 이름순으로 정렬한다.
     * page, size는 호출하는 쪽에서 범위를 정해 넘긴다.
     */
    Page<Place> findPlaces(String region, List<String> signguCodes, EnvPreference.Plan plan,
                           Integer contentTypeId, int page, int size) {
        List<Env> order = plan.order();
        Specification<Place> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ldongRegnCd"), region));
            if (!signguCodes.isEmpty()) {
                predicates.add(root.get("ldongSignguCd").in(signguCodes));
            }
            if (plan.allowed().size() < Env.values().length) {
                predicates.add(root.get("env").in(plan.allowed()));
            }
            if (contentTypeId != null) {
                predicates.add(cb.equal(root.get("contentTypeId"), contentTypeId));
            }
            // 개수 세기 쿼리(Long)에는 정렬을 붙이지 않는다
            if (!order.isEmpty() && query != null && !Long.class.equals(query.getResultType())) {
                var rank = cb.<Integer>selectCase();
                for (int i = 0; i < order.size(); i++) {
                    rank = rank.when(cb.equal(root.get("env"), order.get(i)), i);
                }
                Expression<Integer> rankExpr = rank.otherwise(order.size());
                query.orderBy(cb.asc(rankExpr), cb.asc(root.get("title")), cb.asc(root.get("id")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 실내외 순서가 있으면 정렬은 위 CASE가 맡고, 없으면 이름순
        Sort sort = order.isEmpty()
                ? Sort.by("title").ascending().and(Sort.by("id").ascending())
                : Sort.unsorted();

        return placeRepository.findAll(spec, PageRequest.of(page, size, sort));
    }
}
