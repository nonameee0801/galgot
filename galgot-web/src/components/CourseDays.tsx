import { useState } from 'react'
import type { ReactNode } from 'react'
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useDroppable,
  useSensor,
  useSensors,
} from '@dnd-kit/core'
import type { Announcements, DragEndEvent, DragOverEvent, DragStartEvent, UniqueIdentifier } from '@dnd-kit/core'
import { SortableContext, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { GripVertical, MapPin, Trash2 } from 'lucide-react'
import type { CourseDetail, CoursePlaceItem } from '../api'
import { dayOf, layoutChanges, layoutOf, moveInLayout } from '../course'
import type { DayLayout } from '../course'
import { DAY_STYLES, ENV_LABEL, districtOf, shortAddr, typeName } from '../labels'
import Modal from './Modal'
import StopPhoto from './StopPhoto'

type Change = { coursePlaceId: number; dayNo: number; visitOrder: number }

interface Editing {
  /** 끌어 옮긴 결과(일차나 순서가 바뀐 항목만)를 저장한다. 성공하면 true */
  onReorder: (changes: Change[]) => Promise<boolean>
  /** 확인 팝업에서 [네]를 누른 뒤에 부른다 */
  onRemove: (item: CoursePlaceItem) => void
  /** 저장 중이면 끌기와 버튼을 잠근다 */
  busy: boolean
}

interface Props {
  course: CourseDetail
  /** 지도에서 보기: 그 장소를 고른 채로 코스 지도 팝업을 연다 */
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

/** 일차 목록(빈 일차 포함)의 놓을 자리 id. 항목 id(숫자)와 겹치지 않게 글자로 */
const dayDropId = (dayNo: number) => `day-${dayNo}`

function dropDay(layout: DayLayout, id: UniqueIdentifier): number | null {
  if (typeof id === 'string' && id.startsWith('day-')) return Number(id.slice(4))
  return dayOf(layout, Number(id))
}

/** 카드 안쪽: 큰 사진(위에 방문 순서), 이름, "실외 · 관광지 · 과천시", 주소, 메모, 오른쪽 아이콘 버튼 */
function StopCard({
  item,
  order,
  color,
  handle,
  actions,
}: {
  item: CoursePlaceItem
  order: number
  color: string
  handle?: ReactNode
  actions: ReactNode
}) {
  const district = districtOf(item.place.addr)
  return (
    <>
      {handle}
      <StopPhoto place={item.place} order={order} color={color} large />
      <span className="stop-body">
        <span className="tile-title">{item.place.title}</span>
        <span className="stop-meta">
          <span className={`env env-${item.place.env}`}>{ENV_LABEL[item.place.env]}</span>
          {' · '}
          {typeName(item.place.contentTypeId)}
          {district && ` · ${district}`}
        </span>
        <span className="addr">{shortAddr(item.place.addr)}</span>
        {item.memo && <span className="course-memo">{item.memo}</span>}
      </span>
      <span className="course-stop-actions">{actions}</span>
    </>
  )
}

/** 끌 수 있는 카드. 왼쪽 손잡이(⋮⋮)로만 끌린다(카드 안 버튼을 누를 때 끌리지 않게). 키보드: 손잡이에서 스페이스, 방향키 */
function SortableStop({
  item,
  order,
  color,
  disabled,
  actions,
}: {
  item: CoursePlaceItem
  order: number
  color: string
  disabled: boolean
  actions: ReactNode
}) {
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id: item.id,
    disabled,
  })
  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={isDragging ? 'course-stop is-editable is-placeholder' : 'course-stop is-editable'}
    >
      <StopCard
        item={item}
        order={order}
        color={color}
        actions={actions}
        handle={
          <button
            type="button"
            ref={setActivatorNodeRef}
            className="drag-handle"
            aria-label={`${item.place.title} 옮기기`}
            title="끌어서 옮기기"
            disabled={disabled}
            {...attributes}
            {...listeners}
          >
            <GripVertical size={18} aria-hidden="true" />
          </button>
        }
      />
    </li>
  )
}

