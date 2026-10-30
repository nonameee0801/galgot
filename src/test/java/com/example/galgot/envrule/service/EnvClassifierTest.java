package com.example.galgot.envrule.service;

import com.example.galgot.envrule.domain.RuleType;
import com.example.galgot.envrule.service.EnvClassifier.Result;
import com.example.galgot.envrule.service.EnvClassifier.Rule;
import com.example.galgot.envrule.service.EnvClassifier.Target;
import com.example.galgot.place.domain.Env;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnvClassifierTest {

    private final EnvClassifier classifier = new EnvClassifier();

    private static Rule category(long id, String code, Env env, int priority) {
        return new Rule(id, RuleType.CATEGORY, code, env, priority);
    }

    private static Rule keyword(long id, String word, Env env) {
        return new Rule(id, RuleType.KEYWORD, word, env, 25);
    }

    private static Target place(String id, String l1, String l2, String l3, String title) {
        return new Target(id, l1, l2, l3, title);
    }

    @Test
    void 분류코드가_일치하면_해당_환경으로_분류한다() {
        List<Rule> rules = List.of(category(1, "VE070100", Env.INDOOR, 30), category(2, "NA", Env.OUTDOOR, 10));

        assertEquals(Env.INDOOR,
                classifier.classify(place("1", "VE", "VE07", "VE070100", "가회민화박물관"), rules).env());
        assertEquals(Env.OUTDOOR,
                classifier.classify(place("2", "NA", "NA01", "NA010100", "관악산"), rules).env());
    }

    @Test
    void 규칙이_없으면_BOTH이고_ruleId는_null이다() {
        Result result = classifier.classify(place("1", "HS", "HS01", "HS010100", "경복궁"), List.of());

        assertEquals(Env.BOTH, result.env());
        assertNull(result.ruleId());
    }

    @Test
    void 더_세부적인_분류가_우선한다() {
        // LS02(수상 레저)는 야외지만 LS020700(수영장)은 실내
        List<Rule> rules = List.of(category(1, "LS02", Env.OUTDOOR, 20), category(2, "LS020700", Env.INDOOR, 30));

        Result result = classifier.classify(place("1", "LS", "LS02", "LS020700", "50플러스수영장"), rules);

        assertEquals(Env.INDOOR, result.env());
        assertEquals(2L, result.ruleId());
    }

    @Test
    void 키워드는_중분류보다_우선한다() {
        // VE06(실내) 분류이지만 제목에 '공원'이 있으면 키워드(25)가 중분류(20)보다 앞선다
        List<Rule> rules = List.of(category(1, "VE06", Env.INDOOR, 20), keyword(2, "공원", Env.OUTDOOR));

        assertEquals(Env.OUTDOOR, classifier.classify(place("1", "VE", "VE06", "VE060100", "어느공원 입구"), rules).env());
    }

    @Test
    void 규칙이_없는_분류도_제목_키워드로_분류한다() {
        List<Rule> rules = List.of(keyword(1, "갤러리", Env.INDOOR));

        assertEquals(Env.INDOOR,
                classifier.classify(place("1", "VE", "VE07", "VE070600", "[K드라마 촬영지] 공근혜갤러리"), rules).env());
    }

    @Test
    void 우선순위가_같으면_INDOOR가_우선한다() {
        List<Rule> rules = List.of(keyword(1, "공원", Env.OUTDOOR), keyword(2, "박물관", Env.INDOOR));

        assertEquals(Env.INDOOR, classifier.classify(place("1", "VE", "VE07", "VE070100", "올림픽공원 박물관"), rules).env());
    }

    @Test
    void 특정_장소_규칙이_가장_우선한다() {
        List<Rule> rules = List.of(
                category(1, "NA", Env.OUTDOOR, 10),
                new Rule(2L, RuleType.PLACE, "12345", Env.INDOOR, 100));

        assertEquals(Env.INDOOR, classifier.classify(place("12345", "NA", "NA01", "NA010100", "동굴 관광지"), rules).env());
    }
}
