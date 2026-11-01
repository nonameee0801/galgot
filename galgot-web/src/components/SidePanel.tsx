import { useEffect, useState, type ReactNode } from 'react'
import { ChevronLeft } from 'lucide-react'

const STORAGE_KEY = 'galgot.panelCollapsed'

/** 저장소를 쓸 수 없는 환경(사생활 보호 모드 등)에서도 화면은 그대로 동작해야 한다. */
function readCollapsed(): boolean {
  try {
    return window.localStorage.getItem(STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

interface Props {
  title: string
  children: ReactNode
}

/**
 * 지도 왼쪽(좁은 화면에서는 아래)에 뜨는 목록 패널.
 * 접으면 지도를 넓게 볼 수 있고, 접힘 상태는 이 브라우저에 기억한다.
 */
export default function SidePanel({ title, children }: Props) {
  const [collapsed, setCollapsed] = useState(readCollapsed)

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, collapsed ? '1' : '0')
    } catch {
      // 저장하지 못해도 이번 화면에서는 그대로 동작한다
    }
  }, [collapsed])

  const label = collapsed ? `${title} 펼치기` : `${title} 접기`

  return (
    <aside className="panel" data-collapsed={collapsed} aria-label={title}>
      <div className="panel-head">
        <h2 className="panel-title">{title}</h2>
        <button
          type="button"
          className="icon-btn panel-toggle"
          aria-expanded={!collapsed}
          aria-controls="panel-body"
          aria-label={label}
          title={label}
          onClick={() => setCollapsed((c) => !c)}
        >
          <ChevronLeft size={20} aria-hidden="true" />
        </button>
      </div>
      <div className="panel-body" id="panel-body">
        {children}
      </div>
    </aside>
  )
}
