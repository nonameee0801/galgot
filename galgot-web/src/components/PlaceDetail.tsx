import { useEffect, useState } from 'react'
import { ArrowLeft, ExternalLink } from 'lucide-react'
import { ApiError, fetchPlace } from '../api'
import type { Place } from '../api'
import { ENV_LABEL, inServiceArea, shortAddr, typeName } from '../labels'

interface Props {
  id: number
  onBack: () => void
}

type Result = { id: number; place: Place } | { id: number; error: string }

/** 패널 안 장소 상세. 지도는 그대로 두고 왼쪽 패널만 바뀐다. 주소는 /places/:id 라서 새로고침과 공유가 된다. */
export default function PlaceDetail({ id, onBack }: Props) {
  const [result, setResult] = useState<Result | null>(null)
  const [brokenImage, setBrokenImage] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchPlace(id)
      .then((place) => {
        if (!cancelled) setResult({ id, place })
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setResult({ id, error: e instanceof ApiError ? e.message : '장소 정보를 불러오지 못했어요.' })
        }
      })
    return () => {
      cancelled = true
    }
  }, [id])

  const current = result !== null && result.id === id ? result : null

  return (
    <article className="place-detail" aria-busy={current === null}>
      <button type="button" className="link-btn back-btn" onClick={onBack}>
        <ArrowLeft size={16} aria-hidden="true" />
        목록으로
      </button>

      {current === null && <p className="muted">장소 정보를 불러오는 중이에요</p>}
      {current !== null && 'error' in current && <p role="alert">{current.error}</p>}
      {current !== null && 'place' in current && (
        <>
          {(() => {
            const p = current.place
            const src = p.imageUrl ?? p.thumbnailUrl
            return src !== null && brokenImage !== p.id ? (
              <figure className="detail-photo">
                <img src={src} alt={`${p.title} 사진`} onError={() => setBrokenImage(p.id)} />
                <figcaption>사진: 한국관광공사 TourAPI</figcaption>
              </figure>
            ) : null
          })()}
          <h3 className="detail-title">{current.place.title}</h3>
          <p className="meta">
            <span className={`env env-${current.place.env}`}>{ENV_LABEL[current.place.env]}</span>
            <span>{typeName(current.place.contentTypeId)}</span>
          </p>
          {current.place.addr && <p className="detail-addr">{shortAddr(current.place.addr)}</p>}
          {inServiceArea(current.place.lat, current.place.lng) && (
            <a
              className="btn-line detail-link"
              href={`https://map.kakao.com/link/map/${encodeURIComponent(current.place.title)},${current.place.lat},${current.place.lng}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              카카오맵에서 보기
              <ExternalLink size={14} aria-hidden="true" />
            </a>
          )}
        </>
      )}
    </article>
  )
}
