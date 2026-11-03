package com.example.galgot.course.dto;

import java.util.List;

/** 내 코스 여러 개 지우기 요청. 개수와 주인 확인은 CourseService.deleteAll에서 한다(기획서 18장 F). */
public record DeleteCoursesRequest(List<Long> ids) {
}
