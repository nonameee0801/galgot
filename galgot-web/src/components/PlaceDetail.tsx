import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { ArrowLeft, ExternalLink } from 'lucide-react'
import { ApiError, fetchPlace, fetchPlaceInfo } from '../api'
import type { Place, PlaceInfo } from '../api'
import { ENV_LABEL, shortAddr, typeName } from '../labels'

interface Props {
  id: number
  onBack: () => void
  /** 장소 기본 정보를 불러오면 화면(MainPage)에 알린다. 지역 조건 맞추기와 지도 강조에 쓴다 */
  onLoaded?: (place: Place) => void
}

type Result = { id: number; place: Place } | { id: number; error: string }

type InfoResult = { id: number; info: PlaceInfo } | { id: number; error: string }

/** 화면에 보여 줄 정보 줄(있는 것만, 이 순서). 홈페이지와 소개는 따로 보여 준다. */
function infoRows(info: PlaceInfo): { label: string; value: string }[] {
  const parking =
    info.parking && info.parkingFee
      ? `${info.parking} (주차요금: ${info.parkingFee})`
      : (info.parking ?? (info.parkingFee ? `주차요금: ${info.parkingFee}` : null))
  const rows: { label: string; value: string | null }[] = [
    { label: '이용시간', value: info.useTime },
    { label: '쉬는 날', value: info.restDate },
    { label: '이용요금', value: info.useFee },
    { label: '주차', value: parking },
    { label: '문의', value: info.contact },
    { label: '개장기간', value: info.openPeriod },
    { label: '예약', value: info.reservation },
  ]
  return rows.filter((r): r is { label: string; value: string } => r.value !== null)
}

/** 소개글. 처음 5줄만 보이고, 넘치면 [더 보기]로 펼친다. */
function Overview({ text }: { text: string }) {
  const ref = useRef<HTMLParagraphElement>(null)
  const [open, setOpen] = useState(false)
  const [overflow, setOverflow] = useState(false)

  // 접힌 상태에서 글이 5줄을 넘는지 재서 [더 보기]를 보일지 정한다
  useLayoutEffect(() => {
    const el = ref.current
    if (el && !open) setOverflow(el.scrollHeight > el.clientHeight + 1)
  }, [text, open])

  return (
    <div className="detail-overview">
      <p ref={ref} className={open ? 'overview-text' : 'overview-text is-clamped'}>
        {text}
      </p>
      {(overflow || open) && (
        <button type="button" className="link-btn" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
          {open ? '접기' : '더 보기'}
        </button>
      )}
    </div>
  )
}

/**
 * 장소 상세 정보(기획서 17장): 이용시간, 쉬는 날, 이용요금, 주차, 문의, 개장기간, 예약, 홈페이지, 소개.
 * 있는 항목만 보여 준다. 기본 정보는 먼저 보이고 이 부분은 따로 불러온다(처음 여는 장소는 서버가 TourAPI에서 받는다).
 * 외부 데이터라 글자로만 넣고(HTML로 넣지 않음), 링크는 http, https 주소만 새 창으로 연다.
 */
function PlaceInfoSection({ id }: { id: number }) {
  const [result, setResult] = useState<InfoResult | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchPlaceInfo(id)
      .then((info) => {
        if (!cancelled) setResult({ id, info })
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setResult({ id, error: e instanceof ApiError ? e.message : '자세한 정보를 불러오지 못했어요.' })
        }
      })
    return () => {
      cancelled = true
    }
  }, [id])

  const current = result !== null && result.id === id ? result : null
  if (current === null) return <p className="muted detail-info-state">자세한 정보를 불러오는 중이에요</p>
  if ('error' in current) return <p className="muted detail-info-state">{current.error}</p>

  const { info } = current
  const rows = infoRows(info)
  const links = info.homepages.filter((url) => /^https?:\/\//.test(url))
  if (rows.length === 0 && links.length === 0 && info.overview === null) {
    return <p className="muted detail-info-state">자세한 정보가 아직 없어요.</p>
  }

  return (
    <section className="detail-info" aria-label="이용 정보">
      {(rows.length > 0 || links.length > 0) && (
        <dl className="info-list">
          {rows.map((r) => (
            <div key={r.label}>
              <dt>{r.label}</dt>
              <dd>{r.value}</dd>
            </div>
          ))}
          {links.length > 0 && (
            <div>
              <dt>홈페이지</dt>
              <dd>
                {links.map((url) => (
                  <a key={url} className="info-link" href={url} target="_blank" rel="noopener noreferrer">
                    {url.replace(/^https?:\/\//, '')}
                    <ExternalLink size={12} aria-hidden="true" />
                  </a>
                ))}
              </dd>
            </div>
          )}
        </dl>
      )}
      {info.overview !== null && <Overview text={info.overview} />}
    </section>
  )
}

/** 패널 안 장소 상세. 지도는 그대로 두고 왼쪽 패널만 바뀐다. 주소는 /places/:id 라서 새로고침과 공유가 된다. */
export default function PlaceDetail({ id, onBack, onLoaded }: Props) {
  const [result, setResult] = useState<Result | null>(null)
  const [brokenImage, setBrokenImage] = useState<number | null>(null)
  const loadedRef = useRef(onLoaded)
  useEffect(() => {
    loadedRef.current = onLoaded
  }, [onLoaded])

  useEffect(() => {
    let cancelled = false
    fetchPlace(id)
      .then((place) => {
        if (cancelled) return
        setResult({ id, place })
        loadedRef.current?.(place)
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
              </figure>
            ) : null
          })()}
          <h3 className="detail-title">{current.place.title}</h3>
          <p className="meta">
            <span className={`env env-${current.place.env}`}>{ENV_LABEL[current.place.env]}</span>
            <span>{typeName(current.place.contentTypeId)}</span>
          </p>
          {current.place.addr && <p className="detail-addr">{shortAddr(current.place.addr)}</p>}
          {/* 갈곳 화면 자체가 지도라 바깥 지도 링크(카카오맵에서 보기)는 두지 않고, 이용 정보를 보여 준다 */}
          <PlaceInfoSection id={current.place.id} />
        </>
      )}
    </article>
  )
}
