package com.example.galgot.course.repository;

import com.example.galgot.course.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Course> findByShareCode(String shareCode);

    /** 회원의 코스 수(회원당 상한 확인용, 기획서 18장 G) */
    long countByUserId(Long userId);
}
