import { Link } from 'react-router'
import type { Me } from '../api'
import { clearListSearch } from '../nav'

interface Props {
  me: Me | null
  onLogin: () => void
  onLogout: () => void
}

/** 머리줄. 로그인하면 닉네임이 마이페이지(/my) 링크가 된다. 회원 탈퇴는 마이페이지로 옮겼다(기획서 18장 E). */
export default function Header({ me, onLogin, onLogout }: Props) {
  return (
    <header className="site-header">
      {/* 로고를 누르면 처음 화면으로. 기억해 둔 관광지 목록 조건도 지워 모두 초기값이 된다(기획서 18장 A) */}
      <h1 className="logo">
        <Link to="/" className="logo-link" onClick={clearListSearch}>
          갈곳
          <span className="logo-sub">날씨 맞춤 여행</span>
        </Link>
      </h1>
      {me ? (
        <div className="user">
          <Link to="/my" className="user-link" title="마이페이지">
            {me.nickname}님
          </Link>
          <button type="button" className="btn-line" onClick={onLogout}>
            로그아웃
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
