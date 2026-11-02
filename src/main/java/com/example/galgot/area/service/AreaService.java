package com.example.galgot.area.service;

import com.example.galgot.area.domain.Area;
import com.example.galgot.area.domain.AreaLevel;
import com.example.galgot.area.repository.AreaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * 요청의 region(시도)과 area(구·시군)를 실제 지역으로 바꾼다.
 * - area가 없으면 시도 전체 지역
 * - area가 있으면 그 시도에 속한 구·시군이어야 한다(아니면 400)
 */
@Service
@Transactional(readOnly = true)
public class AreaService {

    private final AreaRepository areaRepository;

    public AreaService(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    public Area resolve(String region, String areaCode) {
        String regn = region == null ? "" : region.trim();
        if (areaCode == null || areaCode.isBlank()) {
            return areaRepository.findById(regn)
                    .filter(Area::isWholeRegion)
                    .orElseThrow(() -> badRequest("아직 지원하지 않는 지역입니다: " + regn));
        }
        String code = areaCode.trim();
        return areaRepository.findById(code)
                .filter(a -> a.getLevel() == AreaLevel.SIGUNGU && a.getLdongRegnCd().equals(regn))
                .orElseThrow(() -> badRequest("이 시도에 없는 지역입니다: " + regn + "/" + code));
    }

    /** 시도 안의 구·시군 목록(가나다순). */
    public List<Area> listSigungu(String region) {
        String regn = region == null ? "" : region.trim();
        return areaRepository.findAllByLdongRegnCdAndLevelOrderByNameAsc(regn, AreaLevel.SIGUNGU);
    }

    /** 지역에 속한 장소의 시군구 코드. 시도 전체면 빈 목록(시군구로 거르지 않음). */
    public List<String> signguCodes(Area area) {
        return area.isWholeRegion() ? List.of() : areaRepository.findSignguCodes(area.getCode());
    }

    /**
     * 장소의 (시도, 시군구 코드)가 속한 구·시군 지역 코드(예: 의정부 150 → 41150, 수원 팔달구 115 → 41110).
     * 코드가 비었거나 연결표에 없으면 빈 값(새로 생긴 시군구 코드 등).
     */
    public Optional<String> areaCodeOf(String ldongRegnCd, String ldongSignguCd) {
        if (ldongRegnCd == null || ldongRegnCd.isBlank() || ldongSignguCd == null || ldongSignguCd.isBlank()) {
            return Optional.empty();
        }
        return areaRepository.findAreaCodes(ldongRegnCd.trim(), ldongSignguCd.trim()).stream().findFirst();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
