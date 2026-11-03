package com.example.galgot.user.repository;

import com.example.galgot.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * 회원 행을 쓰기 잠금으로 읽는다(SELECT … FOR UPDATE). 트랜잭션이 끝날 때까지 같은 회원의 다른 잠금 요청은 기다린다.
     * 코스 수 상한을 셀 때 같은 회원의 만들기 요청을 차례로 처리하려고 쓴다(기획서 18장 G).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
