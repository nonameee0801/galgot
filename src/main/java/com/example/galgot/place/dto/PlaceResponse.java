package com.example.galgot.place.dto;

import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;

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
        Env env
) {
    public static PlaceResponse from(Place p) {
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
                p.getEnv()
        );
    }
}
