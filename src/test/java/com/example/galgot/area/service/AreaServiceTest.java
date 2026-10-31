package com.example.galgot.area.service;

import com.example.galgot.area.domain.Area;
import com.example.galgot.area.domain.AreaLevel;
import com.example.galgot.area.repository.AreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock
    AreaRepository areaRepository;

    AreaService areaService;

    static final Area GYEONGGI = Area.of("41", "41", AreaLevel.SIDO, "경기 전체", 60, 120);
    static final Area GAPYEONG = Area.of("41820", "41", AreaLevel.SIGUNGU, "가평군", 69, 133);
    static final Area JONGNO = Area.of("11110", "11", AreaLevel.SIGUNGU, "종로구", 60, 127);

    @BeforeEach
    void setUp() {
        areaService = new AreaService(areaRepository);
    }

    private static HttpStatus statusOf(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void 지역을_생략하면_시도_전체() {
        when(areaRepository.findById("41")).thenReturn(Optional.of(GYEONGGI));

        assertEquals(GYEONGGI, areaService.resolve(" 41 ", null));
        assertEquals(GYEONGGI, areaService.resolve("41", ""));
    }

    @Test
    void 시도_안의_구시군을_고르면_그_지역() {
        when(areaRepository.findById("41820")).thenReturn(Optional.of(GAPYEONG));

        assertEquals(GAPYEONG, areaService.resolve("41", "41820"));
    }

    @Test
    void 지원하지_않는_시도는_400() {
        when(areaRepository.findById("26")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> areaService.resolve("26", null));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
    }

    @Test
    void 시군구_코드를_시도_자리에_넣으면_400() {
        when(areaRepository.findById("41820")).thenReturn(Optional.of(GAPYEONG));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> areaService.resolve("41820", null));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
    }

    @Test
    void 없는_지역은_400() {
        when(areaRepository.findById("41999")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> areaService.resolve("41", "41999"));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
    }

    @Test
    void 다른_시도의_지역은_400() {
        when(areaRepository.findById("11110")).thenReturn(Optional.of(JONGNO));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> areaService.resolve("41", "11110"));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
    }

    @Test
    void 시도_전체는_시군구로_거르지_않는다() {
        assertTrue(areaService.signguCodes(GYEONGGI).isEmpty());
        verify(areaRepository, never()).findSignguCodes("41");
    }

    @Test
    void 구시군은_이어진_시군구_코드로_거른다() {
        when(areaRepository.findSignguCodes("41820")).thenReturn(List.of("820"));

        assertEquals(List.of("820"), areaService.signguCodes(GAPYEONG));
    }
}
