/**
 * 관광지 목록 탭의 조건(지역, 실내/실외, 테마)을 이 브라우저 탭에 기억한다(기획서 18장 A).
 * 날씨기반 추천 탭은 열 때마다 초기값으로 시작하는데, 두 탭이 주소의 region, area, env, type을 함께 쓰므로
 * 관광지 목록 탭을 떠날 때의 조건을 따로 기억해 두었다가 [관광지 목록]을 누르면 되살린다.
 * 저장소를 쓸 수 없는 환경(사생활 보호 모드 등)이면 기억하지 않고 초기값으로 돌아간다.
 */
const LIST_KEY = 'galgot.listSearch'

/** 관광지 목록 탭의 조건으로 기억할 주소 값(q는 장소 이름 검색어) */
const LIST_PARAMS = ['region', 'area', 'env', 'type', 'q']

/** 주소의 조건 중 관광지 목록 탭의 것만 골라 기억한다. 탭, 날씨기반 추천 조건은 뺀다. */
export function saveListSearch(params: URLSearchParams): void {
  const p = new URLSearchParams()
  for (const key of LIST_PARAMS) {
    const value = params.get(key)
    if (value !== null) p.set(key, value)
  }
  try {
    window.sessionStorage.setItem(LIST_KEY, p.toString())
  } catch {
    // 기억하지 못해도 이번 화면은 그대로 동작한다
  }
}

/** 기억한 관광지 목록 조건(?region=41&area=41820 처럼). 없으면 빈 문자열(초기값). */
export function loadListSearch(): string {
  try {
    const saved = window.sessionStorage.getItem(LIST_KEY)
    return saved ? `?${saved}` : ''
  } catch {
    return ''
  }
}

/** 로고를 누르면 기억한 조건도 지워 모두 초기값으로 돌아간다. */
export function clearListSearch(): void {
  try {
    window.sessionStorage.removeItem(LIST_KEY)
  } catch {
    // 지우지 못하면 다음에 [관광지 목록]을 누를 때 이전 조건이 돌아올 뿐이다
  }
}
