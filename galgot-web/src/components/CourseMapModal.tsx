import { useMemo, useState } from 'react'
import type { CourseDetail } from '../api'
import { DAY_STYLES, ENV_LABEL, shortAddr, typeName } from '../labels'
import { groupByDay } from '../course'
import KakaoMap from './KakaoMap'
import type { RouteDay } from './KakaoMap'
import Modal from './Modal'
import PlaceDetail from './PlaceDetail'

interface Props {
  course: CourseDetail
  /** 처음부터 고른 채로 열 장소(장소 카드의 [지도에서 보기]). 없으면 목록부터 */
  initialPlaceId?: number | null
  onClose: () => void
}

/**
 * 지도에 그릴 수 있는 좌표인지(서울·경기 범위). 서버의 RecommendationService.inServiceArea와 같은 범위다.
 * TourAPI에 좌표가 엉뚱하게 들어간 장소가 있어, 그런 장소가 섞이면 지도가 동아시아 전체로 축소된다.
 */
function inServiceArea(lat: number | null, lng: number | null): boolean {
  return lat !== null && lng !== null && lat >= 36.8 && lat <= 38.4 && lng >= 126.0 && lng <= 128.2
}

/**
 * 코스 지도 팝업(기획서 18장 E). 화면을 크게 쓰고, 왼쪽은 장소 목록, 오른쪽은 지도(일차별 번호 마커와 방문 순서 선)다.
 * 목록이나 마커에서 장소를 고르면 왼쪽이 그 장소의 관광지 정보(지도 화면의 장소 상세와 같음)로 바뀌고,
 * [목록으로]를 누르면 돌아온다. 팝업 밖으로 페이지를 옮기지 않는다.
 * 마이페이지 코스 카드, 내 코스 상세, 공유 코스 보기가 함께 쓴다.
 */
export default function CourseMapModal({ course, initialPlaceId = null, onClose }: Props) {
  const [selectedId, setSelectedId] = useState<number | null>(initialPlaceId)
  const days = useMemo(() => groupByDay(course.places), [course.places])

  // 지도에는 좌표가 서울·경기 범위 안인 장소만 그린다
  const route = useMemo<RouteDay[]>(
    () =>
      days.map((d) => ({
        dayNo: d.dayNo,
        places: d.items
          .filter((i) => inServiceArea(i.place.lat, i.place.lng))
          .map((i) => ({ order: i.visitOrder, place: i.place })),
      })),
    [days],
  )
  const hidden = course.places.length - route.reduce((n, d) => n + d.places.length, 0)

  return (
    <Modal title={`${course.title} · 지도`} onClose={onClose} wide>
      <div className="course-map">
        <div className="course-map-side">
          {selectedId !== null ? (
            <PlaceDetail id={selectedId} onBack={() => setSelectedId(null)} />
          ) : (
            <>
              {hidden > 0 && (
                <p className="hint">좌표가 없거나 잘못된 장소 {hidden}곳은 지도에 표시하지 못했어요.</p>
              )}
              {days.map((d, dayIndex) => {
                const color = DAY_STYLES[dayIndex % DAY_STYLES.length].color
                return (
                  <section key={d.dayNo} aria-label={`${d.dayNo}일차`}>
                    <h3 className="course-day-title" style={{ borderLeftColor: color }}>
                      {d.dayNo}일차
                    </h3>
                    <ol className="stops">
                      {d.items.map((item) => (
                        <li key={item.id}>
                          <button type="button" className="stop" onClick={() => setSelectedId(item.place.id)}>
                            <span className="stop-no" style={{ background: color }}>
                              {item.visitOrder}
                            </span>
                            <span className="tile-body">
                              <span className="tile-title">{item.place.title}</span>
                              <span className="addr">{shortAddr(item.place.addr)}</span>
                              <span className="meta">
                                <span className={`env env-${item.place.env}`}>{ENV_LABEL[item.place.env]}</span>
                                <span>{typeName(item.place.contentTypeId)}</span>
                              </span>
                            </span>
                          </button>
                        </li>
                      ))}
                    </ol>
                  </section>
                )
              })}
            </>
          )}
        </div>
        <div className="course-map-view">
          <KakaoMap
            markers={[]}
            route={route}
            insetLeft={40}
            selectedId={selectedId}
            panelCollapsed={false}
            onSelect={setSelectedId}
            onOpenPanel={() => {}}
          />
        </div>
      </div>
    </Modal>
  )
}
