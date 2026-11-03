package com.example.galgot.course.service;

import com.example.galgot.course.domain.Course;
import com.example.galgot.course.domain.CoursePlace;
import com.example.galgot.course.dto.AddPlaceRequest;
import com.example.galgot.course.dto.CourseDetailResponse;
import com.example.galgot.course.dto.CourseRequest;
import com.example.galgot.course.dto.CourseResponse;
import com.example.galgot.course.dto.ReorderRequest;
import com.example.galgot.course.repository.CourseRepository;
import com.example.galgot.infra.tourapi.TourPlaceItem;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.repository.PlaceRepository;
import com.example.galgot.user.domain.User;
import com.example.galgot.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 공유 링크 보기, 공유받은 코스를 내 코스로 저장하기(기획서 18장 E)와 나만의 코스에 넣기(18장 D). */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    /** 오늘은 2026-11-03(한국 시간)으로 고정 */
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @Mock
    CourseRepository courseRepository;
    @Mock
    PlaceRepository placeRepository;
    @Mock
    UserRepository userRepository;

    CourseService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 11, 3, 9, 0, 0, 0, SEOUL).toInstant(), SEOUL);
        service = new CourseService(courseRepository, placeRepository, userRepository, clock);
        // 회원은 있고 코스는 0개인 것이 기본(코스 수 상한, 기획서 18장 G). 상한을 보지 않는 테스트에서는 쓰이지 않는다
        org.mockito.Mockito.lenient().when(userRepository.findByIdForUpdate(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(Optional.of(User.create("여행자", null)));
    }

    private static Place savedPlace(long id, String title) {
        Place p = Place.from(new TourPlaceItem(String.valueOf(id), 12, null, null, null, title, "서울",
                37.5, 127.0, null, null, null, null, "11", "110"));
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private static CourseRequest newCourse(LocalDate date, Integer tripDays, Long placeId, Integer dayNo) {
        return new CourseRequest("나만의 코스 11/3", "41", date, null, tripDays, placeId, dayNo);
    }

    /** 회원 1의 코스(id 10). */
    private Course ownedCourse(int tripDays) {
        Course course = Course.create(1L, "내 코스", "11", TODAY, tripDays, false);
        ReflectionTestUtils.setField(course, "id", 10L);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        return course;
    }

    // ---- 공유 링크 (18장 E) ----

    @Test
    void 비공개로_만든_코스도_공유_링크로_볼_수_있다() {
        Course privateCourse = Course.create(1L, "서울 하루 코스", "11", null, false);
        when(courseRepository.findByShareCode("code-1")).thenReturn(Optional.of(privateCourse));

        CourseDetailResponse res = service.getShared("code-1");

        assertEquals("서울 하루 코스", res.title());
    }

    @Test
    void 없는_공유_링크는_404() {
        when(courseRepository.findByShareCode("nope")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.getShared("nope"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void 공유받은_코스를_저장하면_저장한_사람의_새_코스가_생긴다() {
        Course source = Course.create(1L, "가평군 당일치기 11/3", "41", null, false);
        when(courseRepository.findByShareCode("code-1")).thenReturn(Optional.of(source));

        CourseDetailResponse res = service.copyShared(7L, "code-1");

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).saveAndFlush(saved.capture());
        assertEquals(7L, saved.getValue().getUserId());
        assertEquals("가평군 당일치기 11/3", res.title());
        assertNotEquals(source.getShareCode(), res.shareCode());
    }

    @Test
    void 없는_링크는_저장하지_않고_404() {
        when(courseRepository.findByShareCode("nope")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.copyShared(7L, "nope"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(courseRepository, never()).saveAndFlush(any());
    }

    // ---- 나만의 코스에 넣기 (18장 D) ----

    @Test
    void 첫_장소와_함께_코스를_만들면_한_번에_저장되고_시도는_장소의_시도다() {
        when(placeRepository.findById(5L)).thenReturn(Optional.of(savedPlace(5L, "경복궁")));

        CourseDetailResponse res = service.create(1L, newCourse(TODAY, 2, 5L, 2));

        verify(courseRepository).saveAndFlush(any(Course.class));
        assertEquals(2, res.tripDays());
        assertEquals("11", res.ldongRegnCd()); // 요청의 41이 아니라 장소의 시도
        assertEquals(1, res.places().size());
        assertEquals(2, res.places().get(0).dayNo());
        assertEquals(1, res.places().get(0).visitOrder());
    }

    @Test
    void 일정을_보내지_않으면_당일치기로_만든다() {
        CourseDetailResponse res = service.create(1L, newCourse(TODAY.plusDays(10), null, null, null));

        assertEquals(1, res.tripDays());
        assertEquals("41", res.ldongRegnCd());
    }

    @Test
    void 출발일이_없거나_지난_날짜면_400이고_저장하지_않는다() {
        ResponseStatusException none = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, newCourse(null, 1, null, null)));
        ResponseStatusException past = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, newCourse(TODAY.minusDays(1), 1, null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, none.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, past.getStatusCode());
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void 첫_장소의_일차가_일정_밖이면_400이고_저장하지_않는다() {
        when(placeRepository.findById(5L)).thenReturn(Optional.of(savedPlace(5L, "경복궁")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, newCourse(TODAY, 1, 5L, 2)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void 없는_장소로_코스를_만들면_404() {
        when(placeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, newCourse(TODAY, 1, 99L, 1)));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void 같은_장소를_또_담으면_409이고_저장하지_않는다() {
        Course course = ownedCourse(2);
        Place place = savedPlace(5L, "경복궁");
        course.addPlace(place, 1, null);
        when(placeRepository.findById(5L)).thenReturn(Optional.of(place));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.addPlace(1L, 10L, new AddPlaceRequest(5L, 2, null)));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void 일정_밖_일차에_담으면_400() {
        ownedCourse(1);
        when(placeRepository.findById(5L)).thenReturn(Optional.of(savedPlace(5L, "경복궁")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.addPlace(1L, 10L, new AddPlaceRequest(5L, 2, null)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void 목록을_장소와_함께_부르면_코스마다_이미_담겼는지_알려_준다() {
        Course has = Course.create(1L, "담긴 코스", "11", TODAY, 1, false);
        has.addPlace(savedPlace(5L, "경복궁"), 1, null);
        Course empty = Course.create(1L, "빈 코스", "11", TODAY, 1, false);
        when(courseRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(has, empty));

        List<CourseResponse> withPlace = service.list(1L, 5L);
        List<CourseResponse> plain = service.list(1L, null);

        assertEquals(Boolean.TRUE, withPlace.get(0).containsPlace());
        assertEquals(Boolean.FALSE, withPlace.get(1).containsPlace());
        assertEquals(null, plain.get(0).containsPlace());
    }

    @Test
    void 장소가_든_일차보다_일정을_줄이면_400() {
        Course course = ownedCourse(2);
        course.addPlace(savedPlace(5L, "경복궁"), 2, null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, 10L, new CourseRequest("내 코스", "11", TODAY, null, 1, null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(2, course.getTripDays());
    }

    @Test
    void 일정을_보내지_않고_수정하면_일정은_그대로다() {
        Course course = ownedCourse(3);

        CourseDetailResponse res = service.update(1L, 10L,
                new CourseRequest("새 이름", "11", TODAY.minusDays(5), null, null, null, null));

        assertEquals(3, res.tripDays());
        assertEquals("새 이름", course.getTitle());
    }

    @Test
    void 순서_바꾸기로_일정_밖_일차에_옮기면_400() {
        Course course = ownedCourse(2);
        CoursePlace cp = course.addPlace(savedPlace(5L, "경복궁"), 1, null);
        ReflectionTestUtils.setField(cp, "id", 50L);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.reorder(1L, 10L, new ReorderRequest(List.of(new ReorderRequest.Item(50L, 3, 1)))));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(1, cp.getDayNo());
    }

    /** 회원 userId의 코스(id). */
    private static Course courseOf(long id, long userId) {
        Course course = Course.create(userId, "코스 " + id, "11", TODAY, 1, false);
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    @Test
    void 내_코스_여러_개를_한_번에_지우고_겹친_id는_하나로_본다() {
        Course a = courseOf(10L, 1L);
        Course b = courseOf(11L, 1L);
        when(courseRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(a, b));

        service.deleteAll(1L, List.of(10L, 11L, 10L));

        verify(courseRepository).delete(a);
        verify(courseRepository).delete(b);
    }

    @Test
    void 남의_코스가_섞이면_403이고_하나도_지우지_않는다() {
        when(courseRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(courseOf(10L, 1L), courseOf(20L, 2L)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.deleteAll(1L, List.of(10L, 20L)));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void 없는_코스가_섞이면_404이고_하나도_지우지_않는다() {
        when(courseRepository.findAllById(List.of(10L, 99L))).thenReturn(List.of(courseOf(10L, 1L)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.deleteAll(1L, List.of(10L, 99L)));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void 지울_코스가_없거나_너무_많으면_400() {
        List<Long> tooMany = java.util.stream.LongStream.rangeClosed(1, CourseService.MAX_BULK_DELETE + 1).boxed().toList();

        ResponseStatusException empty = assertThrows(ResponseStatusException.class,
                () -> service.deleteAll(1L, List.of()));
        ResponseStatusException over = assertThrows(ResponseStatusException.class,
                () -> service.deleteAll(1L, tooMany));

        assertEquals(HttpStatus.BAD_REQUEST, empty.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, over.getStatusCode());
        verify(courseRepository, never()).findAllById(any());
    }

    @Test
    void 최대_개수까지는_한_번에_지운다() {
        List<Long> ids = java.util.stream.LongStream.rangeClosed(1, CourseService.MAX_BULK_DELETE).boxed().toList();
        List<Course> courses = ids.stream().map(id -> courseOf(id, 1L)).toList();
        when(courseRepository.findAllById(ids)).thenReturn(courses);

        service.deleteAll(1L, ids);

        verify(courseRepository, org.mockito.Mockito.times(CourseService.MAX_BULK_DELETE)).delete(any());
    }

    @Test
    void 코스가_상한보다_하나_적으면_만들_수_있다() {
        when(courseRepository.countByUserId(1L)).thenReturn((long) CourseService.MAX_COURSES_PER_USER - 1);

        service.create(1L, newCourse(TODAY, 1, null, null));

        verify(courseRepository).saveAndFlush(any(Course.class));
    }

    @Test
    void 코스가_상한이면_만들기는_409이고_저장하지_않는다() {
        when(courseRepository.countByUserId(1L)).thenReturn((long) CourseService.MAX_COURSES_PER_USER);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, newCourse(TODAY, 1, null, null)));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void 코스가_상한이면_공유_코스_복사도_409() {
        Course shared = Course.create(2L, "남의 코스", "11", TODAY, 1, false);
        when(courseRepository.findByShareCode("abc")).thenReturn(Optional.of(shared));
        when(courseRepository.countByUserId(1L)).thenReturn((long) CourseService.MAX_COURSES_PER_USER);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.copyShared(1L, "abc"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void 코스를_세기_전에_회원_행을_잠근다() {
        service.create(1L, newCourse(TODAY, 1, null, null));

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(userRepository, courseRepository);
        order.verify(userRepository).findByIdForUpdate(1L);
        order.verify(courseRepository).countByUserId(1L);
    }

    @Test
    void 회원이_없으면_코스를_만들_수_없다() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, newCourse(TODAY, 1, null, null)));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
    }
}
