import { useEffect, useState } from 'react'
import { Navigate, Route, Routes, useNavigate, useSearchParams } from 'react-router'
import { RETURN_TO_KEY, fetchMe, logout } from './api.ts'
import type { Me } from './api.ts'
import AuthModal from './components/AuthModal.tsx'
import Header from './components/Header.tsx'
import WithdrawModal from './components/WithdrawModal.tsx'
import CoursePage from './pages/CoursePage.tsx'
import MainPage from './pages/MainPage.tsx'
import MyPage from './pages/MyPage.tsx'
import SharedCoursePage from './pages/SharedCoursePage.tsx'

type OpenModal = 'auth' | 'withdraw' | null

/** 카카오 로그인 전에 저장해 둔 주소를 한 번 꺼내고 지운다. 이 사이트 안의 경로(/...)만 받는다. */
function takeReturnTo(): string | null {
  try {
    const saved = window.sessionStorage.getItem(RETURN_TO_KEY)
    window.sessionStorage.removeItem(RETURN_TO_KEY)
    return saved !== null && saved.startsWith('/') && !saved.startsWith('//') ? saved : null
  } catch {
    return null
  }
}

/** 화면 전체의 틀. 헤더, 로그인 상태, 팝업은 여기서 관리하고 주소에 따라 페이지를 바꾼다. */
export default function App() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [me, setMe] = useState<Me | null>(null)
  // 로그인 여부를 확인했는지. 확인 전에는 마이페이지가 "로그인이 필요해요"를 잠깐 띄우지 않게 기다린다
  const [meLoaded, setMeLoaded] = useState(false)
  const [modal, setModal] = useState<OpenModal>(null)
  // 카카오 로그인에 실패하면 백엔드가 ?login=failed 를 붙여 돌려보낸다
  const [loginFailed, setLoginFailed] = useState(() => params.get('login') === 'failed')

  useEffect(() => {
    fetchMe().then((next) => {
      setMe(next)
      setMeLoaded(true)
    })
  }, [])

  // 카카오 로그인에서 돌아왔으면 로그인 전에 보던 주소로 되돌리고, ?login=failed 는 주소에서 지운다(처음 한 번만)
  useEffect(() => {
    const returnTo = takeReturnTo()
    if (returnTo !== null) {
      navigate(returnTo, { replace: true })
    } else if (window.location.search.includes('login=')) {
      const p = new URLSearchParams(window.location.search)
      p.delete('login')
      const search = p.toString()
      navigate({ pathname: window.location.pathname, search: search ? `?${search}` : '' }, { replace: true })
    }
  }, [navigate])

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
    navigate('/', { replace: true })
  }

  return (
    <div className="app">
      <Header
        me={me}
        onLogin={() => setModal('auth')}
        onLogout={handleLogout}
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
        {/* 날씨기반 추천의 "내 코스에 저장"에 로그인 상태와 로그인 창이 필요하다 */}
        <Route path="/" element={<MainPage me={me} onLogin={() => setModal('auth')} />} />
        <Route path="/places/:id" element={<MainPage me={me} onLogin={() => setModal('auth')} />} />
        {/* 마이페이지와 내 코스(로그인 필요), 공유 코스 보기(로그인 불필요). 기획서 18장 E */}
        <Route
          path="/my"
          element={
            <MyPage
              me={me}
              meLoaded={meLoaded}
              onLogin={() => setModal('auth')}
              onWithdraw={() => setModal('withdraw')}
              onMeChange={setMe}
            />
          }
        />
        <Route
          path="/my/courses/:id"
          element={<CoursePage me={me} meLoaded={meLoaded} onLogin={() => setModal('auth')} />}
        />
        <Route
          path="/s/:shareCode"
          element={<SharedCoursePage me={me} meLoaded={meLoaded} onLogin={() => setModal('auth')} />}
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>

      {modal === 'auth' && <AuthModal onClose={() => setModal(null)} onSuccess={handleAuthSuccess} />}
      {modal === 'withdraw' && <WithdrawModal onClose={() => setModal(null)} onDone={handleWithdrawn} />}
    </div>
  )
}
