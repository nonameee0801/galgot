package com.example.galgot.infra.kma;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 기상청 단기예보(VilageFcstInfoService_2.0/getVilageFcst) 클라이언트.
 * 한 번에 numOfRows=1000으로 받으면 3일치(약 900건)가 한 번에 온다.
 * TourApiClient와 같은 이유로 Map으로 파싱한다(오류 시 items 모양이 달라질 수 있다).
 */
@Component
public class KmaClient {

    private static final DateTimeFormatter BASE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int ROWS_PER_PAGE = 1000;
    private static final int MAX_PAGES = 3;

    private final RestClient restClient;
    private final String baseUrl;
    private final String serviceKey;

    public KmaClient(
            @Value("${external.kma.base-url:https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}") String baseUrl,
            @Value("${external.kma.service-key:}") String serviceKey) {
        this.restClient = RestClient.create();
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    public List<KmaItem> fetchShortTermForecast(int nx, int ny, KmaBaseTime base) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new KmaException("KMA_SERVICE_KEY 환경 변수가 비어 있습니다.");
        }

        List<KmaItem> all = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            Map<String, Object> body = callPage(nx, ny, base, page);
            all.addAll(parseItems(body));
            if (all.size() >= toInt(body.get("totalCount"))) {
                break;
            }
        }
        return all;
    }

    private Map<String, Object> callPage(int nx, int ny, KmaBaseTime base, int pageNo) {
        URI uri = URI.create(baseUrl + "/getVilageFcst"
                + "?serviceKey=" + encodeKey(serviceKey)
                + "&pageNo=" + pageNo
                + "&numOfRows=" + ROWS_PER_PAGE
                + "&dataType=JSON"
                + "&base_date=" + base.baseDate().format(BASE_DATE_FORMAT)
                + "&base_time=" + base.baseTime()
                + "&nx=" + nx
                + "&ny=" + ny);

        Map<String, Object> root;
        try {
            root = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // 인증키 오류 등은 JSON이 아니라 XML로 오기도 한다. 요청 주소(키 포함)는 메시지에 넣지 않는다.
            throw new KmaException("기상청 API 호출에 실패했습니다: " + e.getClass().getSimpleName(), e);
        }

        Map<String, Object> response = asMap(root == null ? null : root.get("response"));
        Map<String, Object> header = asMap(response.get("header"));
        String code = String.valueOf(header.get("resultCode"));
        if (!"00".equals(code)) {
            throw new KmaException("기상청 API 오류 응답: " + code + " " + header.get("resultMsg"));
        }
        return asMap(response.get("body"));
    }

    private static List<KmaItem> parseItems(Map<String, Object> body) {
        // 데이터가 없으면 items가 객체가 아니라 ""로 온다
        Map<String, Object> items = asMap(body.get("items"));
        Object item = items.get("item");
        List<Object> rows = item instanceof List<?> list ? new ArrayList<>(list)
                : item instanceof Map<?, ?> ? List.of(item) : Collections.emptyList();

        List<KmaItem> result = new ArrayList<>();
        for (Object row : rows) {
            Map<String, Object> m = asMap(row);
            result.add(new KmaItem(
                    str(m.get("fcstDate")), str(m.get("fcstTime")),
                    str(m.get("category")), str(m.get("fcstValue"))));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> ? (Map<String, Object>) o : Collections.emptyMap();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static int toInt(Object o) {
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 이미 인코딩된 키(%2B 등 포함)는 그대로, 아니면 인코딩해서 쓴다. */
    private static String encodeKey(String key) {
        return key.contains("%") ? key : URLEncoder.encode(key, StandardCharsets.UTF_8);
    }
}
