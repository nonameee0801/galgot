import type { CoursePlaceItem } from './api'

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
