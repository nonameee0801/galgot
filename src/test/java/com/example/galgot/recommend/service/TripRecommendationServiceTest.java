package com.example.galgot.recommend.service;

import com.example.galgot.area.domain.Area;
import com.example.galgot.area.domain.AreaLevel;
import com.example.galgot.area.service.AreaService;
import com.example.galgot.global.config.RecommendCourseProperties;
import com.example.galgot.global.config.WeatherRuleProperties;
import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.repository.PlaceRepository;
import com.example.galgot.recommend.dto.TripRecommendationResponse;
import com.example.galgot.weather.domain.PrecipType;
import com.example.galgot.weather.domain.Sky;
import com.example.galgot.weather.domain.WeatherForecast;
import com.example.galgot.weather.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripRecommendationServiceTest {

    @Mock
    WeatherService weatherService;
    @Mock
    PlaceRepository placeRepository;
    @Mock
    AreaService areaService;

    TripRecommendationService service;

    private final Area gapyeong = Area.of("41820", "41", AreaLevel.SIGUNGU, "가평군", 69, 133);
    private final LocalDate today = WeatherService.today();

    @BeforeEach
    void setUp() {
        OutdoorJudge judge = new OutdoorJudge(new WeatherRuleProperties(null, null, null));
        service = new TripRecommendationService(weatherService, judge, placeRepository, areaService,
                new RecommendCourseProperties(null), bound -> 0);
    }

    private static WeatherForecast forecast(LocalDate date, PrecipType precip, int pop) {
        WeatherForecast f = WeatherForecast.create("41820", "41", 69, 133, date);
        f.apply(Sky.CLEAR, precip, pop, 18, 5, LocalDateTime.of(2026, 11, 2, 9, 0));
        return f;
    }

    private static Place place(long id, Env env, double x, String signgu) {
        Place p = mock(Place.class);
        lenient().when(p.getId()).thenReturn(id);
        lenient().when(p.getEnv()).thenReturn(env);
        lenient().when(p.getLat()).thenReturn(37.8);
        lenient().when(p.getLng()).thenReturn(127.5 + x * 0.01);
        lenient().when(p.getLdongSignguCd()).thenReturn(signgu);
        return p;
    }

    private int status(Runnable call) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, call::run);
        return e.getStatusCode().value();
    }

    @Test
    void 경기에서_시군을_고르지_않으면_400() {
        assertEquals(400, status(() -> service.recommend("41", null, today, 1, null, null, null, null)));
        assertEquals(400, status(() -> service.recommend("41", " ", today, 1, null, null, null, null)));
        verifyNoInteractions(weatherService, placeRepository);
    }

    @Test
    void 서울에서_구를_보내면_400() {
        assertEquals(400, status(() -> service.recommend("11", "11110", today, 1, null, null, null, null)));
        verifyNoInteractions(weatherService, placeRepository);
    }

    @Test
    void 일정은_1일에서_3일까지() {
        assertEquals(400, status(() -> service.recommend("41", "41820", today, 0, null, null, null, null)));
        assertEquals(400, status(() -> service.recommend("41", "41820", today, 4, null, null, null, null)));
    }

    @Test
    void 출발일은_일정이_날씨_예보_범위_안에_들어가야_한다() {
        // 예보는 오늘부터 3일 뒤까지. 2박3일은 내일 출발까지, 당일치기는 3일 뒤까지
        assertEquals(400, status(() -> service.recommend("41", "41820", today.plusDays(2), 3, null, null, null, null)));
        assertEquals(400, status(() -> service.recommend("41", "41820", today.plusDays(4), 1, null, null, null, null)));
        assertEquals(400, status(() -> service.recommend("41", "41820", today.minusDays(1), 1, null, null, null, null)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 일차마다_그날_날씨로_판정해_그날_추천대로_고른다() {
        LocalDate d1 = today.plusDays(1);
        LocalDate d2 = today.plusDays(2);
        when(areaService.resolve("41", "41820")).thenReturn(gapyeong);
        when(areaService.signguCodes(gapyeong)).thenReturn(List.of("820"));
        when(weatherService.getForecast(gapyeong, d1)).thenReturn(forecast(d1, PrecipType.RAIN, 80));
        when(weatherService.getForecast(gapyeong, d2)).thenReturn(forecast(d2, PrecipType.NONE, 10));
        // 가짜 장소는 thenReturn 밖에서 먼저 만든다(stub 안에서 또 stub을 만들면 Mockito 오류)
        List<Place> places = List.of(
                place(1, Env.INDOOR, 0, "820"), place(2, Env.INDOOR, 1, "820"),
                place(3, Env.INDOOR, 2, "820"), place(4, Env.INDOOR, 3, "820"),
                place(5, Env.OUTDOOR, 4, "820"), place(6, Env.OUTDOOR, 5, "820"),
                place(7, Env.OUTDOOR, 6, "820"), place(8, Env.OUTDOOR, 7, "820"));
        when(placeRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(places);

        TripRecommendationResponse res = service.recommend("41", "41820", d1, 2, null, null, null, null);

        verify(weatherService).getForecast(gapyeong, d1);
        verify(weatherService).getForecast(gapyeong, d2);
        TripRecommendationResponse.Day day1 = res.days().get(0);
        TripRecommendationResponse.Day day2 = res.days().get(1);
        assertEquals(Env.INDOOR, day1.recommendedEnv());
        assertFalse(day1.outdoorSuitable());
        assertEquals(List.of(1L, 2L, 3L, 4L), day1.places().stream().map(s -> s.place().id()).toList());
        assertEquals(Env.OUTDOOR, day2.recommendedEnv());
        assertEquals(List.of(5L, 6L, 7L, 8L), day2.places().stream().map(s -> s.place().id()).toList());
        assertEquals(List.of(1, 2, 3, 4), day1.places().stream().map(TripRecommendationResponse.Stop::order).toList());
        assertEquals(d2, day2.date());
        assertEquals(1L, res.startPlaceId());
        assertTrue(res.rerollAvailable());
        assertNull(day1.notice());
        assertEquals("가평군", res.areaName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 고른_지역이_모자라면_주변_지역_장소에_표시를_붙이고_안내한다() {
        when(areaService.resolve("41", "41820")).thenReturn(gapyeong);
        when(areaService.signguCodes(gapyeong)).thenReturn(List.of("820"));
        when(weatherService.getForecast(gapyeong, today)).thenReturn(forecast(today, PrecipType.RAIN, 80));
        // 좌표가 서울·경기 범위 밖인 장소(TourAPI 오류)는 가장 가까워도 후보에서 빠진다
        Place broken = place(9, Env.INDOOR, 0, "820");
        when(broken.getLat()).thenReturn(19.69);
        List<Place> places = List.of(
                place(1, Env.INDOOR, 0, "820"),
                place(2, Env.INDOOR, 1, "830"), place(3, Env.INDOOR, 2, "830"),
                place(4, Env.INDOOR, 3, "830"),
                broken);
        when(placeRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(places);

        TripRecommendationResponse res = service.recommend("41", "41820", today, 1,
                null, EnvPreference.Choice.INDOOR, null, null);

        List<TripRecommendationResponse.Stop> stops = res.days().get(0).places();
        assertEquals(List.of(1L, 2L, 3L), stops.stream().map(s -> s.place().id()).toList());
        assertEquals(List.of(false, true, true), stops.stream().map(TripRecommendationResponse.Stop::nearby).toList());
        assertEquals("이 지역에 맞는 장소가 적어 주변 지역 장소를 함께 넣었어요.", res.days().get(0).notice());
        assertFalse(res.rerollAvailable());
        assertEquals(EnvPreference.Choice.INDOOR, res.env());
    }

    @Test
    void 안내_문장() {
        assertNull(TripRecommendationService.notice(List.of(
                new TripPlanner.Pick(1, false), new TripPlanner.Pick(2, false)), 2));
        assertEquals("이 지역에 맞는 장소가 적어 2곳만 추천해요.", TripRecommendationService.notice(List.of(
                new TripPlanner.Pick(1, false), new TripPlanner.Pick(2, false)), 4));
        assertEquals("조건에 맞는 장소가 더 없어요. 실내/실외 선택이나 장소 종류를 바꿔 보세요.",
                TripRecommendationService.notice(List.of(), 4));
    }
}
