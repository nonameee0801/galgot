import { useState } from 'react'
import { ArrowDown, ArrowUp, MapPin, Trash2 } from 'lucide-react'
import type { CourseDetail, CoursePlaceItem } from '../api'
import { allDays } from '../course'
import { DAY_STYLES, ENV_LABEL, shortAddr, typeName } from '../labels'
import Modal from './Modal'
import StopPhoto from './StopPhoto'

interface Editing {
  /** 같은 일차 안에서 한 칸 위(-1)나 아래(+1)로 */
  onMove: (item: CoursePlaceItem, step: -1 | 1) => void
  /** 확인 팝업에서 [네]를 누른 뒤에 부른다 */
  onRemove: (item: CoursePlaceItem) => void
  /** 저장 중이면 버튼을 잠근다 */
  busy: boolean
}

interface Props {
  course: CourseDetail
  /** [지도에서 보기]: 그 장소를 고른 채로 코스 지도 팝업을 연다 */
  onShowOnMap: (placeId: number) => void
  /** 없으면 읽기 전용(공유 코스 보기) */
  editing?: Editing
}

/** "2026-11-03"에서 n일 뒤 날짜를 "11/5 (목)"처럼. 시간대에 흔들리지 않게 UTC로 계산한다. */
function dayLabel(travelDate: string | null, dayNo: number): string | null {
  if (travelDate === null) return null
  const [y, m, d] = travelDate.split('-').map(Number)
  const date = new Date(Date.UTC(y, m - 1, d + dayNo - 1))
  const week = '일월화수목금토'[date.getUTCDay()]
  return `${date.getUTCMonth() + 1}/${date.getUTCDate()} (${week})`
}

/**
 * 코스의 일차별 장소 목록(기획서 18장 E). 내 코스 상세와 공유 코스 보기가 함께 쓴다.
 * 카드 오른쪽에 순서 바꾸기(↑ ↓)와 [삭제]를 두고, 삭제는 확인 팝업([네] [아니요])을 거친다.
 * [지도에서 보기]는 페이지를 옮기지 않고 코스 지도 팝업을 그 장소가 고른 채로 연다.
 */
export default function CourseDays({ course, onShowOnMap, editing }: Props) {
  const [confirming, setConfirming] = useState<CoursePlaceItem | null>(null)
  // 일정의 모든 일차를 보여 주고, 장소가 없는 일차는 안내만 한다(기획서 18장 D)
  const days = allDays(course.places, course.tripDays)

  return (
    <div className="course-days">
      {days.map((day, dayIndex) => {
        const date = dayLabel(course.travelDate, day.dayNo)
        const color = DAY_STYLES[dayIndex % DAY_STYLES.length].color
        if (day.items.length === 0) {
          return (
            <section key={day.dayNo} className="course-day" aria-label={`${day.dayNo}일차`}>
              <h3 className="course-day-title" style={{ borderLeftColor: color }}>
                {day.dayNo}일차{date && <span className="muted"> · {date}</span>}
              </h3>
              <p className="muted course-day-empty">아직 담은 장소가 없어요.</p>
            </section>
          )
        }
        return (
          <section key={day.dayNo} className="course-day" aria-label={`${day.dayNo}일차`}>
            <h3 className="course-day-title" style={{ borderLeftColor: color }}>
              {day.dayNo}일차{date && <span className="muted"> · {date}</span>}
            </h3>
            <ol className="course-stops">
              {day.items.map((item, i) => (
                <li key={item.id} className={editing ? 'course-stop is-editable' : 'course-stop'}>
                  <StopPhoto place={item.place} order={item.visitOrder} color={color} />
                  <span className="tile-body">
                    <span className="tile-title">{item.place.title}</span>
                    <span className="addr">{shortAddr(item.place.addr)}</span>
                    <span className="meta">
                      <span className={`env env-${item.place.env}`}>{ENV_LABEL[item.place.env]}</span>
                      <span>{typeName(item.place.contentTypeId)}</span>
                    </span>
                    {item.memo && <span className="course-memo">{item.memo}</span>}
                    <button type="button" className="link-btn map-link" onClick={() => onShowOnMap(item.place.id)}>
                      <MapPin size={14} aria-hidden="true" />
                      지도에서 보기
                    </button>
                  </span>
                  {editing && (
                    <span className="course-stop-actions">
                      <button
                        type="button"
                        className="icon-btn small"
                        aria-label={`${item.place.title} 위로`}
                        title="위로"
                        disabled={editing.busy || i === 0}
                        onClick={() => editing.onMove(item, -1)}
                      >
                        <ArrowUp size={16} aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        className="icon-btn small"
                        aria-label={`${item.place.title} 아래로`}
                        title="아래로"
                        disabled={editing.busy || i === day.items.length - 1}
                        onClick={() => editing.onMove(item, 1)}
                      >
                        <ArrowDown size={16} aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        className="btn-line danger small-btn"
                        disabled={editing.busy}
                        onClick={() => setConfirming(item)}
                      >
                        <Trash2 size={14} aria-hidden="true" />
                        삭제
                      </button>
                    </span>
                  )}
                </li>
              ))}
            </ol>
          </section>
        )
      })}

      {editing && confirming && (
        <ConfirmModal
          title="장소 삭제"
          message={`"${confirming.place.title}"을(를) 코스에서 삭제할까요?`}
          onYes={() => {
            const item = confirming
            setConfirming(null)
            editing.onRemove(item)
          }}
          onNo={() => setConfirming(null)}
        />
      )}
    </div>
  )
}

/** [네] [아니요] 확인 팝업. 브라우저 기본 확인 창(confirm) 대신 화면 안의 팝업을 쓴다. */
export function ConfirmModal({
  title,
  message,
  onYes,
  onNo,
  busy = false,
}: {
  title: string
  message: string
  onYes: () => void
  onNo: () => void
  busy?: boolean
}) {
  return (
    <Modal title={title} onClose={onNo}>
      <p>{message}</p>
      <div className="modal-actions">
        <button type="button" className="btn-line" onClick={onNo}>
          아니요
        </button>
        <button type="button" className="btn-more danger-fill" onClick={onYes} disabled={busy}>
          네
        </button>
      </div>
    </Modal>
  )
}
