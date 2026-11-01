package com.example.galgot.recommend.service;

import com.example.galgot.place.domain.Env;

import java.util.List;

/**
 * 사용자가 고른 실내/실외/둘 다와 그날 날씨 판정으로 "어떤 장소를, 어떤 순서로" 보여 줄지 정하는 순수 로직.
 * DB와 네트워크 없이 테스트할 수 있게 서비스에서 분리했다.
 * <ul>
 *   <li>선택 없음: 그날 추천을 고른 것과 같다(부적합하면 실내, 적합하면 실외). 화면은 처음에 추천 버튼이 눌린 상태로 시작한다</li>
 *   <li>실내: 실내 → 실내외 / 실외: 실외 → 실내외 (날씨와 상관없이 사용자 선택이 우선)</li>
 *   <li>둘 다: 전체를 그날 날씨 우선순위로. 부적합한 날 실내 → 실내외 → 실외, 적합한 날 실외 → 실내외 → 실내</li>
 * </ul>
 * 추천값은 판정만 따른다: 적합하면 OUTDOOR, 부적합하면 INDOOR.
 */
public final class EnvPreference {

    /** 화면의 [실내] [실외] [둘 다] 버튼. */
    public enum Choice {
        INDOOR,
        OUTDOOR,
        ALL
    }

    /**
     * @param allowed     보여 줄 실내외 종류
     * @param order       앞에 올 순서. 비어 있으면 실내외로 정렬하지 않고 이름순
     * @param recommended 그날 추천(OUTDOOR 또는 INDOOR)
     * @param message     화면 안내 문장
     */
    public record Plan(List<Env> allowed, List<Env> order, Env recommended, String message) {
    }

    private static final List<Env> ALL_ENVS = List.of(Env.INDOOR, Env.OUTDOOR, Env.BOTH);

    private EnvPreference() {
    }

    public static Plan plan(Choice choice, OutdoorJudge.Result judged) {
        boolean suitable = judged.outdoorSuitable();
        Env recommended = suitable ? Env.OUTDOOR : Env.INDOOR;
        String bad = suitable ? "" : "야외 활동이 어려운 날이에요(" + String.join(", ", judged.reasons()) + "). ";

        Choice effective = choice != null ? choice : (suitable ? Choice.OUTDOOR : Choice.INDOOR);

        return switch (effective) {
            case INDOOR -> new Plan(List.of(Env.INDOOR, Env.BOTH), List.of(Env.INDOOR, Env.BOTH), recommended,
                    suitable ? "야외 활동하기 좋은 날이지만 고르신 실내 장소를 보여 드려요."
                             : bad + "실내 장소를 보여 드려요.");
            case OUTDOOR -> new Plan(List.of(Env.OUTDOOR, Env.BOTH), List.of(Env.OUTDOOR, Env.BOTH), recommended,
                    suitable ? "야외 활동하기 좋은 날이에요. 실외 장소를 보여 드려요."
                             : bad + "고르신 실외 장소를 보여 드리지만, 나가기 전에 날씨를 꼭 확인하세요.");
            case ALL -> suitable
                    ? new Plan(ALL_ENVS, List.of(Env.OUTDOOR, Env.BOTH, Env.INDOOR), recommended,
                    "야외 활동하기 좋은 날이에요. 실외 장소를 먼저 보여 드려요.")
                    : new Plan(ALL_ENVS, List.of(Env.INDOOR, Env.BOTH, Env.OUTDOOR), recommended,
                    bad + "실내 장소를 먼저 보여 드려요.");
        };
    }
}
