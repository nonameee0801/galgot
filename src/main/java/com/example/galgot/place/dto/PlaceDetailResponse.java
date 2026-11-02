package com.example.galgot.place.dto;

import com.example.galgot.place.domain.PlaceDetail;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 장소 상세 정보. 값이 없는 항목은 null, 홈페이지가 없으면 빈 목록.
 * fetchedAt은 TourAPI에서 받은 시각(화면의 "정보는 바뀔 수 있어요" 안내와 함께 쓴다).
 */
public record PlaceDetailResponse(
        Long placeId,
        String overview,
        String useTime,
        String restDate,
        String useFee,
        String parking,
        String parkingFee,
        String contact,
        List<String> homepages,
        String openPeriod,
        String reservation,
        LocalDateTime fetchedAt
) {
    public static PlaceDetailResponse from(PlaceDetail d) {
        return new PlaceDetailResponse(d.getPlaceId(), d.getOverview(), d.getUseTime(), d.getRestDate(),
                d.getUseFee(), d.getParking(), d.getParkingFee(), d.getContact(), d.getHomepages(),
                d.getOpenPeriod(), d.getReservation(), d.getFetchedAt());
    }
}
