package com.example.galgot.course.controller;

import com.example.galgot.course.dto.AddPlaceRequest;
import com.example.galgot.course.dto.CourseDetailResponse;
import com.example.galgot.course.dto.CourseRequest;
import com.example.galgot.course.dto.CourseResponse;
import com.example.galgot.course.dto.ReorderRequest;
import com.example.galgot.course.service.CourseService;
import com.example.galgot.global.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDetailResponse create(@AuthenticationPrincipal OAuth2User principal,
                                       @Valid @RequestBody CourseRequest request) {
        return courseService.create(CurrentUser.id(principal), request);
    }

    /** 내 코스 목록. placeId를 주면 코스마다 그 장소가 이미 담겼는지(containsPlace)를 함께 준다(나만의 코스에 넣기). */
    @GetMapping
    public List<CourseResponse> list(@AuthenticationPrincipal OAuth2User principal,
                                     @RequestParam(name = "placeId", required = false) Long placeId) {
        return courseService.list(CurrentUser.id(principal), placeId);
    }

    @GetMapping("/{id}")
    public CourseDetailResponse get(@AuthenticationPrincipal OAuth2User principal,
                                    @PathVariable("id") Long id) {
        return courseService.get(CurrentUser.id(principal), id);
    }

    @PutMapping("/{id}")
    public CourseDetailResponse update(@AuthenticationPrincipal OAuth2User principal,
                                       @PathVariable("id") Long id,
                                       @Valid @RequestBody CourseRequest request) {
        return courseService.update(CurrentUser.id(principal), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal OAuth2User principal,
                       @PathVariable("id") Long id) {
        courseService.delete(CurrentUser.id(principal), id);
    }

    @PostMapping("/{id}/places")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDetailResponse addPlace(@AuthenticationPrincipal OAuth2User principal,
                                         @PathVariable("id") Long id,
                                         @Valid @RequestBody AddPlaceRequest request) {
        return courseService.addPlace(CurrentUser.id(principal), id, request);
    }

    @DeleteMapping("/{id}/places/{coursePlaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePlace(@AuthenticationPrincipal OAuth2User principal,
                            @PathVariable("id") Long id,
                            @PathVariable("coursePlaceId") Long coursePlaceId) {
        courseService.removePlace(CurrentUser.id(principal), id, coursePlaceId);
    }

    @PatchMapping("/{id}/places/order")
    public CourseDetailResponse reorder(@AuthenticationPrincipal OAuth2User principal,
                                        @PathVariable("id") Long id,
                                        @Valid @RequestBody ReorderRequest request) {
        return courseService.reorder(CurrentUser.id(principal), id, request);
    }

    /** 공유 링크로 보기. 로그인 없이 접근 가능(SecurityConfig에서 GET만 허용). */
    @GetMapping("/shared/{shareCode}")
    public CourseDetailResponse shared(@PathVariable("shareCode") String shareCode) {
        return courseService.getShared(shareCode);
    }

    /** 공유받은 코스를 내 코스로 저장(복사). 로그인 필요. */
    @PostMapping("/shared/{shareCode}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDetailResponse copyShared(@AuthenticationPrincipal OAuth2User principal,
                                           @PathVariable("shareCode") String shareCode) {
        return courseService.copyShared(CurrentUser.id(principal), shareCode);
    }
}
