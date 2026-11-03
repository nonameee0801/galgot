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

/** 일차별 코스 항목 id 배치(끌어 옮기기, 기획서 18장 H). 키는 일차 번호, 값은 방문 순서대로의 항목 id */
export type DayLayout = Record<number, number[]>

/** 지금 코스의 배치. 일정의 모든 일차를 빈 일차까지 넣는다. */
export function layoutOf(places: CoursePlaceItem[], tripDays: number): DayLayout {
  const layout: DayLayout = {}
  for (const day of allDays(places, tripDays)) layout[day.dayNo] = day.items.map((i) => i.id)
  return layout
}

/** 항목이 든 일차. 없으면 null */
export function dayOf(layout: DayLayout, id: number): number | null {
  for (const [day, ids] of Object.entries(layout)) if (ids.includes(id)) return Number(day)
  return null
}

/**
 * 항목 하나를 toDay의 toIndex 자리로 옮긴 새 배치. 같은 일차 안이면 순서만 바뀐다.
 * toIndex가 목록 길이보다 크면 맨 뒤로. 원래 배치는 바꾸지 않는다.
 */
export function moveInLayout(layout: DayLayout, id: number, toDay: number, toIndex: number): DayLayout {
  const from = dayOf(layout, id)
  if (from === null || layout[toDay] === undefined) return layout
  const next: DayLayout = {}
  for (const [day, ids] of Object.entries(layout)) next[Number(day)] = ids.filter((x) => x !== id)
  const target = next[toDay]
  target.splice(Math.max(0, Math.min(toIndex, target.length)), 0, id)
  return next
}

/**
 * 새 배치에서 일차나 방문 순서가 바뀐 항목만 골라 순서 바꾸기 API에 보낼 모양으로.
 * 방문 순서는 일차마다 1부터 다시 매긴다(빈 자리가 생기지 않게).
 */
export function layoutChanges(
  places: CoursePlaceItem[],
  layout: DayLayout,
): { coursePlaceId: number; dayNo: number; visitOrder: number }[] {
  const before = new Map(places.map((p) => [p.id, p]))
  const changes: { coursePlaceId: number; dayNo: number; visitOrder: number }[] = []
  for (const [day, ids] of Object.entries(layout)) {
    ids.forEach((id, i) => {
      const old = before.get(id)
      const dayNo = Number(day)
      if (old && (old.dayNo !== dayNo || old.visitOrder !== i + 1)) {
        changes.push({ coursePlaceId: id, dayNo, visitOrder: i + 1 })
      }
    })
  }
  return changes
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
