package com.example.galgot.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import com.example.galgot.infra.tourapi.TourPlaceItem;

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false, unique = true, length = 20)
    private String contentId;

    @Column(name = "content_type_id", nullable = false)
    private Integer contentTypeId;

    @Column(name = "lcls_systm1", length = 10)
    private String lclsSystm1;

    @Column(name = "lcls_systm2", length = 10)
    private String lclsSystm2;

    @Column(name = "lcls_systm3", length = 20)
    private String lclsSystm3;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "addr", length = 500)
    private String addr;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "copyright_type", length = 10)
    private String copyrightType;

    @Column(name = "source_modified_at")
    private LocalDateTime sourceModifiedAt;

    @Column(name = "ldong_regn_cd", length = 5)
    private String ldongRegnCd;

    @Column(name = "ldong_signgu_cd", length = 5)
    private String ldongSignguCd;

    @Enumerated(EnumType.STRING)
    @Column(name = "env", nullable = false, length = 10)
    private Env env = Env.BOTH;

    @Column(name = "env_rule_id")
    private Long envRuleId;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    protected Place() {
    }

    public static Place from(TourPlaceItem item) {
        Place place = new Place();
        place.contentId = item.contentId();
        place.apply(item);
        return place;
    }

    /** TourAPI 원본 값으로 갱신한다. env(실내/야외 분류)는 건드리지 않는다. */
    public void apply(TourPlaceItem item) {
        this.contentTypeId = item.contentTypeId();
        this.lclsSystm1 = item.lclsSystm1();
        this.lclsSystm2 = item.lclsSystm2();
        this.lclsSystm3 = item.lclsSystm3();
        this.title = item.title();
        this.addr = item.addr();
        this.lat = item.lat();
        this.lng = item.lng();
        this.imageUrl = item.imageUrl();
        this.thumbnailUrl = item.thumbnailUrl();
        this.copyrightType = item.copyrightType();
        this.sourceModifiedAt = item.modifiedAt();
        this.ldongRegnCd = item.ldongRegnCd();
        this.ldongSignguCd = item.ldongSignguCd();
        this.syncedAt = LocalDateTime.now();
    }

    /** 분류 규칙 적용 결과를 반영한다. */
    public void classify(Env env, Long envRuleId) {
        this.env = env;
        this.envRuleId = envRuleId;
    }

    public Long getId() { return id; }
    public String getContentId() { return contentId; }
    public Integer getContentTypeId() { return contentTypeId; }
    public String getLclsSystm1() { return lclsSystm1; }
    public String getLclsSystm2() { return lclsSystm2; }
    public String getLclsSystm3() { return lclsSystm3; }
    public String getTitle() { return title; }
    public String getAddr() { return addr; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public String getImageUrl() { return imageUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getCopyrightType() { return copyrightType; }
    public LocalDateTime getSourceModifiedAt() { return sourceModifiedAt; }
    public String getLdongRegnCd() { return ldongRegnCd; }
    public String getLdongSignguCd() { return ldongSignguCd; }
    public Env getEnv() { return env; }
    public Long getEnvRuleId() { return envRuleId; }
    public LocalDateTime getSyncedAt() { return syncedAt; }
}
