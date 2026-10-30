package com.example.galgot.infra.kma;

/** 단기예보 응답의 항목 1개. 값(fcstValue)은 숫자도 문자열로 온다. */
public record KmaItem(String fcstDate, String fcstTime, String category, String fcstValue) {
}
