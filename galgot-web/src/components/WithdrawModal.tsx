import { useState } from 'react'
import { ApiError, withdraw } from '../api'
import Modal from './Modal'

interface Props {
  onClose: () => void
  onDone: () => void
}

/** 탈퇴 전 확인 창. 되돌릴 수 없다는 점을 먼저 알려 준다. */
export default function WithdrawModal({ onClose, onDone }: Props) {
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleWithdraw() {
    setSubmitting(true)
    setError(null)
    try {
      await withdraw()
      onDone()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '탈퇴하지 못했어요. 잠시 뒤에 다시 시도해 주세요.')
      setSubmitting(false)
    }
  }

  return (
    <Modal title="회원 탈퇴" onClose={onClose}>
      <p>탈퇴하면 내 코스와 담아 둔 장소가 모두 지워지고, 되돌릴 수 없어요.</p>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="modal-actions">
        <button type="button" className="btn-line" onClick={onClose} disabled={submitting}>
          취소
        </button>
        <button type="button" className="btn-danger" onClick={handleWithdraw} disabled={submitting}>
          {submitting ? '처리하는 중' : '탈퇴하기'}
        </button>
      </div>
    </Modal>
  )
}
