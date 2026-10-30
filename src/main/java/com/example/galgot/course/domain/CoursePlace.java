package com.example.galgot.course.domain;

import com.example.galgot.place.domain.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 코스에 담긴 장소 한 건. 몇 일차의 몇 번째 방문인지를 가진다. */
@Entity
@Table(name = "course_places")
public class CoursePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "day_no", nullable = false)
    private int dayNo;

    @Column(name = "visit_order", nullable = false)
    private int visitOrder;

    @Column(name = "memo", length = 500)
    private String memo;

    protected CoursePlace() {
    }

    CoursePlace(Course course, Place place, int dayNo, int visitOrder, String memo) {
        this.course = course;
        this.place = place;
        this.dayNo = dayNo;
        this.visitOrder = visitOrder;
        this.memo = memo;
    }

    public void moveTo(int dayNo, int visitOrder) {
        this.dayNo = dayNo;
        this.visitOrder = visitOrder;
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public Place getPlace() { return place; }
    public int getDayNo() { return dayNo; }
    public int getVisitOrder() { return visitOrder; }
    public String getMemo() { return memo; }
}
