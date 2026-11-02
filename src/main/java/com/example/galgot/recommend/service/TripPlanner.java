package com.example.galgot.recommend.service;

import com.example.galgot.place.domain.Env;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * 일차별 코스(하루 N곳)를 고르는 순수 로직. DB와 네트워크 없이 테스트할 수 있게 서비스에서 분리했다(기획서 16장).
 * <ol>
 *   <li>일차마다 그날의 그룹 우선순위(EnvPreference.plan(...).order())를 따른다. 앞 그룹이 모자랄 때만 다음 그룹에서 고른다</li>
 *   <li>1일차 첫 장소: startPlaceId가 후보에 있으면 그 장소, 없으면 고른 지역 후보의 첫 그룹에서 무작위.
 *       고른 지역 후보가 0곳이면 지역 대표 위치(origin)에서 가장 가까운 곳</li>
 *   <li>2일차부터 첫 장소: 전날 마지막 장소에서 가장 가까운 곳(그 근처에서 묵는다고 본다)</li>
 *   <li>다음 장소: 직전 장소에서 가장 가까운 곳. 거리는 위경도 직선거리, 같으면 id가 작은 곳</li>
 *   <li>앞 일차에 쓴 장소는 다시 쓰지 않는다</li>
 *   <li>그날 고른 지역 후보가 2곳 이상이면 그 지역에서만(2~3곳이면 그만큼만). 0~1곳이면 같은 시도의 다른 지역까지 넓혀
 *       {@link #NEARBY_FILL}곳까지 채우고, 넓혀서 넣은 장소에는 nearby 표시를 붙인다</li>
 * </ol>
 * 무작위는 바깥에서 넣는다(randomIndex: 개수 → 0 이상 개수 미만의 번호). 테스트에서는 고정값을 넣어 결과를 고정한다.
 */
public final class TripPlanner {

    /** 고른 지역 후보가 0~1곳일 때 주변 지역까지 넓혀 채우는 개수. */
    static final int NEARBY_FILL = 3;

    /**
     * @param inArea 고른 지역(서울 전체 또는 고른 시·군) 안의 장소인지. 아니면 같은 시도의 다른 지역
     */
    public record Candidate(long id, Env env, double lat, double lng, boolean inArea) {
    }

    /** @param nearby 고른 지역이 아니라 주변 지역에서 채운 장소 */
    public record Pick(long id, boolean nearby) {
    }

    /**
     * @param startPlaceId    1일차 첫 장소(없으면 null). 화면은 주소에 남겨 같은 코스를 다시 만든다
     * @param rerollAvailable 지금 시작 장소 말고 고를 수 있는 시작 장소가 있는지("다른 추천 받기" 버튼 표시)
     * @param days            일차별 방문 순서
     */
    public record Result(Long startPlaceId, boolean rerollAvailable, List<List<Pick>> days) {
    }

    private TripPlanner() {
    }

    /**
     * @param dayOrders           일차별 그룹 우선순위. 크기가 곧 일수다
     * @param candidates          시도 전체 후보(좌표가 있고, 사용자가 고른 유형에 맞는 장소)
     * @param perDay              하루에 고를 개수
     * @param originLat           고른 지역 대표 위치. 1일차에 고른 지역 후보가 0곳일 때만 쓴다
     * @param originLng           위와 같음
     * @param startPlaceId        주소로 다시 들어올 때의 시작 장소(없으면 null)
     * @param excludeStartPlaceId "다른 추천 받기" 때 뺄 지금 시작 장소(없으면 null)
     * @param randomIndex         개수를 받아 0 이상 개수 미만의 번호를 돌려준다
     */
    public static Result plan(List<List<Env>> dayOrders, List<Candidate> candidates, int perDay,
                              double originLat, double originLng,
                              Long startPlaceId, Long excludeStartPlaceId, IntUnaryOperator randomIndex) {
        // 무작위 번호가 같은 장소를 가리키도록 id 순으로 고정한다
        List<Candidate> sorted = candidates.stream().sorted(Comparator.comparingLong(Candidate::id)).toList();
        Set<Long> used = new HashSet<>();
        List<List<Pick>> days = new ArrayList<>();
        Candidate prev = null;
        Long start = null;
        boolean rerollAvailable = false;

        for (int d = 0; d < dayOrders.size(); d++) {
            List<Env> order = dayOrders.get(d);
            List<Candidate> remaining = sorted.stream()
                    .filter(c -> !used.contains(c.id()) && order.contains(c.env()))
                    .toList();
            List<Candidate> areaPool = remaining.stream().filter(Candidate::inArea).toList();

            List<Candidate> pool;
            int limit;
            if (areaPool.size() >= 2) {
                pool = areaPool;
                limit = Math.min(perDay, areaPool.size());
            } else {
                pool = remaining;
                limit = Math.min(Math.min(perDay, NEARBY_FILL), remaining.size());
            }

            List<Pick> picks = new ArrayList<>();
            List<Candidate> left = new ArrayList<>(pool);
            Candidate current = null;

            if (limit > 0) {
                if (d == 0) {
                    rerollAvailable = areaPool.size() > 1;
                    current = firstOfTrip(areaPool, left, order, originLat, originLng,
                            startPlaceId, excludeStartPlaceId, randomIndex);
                    start = current.id();
                } else if (prev != null) {
                    current = nearestByGroup(left, order, prev.lat(), prev.lng());
                } else {
                    // 앞 일차에 한 곳도 못 넣었으면 지역 대표 위치에서 시작한다
                    current = nearestByGroup(left, order, originLat, originLng);
                }
                take(current, left, picks, used);

                while (picks.size() < limit) {
                    current = nearestByGroup(left, order, current.lat(), current.lng());
                    take(current, left, picks, used);
                }
                prev = current;
            }
            days.add(List.copyOf(picks));
        }
        return new Result(start, rerollAvailable, List.copyOf(days));
    }

    /** 1일차 첫 장소. startPlaceId가 고른 지역 후보에 있으면 그 장소, 아니면 무작위, 지역 후보가 없으면 대표 위치에서 가장 가까운 곳. */
    private static Candidate firstOfTrip(List<Candidate> areaPool, List<Candidate> left, List<Env> order,
                                         double originLat, double originLng,
                                         Long startPlaceId, Long excludeStartPlaceId, IntUnaryOperator randomIndex) {
        if (areaPool.isEmpty()) {
            return nearestByGroup(left, order, originLat, originLng);
        }
        if (startPlaceId != null && !startPlaceId.equals(excludeStartPlaceId)) {
            for (Candidate c : areaPool) {
                if (c.id() == startPlaceId) {
                    return c;
                }
            }
        }
        List<Candidate> choosable = areaPool.stream()
                .filter(c -> excludeStartPlaceId == null || c.id() != excludeStartPlaceId)
                .toList();
        if (choosable.isEmpty()) {
            choosable = areaPool; // 뺄 수 없으면(후보가 그 장소뿐) 그대로 쓴다
        }
        List<Candidate> group = firstGroup(choosable, order);
        int index = randomIndex.applyAsInt(group.size());
        return group.get(Math.floorMod(index, group.size()));
    }

    /** 우선순위가 가장 앞이면서 후보가 있는 그룹. */
    private static List<Candidate> firstGroup(List<Candidate> list, List<Env> order) {
        for (Env env : order) {
            List<Candidate> group = list.stream().filter(c -> c.env() == env).toList();
            if (!group.isEmpty()) {
                return group;
            }
        }
        return list;
    }

    /** 앞 그룹부터 보고, 그 그룹 안에서 (lat, lng)에 가장 가까운 곳. 거리가 같으면 id가 작은 곳. */
    private static Candidate nearestByGroup(List<Candidate> left, List<Env> order, double lat, double lng) {
        List<Candidate> group = firstGroup(left, order);
        return group.stream()
                .min(Comparator.<Candidate>comparingDouble(c -> distanceKm(lat, lng, c.lat(), c.lng()))
                        .thenComparingLong(Candidate::id))
                .orElseThrow();
    }

    private static void take(Candidate c, List<Candidate> left, List<Pick> picks, Set<Long> used) {
        left.remove(c);
        used.add(c.id());
        picks.add(new Pick(c.id(), !c.inArea()));
    }

    /** 두 위경도 사이의 직선거리(km, 하버사인 공식). 실제 이동 거리와는 다르다(강, 산을 사이에 둔 경우 등). */
    static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
