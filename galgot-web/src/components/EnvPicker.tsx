import { Building2, Sun, Trees, Umbrella } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { Env, EnvChoice } from '../api'
import { ENV_CHOICES } from '../labels'

/** 버튼 앞 아이콘: 실내는 건물, 실외는 나무, 둘 다는 둘을 함께 */
const ICONS: Record<EnvChoice, LucideIcon[]> = {
  INDOOR: [Building2],
  OUTDOOR: [Trees],
  ALL: [Building2, Trees],
}

interface Props {
  value: EnvChoice | null
  /** 그날 추천(INDOOR 또는 OUTDOOR). 아직 모르면 null */
  recommended: Env | null
  onChange: (next: EnvChoice) => void
  /** 장소 이름으로 검색하는 중이면 실내/실외로 거르지 않으므로 버튼을 흐리게 막는다 */
  disabled?: boolean
}

/**
 * [실내] [실외] [둘 다] 글자 버튼. 순서는 고정이다.
 * 직접 고르기 전에는 그날 추천 버튼이 눌린 상태로 보이고(서버도 env 없이 부르면 추천대로 보여 준다),
 * 추천 버튼에는 날씨 아이콘(비 오는 날 우산, 맑은 날 해) 배지와 강조 테두리를 붙인다. 항상 셋 중 하나가 눌려 있다.
 */
export default function EnvPicker({ value, recommended, onChange, disabled = false }: Props) {
  const selected = value ?? recommended
  return (
    <div className={disabled ? 'env-picker is-disabled' : 'env-picker'} role="group" aria-label="실내 실외 선택">
      {ENV_CHOICES.map((c) => {
        const isRecommended = recommended !== null && c.code === recommended
        return (
          <button
            key={c.code}
            type="button"
            className={isRecommended ? 'env-btn is-rec' : 'env-btn'}
            aria-pressed={selected === c.code}
            disabled={disabled}
            onClick={() => {
              if (selected !== c.code) onChange(c.code)
            }}
          >
            <span className="env-icons" aria-hidden="true">
              {ICONS[c.code].map((Icon, i) => (
                <Icon key={i} size={16} strokeWidth={2} />
              ))}
            </span>
            {c.name}
            {isRecommended && (
              <span className="rec-badge">
                {recommended === 'INDOOR' ? (
                  <Umbrella size={12} strokeWidth={2.5} aria-hidden="true" />
                ) : (
                  <Sun size={12} strokeWidth={2.5} aria-hidden="true" />
                )}
                추천
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}
