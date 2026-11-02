package com.example.galgot.course.domain;

import com.example.galgot.infra.tourapi.TourPlaceItem;
import com.example.galgot.place.domain.Place;
import com.example.galgot.course.dto.CourseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    /** DB에 저장하지 않으면 id가 비어 있어, 빼기 테스트에서는 id를 직접 넣는다. */
    private static CoursePlace add(Course course, long id, String title, int dayNo) {
        CoursePlace cp = course.addPlace(place(String.valueOf(id), title), dayNo, null);
        ReflectionTestUtils.setField(cp, "id", id);
        return cp;
    }

    private static List<String> titlesOfDay(Course course, int dayNo) {
        return course.getPlaces().stream()
                .filter(cp -> cp.getDayNo() == dayNo)
                .sorted((a, b) -> Integer.compare(a.getVisitOrder(), b.getVisitOrder()))
                .map(cp -> cp.getVisitOrder() + ":" + cp.getPlace().getTitle())
                .toList();
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

    @Test
    void 장소를_빼면_그_일차의_방문_순서를_1부터_다시_매긴다() {
        Course course = course();
        add(course, 10L, "경복궁", 1);
        add(course, 11L, "국립중앙박물관", 1);
        add(course, 12L, "남산타워", 1);
        add(course, 13L, "청계천", 1);

        assertTrue(course.removePlace(11L));

        assertEquals(List.of("1:경복궁", "2:남산타워", "3:청계천"), titlesOfDay(course, 1));
    }

    @Test
    void 장소를_빼도_다른_일차의_순서는_그대로다() {
        Course course = course();
        add(course, 10L, "경복궁", 1);
        add(course, 11L, "국립중앙박물관", 1);
        add(course, 20L, "북한산", 2);
        add(course, 21L, "도봉산", 2);

        course.removePlace(10L);

        assertEquals(List.of("1:국립중앙박물관"), titlesOfDay(course, 1));
        assertEquals(List.of("1:북한산", "2:도봉산"), titlesOfDay(course, 2));
    }

    @Test
    void 일차의_마지막_장소를_빼면_그_일차는_사라진다() {
        Course course = course();
        add(course, 10L, "경복궁", 1);
        add(course, 20L, "북한산", 2);
        assertEquals(2, course.dayCount());

        course.removePlace(20L);

        assertEquals(1, course.dayCount());
    }

    @Test
    void 목록_응답에는_장소_수와_일차_수가_들어간다() {
        Course course = course();
        add(course, 10L, "경복궁", 1);
        add(course, 11L, "국립중앙박물관", 1);
        add(course, 20L, "북한산", 2);

        CourseResponse res = CourseResponse.from(course);

        assertEquals(3, res.placeCount());
        assertEquals(2, res.dayCount());
        assertEquals(0, CourseResponse.from(course()).dayCount());
    }

    @Test
    void 복사본은_장소와_일차_순서_메모를_그대로_옮기고_주인과_공유_코드는_새로_정한다() {
        Course source = Course.create(1L, "가평군 1박2일 11/3", "41", java.time.LocalDate.of(2026, 11, 3), true);
        add(source, 20L, "북한산", 2);
        add(source, 10L, "경복궁", 1);
        CoursePlace memo = source.addPlace(place("11", "국립중앙박물관"), 1, "오전에");
        ReflectionTestUtils.setField(memo, "id", 11L);

        Course copy = Course.copyOf(source, 2L);

        assertEquals(2L, copy.getUserId());
        assertEquals("가평군 1박2일 11/3", copy.getTitle());
        assertEquals("41", copy.getLdongRegnCd());
        assertEquals(source.getTravelDate(), copy.getTravelDate());
        assertFalse(copy.getShareCode().equals(source.getShareCode()));
        assertEquals(List.of("1:경복궁", "2:국립중앙박물관"), titlesOfDay(copy, 1));
        assertEquals(List.of("1:북한산"), titlesOfDay(copy, 2));
        assertEquals("오전에", copy.getPlaces().stream()
                .filter(cp -> cp.getPlace().getTitle().equals("국립중앙박물관")).findFirst().orElseThrow().getMemo());
        // 복사본의 항목은 원래 코스의 항목과 다른 객체다(따로 움직인다)
        assertTrue(copy.getPlaces().stream().allMatch(cp -> cp.getCourse() == copy));
        assertEquals(3, source.getPlaces().size());
    }
}
