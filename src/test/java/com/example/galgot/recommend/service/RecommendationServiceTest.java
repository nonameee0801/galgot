package com.example.galgot.recommend.service;

import com.example.galgot.area.domain.Area;
import com.example.galgot.area.domain.AreaLevel;
import com.example.galgot.area.service.AreaService;
import com.example.galgot.global.config.WeatherRuleProperties;
import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.repository.PlaceRepository;
import com.example.galgot.recommend.dto.MarkerResponse;
import com.example.galgot.weather.domain.PrecipType;
import com.example.galgot.weather.domain.Sky;
import com.example.galgot.weather.domain.WeatherForecast;
import com.example.galgot.weather.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    WeatherService weatherService;
    @Mock
    PlaceRepository placeRepository;
    @Mock
    AreaService areaService;

    RecommendationService service;

    private final Area gapyeong = Area.of("41820", "41", AreaLevel.SIGUNGU, "가평군", 69, 133);
    private final LocalDate today = LocalDate.of(2026, 11, 2);

    @BeforeEach
    void setUp() {
        OutdoorJudge judge = new OutdoorJudge(new WeatherRuleProperties(null, null, null));
        service = new RecommendationService(weatherService, judge, placeRepository, areaService);

        WeatherForecast clear = WeatherForecast.create("41820", "41", 69, 133, today);
        clear.apply(Sky.CLEAR, PrecipType.NONE, 10, 18, 5, LocalDateTime.of(2026, 11, 2, 9, 0));
        // 범위 경계값 테스트처럼 서비스를 부르지 않는 테스트도 있어 느슨하게 준비한다
        lenient().when(weatherService.getForecast(gapyeong, today)).thenReturn(clear);
        lenient().when(areaService.signguCodes(gapyeong)).thenReturn(List.of("820"));
    }

    private static Place place(long id, String title, Double lat, Double lng, Env env) {
        // 걸러지는 장소는 일부 값만 읽히므로 쓰이지 않는 값이 있어도 테스트가 실패하지 않게 둔다
        Place p = mock(Place.class);
        lenient().when(p.getLat()).thenReturn(lat);
        lenient().when(p.getLng()).thenReturn(lng);
        lenient().when(p.getId()).thenReturn(id);
        lenient().when(p.getTitle()).thenReturn(title);
        lenient().when(p.getEnv()).thenReturn(env);
        return p;
    }

    @Test
    @SuppressWarnings("unchecked")
    void 마커는_좌표가_있는_장소만_가벼운_정보로_돌려준다() {
        Place withCoords = place(1L, "가평 수목원", 37.8, 127.5, Env.OUTDOOR);
        Place noCoords = place(2L, "좌표 없는 곳", null, null, Env.OUTDOOR);
        when(placeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(withCoords, noCoords)));

        List<MarkerResponse> markers = service.markers(gapyeong, today, null, null, null);

        assertEquals(List.of(new MarkerResponse(1L, "가평 수목원", 37.8, 127.5, Env.OUTDOOR)), markers);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 좌표가_서울경기_범위_밖이면_마커에서_뺀다() {
        Place ok = place(1L, "가평 수목원", 37.8, 127.5, Env.OUTDOOR);
        Place wrongCoords = place(3L, "좌표가 남중국해인 곳", 19.69442748, 117.9925662504, Env.INDOOR);
        when(placeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ok, wrongCoords)));

        List<MarkerResponse> markers = service.markers(gapyeong, today, null, null, null);

        assertEquals(List.of(1L), markers.stream().map(MarkerResponse::id).toList());
    }

    @Test
    void 서울경기_범위는_경계값을_포함한다() {
        assertTrue(RecommendationService.inServiceArea(36.8, 126.0));
        assertTrue(RecommendationService.inServiceArea(38.4, 128.2));
        assertTrue(RecommendationService.inServiceArea(37.082051143, 128.0000201927)); // 경기 동쪽 끝 실제 장소
        assertFalse(RecommendationService.inServiceArea(36.79, 127.0));
        assertFalse(RecommendationService.inServiceArea(37.5, 128.21));
        assertFalse(RecommendationService.inServiceArea(null, 127.0));
        assertFalse(RecommendationService.inServiceArea(37.5, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 마커는_페이지_없이_상한까지_한_번에_가져온다() {
        when(placeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.markers(gapyeong, today, 12, EnvPreference.Choice.ALL, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(placeRepository).findAll(any(Specification.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(RecommendationService.MAX_MARKERS, pageable.getValue().getPageSize());
        verify(weatherService).getForecast(eq(gapyeong), eq(today));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 검색어가_있으면_고른_지역으로_거르지_않고_날씨는_고른_지역_기준이다() {
        when(placeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.recommend(gapyeong, today, 12, EnvPreference.Choice.INDOOR, "  박물관 ", 0, 12);

        // 지역의 시군구 코드로 거르지 않는다(서울·경기 전체에서 이름으로만)
        verify(areaService, never()).signguCodes(any());
        verify(weatherService).getForecast(eq(gapyeong), eq(today));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 검색어가_공백뿐이면_지역_조건으로_찾는다() {
        when(placeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.markers(gapyeong, today, null, null, "   ");

        verify(areaService).signguCodes(gapyeong);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 검색_결과_마커도_서울경기_범위_밖_좌표는_뺀다() {
        Place museum = place(5L, "국립중앙박물관", 37.52, 126.98, Env.INDOOR);
        Place wrongCoords = place(6L, "박물관(좌표 오류)", 19.69, 117.99, Env.INDOOR);
        when(placeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(museum, wrongCoords)));

        List<MarkerResponse> markers = service.markers(gapyeong, today, 14, EnvPreference.Choice.OUTDOOR, "박물관");

        assertEquals(List.of(5L), markers.stream().map(MarkerResponse::id).toList());
        verify(areaService, never()).signguCodes(any());
    }
}
