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
  selectedId: number | null
  onSelect: (id: number) => void
  /** 장소 이름 검색어. 있으면 제목을 "검색 결과"로 바꾸고, 종류 버튼은 지역·실내/실외 버튼처럼 흐리게 막는다(검색은 종류로 거르지 않는다) */
  keyword?: string | null
}

/** 패널 폭에 맞춘 한 줄 카드: 왼쪽 작은 사진, 오른쪽 이름과 주소. 누르면 지도에서 그 장소로 가고 상세가 열린다. */
function Tile({ place, selected, onSelect }: { place: Place; selected: boolean; onSelect: (id: number) => void }) {
  const [broken, setBroken] = useState(false)
  const src = place.thumbnailUrl ?? place.imageUrl
  const showImage = src !== null && !broken

  return (
    <li>
      <button
        type="button"
        className={selected ? 'tile is-selected' : 'tile'}
        aria-current={selected ? 'true' : undefined}
        onClick={() => onSelect(place.id)}
      >
        <span className="thumb">
          {showImage ? (
            <img src={src} alt="" loading="lazy" onError={() => setBroken(true)} />
          ) : (
            <span className="thumb-fallback">{typeName(place.contentTypeId)}</span>
          )}
        </span>
        <span className="tile-body">
          <span className="tile-title">{place.title}</span>
          <span className="addr">{shortAddr(place.addr)}</span>
          <span className="meta">
            <span className={`env env-${place.env}`}>{ENV_LABEL[place.env]}</span>
            <span>{typeName(place.contentTypeId)}</span>
          </span>
        </span>
      </button>
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
  selectedId,
  onSelect,
  keyword = null,
}: Props) {
  const searching = keyword !== null
  return (
    <section>
      <div className="list-head">
        <h3>
          {searching ? `'${keyword}' 검색 결과` : '추천 장소'} {total.toLocaleString('ko-KR')}곳
        </h3>
        <div className={searching ? 'chips is-disabled' : 'chips'} role="group" aria-label="장소 종류">
          {TYPES.map((t) => (
            <button
              key={t.name}
              type="button"
              className="chip"
              aria-pressed={t.code === type}
              disabled={searching}
              onClick={() => onType(t.code)}
            >
              {t.name}
            </button>
          ))}
        </div>
      </div>

      {places.length === 0 && !loading ? (
        <div className="notice">
          <p>
            {searching
              ? '이름에 검색어가 들어간 장소가 없어요. 다른 낱말로 찾아 보세요.'
              : '조건에 맞는 장소가 없어요. 다른 종류나 지역을 골라 보세요.'}
          </p>
        </div>
      ) : (
        <ul className="tiles">
          {places.map((p) => (
            <Tile key={p.id} place={p} selected={p.id === selectedId} onSelect={onSelect} />
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
