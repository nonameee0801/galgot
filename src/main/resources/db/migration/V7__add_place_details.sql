-- 갈곳 V7: 장소 상세 정보 (기획서 17장)
-- TourAPI 상세 API(detailCommon2, detailIntro2)의 응답을 정리해 장소당 1행으로 저장한다.
-- 누군가 상세를 처음 열 때 받아 저장하고, fetched_at이 오래되면(기본 30일) 다시 받는다.
-- 유형마다 다른 응답 항목 이름(usetime, usetimeculture, usetimeleports 등)은 하나의 컬럼으로 모은다.
-- homepage는 받은 글에서 http, https 주소만 뽑아 줄바꿈으로 이어 저장한다(원문은 저장하지 않는다).
-- TourAPI가 정보를 하나도 주지 않은 장소도 빈 행으로 저장해 다시 부르지 않게 한다.
-- 긴 값은 저장할 때 1000자에서 자른다(소개글은 TEXT).

CREATE TABLE place_details (
    place_id     BIGINT        NOT NULL,
    overview     TEXT          NULL,
    use_time     VARCHAR(1000) NULL,
    rest_date    VARCHAR(1000) NULL,
    use_fee      VARCHAR(1000) NULL,
    parking      VARCHAR(1000) NULL,
    parking_fee  VARCHAR(1000) NULL,
    contact      VARCHAR(1000) NULL,
    homepage     VARCHAR(1000) NULL,
    open_period  VARCHAR(1000) NULL,
    reservation  VARCHAR(1000) NULL,
    fetched_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (place_id),
    CONSTRAINT fk_place_details_place FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
