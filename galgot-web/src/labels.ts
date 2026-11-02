import type { Env, EnvChoice, Weather } from './api.ts'

export const REGIONS = [
  { code: '11', name: '서울' },
  { code: '41', name: '경기' },
]

export const TYPES: { code: number | null; name: string }[] = [
  { code: null, name: '전체' },
  { code: 12, name: '관광지' },
  { code: 14, name: '문화시설' },
  { code: 28, name: '레포츠' },
]

export const ENV_LABEL: Record<Env, string> = {
  INDOOR: '실내',
  OUTDOOR: '실외',
  BOTH: '실내외',
}

export const ENV_CHOICES: { code: EnvChoice; name: string }[] = [
  { code: 'INDOOR', name: '실내' },
  { code: 'OUTDOOR', name: '실외' },
  { code: 'ALL', name: '둘 다' },
]

export function typeName(contentTypeId: number): string {
  return TYPES.find((t) => t.code === contentTypeId)?.name ?? '장소'
}

const SKY_LABEL = {
  CLEAR: '맑음',
  MOSTLY_CLOUDY: '구름 많음',
  OVERCAST: '흐림',
} as const

const PRECIP_LABEL = {
  NONE: '',
  SHOWER: '소나기',
  RAIN: '비',
  RAIN_SNOW: '비 또는 눈',
  SNOW: '눈',
} as const

/** "구름 많음", "흐림, 비" 처럼 하늘과 강수를 한 줄로. */
export function weatherText(w: Weather): string {
  const parts: string[] = []
  if (w.sky) parts.push(SKY_LABEL[w.sky])
  if (w.precipType && w.precipType !== 'NONE') parts.push(PRECIP_LABEL[w.precipType])
  return parts.length > 0 ? parts.join(', ') : '정보 없음'
}

export type Mood = 'clear' | 'cloud' | 'wet' | 'snow'

/** 페이지 배경 분위기를 정하는 값. 강수가 있으면 하늘 상태보다 우선한다. */
export function moodOf(w: Weather): Mood {
  if (w.precipType === 'SNOW' || w.precipType === 'RAIN_SNOW') return 'snow'
  if (w.precipType && w.precipType !== 'NONE') return 'wet'
  if (w.sky === 'CLEAR') return 'clear'
  return 'cloud'
}

export interface DayOption {
  date: string // yyyy-MM-dd
  name: string // 오늘, 내일 ...
  topic: string // 오늘은, 모레는 ...
  short: string // 10/30
}

const DAY_NAMES = [
  { name: '오늘', topic: '오늘은' },
  { name: '내일', topic: '내일은' },
  { name: '모레', topic: '모레는' },
  { name: '글피', topic: '글피는' },
]

function toDateString(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 오늘부터 3일 뒤까지 (기상청 단기예보가 제공하는 범위). */
export function dayOptions(now: Date): DayOption[] {
  return DAY_NAMES.map((n, i) => {
    const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() + i)
    return { date: toDateString(d), name: n.name, topic: n.topic, short: `${d.getMonth() + 1}/${d.getDate()}` }
  })
}

/** "서울특별시 종로구 ..." → "종로구 ..." 처럼 앞의 시도 이름을 줄인다. */
export function shortAddr(addr: string | null): string {
  if (!addr) return ''
  return addr.replace(/^(서울특별시|경기도)\s*/, '')
}
