package com.example.galgot.course.service;

import com.example.galgot.course.domain.Course;
import com.example.galgot.course.domain.CoursePlace;
import com.example.galgot.course.dto.AddPlaceRequest;
import com.example.galgot.course.dto.CourseDetailResponse;
import com.example.galgot.course.dto.CourseRequest;
import com.example.galgot.course.dto.CourseResponse;
import com.example.galgot.course.dto.ReorderRequest;
import com.example.galgot.course.repository.CourseRepository;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.repository.PlaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 코스 관련 규칙.
 * - 내 코스만 수정, 삭제할 수 있다 (남의 코스는 403)
 * - 공유 링크(추측할 수 없는 UUID)를 아는 사람은 로그인 없이 코스를 볼 수 있다. 공개/비공개는 쓰지 않는다(2026-11-03 결정)
 */
@Service
@Transactional
public class CourseService {

    static final int MAX_PLACES_PER_COURSE = 50;

    private final CourseRepository courseRepository;
    private final PlaceRepository placeRepository;

    public CourseService(CourseRepository courseRepository, PlaceRepository placeRepository) {
        this.courseRepository = courseRepository;
        this.placeRepository = placeRepository;
    }

    public CourseDetailResponse create(Long userId, CourseRequest request) {
        Course course = Course.create(
                userId,
                request.title().trim(),
                blankToNull(request.ldongRegnCd()),
                request.travelDate(),
                Boolean.TRUE.equals(request.publicCourse()));
        courseRepository.save(course);
        return CourseDetailResponse.from(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> list(Long userId) {
        return courseRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse get(Long userId, Long courseId) {
        return CourseDetailResponse.from(getOwned(userId, courseId));
    }

    public CourseDetailResponse update(Long userId, Long courseId, CourseRequest request) {
        Course course = getOwned(userId, courseId);
        course.update(
                request.title().trim(),
                blankToNull(request.ldongRegnCd()),
                request.travelDate(),
                Boolean.TRUE.equals(request.publicCourse()));
        return CourseDetailResponse.from(course);
    }

    public void delete(Long userId, Long courseId) {
        courseRepository.delete(getOwned(userId, courseId));
    }

    public CourseDetailResponse addPlace(Long userId, Long courseId, AddPlaceRequest request) {
        Course course = getOwned(userId, courseId);
        if (course.getPlaces().size() >= MAX_PLACES_PER_COURSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "한 코스에는 장소를 최대 " + MAX_PLACES_PER_COURSE + "개까지 담을 수 있습니다.");
        }
        Place place = placeRepository.findById(request.placeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."));

        int dayNo = request.dayNo() == null ? 1 : request.dayNo();
        course.addPlace(place, dayNo, blankToNull(request.memo()));

        // 새 항목의 id가 응답에 필요하므로 여기서 먼저 DB에 반영한다
        courseRepository.saveAndFlush(course);
        return CourseDetailResponse.from(course);
    }

    public void removePlace(Long userId, Long courseId, Long coursePlaceId) {
        Course course = getOwned(userId, courseId);
        if (!course.removePlace(coursePlaceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "코스에 담긴 장소를 찾을 수 없습니다.");
        }
    }

    public CourseDetailResponse reorder(Long userId, Long courseId, ReorderRequest request) {
        Course course = getOwned(userId, courseId);
        Map<Long, CoursePlace> byId = course.getPlaces().stream()
                .collect(Collectors.toMap(CoursePlace::getId, Function.identity()));

        for (ReorderRequest.Item item : request.items()) {
            CoursePlace target = byId.get(item.coursePlaceId());
            if (target == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "이 코스에 없는 항목입니다: " + item.coursePlaceId());
            }
            target.moveTo(item.dayNo(), item.visitOrder());
        }
        return CourseDetailResponse.from(course);
    }

    /** 공유 링크로 조회. 없는 링크는 404. (2026-11-03부터 공개 여부를 보지 않는다. is_public 컬럼은 남겨 두고 쓰지 않음) */
    @Transactional(readOnly = true)
    public CourseDetailResponse getShared(String shareCode) {
        return CourseDetailResponse.from(findShared(shareCode));
    }

    private Course findShared(String shareCode) {
        return courseRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."));
    }

    private Course getOwned(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."));
        if (!course.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 코스가 아닙니다.");
        }
        return course;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
