import { useEffect, useState } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { fetchMe, logout } from './api.ts'
import type { Me } from './api.ts'
import AuthModal from './components/AuthModal.tsx'
import Header from './components/Header.tsx'
import WithdrawModal from './components/WithdrawModal.tsx'
import MainPage from './pages/MainPage.tsx'

type OpenModal = 'auth' | 'withdraw' | null

/** 카카오 로그인에 실패하면 백엔드가 ?login=failed 를 붙여 돌려보낸다. 한 번 읽고 주소에서 지운다. */
function readLoginFailed(): boolean {
  const url = new URL(window.location.href)
  if (url.searchParams.get('login') !== 'failed') return false
  url.searchParams.delete('login')
  window.history.replaceState(null, '', url.pathname + url.search + url.hash)
  return true
}

/** 화면 전체의 틀. 헤더, 로그인 상태, 팝업은 여기서 관리하고 주소에 따라 페이지를 바꾼다. */
export default function App() {
  const [me, setMe] = useState<Me | null>(null)
  const [modal, setModal] = useState<OpenModal>(null)
  const [loginFailed, setLoginFailed] = useState(readLoginFailed)

  useEffect(() => {
    fetchMe().then(setMe)
  }, [])

  async function handleLogout() {
    try {
      await logout()
      setMe(null)
    } catch {
      // 실패하면 로그인 상태를 그대로 둔다
    }
  }

  function handleAuthSuccess(next: Me) {
    setMe(next)
    setModal(null)
    setLoginFailed(false)
  }

  function handleWithdrawn() {
    setMe(null)
    setModal(null)
  }

  return (
    <div className="app">
      <Header
        me={me}
        onLogin={() => setModal('auth')}
        onLogout={handleLogout}
        onWithdraw={() => setModal('withdraw')}
      />
      {loginFailed && (
        <div className="banner" role="alert">
          <span>카카오 로그인에 실패했어요. 다시 시도해 주세요.</span>
          <button type="button" className="link-btn" onClick={() => setLoginFailed(false)}>
            닫기
          </button>
        </div>
      )}

      <Routes>
        <Route path="/" element={<MainPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>

      {modal === 'auth' && <AuthModal onClose={() => setModal(null)} onSuccess={handleAuthSuccess} />}
      {modal === 'withdraw' && <WithdrawModal onClose={() => setModal(null)} onDone={handleWithdrawn} />}
    </div>
  )
}
