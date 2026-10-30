package com.example.galgot.infra.kma;

/** 기상청 API 호출 또는 응답 해석에 실패했을 때. 메시지에 인증키가 들어가지 않도록 주의한다. */
public class KmaException extends RuntimeException {

    public KmaException(String message) {
        super(message);
    }

    public KmaException(String message, Throwable cause) {
        super(message, cause);
    }
}
