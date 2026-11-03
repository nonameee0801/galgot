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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 코스 관련 규칙.
 * - 내 코스만 수정, 삭제할 수 있다 (남의 코스는 403)
 * - 공유 링크(추측할 수 없는 UUID)를 아는 사람은 로그인 없이 코스를 볼 수 있다. 공개/비공개는 쓰지 않는다(2026-11-03 결정)
 * - 공유받은 코스는 로그인한 사람이 내 코스로 복사해 저장할 수 있다
 * - 코스에는 일정(1~3일)이 있고, 장소는 그 일차 안에만 담는다. 같은 장소는 한 코스에 한 번만(409) (기획서 18장 D)
 * - 새 코스의 출발일은 필수이고 오늘(한국 시간) 이전이면 400. 기존 코스를 수정할 때는 날짜를 따지지 않는다
 */
@Service
@Transactional
public class CourseService {

    static final int MAX_PLACES_PER_COURSE = 50;

    private final CourseRepository courseRepository;
    private final PlaceRepository placeRepository;
    private final Clock clock;

    @Autowired
    public CourseService(CourseRepository courseRepository, PlaceRepository placeRepository) {
        this(courseRepository, placeRepository, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    /** 테스트에서 오늘 날짜를 고정할 때 쓴다. */
    CourseService(CourseRepository courseRepository, PlaceRepository placeRepository, Clock clock) {
        this.courseRepository = courseRepository;
        this.placeRepository = placeRepository;
        this.clock = clock;
    }

    /**
     * 코스를 만든다. placeId가 있으면 그 장소를 dayNo(없으면 1)일차에 함께 넣어 한 번에 저장한다(빈 코스가 남지 않게).
     * 이때 코스의 시도는 그 장소의 시도로 한다.
     */
    public CourseDetailResponse create(Long userId, CourseRequest request) {
        LocalDate travelDate = request.travelDate();
        if (travelDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "출발일을 골라 주세요.");
        }
        if (travelDate.isBefore(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지난 날짜로는 코스를 만들 수 없습니다.");
        }
        Place first = request.placeId() == null ? null : findPlace(request.placeId());
        String ldongRegnCd = first != null ? first.getLdongRegnCd() : blankToNull(request.ldongRegnCd());
        int tripDays = request.tripDays() == null ? 1 : request.tripDays();

        Course course = rule(() -> Course.create(
                userId,
                request.title().trim(),
                ldongRegnCd,
                travelDate,
                tripDays,
                Boolean.TRUE.equals(request.publicCourse())));
        if (first != null) {
            int dayNo = request.dayNo() == null ? 1 : request.dayNo();
            rule(() -> course.addPlace(first, dayNo, null));
        }
        courseRepository.saveAndFlush(course);
        return CourseDetailResponse.from(course);
    }

    /** placeId를 주면 코스마다 그 장소가 이미 담겼는지(containsPlace)를 함께 준다. */
    @Transactional(readOnly = true)
    public List<CourseResponse> list(Long userId, Long placeId) {
        return courseRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(c -> CourseResponse.from(c, placeId))
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
        if (request.tripDays() != null) {
            rule(() -> {
                course.changeTripDays(request.tripDays());
                return null;
            });
        }
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
        Place place = findPlace(request.placeId());

        int dayNo = request.dayNo() == null ? 1 : request.dayNo();
        rule(() -> course.addPlace(place, dayNo, blankToNull(request.memo())));

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
            rule(() -> {
                course.checkDayInTrip(item.dayNo());
                return null;
            });
            target.moveTo(item.dayNo(), item.visitOrder());
        }
        return CourseDetailResponse.from(course);
    }

    /** 공유 링크로 조회. 없는 링크는 404. (2026-11-03부터 공개 여부를 보지 않는다. is_public 컬럼은 남겨 두고 쓰지 않음) */
    @Transactional(readOnly = true)
    public CourseDetailResponse getShared(String shareCode) {
        return CourseDetailResponse.from(findShared(shareCode));
    }

    /** 공유받은 코스를 내 코스로 복사해 저장한다(한 번의 저장으로 코스와 장소를 함께). 없는 링크는 404. */
    public CourseDetailResponse copyShared(Long userId, String shareCode) {
        Course copy = Course.copyOf(findShared(shareCode), userId);
        courseRepository.saveAndFlush(copy);
        return CourseDetailResponse.from(copy);
    }

    private Course findShared(String shareCode) {
        return courseRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."));
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."));
    }

    private Course getOwned(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."));
        if (!course.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 코스가 아닙니다.");
        }
        return course;
    }

    /**
     * 코스 도메인의 규칙 위반을 HTTP 응답으로 바꾼다.
     * 같은 장소 두 번(IllegalStateException)은 409, 일정·일차 규칙(IllegalArgumentException)은 400.
     */
    private static <T> T rule(Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
