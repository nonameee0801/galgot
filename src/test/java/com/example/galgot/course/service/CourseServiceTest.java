package com.example.galgot.course.service;

import com.example.galgot.course.domain.Course;
import com.example.galgot.course.dto.CourseDetailResponse;
import com.example.galgot.course.repository.CourseRepository;
import com.example.galgot.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 공유 링크 보기와 공유받은 코스를 내 코스로 저장하기(기획서 18장 E). */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    CourseRepository courseRepository;
    @Mock
    PlaceRepository placeRepository;

    CourseService service;

    @BeforeEach
    void setUp() {
        service = new CourseService(courseRepository, placeRepository);
    }

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
}
