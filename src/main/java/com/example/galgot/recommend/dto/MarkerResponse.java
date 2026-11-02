package com.example.galgot.recommend.dto;

import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;

/** 지도 마커용 가벼운 장소 정보. 좌표가 있는 장소만 만든다. */
public record MarkerResponse(Long id, String title, double lat, double lng, Env env) {

    public static MarkerResponse from(Place p) {
        return new MarkerResponse(p.getId(), p.getTitle(), p.getLat(), p.getLng(), p.getEnv());
    }
}
