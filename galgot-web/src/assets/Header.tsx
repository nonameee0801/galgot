import { loginUrl, type Me } from '../api'

interface Props {
  me: Me | null
  onLogout: () => void
}

export default function Header({ me, onLogout }: Props) {
  return (
    <header className="site-header">
      <h1 className="logo">갈곳</h1>
      {me ? (
        <div className="user">
          <span>{me.nickname}님</span>
          <button type="button" className="btn-line" onClick={onLogout}>
            로그아웃
          </button>
        </div>
      ) : (
        <a className="btn-kakao" href={loginUrl}>
          카카오로 로그인
        </a>
      )}
    </header>
  )
}
