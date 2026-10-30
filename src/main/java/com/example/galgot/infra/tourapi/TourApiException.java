package com.example.galgot.infra.tourapi;

/** TourAPI 호출 실패(인증, 호출 한도 초과, 응답 형식 오류 등). */
public class TourApiException extends RuntimeException {

    public TourApiException(String message) {
        super(message);
    }

    public TourApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
