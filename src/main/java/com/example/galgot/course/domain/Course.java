package com.example.galgot.course.domain;

import com.example.galgot.place.domain.Place;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 코스를 만든 회원 ID (users.id) */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "ldong_regn_cd", length = 5)
    private String ldongRegnCd;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    /** true면 공유 링크(shareCode)로 누구나 볼 수 있다 */
    @Column(name = "is_public", nullable = false)
    private boolean publicCourse;

    @Column(name = "share_code", nullable = false, unique = true, length = 36)
    private String shareCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNo ASC, visitOrder ASC, id ASC")
    private List<CoursePlace> places = new ArrayList<>();

    protected Course() {
    }

    public static Course create(Long userId, String title, String ldongRegnCd,
                                LocalDate travelDate, boolean publicCourse) {
        Course course = new Course();
        course.userId = userId;
        course.title = title;
        course.ldongRegnCd = ldongRegnCd;
        course.travelDate = travelDate;
        course.publicCourse = publicCourse;
        course.shareCode = UUID.randomUUID().toString();
        course.createdAt = LocalDateTime.now();
        return course;
    }

    public void update(String title, String ldongRegnCd, LocalDate travelDate, boolean publicCourse) {
        this.title = title;
        this.ldongRegnCd = ldongRegnCd;
        this.travelDate = travelDate;
        this.publicCourse = publicCourse;
    }

    /** 해당 일차의 맨 뒤에 장소를 추가한다. 방문 순서는 그 일차의 최댓값 + 1. */
    public CoursePlace addPlace(Place place, int dayNo, String memo) {
        int nextOrder = places.stream()
                .filter(cp -> cp.getDayNo() == dayNo)
                .mapToInt(CoursePlace::getVisitOrder)
                .max()
                .orElse(0) + 1;
        CoursePlace coursePlace = new CoursePlace(this, place, dayNo, nextOrder, memo);
        places.add(coursePlace);
        return coursePlace;
    }

    /**
     * 항목을 빼고, 그 일차에 남은 장소의 방문 순서를 1부터 다시 매긴다(1, 3, 4처럼 빈 번호가 남지 않게).
     * @return 해당 항목을 찾아 삭제했으면 true
     */
    public boolean removePlace(Long coursePlaceId) {
        if (coursePlaceId == null) {
            return false;
        }
        CoursePlace target = places.stream()
                .filter(cp -> coursePlaceId.equals(cp.getId()))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return false;
        }
        places.remove(target);
        renumber(target.getDayNo());
        return true;
    }

    /** 그 일차의 장소를 지금 순서대로 1, 2, 3…으로 다시 매긴다. 다른 일차는 건드리지 않는다. */
    private void renumber(int dayNo) {
        List<CoursePlace> day = places.stream()
                .filter(cp -> cp.getDayNo() == dayNo)
                .sorted(Comparator.comparingInt(CoursePlace::getVisitOrder)
                        .thenComparing(CoursePlace::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (int i = 0; i < day.size(); i++) {
            day.get(i).moveTo(dayNo, i + 1);
        }
    }

    /** 장소가 담긴 일차 수(서로 다른 일차의 개수). 비어 있으면 0. */
    public int dayCount() {
        return (int) places.stream().mapToInt(CoursePlace::getDayNo).distinct().count();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getLdongRegnCd() { return ldongRegnCd; }
    public LocalDate getTravelDate() { return travelDate; }
    public boolean isPublicCourse() { return publicCourse; }
    public String getShareCode() { return shareCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<CoursePlace> getPlaces() { return places; }
}
