package com.example.galgot.infra.tourapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 한국관광공사 TourAPI(KorService2) 클라이언트.
 * JSON 파싱은 Map으로 처리한다(응답이 없을 때 items가 객체가 아니라 ""로 오는 경우 때문).
 */
@Component
public class TourApiClient {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RestClient restClient;
    private final String baseUrl;
    private final String serviceKey;

    public TourApiClient(@Value("${external.tourapi.base-url}") String baseUrl,
                         @Value("${external.tourapi.service-key:}") String serviceKey) {
        this.restClient = RestClient.create();
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    /** 지역(법정동 시도 코드) + 콘텐츠 유형으로 장소 목록을 한 페이지 조회한다. */
    public TourPage fetchAreaBasedList(String ldongRegnCd, int contentTypeId, int pageNo, int numOfRows) {
        Map<String, Object> body = call("/areaBasedList2", "&numOfRows=" + numOfRows
                + "&pageNo=" + pageNo
                + "&lDongRegnCd=" + ldongRegnCd
                + "&contentTypeId=" + contentTypeId);
        int totalCount = toInt(body.get("totalCount"));

        List<TourPlaceItem> items = new ArrayList<>();
        for (Map<String, Object> raw : extractItems(body.get("items"))) {
            TourPlaceItem item = toItem(raw);
            if (item != null) {
                items.add(item);
            }
        }
        return new TourPage(totalCount, items);
    }

    /**
     * 장소 하나의 상세 정보. 공통 정보(detailCommon2)와 유형별 소개(detailIntro2)를 차례로 불러 정리한다(호출 2번).
     * 유형별 소개가 없는 장소도 있다(그때는 공통 정보만 정리된다).
     */
    public TourDetailItem fetchDetail(String contentId, int contentTypeId) {
        String id = URLEncoder.encode(contentId, StandardCharsets.UTF_8);
        Map<String, Object> common = firstItem(call("/detailCommon2", "&contentId=" + id));
        Map<String, Object> intro = firstItem(call("/detailIntro2", "&contentId=" + id + "&contentTypeId=" + contentTypeId));
        return TourDetailParser.parse(common, intro);
    }

    /** TourAPI 한 번 호출. 성공 코드(0000)가 아니면 TourApiException. 응답의 body를 돌려준다. */
    private Map<String, Object> call(String path, String params) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new TourApiException("TOURAPI_SERVICE_KEY 환경 변수가 비어 있습니다.");
        }

        URI uri = URI.create(baseUrl + path
                + "?serviceKey=" + encodeKey(serviceKey)
                + "&MobileOS=ETC&MobileApp=galgot&_type=json"
                + params);

        Map<String, Object> root;
        try {
            root = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new TourApiException("TourAPI 호출 또는 응답 해석에 실패했습니다: " + e.getMessage(), e);
        }

        Map<String, Object> response = asMap(root == null ? null : root.get("response"));
        Map<String, Object> header = asMap(response.get("header"));
        String resultCode = str(header.get("resultCode"));
        if (!"0000".equals(resultCode)) {
            throw new TourApiException("TourAPI 오류 resultCode=" + resultCode
                    + ", resultMsg=" + str(header.get("resultMsg")));
        }

        return asMap(response.get("body"));
    }

    /** 상세 API의 item은 하나다. 없으면 빈 Map. */
    private static Map<String, Object> firstItem(Map<String, Object> body) {
        List<Map<String, Object>> items = extractItems(body.get("items"));
        return items.isEmpty() ? Collections.emptyMap() : items.get(0);
    }

    /** 인증키가 이미 인코딩된 값(%포함)이면 그대로, 디코딩된 값이면 인코딩해서 쓴다. */
    private static String encodeKey(String key) {
        return key.contains("%") ? key : URLEncoder.encode(key, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractItems(Object itemsObj) {
        // 결과가 없으면 items가 "" 로 온다
        if (!(itemsObj instanceof Map<?, ?> itemsMap)) {
            return Collections.emptyList();
        }
        Object item = itemsMap.get("item");
        if (item instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    result.add((Map<String, Object>) m);
                }
            }
            return result;
        }
        if (item instanceof Map<?, ?> single) {
            return List.of((Map<String, Object>) single);
        }
        return Collections.emptyList();
    }

    private static TourPlaceItem toItem(Map<String, Object> raw) {
        String contentId = str(raw.get("contentid"));
        String title = str(raw.get("title"));
        if (contentId == null || title == null) {
            return null;
        }
        // mapx = 경도(lng), mapy = 위도(lat)
        return new TourPlaceItem(
                contentId,
                toInt(raw.get("contenttypeid")),
                str(raw.get("lclsSystm1")),
                str(raw.get("lclsSystm2")),
                str(raw.get("lclsSystm3")),
                title,
                str(raw.get("addr1")),
                toDouble(raw.get("mapy")),
                toDouble(raw.get("mapx")),
                str(raw.get("firstimage")),
                str(raw.get("firstimage2")),
                str(raw.get("cpyrhtDivCd")),
                toTime(raw.get("modifiedtime")),
                str(raw.get("lDongRegnCd")),
                str(raw.get("lDongSignguCd"))
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Collections.emptyMap();
    }

    /** 빈 문자열은 null로 */
    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static int toInt(Object o) {
        String s = str(o);
        if (s == null) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Double toDouble(Object o) {
        String s = str(o);
        if (s == null) {
            return null;
        }
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDateTime toTime(Object o) {
        String s = str(o);
        if (s == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, TIME_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }
}
