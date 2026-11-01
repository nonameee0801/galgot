import { Sun, Umbrella } from 'lucide-react'
import type { Env, EnvChoice } from '../api'
import { ENV_CHOICES } from '../labels'

interface Props {
  value: EnvChoice | null
  /** 그날 추천(INDOOR 또는 OUTDOOR). 아직 모르면 null */
  recommended: Env | null
  onChange: (next: EnvChoice) => void
}

/**
 * [실내] [실외] [둘 다] 글자 버튼. 순서는 고정이다.
 * 직접 고르기 전에는 그날 추천 버튼이 눌린 상태로 보이고(서버도 env 없이 부르면 추천대로 보여 준다),
 * 추천 버튼에는 날씨 아이콘(비 오는 날 우산, 맑은 날 해) 배지와 강조 테두리를 붙인다. 항상 셋 중 하나가 눌려 있다.
 */
export default function EnvPicker({ value, recommended, onChange }: Props) {
  const selected = value ?? recommended
  return (
    <div className="env-picker" role="group" aria-label="실내 실외 선택">
      {ENV_CHOICES.map((c) => {
        const isRecommended = recommended !== null && c.code === recommended
        return (
          <button
            key={c.code}
            type="button"
            className={isRecommended ? 'env-btn is-rec' : 'env-btn'}
            aria-pressed={selected === c.code}
            onClick={() => {
              if (selected !== c.code) onChange(c.code)
            }}
          >
            {c.name}
            {isRecommended && (
              <span className="rec-badge">
                {recommended === 'INDOOR' ? (
                  <Umbrella size={16} strokeWidth={2.5} aria-hidden="true" />
                ) : (
                  <Sun size={16} strokeWidth={2.5} aria-hidden="true" />
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
