package com.example.galgot.area.controller;

import com.example.galgot.area.dto.AreaResponse;
import com.example.galgot.area.service.AreaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /api/areas?region=41 : 시도 안의 구·시군 목록(가나다순). 로그인 없이 조회. */
@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @GetMapping
    public List<AreaResponse> list(@RequestParam(name = "region", defaultValue = "11") String region) {
        return areaService.listSigungu(region).stream().map(AreaResponse::from).toList();
    }
}
