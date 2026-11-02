package com.example.galgot.place.controller;

import com.example.galgot.place.domain.Env;
import com.example.galgot.place.dto.PageResponse;
import com.example.galgot.place.dto.PlaceDetailResponse;
import com.example.galgot.place.dto.PlaceResponse;
import com.example.galgot.place.service.PlaceDetailService;
import com.example.galgot.place.service.PlaceQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceQueryService queryService;
    private final PlaceDetailService detailService;

    public PlaceController(PlaceQueryService queryService, PlaceDetailService detailService) {
        this.queryService = queryService;
        this.detailService = detailService;
    }

    /**
     * 장소 목록 조회. 모든 파라미터는 선택이다.
     * 예) /api/places?region=11&env=INDOOR&keyword=박물관&page=0&size=20
     */
    @GetMapping
    public PageResponse<PlaceResponse> list(
            @RequestParam(name = "region", required = false) String region,
            @RequestParam(name = "signgu", required = false) String signgu,
            @RequestParam(name = "env", required = false) Env env,
            @RequestParam(name = "type", required = false) Integer contentTypeId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        return PageResponse.of(
                queryService.search(region, signgu, env, contentTypeId, keyword, page, size),
                PlaceResponse::from);
    }

    @GetMapping("/{id}")
    public PlaceResponse detail(@PathVariable("id") Long id) {
        return queryService.find(id)
                .map(PlaceResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."));
    }

    /**
     * GET /api/places/{id}/detail
     * 장소 상세 정보(소개, 이용시간, 쉬는 날, 이용요금, 주차, 문의처, 홈페이지 등). 처음 열면 TourAPI에서 받아 저장하고
     * 30일 동안 다시 쓴다. 없는 장소는 404, 받지 못했고 저장된 값도 없으면 503. 로그인 없이 조회.
     */
    @GetMapping("/{id}/detail")
    public PlaceDetailResponse detailInfo(@PathVariable("id") Long id) {
        return detailService.get(id);
    }
}
