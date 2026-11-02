interface Props {
  /** 지도를 못 불러왔을 때 보여 줄 안내. 없으면 연한 배경만 깐다. */
  message?: string
}

/** 지도 자리. 카카오 지도를 불러오는 중이거나 불러오지 못했을 때 보인다. 목록은 그대로 쓸 수 있다. */
export default function MapPlaceholder({ message }: Props) {
  return (
    <div className="map" aria-hidden={message ? undefined : true}>
      {message && (
        <p className="map-message" role="status">
          {message}
        </p>
      )}
    </div>
  )
}
