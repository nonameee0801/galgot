package com.example.galgot.area.dto;

import com.example.galgot.area.domain.Area;

public record AreaResponse(String code, String name) {

    public static AreaResponse from(Area area) {
        return new AreaResponse(area.getCode(), area.getName());
    }
}