/** 한 일차의 목록. 빈 일차에도 끌어다 놓을 수 있게 목록 전체를 놓을 자리로 둔다 */
function DayDrop({ dayNo, empty, children }: { dayNo: number; empty: boolean; children: ReactNode }) {
  const { setNodeRef, isOver } = useDroppable({ id: dayDropId(dayNo) })
  return (
    <ol ref={setNodeRef} className={`course-stops${empty ? ' is-empty' : ''}${isOver ? ' is-over' : ''}`}>
      {children}
      {empty && <li className="course-day-empty muted">아직 담은 장소가 없어요. 여기로 끌어다 놓을 수 있어요.</li>}
    </ol>
  )
}

/** 스크린 리더 안내(기본은 영어라 한국어로) */
function announcements(title: (id: UniqueIdentifier) => string): Announcements {
  return {
    onDragStart: ({ active }) => `${title(active.id)}을(를) 들었어요. 방향키로 옮기고 스페이스로 놓아요.`,
    onDragOver: ({ active, over }) => (over ? `${title(active.id)}을(를) 옮기는 중이에요.` : undefined),
    onDragEnd: ({ active, over }) =>
      over ? `${title(active.id)}을(를) 놓았어요.` : `${title(active.id)}을(를) 제자리에 두었어요.`,
    onDragCancel: ({ active }) => `옮기기를 그만뒀어요. ${title(active.id)}은(는) 제자리에 있어요.`,
  }
}

/**
 * 코스의 일차별 장소 목록(기획서 18장 E, H). 내 코스 상세와 공유 코스 보기가 함께 쓴다.
 * 카드는 큰 사진(위에 방문 순서), 이름, "실외 · 관광지 · 과천시", 주소. 오른쪽에 아이콘 버튼
 * (지도에서 보기, 내 코스면 삭제). 삭제는 확인 팝업([네] [아니요])을 거친다.
 * 내 코스면 카드 왼쪽 손잡이를 끌어 같은 일차 안 순서나 다른 일차로 옮긴다. 놓으면 바뀐 항목만 한 번에 저장하고,
 * 실패하면 원래 자리로 돌아간다.
 */
