package com.example.galgot.course.service;

import com.example.galgot.course.domain.Course;
import com.example.galgot.course.dto.CourseDetailResponse;
import com.example.galgot.course.repository.CourseRepository;
import com.example.galgot.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** 공유 링크 보기(기획서 18장 E). */
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
}
