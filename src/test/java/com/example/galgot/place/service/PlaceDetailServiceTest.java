package com.example.galgot.place.service;

import com.example.galgot.infra.tourapi.TourApiClient;
import com.example.galgot.infra.tourapi.TourApiException;
import com.example.galgot.infra.tourapi.TourDetailItem;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.domain.PlaceDetail;
import com.example.galgot.place.dto.PlaceDetailResponse;
import com.example.galgot.place.repository.PlaceDetailRepository;
import com.example.galgot.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceDetailServiceTest {

    @Mock
    PlaceRepository placeRepository;
    @Mock
    PlaceDetailRepository detailRepository;
    @Mock
    TourApiClient tourApiClient;

    PlaceDetailService service;
    Place place;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final LocalDateTime now = LocalDateTime.of(2026, 11, 2, 19, 0);

    private static final TourDetailItem ITEM = new TourDetailItem(
            "소개", "09:00~18:00", "매주 월요일", "무료", "가능", null, "031-000-0000",
            List.of("https://a.kr", "https://b.kr"), null, null);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(now.atZone(ZONE).toInstant(), ZONE);
        service = new PlaceDetailService(placeRepository, detailRepository, tourApiClient, 30, clock);

        place = mock(Place.class);
        lenient().when(place.getId()).thenReturn(7L);
        lenient().when(place.getContentId()).thenReturn("130220");
        lenient().when(place.getContentTypeId()).thenReturn(14);
        lenient().when(detailRepository.save(any(PlaceDetail.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static PlaceDetail saved(LocalDateTime fetchedAt, String useTime) {
        PlaceDetail d = PlaceDetail.create(7L);
        d.apply(new TourDetailItem(null, useTime, null, null, null, null, null, List.of(), null, null), fetchedAt);
        return d;
    }

    @Test
    void 없는_장소는_404() {
        when(placeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.get(99L));

        assertEquals(404, e.getStatusCode().value());
        verifyNoInteractions(tourApiClient);
    }

    @Test
    void 저장값이_30일_안이면_TourAPI를_부르지_않는다() {
        PlaceDetail fresh = saved(now.minusDays(29), "저장된 이용시간");
        when(placeRepository.findById(7L)).thenReturn(Optional.of(place));
        when(detailRepository.findById(7L)).thenReturn(Optional.of(fresh));

        PlaceDetailResponse res = service.get(7L);

        assertEquals("저장된 이용시간", res.useTime());
        verifyNoInteractions(tourApiClient);
        verify(detailRepository, never()).save(any());
    }

    @Test
    void 저장값이_없으면_받아서_저장한다() {
        when(placeRepository.findById(7L)).thenReturn(Optional.of(place));
        when(detailRepository.findById(7L)).thenReturn(Optional.empty());
        when(tourApiClient.fetchDetail("130220", 14)).thenReturn(ITEM);

        PlaceDetailResponse res = service.get(7L);

        ArgumentCaptor<PlaceDetail> captor = ArgumentCaptor.forClass(PlaceDetail.class);
        verify(detailRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getPlaceId());
        assertEquals(now, captor.getValue().getFetchedAt());
        assertEquals("09:00~18:00", res.useTime());
        assertEquals("031-000-0000", res.contact());
        assertEquals(List.of("https://a.kr", "https://b.kr"), res.homepages());
        assertNull(res.parkingFee());
    }

    @Test
    void 저장값이_30일이_지나면_다시_받아_바꾼다() {
        PlaceDetail old = saved(now.minusDays(31), "예전 이용시간");
        when(placeRepository.findById(7L)).thenReturn(Optional.of(place));
        when(detailRepository.findById(7L)).thenReturn(Optional.of(old));
        when(tourApiClient.fetchDetail("130220", 14)).thenReturn(ITEM);

        PlaceDetailResponse res = service.get(7L);

        assertEquals("09:00~18:00", res.useTime());
        assertEquals(now, res.fetchedAt());
        verify(detailRepository).save(old);
    }

    @Test
    void 호출이_실패하면_오래된_값이라도_쓴다() {
        PlaceDetail old = saved(now.minusDays(40), "예전 이용시간");
        when(placeRepository.findById(7L)).thenReturn(Optional.of(place));
        when(detailRepository.findById(7L)).thenReturn(Optional.of(old));
        when(tourApiClient.fetchDetail("130220", 14)).thenThrow(new TourApiException("timeout"));

        PlaceDetailResponse res = service.get(7L);

        assertEquals("예전 이용시간", res.useTime());
        verify(detailRepository, never()).save(any());
    }

    @Test
    void 호출이_실패하고_저장값도_없으면_503() {
        when(placeRepository.findById(7L)).thenReturn(Optional.of(place));
        when(detailRepository.findById(7L)).thenReturn(Optional.empty());
        when(tourApiClient.fetchDetail("130220", 14)).thenThrow(new TourApiException("timeout"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.get(7L));

        assertEquals(503, e.getStatusCode().value());
    }

    @Test
    void 정보가_하나도_없어도_빈_행으로_저장해_다시_부르지_않게_한다() {
        TourDetailItem empty = new TourDetailItem(null, null, null, null, null, null, null, List.of(), null, null);
        when(placeRepository.findById(7L)).thenReturn(Optional.of(place));
        when(detailRepository.findById(7L)).thenReturn(Optional.empty());
        when(tourApiClient.fetchDetail("130220", 14)).thenReturn(empty);

        PlaceDetailResponse res = service.get(7L);

        verify(detailRepository).save(any(PlaceDetail.class));
        assertNull(res.overview());
        assertEquals(List.of(), res.homepages());
        assertEquals(now, res.fetchedAt());
    }

    @Test
    void 기간_경계는_30일을_넘어야_오래된_것으로_본다() {
        assertEquals(false, service.isStale(saved(now.minusDays(30), null), now));
        assertEquals(true, service.isStale(saved(now.minusDays(30).minusSeconds(1), null), now));
    }
}