export default function CourseDays({ course, onShowOnMap, editing }: Props) {
  const [confirming, setConfirming] = useState<CoursePlaceItem | null>(null)
  // 끄는 중이거나 저장을 기다리는 동안의 배치. 서버에서 새 코스가 오면(course가 바뀌면) 저절로 버려진다
  const [pending, setPending] = useState<{ course: CourseDetail; layout: DayLayout } | null>(null)
  const [activeId, setActiveId] = useState<number | null>(null)
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )

  const initial = layoutOf(course.places, course.tripDays)
  const layout = pending !== null && pending.course === course ? pending.layout : initial
  const byId = new Map(course.places.map((p) => [p.id, p]))
  const dayNos = Object.keys(layout)
    .map(Number)
    .sort((a, b) => a - b)
  const titleOf = (id: UniqueIdentifier) => byId.get(Number(id))?.place.title ?? '장소'

  function actionsFor(item: CoursePlaceItem): ReactNode {
    return (
      <>
        <button
          type="button"
          className="icon-btn small"
          aria-label={`${item.place.title} 지도에서 보기`}
          title="지도에서 보기"
          onClick={() => onShowOnMap(item.place.id)}
        >
          <MapPin size={17} aria-hidden="true" />
        </button>
        {editing && (
          <button
            type="button"
            className="icon-btn small danger-icon"
            aria-label={`${item.place.title} 삭제`}
            title="삭제"
            disabled={editing.busy}
            onClick={() => setConfirming(item)}
          >
            <Trash2 size={17} aria-hidden="true" />
          </button>
        )}
      </>
    )
  }

  function dayHead(dayNo: number, index: number) {
    const date = dayLabel(course.travelDate, dayNo)
    return (
      <h3 className="course-day-title" style={{ borderLeftColor: DAY_STYLES[index % DAY_STYLES.length].color }}>
        {dayNo}일차{date && <span className="muted"> · {date}</span>}
      </h3>
    )
  }

  function onDragStart({ active }: DragStartEvent) {
    setActiveId(Number(active.id))
    setPending({ course, layout })
  }

  // 다른 일차 목록 위로 오면 바로 그 일차로 옮겨 보여 준다(놓기 전에도 들어갈 자리가 보이게)
  function onDragOver({ active, over }: DragOverEvent) {
    if (!over) return
    const id = Number(active.id)
    const from = dayOf(layout, id)
    const to = dropDay(layout, over.id)
    if (from === null || to === null || from === to) return
    const overIndex = typeof over.id === 'number' ? layout[to].indexOf(over.id) : -1
    setPending({ course, layout: moveInLayout(layout, id, to, overIndex < 0 ? layout[to].length : overIndex) })
  }

  async function onDragEnd({ active, over }: DragEndEvent) {
    setActiveId(null)
    if (!over || !editing) {
      setPending(null)
      return
    }
    const id = Number(active.id)
    const to = dropDay(layout, over.id)
    let next = layout
    if (to !== null && typeof over.id === 'number' && over.id !== id) {
      next = moveInLayout(layout, id, to, layout[to].indexOf(over.id))
    }
    const changes = layoutChanges(course.places, next)
    if (changes.length === 0) {
      setPending(null)
      return
    }
    setPending({ course, layout: next })
    const ok = await editing.onReorder(changes)
    if (!ok) setPending(null)
  }

  if (!editing) {
    // 읽기 전용(공유 코스 보기): 끌기 없이 목록만
    return (
      <div className="course-days">
        {dayNos.map((dayNo, index) => {
          const items = layout[dayNo].map((id) => byId.get(id)).filter((x) => x !== undefined)
          const color = DAY_STYLES[index % DAY_STYLES.length].color
          return (
            <section key={dayNo} className="course-day" aria-label={`${dayNo}일차`}>
              {dayHead(dayNo, index)}
              {items.length === 0 ? (
                <p className="muted course-day-empty">아직 담은 장소가 없어요.</p>
              ) : (
                <ol className="course-stops">
                  {items.map((item, i) => (
                    <li key={item.id} className="course-stop">
                      <StopCard item={item} order={i + 1} color={color} actions={actionsFor(item)} />
                    </li>
                  ))}
                </ol>
              )}
            </section>
          )
        })}
      </div>
    )
  }

  const activeItem = activeId !== null ? byId.get(activeId) : undefined
  const activeDay = activeId !== null ? dayOf(layout, activeId) : null
  const activeIndex = activeDay !== null ? dayNos.indexOf(activeDay) : 0

  return (
    <div className="course-days">
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        accessibility={{
          announcements: announcements(titleOf),
          screenReaderInstructions: {
            draggable: '스페이스를 누르면 들고, 방향키로 옮긴 뒤 스페이스로 놓아요. Esc를 누르면 그만둬요.',
          },
        }}
        onDragStart={onDragStart}
        onDragOver={onDragOver}
        onDragEnd={(e) => void onDragEnd(e)}
        onDragCancel={() => {
          setActiveId(null)
          setPending(null)
        }}
      >
        {dayNos.map((dayNo, index) => {
          const ids = layout[dayNo]
          const color = DAY_STYLES[index % DAY_STYLES.length].color
          return (
            <section key={dayNo} className="course-day" aria-label={`${dayNo}일차`}>
              {dayHead(dayNo, index)}
              <SortableContext id={dayDropId(dayNo)} items={ids} strategy={verticalListSortingStrategy}>
                <DayDrop dayNo={dayNo} empty={ids.length === 0}>
                  {ids.map((id, i) => {
                    const item = byId.get(id)
                    if (!item) return null
                    return (
                      <SortableStop
                        key={id}
                        item={item}
                        order={i + 1}
                        color={color}
                        disabled={editing.busy}
                        actions={actionsFor(item)}
                      />
                    )
                  })}
                </DayDrop>
              </SortableContext>
            </section>
          )
        })}

        {/* 끄는 동안 손에 들린 카드(살짝 기울고 그림자) */}
        <DragOverlay>
          {activeItem && activeDay !== null ? (
            <div className="course-stop is-editable is-lifted">
              <StopCard
                item={activeItem}
                order={layout[activeDay].indexOf(activeItem.id) + 1}
                color={DAY_STYLES[activeIndex % DAY_STYLES.length].color}
                actions={null}
                handle={
                  <span className="drag-handle">
                    <GripVertical size={18} aria-hidden="true" />
                  </span>
                }
              />
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>

      {confirming && (
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
