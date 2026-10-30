import { useState } from 'react'
import type { Place } from '../api'
import { ENV_LABEL, TYPES, shortAddr, typeName } from '../labels'

interface Props {
  places: Place[]
  total: number
  type: number | null
  onType: (t: number | null) => void
  loading: boolean
  hasMore: boolean
  loadingMore: boolean
  moreError: string | null
  onMore: () => void
}

function Tile({ place }: { place: Place }) {
  const [broken, setBroken] = useState(false)
  const src = place.thumbnailUrl ?? place.imageUrl
  const showImage = src !== null && !broken

  return (
    <li className="tile">
      <div className="thumb">
        {showImage ? (
          <img src={src} alt="" loading="lazy" onError={() => setBroken(true)} />
        ) : (
          <span className="thumb-fallback">{typeName(place.contentTypeId)}</span>
        )}
      </div>
      <h3>{place.title}</h3>
      <p className="addr">{shortAddr(place.addr)}</p>
      <p className="meta">
        <span className={`env env-${place.env}`}>{ENV_LABEL[place.env]}</span>
        <span>{typeName(place.contentTypeId)}</span>
      </p>
    </li>
  )
}

export default function PlaceList({
  places,
  total,
  type,
  onType,
  loading,
  hasMore,
  loadingMore,
  moreError,
  onMore,
}: Props) {
  return (
    <section>
      <div className="list-head">
        <h2>추천 장소 {total.toLocaleString('ko-KR')}곳</h2>
        <div className="chips" role="group" aria-label="장소 종류">
          {TYPES.map((t) => (
            <button
              key={t.name}
              type="button"
              className="chip"
              aria-pressed={t.code === type}
              onClick={() => onType(t.code)}
            >
              {t.name}
            </button>
          ))}
        </div>
      </div>

      {places.length === 0 && !loading ? (
        <div className="notice">
          <p>조건에 맞는 장소가 없어요. 다른 종류나 지역을 골라 보세요.</p>
        </div>
      ) : (
        <ul className="tiles">
          {places.map((p) => (
            <Tile key={p.id} place={p} />
          ))}
        </ul>
      )}

      {hasMore && (
        <div className="more">
          <button type="button" className="btn-more" onClick={onMore} disabled={loadingMore}>
            {loadingMore ? '불러오는 중' : '장소 더 보기'}
          </button>
        </div>
      )}
      {moreError && <p className="more-error">{moreError}</p>}
    </section>
  )
}
