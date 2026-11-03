import { useNavigate } from 'react-router'
import Modal from './Modal'

interface Props {
  /** 저장한 코스 이름 */
  title: string
  courseId: number
  /** [괜찮아요]나 닫기(Esc, ×, 바깥)를 눌렀을 때 */
  onLater: () => void
}

/**
 * 코스를 통째로 저장했을 때 띄우는 팝업(날씨기반 추천 저장, 공유 코스 저장).
 * [내 코스 보러 가기]는 저장한 코스 상세로, [괜찮아요]는 부른 화면이 정한 곳으로 간다.
 */
export default function SavedModal({ title, courseId, onLater }: Props) {
  const navigate = useNavigate()
  return (
    <Modal title="내 코스에 저장했어요" onClose={onLater}>
      <p className="saved-text">
        ‘{title}’ 코스를 내 코스에 저장했어요.
        <br />
        지금 보러 갈까요?
      </p>
      <div className="modal-actions">
        <button type="button" className="btn-line" onClick={onLater}>
          괜찮아요
        </button>
        <button type="button" className="btn-fill" onClick={() => navigate(`/my/courses/${courseId}`)}>
          내 코스 보러 가기
        </button>
      </div>
    </Modal>
  )
}
