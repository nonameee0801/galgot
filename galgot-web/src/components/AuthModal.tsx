import { useState, type FormEvent } from 'react'
import { ApiError, isValidPassword, login, loginUrl, signup, type Me } from '../api'
import Modal from './Modal'

type Mode = 'login' | 'signup'

interface Props {
  onClose: () => void
  onSuccess: (me: Me) => void
}

/** 로그인과 회원가입 창. 카카오 버튼과 이메일 폼이 있고, 아래 버튼으로 두 모드를 오간다. */
export default function AuthModal({ onClose, onSuccess }: Props) {
  const [mode, setMode] = useState<Mode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const isSignup = mode === 'signup'

  function switchMode() {
    setMode(isSignup ? 'login' : 'signup')
    setError(null)
    setPassword('')
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (isSignup && !isValidPassword(password)) {
      setError('비밀번호는 영문과 숫자를 섞어 8~64자로 입력해 주세요.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const me = isSignup ? await signup(email, password, nickname) : await login(email, password)
      onSuccess(me)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '요청을 처리하지 못했어요. 잠시 뒤에 다시 시도해 주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title={isSignup ? '회원가입' : '로그인'} onClose={onClose}>
      <a className="btn-kakao btn-block" href={loginUrl}>
        카카오로 시작하기
      </a>
      <p className="divider">또는 이메일로</p>

      <form className="form" onSubmit={handleSubmit}>
        <label className="field">
          <span>이메일</span>
          <input
            type="email"
            autoComplete="email"
            required
            maxLength={255}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>

        {isSignup && (
          <label className="field">
            <span>
              닉네임 <small>(선택)</small>
            </span>
            <input
              type="text"
              autoComplete="nickname"
              maxLength={50}
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
            />
          </label>
        )}

        <label className="field">
          <span>비밀번호</span>
          <input
            type="password"
            autoComplete={isSignup ? 'new-password' : 'current-password'}
            required
            maxLength={64}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {isSignup && <small className="hint">영문과 숫자를 섞어 8~64자</small>}
        </label>

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        <button type="submit" className="btn-more btn-block" disabled={submitting}>
          {submitting ? '처리하는 중' : isSignup ? '가입하기' : '로그인'}
        </button>
      </form>

      <p className="switch">
        {isSignup ? '이미 계정이 있나요?' : '처음이신가요?'}{' '}
        <button type="button" className="link-btn" onClick={switchMode}>
          {isSignup ? '로그인' : '회원가입'}
        </button>
      </p>
    </Modal>
  )
}
