import type { CoursePlaceItem, CourseSummary } from './api'

/** 코스의 장소를 일차별로 묶는다. 서버가 일차, 방문 순서대로 주므로 순서는 그대로 쓴다. */
export function groupByDay(places: CoursePlaceItem[]): { dayNo: number; items: CoursePlaceItem[] }[] {
  const days: { dayNo: number; items: CoursePlaceItem[] }[] = []
  for (const item of places) {
    const last = days[days.length - 1]
    if (last && last.dayNo === item.dayNo) last.items.push(item)
    else days.push({ dayNo: item.dayNo, items: [item] })
  }
  return days
}

/**
 * 일정(tripDays)의 모든 일차를 1일차부터 채운다. 장소가 없는 일차는 빈 목록이다(나만의 코스에 넣기, 기획서 18장 D).
 * 예전 코스처럼 일정보다 뒤 일차에 장소가 있으면 그 일차까지 보여 준다.
 */
export function allDays(
  places: CoursePlaceItem[],
  tripDays: number,
): { dayNo: number; items: CoursePlaceItem[] }[] {
  const grouped = groupByDay(places)
  const lastUsed = grouped.reduce((n, d) => Math.max(n, d.dayNo), 0)
  const total = Math.max(tripDays, lastUsed)
  return Array.from({ length: total }, (_, i) => ({
    dayNo: i + 1,
    items: grouped.find((d) => d.dayNo === i + 1)?.items ?? [],
  }))
}

/**
 * 나만의 코스에 넣기 팝업이 처음 골라 둘 코스: 맨 위(최근에 만든) 코스 중 그 장소가 아직 없고 50곳이 차지 않은 첫 코스.
 * 없으면 null(새 코스 만들기).
 */
export function firstAddableCourse(list: CourseSummary[]): CourseSummary | null {
  return list.find((c) => !c.containsPlace && c.placeCount < MAX_PLACES_PER_COURSE) ?? null
}

/** 처음 골라 둘 일차: 장소가 든 마지막 일차(비어 있으면 1일차). 일정보다 뒤면 일정의 마지막 날. */
export function defaultDay(course: Pick<CourseSummary, 'lastDay' | 'tripDays'>): number {
  return Math.min(Math.max(course.lastDay, 1), course.tripDays)
}

/** 한 코스에 담을 수 있는 장소 수(서버 CourseService.MAX_PLACES_PER_COURSE와 같다) */
export const MAX_PLACES_PER_COURSE = 50
