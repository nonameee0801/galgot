import type { ReactNode } from 'react'
import { ChevronLeft } from 'lucide-react'

interface Props {
  title: string
  collapsed: boolean
  onToggle: () => void
  children: ReactNode
}

/**
 * 지도 왼쪽(좁은 화면에서는 아래)에 뜨는 목록 패널. 접으면 지도를 넓게 볼 수 있다.
 * 접힘 상태는 지도도 알아야 해서(정보창, 범위 여백) 화면(MainPage)이 들고 있고, 이 브라우저에 기억한다.
 */
export default function SidePanel({ title, collapsed, onToggle, children }: Props) {
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
          onClick={onToggle}
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
