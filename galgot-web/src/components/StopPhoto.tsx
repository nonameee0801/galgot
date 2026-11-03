import { useState } from 'react'
import type { Place } from '../api'
import PhotoFallback from './PhotoFallback'

interface Props {
  place: Place
  /** 방문 순서. 있으면 사진 왼쪽 위에 번호 */
  order?: number
  /** 번호 바탕색(일차 색) */
  color?: string
}

/** 코스 카드의 작은 사진(기획서 19장). 사진이 없으면 실내/실외 그림, 방문 순서가 있으면 왼쪽 위에 번호. */
export default function StopPhoto({ place, order, color }: Props) {
  const [broken, setBroken] = useState(false)
  const src = place.thumbnailUrl ?? place.imageUrl
  return (
    <span className="stop-photo">
      {src !== null && !broken ? (
        <img src={src} alt="" loading="lazy" onError={() => setBroken(true)} />
      ) : (
        <PhotoFallback env={place.env} />
      )}
      {order !== undefined && (
        <span className="stop-no" style={color ? { background: color } : undefined}>
          {order}
        </span>
      )}
    </span>
  )
}
