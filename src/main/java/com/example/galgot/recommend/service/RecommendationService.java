package com.example.galgot.recommend.service;

import com.example.galgot.area.domain.Area;
import com.example.galgot.area.service.AreaService;
import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.dto.PageResponse;
import com.example.galgot.place.dto.PlaceResponse;
import com.example.galgot.place.repository.PlaceRepository;
import com.example.galgot.recommend.dto.RecommendationResponse;
import com.example.galgot.weather.domain.WeatherForecast;
import com.example.galgot.weather.dto.WeatherResponse;
import com.example.galgot.weather.service.WeatherService;
import jakarta.persistence.criteria.Predicate;
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
 * - 야외 부적합한 날: OUTDOOR 제외, INDOOR 먼저 → BOTH 순
 * - 적합한 날: 모든 장소
 * WeatherService(외부 호출 포함)를 트랜잭션 없이 부르고, 장소 조회는 repository 호출 한 번으로 끝난다.
 */
@Service
public class RecommendationService {

    static final int MAX_PAGE_SIZE = 100;

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

    public RecommendationResponse recommend(Area area, LocalDate date, Integer contentTypeId, int page, int size) {
        WeatherForecast forecast = weatherService.getForecast(area, date);
        OutdoorJudge.Result judged = outdoorJudge.judge(
                forecast.getPrecipType(), forecast.getPop(), forecast.getTempMax(), forecast.getTempMin());

        Page<Place> places = findPlaces(area.getLdongRegnCd(), areaService.signguCodes(area),
                judged.outdoorSuitable(), contentTypeId, page, size);

        String message = judged.outdoorSuitable()
                ? "야외 활동하기 좋은 날이에요. 모든 장소를 보여 드려요."
                : "야외 활동이 어려운 날이에요(" + String.join(", ", judged.reasons())
                  + "). 실내 장소를 먼저 보여 드려요.";

        return new RecommendationResponse(
                WeatherResponse.from(forecast, area),
                judged.outdoorSuitable(),
                judged.reasons(),
                message,
                PageResponse.of(places, PlaceResponse::from));
    }

    /** signguCodes가 비어 있으면 시도 전체, 아니면 그 시군구 코드의 장소만. */
    Page<Place> findPlaces(String region, List<String> signguCodes, boolean outdoorSuitable,
                           Integer contentTypeId, int page, int size) {
        Specification<Place> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ldongRegnCd"), region));
            if (!signguCodes.isEmpty()) {
                predicates.add(root.get("ldongSignguCd").in(signguCodes));
            }
            if (!outdoorSuitable) {
                predicates.add(root.get("env").in(Env.INDOOR, Env.BOTH));
            }
            if (contentTypeId != null) {
                predicates.add(cb.equal(root.get("contentTypeId"), contentTypeId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = outdoorSuitable
                ? Sort.by("title").ascending().and(Sort.by("id").ascending())
                // env는 문자열로 저장돼 있어 내림차순이면 INDOOR가 BOTH보다 앞에 온다("INDOOR" > "BOTH")
                : Sort.by(Sort.Order.desc("env"), Sort.Order.asc("title"), Sort.Order.asc("id"));

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return placeRepository.findAll(spec, PageRequest.of(safePage, safeSize, sort));
    }
}
