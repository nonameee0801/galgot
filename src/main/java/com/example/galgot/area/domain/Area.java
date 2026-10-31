package com.example.galgot.area.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 추천과 날씨의 지역 단위. 데이터는 V6 마이그레이션의 시드로만 들어오고 앱에서는 읽기만 한다.
 * nx, ny는 이 지역 날씨를 받을 기상청 격자, lat, lng는 그 격자를 정한 대표 지점이다.
 */
@Entity
@Table(name = "areas")
public class Area {

    @Id
    @Column(name = "area_code", length = 5)
    private String code;

    @Column(name = "ldong_regn_cd", nullable = false, length = 5)
    private String ldongRegnCd;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_level", nullable = false, length = 10)
    private AreaLevel level;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "nx", nullable = false)
    private int nx;

    @Column(name = "ny", nullable = false)
    private int ny;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lng", nullable = false)
    private double lng;

    protected Area() {
    }

    /** 테스트와 같이 DB 없이 지역을 만들 때 쓴다. */
    public static Area of(String code, String ldongRegnCd, AreaLevel level, String name, int nx, int ny) {
        Area area = new Area();
        area.code = code;
        area.ldongRegnCd = ldongRegnCd;
        area.level = level;
        area.name = name;
        area.nx = nx;
        area.ny = ny;
        return area;
    }

    public boolean isWholeRegion() {
        return level == AreaLevel.SIDO;
    }

    public String getCode() { return code; }
    public String getLdongRegnCd() { return ldongRegnCd; }
    public AreaLevel getLevel() { return level; }
    public String getName() { return name; }
    public int getNx() { return nx; }
    public int getNy() { return ny; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
}
