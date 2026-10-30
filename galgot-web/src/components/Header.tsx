import type { Me } from '../api'

interface Props {
  me: Me | null
  onLogin: () => void
  onLogout: () => void
  onWithdraw: () => void
}

export default function Header({ me, onLogin, onLogout, onWithdraw }: Props) {
  return (
    <header className="site-header">
      <h1 className="logo">갈곳</h1>
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
