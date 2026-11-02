package com.example.galgot.place.dto;

import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;

/**
 * 장소. areaCode는 장소가 속한 구·시군 지역 코드로, 상세(GET /api/places/{id})에서만 채우고 목록에서는 null이다
 * (상세를 주소로 바로 열었을 때 화면의 지역 조건을 그 장소에 맞추는 데 쓴다).
 */
public record PlaceResponse(
        Long id,
        String contentId,
        Integer contentTypeId,
        String title,
        String addr,
        Double lat,
        Double lng,
        String imageUrl,
        String thumbnailUrl,
        String copyrightType,
        String ldongRegnCd,
        String ldongSignguCd,
        Env env,
        String areaCode
) {
    public static PlaceResponse from(Place p) {
        return from(p, null);
    }

    public static PlaceResponse from(Place p, String areaCode) {
        return new PlaceResponse(
                p.getId(),
                p.getContentId(),
                p.getContentTypeId(),
                p.getTitle(),
                p.getAddr(),
                p.getLat(),
                p.getLng(),
                p.getImageUrl(),
                p.getThumbnailUrl(),
                p.getCopyrightType(),
                p.getLdongRegnCd(),
                p.getLdongSignguCd(),
                p.getEnv(),
                areaCode
        );
    }
}
