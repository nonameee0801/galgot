package com.example.galgot.recommend.service;

import com.example.galgot.place.domain.Env;
import com.example.galgot.recommend.service.EnvPreference.Choice;
import com.example.galgot.recommend.service.EnvPreference.Plan;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvPreferenceTest {

    private static final OutdoorJudge.Result GOOD = new OutdoorJudge.Result(true, List.of());
    private static final OutdoorJudge.Result RAINY = new OutdoorJudge.Result(false, List.of("강수확률 70%"));

    @Test
    void 선택이_없으면_적합한_날은_실외를_고른_것과_같다() {
        Plan p = EnvPreference.plan(null, GOOD);

        assertEquals(EnvPreference.plan(Choice.OUTDOOR, GOOD), p);
        assertEquals(List.of(Env.OUTDOOR, Env.BOTH), p.order());
        assertEquals(Env.OUTDOOR, p.recommended());
    }

    @Test
    void 선택이_없으면_부적합한_날은_실내를_고른_것과_같다() {
        Plan p = EnvPreference.plan(null, RAINY);

        assertEquals(EnvPreference.plan(Choice.INDOOR, RAINY), p);
        assertEquals(Set.of(Env.INDOOR, Env.BOTH), Set.copyOf(p.allowed()));
        assertEquals(List.of(Env.INDOOR, Env.BOTH), p.order());
        assertEquals(Env.INDOOR, p.recommended());
    }

    @Test
    void 실내를_고르면_날씨와_상관없이_실내_다음_실내외다() {
        for (OutdoorJudge.Result judged : List.of(GOOD, RAINY)) {
            Plan p = EnvPreference.plan(Choice.INDOOR, judged);

            assertEquals(Set.of(Env.INDOOR, Env.BOTH), Set.copyOf(p.allowed()));
            assertEquals(List.of(Env.INDOOR, Env.BOTH), p.order());
        }
    }

    @Test
    void 비_오는_날_실외를_골라도_실외_장소를_먼저_보여_준다() {
        Plan p = EnvPreference.plan(Choice.OUTDOOR, RAINY);

        assertEquals(Set.of(Env.OUTDOOR, Env.BOTH), Set.copyOf(p.allowed()));
        assertEquals(List.of(Env.OUTDOOR, Env.BOTH), p.order());
        assertEquals(Env.INDOOR, p.recommended()); // 추천은 판정을 따른다
    }

    @Test
    void 비_오는_날_실외를_고르면_판정_이유와_주의를_안내한다() {
        Plan p = EnvPreference.plan(Choice.OUTDOOR, RAINY);

        assertTrue(p.message().contains("강수확률 70%"));
        assertTrue(p.message().contains("날씨를 꼭 확인"));
    }

    @Test
    void 둘_다를_고르면_전체를_그날_날씨_우선순위로_보여_준다() {
        Plan rainy = EnvPreference.plan(Choice.ALL, RAINY);
        Plan good = EnvPreference.plan(Choice.ALL, GOOD);

        assertEquals(3, rainy.allowed().size());
        assertEquals(List.of(Env.INDOOR, Env.BOTH, Env.OUTDOOR), rainy.order());
        assertEquals(List.of(Env.OUTDOOR, Env.BOTH, Env.INDOOR), good.order());
    }

    @Test
    void 추천값은_판정만_따른다() {
        for (Choice c : Choice.values()) {
            assertEquals(Env.OUTDOOR, EnvPreference.plan(c, GOOD).recommended());
            assertEquals(Env.INDOOR, EnvPreference.plan(c, RAINY).recommended());
        }
    }
}
