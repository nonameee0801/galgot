package com.example.galgot.infra.tourapi;

import java.time.LocalDateTime;

/** TourAPI 응답 한 건을 우리 쪽 타입으로 정리한 값. 빈 문자열은 null로 변환되어 있다. */
public record TourPlaceItem(
        String contentId,
        Integer contentTypeId,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3,
        String title,
        String addr,
        Double lat,
        Double lng,
        String imageUrl,
        String thumbnailUrl,
        String copyrightType,
        LocalDateTime modifiedAt,
        String ldongRegnCd,
        String ldongSignguCd
) {
}
