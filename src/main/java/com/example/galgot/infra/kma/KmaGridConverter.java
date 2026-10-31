package com.example.galgot.infra.kma;

/**
 * 위경도 → 기상청 단기예보 격자(nx, ny) 변환.
 * 기상청 활용가이드의 람베르트 정각원추도법(LCC) 공식을 그대로 옮겼다. DB와 네트워크 없이 계산만 한다.
 * 지역 시드(V6)의 격자 값이 위경도와 맞는지 테스트에서 이 클래스로 교차 확인한다.
 */
public final class KmaGridConverter {

    public record Grid(int nx, int ny) {
    }

    private static final double EARTH_RADIUS_KM = 6371.00877;
    private static final double GRID_KM = 5.0;
    private static final double STANDARD_LAT1 = 30.0;
    private static final double STANDARD_LAT2 = 60.0;
    private static final double ORIGIN_LNG = 126.0;
    private static final double ORIGIN_LAT = 38.0;
    private static final double ORIGIN_X = 43;
    private static final double ORIGIN_Y = 136;

    private static final double DEG = Math.PI / 180.0;
    private static final double RE = EARTH_RADIUS_KM / GRID_KM;
    private static final double SN;
    private static final double SF;
    private static final double RO;

    static {
        double slat1 = STANDARD_LAT1 * DEG;
        double slat2 = STANDARD_LAT2 * DEG;
        double olat = ORIGIN_LAT * DEG;
        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        SN = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        SF = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), SN) * Math.cos(slat1) / SN;
        RO = RE * SF / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), SN);
    }

    private KmaGridConverter() {
    }

    public static Grid toGrid(double lat, double lng) {
        double ra = RE * SF / Math.pow(Math.tan(Math.PI * 0.25 + lat * DEG * 0.5), SN);
        double theta = lng * DEG - ORIGIN_LNG * DEG;
        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }
        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }
        theta *= SN;
        int nx = (int) Math.floor(ra * Math.sin(theta) + ORIGIN_X + 0.5);
        int ny = (int) Math.floor(RO - ra * Math.cos(theta) + ORIGIN_Y + 0.5);
        return new Grid(nx, ny);
    }
}
