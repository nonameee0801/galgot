package com.example.galgot.area.repository;

import com.example.galgot.area.domain.Area;
import com.example.galgot.area.domain.AreaLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, String> {

    List<Area> findAllByLdongRegnCdAndLevelOrderByNameAsc(String ldongRegnCd, AreaLevel level);

    /**
     * 지역에 속한 장소의 시군구 코드(places.ldong_signgu_cd). 수원시처럼 구가 있는 시는 여러 개다.
     * area_signgus는 조회 전용 연결표라 엔티티 없이 바로 읽는다.
     */
    @Query(value = "SELECT ldong_signgu_cd FROM area_signgus WHERE area_code = :areaCode", nativeQuery = true)
    List<String> findSignguCodes(@Param("areaCode") String areaCode);

    /**
     * 장소의 (시도, 시군구 코드)가 속한 지역 코드. 연결표의 키가 (시도, 시군구 코드)라 많아야 1개다.
     * 장소 상세를 주소로 바로 열었을 때 화면의 지역 조건을 그 장소에 맞추는 데 쓴다.
     */
    @Query(value = "SELECT area_code FROM area_signgus WHERE ldong_regn_cd = :regn AND ldong_signgu_cd = :signgu",
            nativeQuery = true)
    List<String> findAreaCodes(@Param("regn") String ldongRegnCd, @Param("signgu") String ldongSignguCd);
}
