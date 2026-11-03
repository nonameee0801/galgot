import { useState } from 'react'
import type { WeatherKind } from '../labels'

interface Props {
  /** 표지 사진(지금 목록이나 추천 결과의 첫 장소 사진). 없거나 불러오지 못하면 날씨 그림으로 대신한다 */
  photo: string | null
  /** 날씨 효과(비 오면 빗줄기, 눈 오면 눈송이, 맑으면 따뜻한 빛, 흐리면 회색 막). 날씨를 아직 모르면 null */
  kind: WeatherKind | null
  /** 위 작은 글씨. 예: "서울 · 종로구 · 오늘" */
  where: string
  /** 판정 문장. 예: "비 오는 종로, 실내 여행 어때요?" */
  headline: string
}

/**
 * 왼쪽 패널 맨 위의 표지(기획서 19장). 사진 위에 그날 날씨 효과를 옅게 얹고 판정 문장을 보여 준다.
 * 날씨 효과는 CSS로만 그리며, 움직임 줄이기를 켠 사용자에게는 멈춘 그림으로 보인다.
 */
export default function PanelCover({ photo, kind, where, headline }: Props) {
  // 사진을 불러오지 못하면 날씨 그림으로 바꾼다. 사진 주소가 바뀌면 다시 시도한다
  const [broken, setBroken] = useState<string | null>(null)
  const showPhoto = photo !== null && broken !== photo

  return (
    <div className="cover" data-weather={kind ?? 'none'}>
      {showPhoto && <img className="cover-photo" src={photo} alt="" onError={() => setBroken(photo)} />}
      <span className="cover-fx" aria-hidden="true" />
      <div className="cover-text">
        <p className="cover-where">{where}</p>
        <p className="cover-title" aria-live="polite">
          {headline}
        </p>
      </div>
    </div>
  )
}
