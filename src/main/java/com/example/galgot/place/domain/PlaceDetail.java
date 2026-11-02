package com.example.galgot.place.domain;

import com.example.galgot.infra.tourapi.TourDetailItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 장소 상세 정보(V7 place_details). TourAPI 상세 응답을 정리해 장소당 1행으로 저장한 값이다.
 * 홈페이지는 뽑아 낸 주소들을 줄바꿈으로 이어 저장한다(원문은 저장하지 않는다, 개발 기록 2-22).
 */
@Entity
@Table(name = "place_details")
public class PlaceDetail {

    @Id
    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Column(name = "use_time", length = 1000)
    private String useTime;

    @Column(name = "rest_date", length = 1000)
    private String restDate;

    @Column(name = "use_fee", length = 1000)
    private String useFee;

    @Column(name = "parking", length = 1000)
    private String parking;

    @Column(name = "parking_fee", length = 1000)
    private String parkingFee;

    @Column(name = "contact", length = 1000)
    private String contact;

    @Column(name = "homepage", length = 1000)
    private String homepage;

    @Column(name = "open_period", length = 1000)
    private String openPeriod;

    @Column(name = "reservation", length = 1000)
    private String reservation;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    protected PlaceDetail() {
    }

    public static PlaceDetail create(Long placeId) {
        PlaceDetail detail = new PlaceDetail();
        detail.placeId = placeId;
        return detail;
    }

    /** TourAPI에서 새로 받은 값으로 모두 바꾼다(정보가 없어진 항목은 null이 된다). */
    public void apply(TourDetailItem item, LocalDateTime fetchedAt) {
        this.overview = item.overview();
        this.useTime = item.useTime();
        this.restDate = item.restDate();
        this.useFee = item.useFee();
        this.parking = item.parking();
        this.parkingFee = item.parkingFee();
        this.contact = item.contact();
        this.homepage = item.homepages().isEmpty() ? null : String.join("\n", item.homepages());
        this.openPeriod = item.openPeriod();
        this.reservation = item.reservation();
        this.fetchedAt = fetchedAt;
    }

    public List<String> getHomepages() {
        return homepage == null ? List.of() : Arrays.stream(homepage.split("\n")).filter(s -> !s.isBlank()).toList();
    }

    public Long getPlaceId() { return placeId; }
    public String getOverview() { return overview; }
    public String getUseTime() { return useTime; }
    public String getRestDate() { return restDate; }
    public String getUseFee() { return useFee; }
    public String getParking() { return parking; }
    public String getParkingFee() { return parkingFee; }
    public String getContact() { return contact; }
    public String getOpenPeriod() { return openPeriod; }
    public String getReservation() { return reservation; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
}
