package com.example.galgot.infra.tourapi;

import java.util.List;

/**
 * TourAPI 상세 응답(detailCommon2 + detailIntro2)을 유형과 상관없는 이름으로 정리한 값. 없는 항목은 null.
 *
 * @param contact   문의처(infocenter 계열, 없으면 tel)
 * @param homepages 홈페이지 글에서 뽑은 http, https 주소(없으면 빈 목록)
 */
public record TourDetailItem(
        String overview,
        String useTime,
        String restDate,
        String useFee,
        String parking,
        String parkingFee,
        String contact,
        List<String> homepages,
        String openPeriod,
        String reservation
) {
}
