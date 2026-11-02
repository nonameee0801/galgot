// 카카오 지도 SDK를 한 번만 불러온다. 키는 환경 변수 VITE_KAKAO_MAP_KEY로 넣는다(npm run dev를 실행하는 환경에 설정).
// 브라우저에 그대로 보이는 JavaScript 키라서 카카오 개발자 콘솔의 허용 도메인으로 보호한다.
const KEY: string | undefined = import.meta.env.VITE_KAKAO_MAP_KEY

let loading: Promise<typeof kakao> | null = null

export function hasKakaoKey(): boolean {
  return typeof KEY === 'string' && KEY.trim() !== ''
}

/** 묶음 표시(clusterer) 라이브러리까지 불러온다. 실패하면 다음 호출에서 다시 시도할 수 있다. */
export function loadKakaoMaps(): Promise<typeof kakao> {
  if (loading) return loading

  loading = new Promise<typeof kakao>((resolve, reject) => {
    if (!hasKakaoKey()) {
      reject(new Error('VITE_KAKAO_MAP_KEY가 설정되지 않았어요.'))
      return
    }
    const ready = () => {
      const k = window.kakao
      if (!k?.maps) {
        reject(new Error('카카오 지도 SDK를 찾지 못했어요.'))
        return
      }
      k.maps.load(() => resolve(k))
    }
    if (window.kakao?.maps) {
      ready()
      return
    }
    const script = document.createElement('script')
    script.src =
      'https://dapi.kakao.com/v2/maps/sdk.js?autoload=false&libraries=clusterer&appkey=' + encodeURIComponent(KEY!.trim())
    script.async = true
    script.onload = ready
    script.onerror = () => reject(new Error('카카오 지도 스크립트를 불러오지 못했어요.'))
    document.head.appendChild(script)
  })

  loading.catch(() => {
    loading = null
  })
  return loading
}
