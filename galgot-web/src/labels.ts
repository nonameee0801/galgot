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

export type WeatherKind = 'clear' | 'cloud' | 'rain' | 'snow'

/** "서울 전체", "경기 · 가평군"처럼 날씨를 받은 지역을 한 줄로. */
export function areaLabel(w: Weather): string {
  if (w.area === w.region) return w.areaName
  const regionName = REGIONS.find((r) => r.code === w.region)?.name ?? ''
  return regionName ? `${regionName} · ${w.areaName}` : w.areaName
}

/** 표지의 날씨 효과와 날씨 아이콘을 정하는 값. 강수가 있으면 하늘 상태보다 우선한다. */
export function weatherKind(w: Weather): WeatherKind {
  if (w.precipType === 'SNOW' || w.precipType === 'RAIN_SNOW') return 'snow'
  if (w.precipType && w.precipType !== 'NONE') return 'rain'
  if (w.sky === 'CLEAR') return 'clear'
  return 'cloud'
}

/**
 * 판정 문장에 넣는 짧은 지역 이름: "서울 전체" → "서울", "종로구" → "종로", "가평군" → "가평".
 * "중구"처럼 끝 글자를 떼면 한 글자만 남는 이름은 그대로 둔다.
 */
export function shortAreaName(name: string): string {
  const n = name.replace(/\s*전체$/, '').trim()
  return n.length > 2 && /[구시군]$/.test(n) ? n.slice(0, -1) : n
}

/**
 * 표지의 판정 문장(여행 말투, 기획서 19장). 야외 적합 여부는 서버 판정(outdoorSuitable)을 그대로 따르고,
 * 날씨 종류와 기온으로 문장만 고른다. 기온 기준(33도, 영하 10도)은 서버 기본값과 같지만 문장을 고를 때만 쓴다.
 */
export function travelHeadline(w: Weather, outdoorSuitable: boolean, place: string): string {
  const kind = weatherKind(w)
  if (kind === 'snow') return `눈 오는 ${place}, 따뜻한 실내로 가요`
  if (kind === 'rain') return `비 오는 ${place}, 실내 여행 어때요?`
  if (!outdoorSuitable) {
    if (w.tempMax !== null && w.tempMax >= 33) return `무더운 ${place}, 시원한 실내로 피해요`
    if (w.tempMin !== null && w.tempMin <= -10) return `꽁꽁 언 ${place}, 따뜻한 실내로 가요`
    return `비 소식 있는 ${place}, 실내 여행 어때요?`
  }
  if (kind === 'clear') return `맑은 ${place}, 걷기 좋은 날이에요`
  return `구름 낀 ${place}, 산책하기 괜찮아요`
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

/** "서울특별시 종로구 효자로 12" → "종로구"처럼 주소에서 구·시군만. 없으면 빈 글자 */
export function districtOf(addr: string | null): string {
  return shortAddr(addr).split(' ')[0] ?? ''
}

/** 날씨기반 추천의 일정 길이. 날씨 예보가 3일 뒤까지라 2박3일까지만 둔다. */
export const TRIP_LENGTHS: { days: number; name: string }[] = [
  { days: 1, name: '당일치기' },
  { days: 2, name: '1박2일' },
  { days: 3, name: '2박3일' },
]

export function tripLengthName(days: number): string {
  return TRIP_LENGTHS.find((t) => t.days === days)?.name ?? `${days}일`
}

/** 일정의 마지막 날까지 예보가 있는 출발일만. 당일치기는 오늘~3일 뒤, 1박2일은 ~2일 뒤, 2박3일은 ~1일 뒤. */
export function startOptions(all: DayOption[], days: number): DayOption[] {
  return all.slice(0, Math.max(1, all.length - (days - 1)))
}

/** 일차별 지도 색. 색만으로 나누지 않도록 선 모양(실선, 긴 점선, 점선)도 함께 바꾼다. */
export const DAY_STYLES: { color: string; line: 'solid' | 'shortdash' | 'shortdot' }[] = [
  { color: '#0e6f73', line: 'solid' },
  { color: '#c2611b', line: 'shortdash' },
  { color: '#6a4fb3', line: 'shortdot' },
]
