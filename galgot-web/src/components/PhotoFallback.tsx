import { Building2, Trees } from 'lucide-react'
import type { Env } from '../api'

/** 사진이 없는 장소의 대체 그림: 연한 바탕에 실내는 건물, 실외는 나무, 실내외는 둘 다(기획서 19장) */
export default function PhotoFallback({ env }: { env: Env }) {
  return (
    <span className={`photo-fallback photo-fallback-${env}`} aria-hidden="true">
      {env !== 'OUTDOOR' && <Building2 size={28} strokeWidth={1.6} />}
      {env !== 'INDOOR' && <Trees size={28} strokeWidth={1.6} />}
    </span>
  )
}
