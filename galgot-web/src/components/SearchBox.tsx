import { useState } from 'react'
import type { FormEvent } from 'react'
import { Search, X } from 'lucide-react'

interface Props {
  /** 지금 검색어(주소의 q). 없으면 null */
  value: string | null
  /** 검색어를 정하거나(문자열) 검색을 푼다(null) */
  onSearch: (keyword: string | null) => void
}

/** 검색어 최대 길이(서버 KeywordSearch.MAX_LENGTH와 같다). */
const MAX_LENGTH = 50

/**
 * 관광지 목록의 장소 이름 검색칸(기획서 18장 B). Enter나 [검색]으로 찾고, ✕로 검색을 푼다.
 * 글자를 칠 때마다 찾지 않고 정했을 때만 찾는다(요청이 몰리지 않게).
 */
export default function SearchBox({ value, onSearch }: Props) {
  const [text, setText] = useState(value ?? '')
  // 주소의 검색어가 바뀌면(뒤로 가기, 로고, 탭 이동) 칸의 글자도 맞춘다
  const [synced, setSynced] = useState(value)
  if (synced !== value) {
    setSynced(value)
    setText(value ?? '')
  }

  function submit(e: FormEvent) {
    e.preventDefault()
    const keyword = text.trim().replace(/\s+/g, ' ')
    onSearch(keyword === '' ? null : keyword)
  }

  function clear() {
    setText('')
    onSearch(null)
  }

  return (
    <form className="search-box" role="search" onSubmit={submit}>
      <label className="sr-only" htmlFor="place-search">
        장소 이름 검색
      </label>
      <input
        id="place-search"
        type="search"
        value={text}
        maxLength={MAX_LENGTH}
        placeholder="장소 이름으로 찾기 (서울·경기 전체)"
        autoComplete="off"
        onChange={(e) => setText(e.target.value)}
      />
      {(text !== '' || value !== null) && (
        <button type="button" className="icon-btn search-clear" aria-label="검색 지우기" title="검색 지우기" onClick={clear}>
          <X size={16} aria-hidden="true" />
        </button>
      )}
      <button type="submit" className="btn-line search-submit">
        <Search size={16} aria-hidden="true" />
        검색
      </button>
    </form>
  )
}
