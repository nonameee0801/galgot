package com.example.galgot.course.domain;

import com.example.galgot.infra.tourapi.TourPlaceItem;
import com.example.galgot.place.domain.Place;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.example.galgot.course.domain.Course;
import com.example.galgot.course.domain.CoursePlace;

class CourseTest {

    private static Place place(String contentId, String title) {
        return Place.from(new TourPlaceItem(contentId, 12, null, null, null, title, "서울",
                37.5, 127.0, null, null, null, null, "11", "110"));
    }

    private static Course course() {
        return Course.create(1L, "서울 하루 코스", "11", null, false);
    }

    @Test
    void 같은_일차에_추가하면_방문_순서가_1_2_3으로_늘어난다() {
        Course course = course();

        CoursePlace first = course.addPlace(place("1", "경복궁"), 1, null);
        CoursePlace second = course.addPlace(place("2", "국립중앙박물관"), 1, null);
        CoursePlace third = course.addPlace(place("3", "남산타워"), 1, "야경");

        assertEquals(1, first.getVisitOrder());
        assertEquals(2, second.getVisitOrder());
        assertEquals(3, third.getVisitOrder());
    }

    @Test
    void 방문_순서는_일차별로_따로_센다() {
        Course course = course();

        course.addPlace(place("1", "경복궁"), 1, null);
        course.addPlace(place("2", "국립중앙박물관"), 1, null);
        CoursePlace dayTwoFirst = course.addPlace(place("3", "북한산"), 2, null);

        assertEquals(2, dayTwoFirst.getDayNo());
        assertEquals(1, dayTwoFirst.getVisitOrder());
    }

    @Test
    void 존재하지_않는_항목을_삭제하면_false를_돌려준다() {
        Course course = course();
        course.addPlace(place("1", "경복궁"), 1, null);

        assertFalse(course.removePlace(999L));
        assertFalse(course.removePlace(null));
        assertEquals(1, course.getPlaces().size());
    }

    @Test
    void 공유_코드는_코스마다_다르게_만들어진다() {
        assertFalse(course().getShareCode().equals(course().getShareCode()));
    }
}
