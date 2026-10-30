package com.example.galgot.envrule.service;

import com.example.galgot.envrule.domain.RuleType;
import com.example.galgot.place.domain.Env;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 장소 한 건에 규칙을 적용해 실내/야외를 정하는 순수 로직 (DB, 외부 API와 무관).
 *
 * 1) 일치하는 규칙 중 priority가 가장 높은 것을 쓴다.
 * 2) priority가 같으면 INDOOR > BOTH > OUTDOOR 순으로 고른다.
 *    (비 오는 날 야외 장소를 추천하는 실수가 가장 나쁘므로 보수적으로 판단)
 * 3) 일치하는 규칙이 없으면 BOTH.
 */
@Component
public class EnvClassifier {

    public record Rule(Long id, RuleType type, String matchValue, Env env, int priority) {
    }

    public record Target(String contentId, String lclsSystm1, String lclsSystm2, String lclsSystm3, String title) {
    }

    public record Result(Env env, Long ruleId) {
    }

    public Result classify(Target target, List<Rule> rules) {
        return rules.stream()
                .filter(rule -> matches(rule, target))
                .min(Comparator
                        .comparingInt((Rule r) -> -r.priority())
                        .thenComparingInt(r -> tieRank(r.env()))
                        .thenComparing(r -> r.id() == null ? Long.MAX_VALUE : r.id()))
                .map(rule -> new Result(rule.env(), rule.id()))
                .orElse(new Result(Env.BOTH, null));
    }

    private static boolean matches(Rule rule, Target t) {
        String value = rule.matchValue();
        return switch (rule.type()) {
            case PLACE -> value.equals(t.contentId());
            case CATEGORY -> value.equals(t.lclsSystm1())
                    || value.equals(t.lclsSystm2())
                    || value.equals(t.lclsSystm3());
            case KEYWORD -> t.title() != null && t.title().contains(value);
        };
    }

    private static int tieRank(Env env) {
        return switch (env) {
            case INDOOR -> 0;
            case BOTH -> 1;
            case OUTDOOR -> 2;
        };
    }
}
