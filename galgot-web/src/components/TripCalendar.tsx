import { useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { DayOption } from '../labels'

interface Props {
  /** 고를 수 있는 출발일(일정의 마지막 날까지 예보가 있는 날). 날씨기반 추천에서 쓴다 */
  starts?: DayOption[]
  /** starts 대신 이 날(오늘) 이후 아무 날이나 고르게 할 때(나만의 코스의 새 코스, 기획서 18장 D). 달은 계속 넘길 수 있다 */
  minDate?: string
  start: string
  /** 일정 길이. 출발일부터 이만큼을 여행 기간으로 표시한다 */
  days: number
  onPick: (date: string) => void
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

function toDateString(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function parse(date: string): Date {
  const [y, m, d] = date.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/**
 * 출발일을 고르는 달력 박스. 항상 펼쳐 둔다.
 * 날씨 예보가 있는 날(고를 수 있는 출발일)만 누를 수 있고, 고른 출발일부터 일정 길이만큼 여행 기간을 이어서 표시한다.
 * 고를 수 있는 날이 다음 달까지 걸치면(예: 10/30~11/2) 달을 넘길 수 있다.
 */
export default function TripCalendar({ starts, minDate, start, days, onPick }: Props) {
  const startDate = parse(start)
  const min = starts ? starts[0].date : (minDate ?? start)
  const first = parse(min)
  const last = starts ? parse(starts[starts.length - 1].date) : null
  const [month, setMonth] = useState(() => new Date(startDate.getFullYear(), startDate.getMonth(), 1))

  const canPrev = month > new Date(first.getFullYear(), first.getMonth(), 1)
  const canNext = last === null || month < new Date(last.getFullYear(), last.getMonth(), 1)

  const allowedSet = starts ? new Set(starts.map((d) => d.date)) : null
  // 날짜 문자열(yyyy-MM-dd)은 글자 순서가 날짜 순서와 같다
  const isAllowed = (date: string) => (allowedSet ? allowedSet.has(date) : date >= min)
  const blockedReason = starts ? ', 예보가 없어 고를 수 없음' : ', 지난 날짜라 고를 수 없음'
  const tripDates = new Set(
    Array.from({ length: days }, (_, i) => toDateString(new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate() + i))),
  )
  const today = min

  // 이번 달 1일이 있는 주의 일요일부터 6주 이내로 채운다
  const cells: (Date | null)[] = []
  for (let i = 0; i < month.getDay(); i++) cells.push(null)
  const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate()
  for (let d = 1; d <= daysInMonth; d++) cells.push(new Date(month.getFullYear(), month.getMonth(), d))
  while (cells.length % 7 !== 0) cells.push(null)

  const move = (delta: number) => setMonth(new Date(month.getFullYear(), month.getMonth() + delta, 1))

  return (
    <div className="cal">
      <div className="cal-head">
        <button type="button" className="icon-btn" aria-label="이전 달" disabled={!canPrev} onClick={() => move(-1)}>
          <ChevronLeft size={18} aria-hidden="true" />
        </button>
        <strong aria-live="polite">
          {month.getFullYear()}년 {month.getMonth() + 1}월
        </strong>
        <button type="button" className="icon-btn" aria-label="다음 달" disabled={!canNext} onClick={() => move(1)}>
          <ChevronRight size={18} aria-hidden="true" />
        </button>
      </div>
      <div className="cal-grid" role="grid" aria-label="출발일 달력">
        {WEEKDAYS.map((w) => (
          <span key={w} className="cal-weekday" role="columnheader">
            {w}
          </span>
        ))}
        {cells.map((d, i) => {
          if (d === null) return <span key={`blank-${i}`} className="cal-blank" />
          const date = toDateString(d)
          const ok = isAllowed(date)
          const inTrip = tripDates.has(date)
          const classes = ['cal-day']
          if (inTrip) classes.push('in-trip')
          if (date === today) classes.push('is-today')
          return (
            <button
              key={date}
              type="button"
              role="gridcell"
              className={classes.join(' ')}
              aria-pressed={date === start}
              aria-label={`${d.getMonth() + 1}월 ${d.getDate()}일${date === today ? ' 오늘' : ''}${ok ? '' : blockedReason}`}
              disabled={!ok}
              onClick={() => onPick(date)}
            >
              {d.getDate()}
            </button>
          )
        })}
      </div>
    </div>
  )
}
