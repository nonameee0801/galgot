package com.example.galgot.recommend.service;

import com.example.galgot.global.config.WeatherRuleProperties;
import com.example.galgot.weather.domain.PrecipType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutdoorJudgeTest {

    private final OutdoorJudge judge = new OutdoorJudge(new WeatherRuleProperties(null, null, null));

    @Test
    void 맑고_선선한_날은_야외_적합이다() {
        OutdoorJudge.Result r = judge.judge(PrecipType.NONE, 10, 20, 8);

        assertTrue(r.outdoorSuitable());
        assertTrue(r.reasons().isEmpty());
    }

    @Test
    void 비가_오면_부적합이다() {
        OutdoorJudge.Result r = judge.judge(PrecipType.RAIN, 20, 20, 8);

        assertFalse(r.outdoorSuitable());
        assertEquals(1, r.reasons().size());
    }

    @Test
    void 강수확률_기준값_이상이면_부적합이고_바로_아래는_적합이다() {
        assertFalse(judge.judge(PrecipType.NONE, 60, 20, 8).outdoorSuitable());
        assertTrue(judge.judge(PrecipType.NONE, 59, 20, 8).outdoorSuitable());
    }

    @Test
    void 폭염과_한파는_부적합이다() {
        assertFalse(judge.judge(PrecipType.NONE, 0, 33, 25).outdoorSuitable());
        assertTrue(judge.judge(PrecipType.NONE, 0, 32, 25).outdoorSuitable());
        assertFalse(judge.judge(PrecipType.NONE, 0, -2, -10).outdoorSuitable());
        assertTrue(judge.judge(PrecipType.NONE, 0, -2, -9).outdoorSuitable());
    }

    @Test
    void 이유는_걸린_조건마다_하나씩_나온다() {
        OutdoorJudge.Result r = judge.judge(PrecipType.SNOW, 80, 0, -12);

        assertEquals(3, r.reasons().size());
    }

    @Test
    void 값이_없으면_그_항목은_판단에서_뺀다() {
        assertTrue(judge.judge(null, null, null, null).outdoorSuitable());
    }

    @Test
    void 기준값을_바꾸면_판단도_바뀐다() {
        OutdoorJudge strict = new OutdoorJudge(new WeatherRuleProperties(30, 30, 0));

        assertFalse(strict.judge(PrecipType.NONE, 30, 20, 8).outdoorSuitable());
        assertFalse(strict.judge(PrecipType.NONE, 0, 20, 0).outdoorSuitable());
    }
}
