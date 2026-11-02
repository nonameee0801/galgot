package com.example.galgot.recommend.service;

import com.example.galgot.place.domain.Env;
import com.example.galgot.recommend.service.TripPlanner.Candidate;
import com.example.galgot.recommend.service.TripPlanner.Pick;
import com.example.galgot.recommend.service.TripPlanner.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripPlannerTest {

    private static final List<Env> INDOOR_FIRST = List.of(Env.INDOOR, Env.BOTH);           // 실내 선택
    private static final List<Env> OUTDOOR_FIRST = List.of(Env.OUTDOOR, Env.BOTH);         // 실외 선택
    private static final List<Env> RAINY_ALL = List.of(Env.INDOOR, Env.BOTH, Env.OUTDOOR); // 둘 다, 부적합한 날
    private static final List<Env> CLEAR_ALL = List.of(Env.OUTDOOR, Env.BOTH, Env.INDOOR); // 둘 다, 적합한 날

    private static final IntUnaryOperator FIRST = bound -> 0;
    private static final double BASE_LAT = 37.0;
    private static final double BASE_LNG = 127.0;

    /** 위도 37.0에서 동쪽으로 x칸(0.01도, 약 0.9km)만큼 떨어진 장소. */
    private static Candidate at(long id, Env env, double x, boolean inArea) {
        return new Candidate(id, env, BASE_LAT, BASE_LNG + x * 0.01, inArea);
    }

    private static Candidate in(long id, Env env, double x) {
        return at(id, env, x, true);
    }

    private static Candidate near(long id, Env env, double x) {
        return at(id, env, x, false);
    }

    private static List<Long> ids(List<Pick> picks) {
        return picks.stream().map(Pick::id).toList();
    }

    private static Result plan(List<List<Env>> orders, List<Candidate> candidates, int perDay, IntUnaryOperator random) {
        return TripPlanner.plan(orders, candidates, perDay, BASE_LAT, BASE_LNG, null, null, random);
    }

    @Test
    void 하루_4곳을_직전_장소에서_가까운_순서로_고른다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(2, Env.INDOOR, 5), in(3, Env.INDOOR, 1),
                in(4, Env.INDOOR, 3), in(5, Env.INDOOR, 9));

        Result r = plan(List.of(INDOOR_FIRST), list, 4, FIRST);

        assertEquals(List.of(1L, 3L, 4L, 2L), ids(r.days().get(0)));
        assertEquals(1L, r.startPlaceId());
    }

    @Test
    void 거리가_같으면_id가_작은_곳을_고른다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(7, Env.INDOOR, 2), in(3, Env.INDOOR, -2));

        Result r = plan(List.of(INDOOR_FIRST), list, 2, FIRST);

        assertEquals(List.of(1L, 3L), ids(r.days().get(0)));
    }

    @Test
    void 실내를_고르면_실내를_먼저_채우고_실내외는_모자랄_때만_넣는다() {
        // 실내외(2, 3)가 더 가까워도 실내(4)를 먼저 고른다
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(2, Env.BOTH, 1), in(3, Env.BOTH, 2),
                in(4, Env.INDOOR, 8), in(5, Env.OUTDOOR, 0.5));

        Result r = plan(List.of(INDOOR_FIRST), list, 4, FIRST);

        assertEquals(List.of(1L, 4L, 3L, 2L), ids(r.days().get(0)));
    }

    @Test
    void 첫_그룹이_비어_있으면_다음_그룹에서_첫_장소를_고른다() {
        List<Candidate> list = List.of(in(1, Env.BOTH, 0), in(2, Env.BOTH, 1), in(3, Env.OUTDOOR, 2));

        Result r = plan(List.of(INDOOR_FIRST), list, 4, FIRST);

        assertEquals(List.of(1L, 2L), ids(r.days().get(0)));
    }

    @Test
    void 여러_날에_같은_장소를_다시_쓰지_않고_2일차는_전날_마지막_장소_근처에서_시작한다() {
        List<Candidate> list = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            list.add(in(i, Env.INDOOR, i));
        }

        Result r = plan(List.of(INDOOR_FIRST, INDOOR_FIRST), list, 4, FIRST);

        assertEquals(List.of(1L, 2L, 3L, 4L), ids(r.days().get(0)));
        assertEquals(List.of(5L, 6L, 7L, 8L), ids(r.days().get(1)));
    }

    @Test
    void 날마다_판정이_다르면_그날_우선순위로_고른다() {
        List<Candidate> list = List.of(
                in(1, Env.INDOOR, 0), in(2, Env.INDOOR, 1),
                in(3, Env.OUTDOOR, 2), in(4, Env.OUTDOOR, 3),
                in(5, Env.BOTH, 1.5));

        // 1일차 비(실내 먼저), 2일차 맑음(실외 먼저)
        Result r = plan(List.of(RAINY_ALL, CLEAR_ALL), list, 2, FIRST);

        assertEquals(List.of(1L, 2L), ids(r.days().get(0)));
        assertEquals(List.of(3L, 4L), ids(r.days().get(1)));
    }

    @Test
    void 고른_지역_후보가_2_3곳이면_주변_지역으로_넓히지_않고_그만큼만_넣는다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(2, Env.INDOOR, 1), in(3, Env.INDOOR, 2),
                near(4, Env.INDOOR, 0.5), near(5, Env.INDOOR, 1.5));

        Result r = plan(List.of(INDOOR_FIRST), list, 4, FIRST);

        assertEquals(List.of(1L, 2L, 3L), ids(r.days().get(0)));
        assertTrue(r.days().get(0).stream().noneMatch(Pick::nearby));
    }

    @Test
    void 고른_지역_후보가_1곳이면_그곳에서_시작해_주변_지역으로_3곳까지_채운다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0),
                near(4, Env.INDOOR, 5), near(5, Env.INDOOR, 1), near(6, Env.INDOOR, 2), near(7, Env.INDOOR, 3));

        Result r = plan(List.of(INDOOR_FIRST), list, 4, FIRST);

        assertEquals(List.of(new Pick(1, false), new Pick(5, true), new Pick(6, true)), r.days().get(0));
    }

    @Test
    void 고른_지역_후보가_0곳이면_지역_대표_위치에서_가까운_주변_지역_장소로_3곳까지_채운다() {
        // 예: 오산시 · 실내 · 레포츠(2026-11-02 DB 조회 기준 0곳). 대표 위치는 x=0
        List<Candidate> list = List.of(in(1, Env.OUTDOOR, 0),
                near(4, Env.INDOOR, 6), near(5, Env.BOTH, 1), near(6, Env.INDOOR, 3), near(7, Env.INDOOR, 9));

        Result r = plan(List.of(INDOOR_FIRST), list, 4, FIRST);

        assertEquals(List.of(6L, 4L, 7L), ids(r.days().get(0)));
        assertTrue(r.days().get(0).stream().allMatch(Pick::nearby));
        assertEquals(6L, r.startPlaceId());
        assertFalse(r.rerollAvailable());
    }

    @Test
    void 후보가_모자라면_있는_만큼만_넣는다() {
        List<Candidate> list = List.of(near(4, Env.INDOOR, 1));

        Result r = plan(List.of(INDOOR_FIRST, INDOOR_FIRST), list, 4, FIRST);

        assertEquals(List.of(4L), ids(r.days().get(0)));
        assertEquals(List.of(), r.days().get(1));
    }

    @Test
    void 후보가_없으면_빈_코스이고_시작_장소도_없다() {
        Result r = plan(List.of(INDOOR_FIRST), List.of(in(1, Env.OUTDOOR, 0)), 4, FIRST);

        assertEquals(List.of(), r.days().get(0));
        assertNull(r.startPlaceId());
        assertFalse(r.rerollAvailable());
    }

    @Test
    void 첫_장소는_첫_그룹에서_무작위로_고른다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(2, Env.INDOOR, 5), in(3, Env.BOTH, 9));

        Result r = plan(List.of(INDOOR_FIRST), list, 1, bound -> bound - 1);

        assertEquals(2L, r.startPlaceId()); // 실내(1, 2) 중 마지막. 실내외(3)는 첫 장소 후보가 아니다
    }

    @Test
    void 시작_장소를_주면_무작위와_상관없이_같은_코스를_만든다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(2, Env.INDOOR, 5), in(3, Env.INDOOR, 6));

        Result a = TripPlanner.plan(List.of(INDOOR_FIRST), list, 3, BASE_LAT, BASE_LNG, 2L, null, bound -> 0);
        Result b = TripPlanner.plan(List.of(INDOOR_FIRST), list, 3, BASE_LAT, BASE_LNG, 2L, null, bound -> bound - 1);

        assertEquals(List.of(2L, 3L, 1L), ids(a.days().get(0)));
        assertEquals(a, b);
    }

    @Test
    void 후보에_없는_시작_장소는_무시하고_무작위로_고른다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(2, Env.INDOOR, 5));

        Result r = TripPlanner.plan(List.of(INDOOR_FIRST), list, 2, BASE_LAT, BASE_LNG, 99L, null, FIRST);

        assertEquals(1L, r.startPlaceId());
    }

    @Test
    void 다른_추천_받기는_지금_시작_장소를_빼고_다시_고른다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(2, Env.INDOOR, 5), in(3, Env.INDOOR, 6));

        Result r = TripPlanner.plan(List.of(INDOOR_FIRST), list, 3, BASE_LAT, BASE_LNG, null, 1L, FIRST);

        assertEquals(2L, r.startPlaceId());
        assertTrue(r.days().get(0).contains(new Pick(1, false))); // 시작 장소에서만 빠지고 코스에는 들어갈 수 있다
        assertTrue(r.rerollAvailable());
    }

    @Test
    void 고른_지역에_시작_장소_후보가_하나뿐이면_다른_추천_받기를_할_수_없다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), near(4, Env.INDOOR, 1), near(5, Env.INDOOR, 2));

        Result r = plan(List.of(INDOOR_FIRST), list, 4, FIRST);
        Result again = TripPlanner.plan(List.of(INDOOR_FIRST), list, 4, BASE_LAT, BASE_LNG, null, 1L, FIRST);

        assertFalse(r.rerollAvailable());
        assertEquals(1L, again.startPlaceId()); // 뺄 수 없으면 같은 장소에서 시작한다
    }

    @Test
    void 하루_개수는_설정값을_따른다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(2, Env.INDOOR, 1), in(3, Env.INDOOR, 2),
                in(4, Env.INDOOR, 3), in(5, Env.INDOOR, 4), in(6, Env.INDOOR, 5));

        Result r = plan(List.of(INDOOR_FIRST), list, 5, FIRST);

        assertEquals(5, r.days().get(0).size());
    }

    @Test
    void 실외를_고르면_비_오는_날에도_실외를_먼저_고른다() {
        List<Candidate> list = List.of(in(1, Env.INDOOR, 0), in(2, Env.OUTDOOR, 3), in(3, Env.OUTDOOR, 4),
                in(4, Env.BOTH, 1));

        Result r = plan(List.of(OUTDOOR_FIRST), list, 3, FIRST);

        assertEquals(List.of(2L, 3L, 4L), ids(r.days().get(0)));
    }

    @Test
    void 직선거리는_하버사인으로_계산한다() {
        // 위도 1도는 약 111km
        assertEquals(111.19, TripPlanner.distanceKm(37.0, 127.0, 38.0, 127.0), 0.05);
        assertEquals(0.0, TripPlanner.distanceKm(37.5, 127.0, 37.5, 127.0), 1e-9);
    }
}
