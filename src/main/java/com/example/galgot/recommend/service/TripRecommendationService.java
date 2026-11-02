package com.example.galgot.recommend.service;

import com.example.galgot.area.domain.Area;
import com.example.galgot.area.service.AreaService;
import com.example.galgot.global.config.RecommendCourseProperties;
import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.dto.PlaceResponse;
import com.example.galgot.place.repository.PlaceRepository;
import com.example.galgot.recommend.dto.TripRecommendationResponse;
import com.example.galgot.weather.domain.WeatherForecast;
import com.example.galgot.weather.dto.WeatherResponse;
import com.example.galgot.weather.service.WeatherService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

/**
 * 날씨기반 일차별 코스 추천(기획서 16장 3단계).
 * 일차마다 그날 날씨로 판정하고, 코스는 TripPlanner(순수 로직)가 고른다.
 * <ul>
 *   <li>서울은 구를 고르지 않고 서울 전체에서 짠다(area를 보내면 400). 경기는 시·군을 꼭 고른다(없으면 400)</li>
 *   <li>후보는 시도 전체에서 한 번만 읽고, 고른 지역 안인지만 표시한다. 고른 지역이 모자라면 주변 지역에서 채우기 위해서다</li>
 *   <li>좌표가 없거나 서울·경기 범위 밖인 장소는 후보에서 뺀다(지도 마커와 같은 기준)</li>
 * </ul>
 * WeatherService(외부 호출 포함)를 트랜잭션 없이 부르고, 장소 조회는 repository 호출 한 번으로 끝난다.
 */
@Service
public class TripRecommendationService {

    static final int MAX_DAYS = 3;
    /** 구를 고르지 않고 시도 전체로 짜는 시도. 다른 시도(경기)는 시·군을 꼭 고른다. */
    static final String WHOLE_REGION_ONLY = "11";

    private final WeatherService weatherService;
    private final OutdoorJudge outdoorJudge;
    private final PlaceRepository placeRepository;
    private final AreaService areaService;
    private final int placesPerDay;
    private final IntUnaryOperator randomIndex;

    @Autowired
    public TripRecommendationService(WeatherService weatherService, OutdoorJudge outdoorJudge,
                                     PlaceRepository placeRepository, AreaService areaService,
                                     RecommendCourseProperties properties) {
        this(weatherService, outdoorJudge, placeRepository, areaService, properties, new Random()::nextInt);
    }

    /** 테스트에서 무작위를 고정할 때 쓴다. */
    TripRecommendationService(WeatherService weatherService, OutdoorJudge outdoorJudge,
                              PlaceRepository placeRepository, AreaService areaService,
                              RecommendCourseProperties properties, IntUnaryOperator randomIndex) {
        this.weatherService = weatherService;
        this.outdoorJudge = outdoorJudge;
        this.placeRepository = placeRepository;
        this.areaService = areaService;
        this.placesPerDay = properties.placesPerDay();
        this.randomIndex = randomIndex;
    }

    public TripRecommendationResponse recommend(String region, String areaCode, LocalDate start, int days,
                                                Integer contentTypeId, EnvPreference.Choice choice,
                                                Long startPlaceId, Long excludeStartPlaceId) {
        String regn = region == null ? "" : region.trim();
        boolean hasArea = areaCode != null && !areaCode.isBlank();
        if (days < 1 || days > MAX_DAYS) {
            throw badRequest("일정은 1일에서 " + MAX_DAYS + "일까지 고를 수 있습니다.");
        }
        if (WHOLE_REGION_ONLY.equals(regn) && hasArea) {
            throw badRequest("서울은 구를 고르지 않고 서울 전체로 추천합니다. area를 빼고 요청해 주세요.");
        }
        if (!WHOLE_REGION_ONLY.equals(regn) && !hasArea) {
            throw badRequest("경기는 시·군을 골라 주세요.");
        }
        LocalDate today = WeatherService.today();
        LocalDate lastAllowed = today.plusDays(WeatherService.MAX_DAYS_AHEAD - (days - 1));
        if (start.isBefore(today) || start.isAfter(lastAllowed)) {
            throw badRequest("이 일정의 출발일은 오늘부터 " + lastAllowed + "까지 고를 수 있습니다(날씨 예보 범위).");
        }
        Area area = areaService.resolve(regn, areaCode);

        // 1) 일차별 날씨와 판정
        List<WeatherForecast> forecasts = new ArrayList<>();
        List<OutdoorJudge.Result> judged = new ArrayList<>();
        List<EnvPreference.Plan> plans = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            WeatherForecast f = weatherService.getForecast(area, start.plusDays(d));
            OutdoorJudge.Result j = outdoorJudge.judge(f.getPrecipType(), f.getPop(), f.getTempMax(), f.getTempMin());
            forecasts.add(f);
            judged.add(j);
            plans.add(EnvPreference.plan(choice, j));
        }

