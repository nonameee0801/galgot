import { Link } from 'react-router'
import type { Me } from '../api'
import { clearListSearch } from '../nav'

interface Props {
  me: Me | null
  onLogin: () => void
  onLogout: () => void
  onWithdraw: () => void
}

export default function Header({ me, onLogin, onLogout, onWithdraw }: Props) {
  return (
    <header className="site-header">
      {/* 로고를 누르면 처음 화면으로. 기억해 둔 관광지 목록 조건도 지워 모두 초기값이 된다(기획서 18장 A) */}
      <h1 className="logo">
        <Link to="/" className="logo-link" onClick={clearListSearch}>
          갈곳
        </Link>
      </h1>
      {me ? (
        <div className="user">
          <span>{me.nickname}님</span>
          <button type="button" className="btn-line" onClick={onLogout}>
            로그아웃
          </button>
          <button type="button" className="link-btn" onClick={onWithdraw}>
            회원 탈퇴
          </button>
        </div>
      ) : (
        <button type="button" className="btn-line" onClick={onLogin}>
          로그인
        </button>
      )}
    </header>
  )
}