        // 2) 후보: 시도 전체에서 그 일정 동안 쓰일 수 있는 실내외 종류만
        Set<Env> envs = EnumSet.noneOf(Env.class);
        plans.forEach(p -> envs.addAll(p.allowed()));
        List<Place> places = findCandidates(area.getLdongRegnCd(), envs, contentTypeId);
        List<String> signgu = areaService.signguCodes(area);
        List<TripPlanner.Candidate> candidates = places.stream()
                .filter(p -> RecommendationService.inServiceArea(p.getLat(), p.getLng()))
                .map(p -> new TripPlanner.Candidate(p.getId(), p.getEnv(), p.getLat(), p.getLng(),
                        signgu.isEmpty() || signgu.contains(p.getLdongSignguCd())))
                .toList();

        // 3) 코스 고르기
        TripPlanner.Result result = TripPlanner.plan(
                plans.stream().map(EnvPreference.Plan::order).toList(),
                candidates, placesPerDay, area.getLat(), area.getLng(),
                startPlaceId, excludeStartPlaceId, randomIndex);

        Map<Long, Place> byId = places.stream().collect(Collectors.toMap(Place::getId, Function.identity()));
        List<TripRecommendationResponse.Day> dayList = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            List<TripPlanner.Pick> picks = result.days().get(d);
            List<TripRecommendationResponse.Stop> stops = new ArrayList<>();
            for (int i = 0; i < picks.size(); i++) {
                TripPlanner.Pick pick = picks.get(i);
                stops.add(new TripRecommendationResponse.Stop(i + 1, pick.nearby(),
                        PlaceResponse.from(byId.get(pick.id()))));
            }
            EnvPreference.Plan plan = plans.get(d);
            dayList.add(new TripRecommendationResponse.Day(
                    d + 1,
                    start.plusDays(d),
                    WeatherResponse.from(forecasts.get(d), area),
                    judged.get(d).outdoorSuitable(),
                    plan.recommended(),
                    judged.get(d).reasons(),
                    plan.message(),
                    List.copyOf(stops),
                    notice(picks, placesPerDay)));
        }

        return new TripRecommendationResponse(regn, area.getCode(), area.getName(), start, days, choice,
                result.startPlaceId(), result.rerollAvailable(), List.copyOf(dayList));
    }

    /** 장소가 모자라거나 주변 지역에서 채웠을 때의 안내. 없으면 null. */
    static String notice(List<TripPlanner.Pick> picks, int perDay) {
        if (picks.isEmpty()) {
            return "조건에 맞는 장소가 더 없어요. 실내/실외 선택이나 장소 종류를 바꿔 보세요.";
        }
        boolean nearby = picks.stream().anyMatch(TripPlanner.Pick::nearby);
        if (nearby) {
            return "이 지역에 맞는 장소가 적어 주변 지역 장소를 함께 넣었어요.";
        }
        if (picks.size() < perDay) {
            return "이 지역에 맞는 장소가 적어 " + picks.size() + "곳만 추천해요.";
        }
        return null;
    }

    /** 시도 전체에서 실내외 종류와 유형이 맞는 장소. id 순으로 읽는다. */
    List<Place> findCandidates(String region, Set<Env> envs, Integer contentTypeId) {
        Specification<Place> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ldongRegnCd"), region));
            predicates.add(cb.isNotNull(root.get("lat")));
            predicates.add(cb.isNotNull(root.get("lng")));
            if (envs.size() < Env.values().length) {
                predicates.add(root.get("env").in(envs));
            }
            if (contentTypeId != null) {
                predicates.add(cb.equal(root.get("contentTypeId"), contentTypeId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return placeRepository.findAll(spec, Sort.by("id").ascending());
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
