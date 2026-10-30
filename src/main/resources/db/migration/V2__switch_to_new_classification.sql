-- 갈곳 V2: TourAPI 신분류체계(lclsSystm)와 법정동코드(lDongRegnCd/lDongSignguCd) 기준으로 전환
-- 실제 응답에서 cat1~3, areacode, sigungucode는 비어 있고 신규 필드만 채워져 있음을 확인함

-- places: 지역 코드 컬럼 이름 변경 + 타입을 문자열로 (코드가 문자열 값)
ALTER TABLE places RENAME INDEX idx_places_area_env TO idx_places_region_env;

ALTER TABLE places
    CHANGE COLUMN area_code    ldong_regn_cd  VARCHAR(5)  NULL,
    CHANGE COLUMN sigungu_code ldong_signgu_cd VARCHAR(5) NULL;

-- places: 분류 컬럼 이름 변경 (cat1~3 -> lcls_systm1~3)
ALTER TABLE places
    CHANGE COLUMN cat1 lcls_systm1 VARCHAR(10) NULL,
    CHANGE COLUMN cat2 lcls_systm2 VARCHAR(10) NULL,
    CHANGE COLUMN cat3 lcls_systm3 VARCHAR(20) NULL;

-- places: 응답에서 확인된 추가 필드
ALTER TABLE places
    ADD COLUMN thumbnail_url      VARCHAR(500) NULL AFTER image_url,
    ADD COLUMN copyright_type     VARCHAR(10)  NULL AFTER thumbnail_url,
    ADD COLUMN source_modified_at DATETIME     NULL AFTER copyright_type;

-- courses: 지역 코드를 법정동 시도 코드(문자열)로
ALTER TABLE courses
    CHANGE COLUMN area_code ldong_regn_cd VARCHAR(5) NULL;

-- weather_forecasts: 지역 코드를 법정동 시도 코드(문자열)로
ALTER TABLE weather_forecasts RENAME INDEX uk_weather_area_date TO uk_weather_region_date;

ALTER TABLE weather_forecasts
    CHANGE COLUMN area_code ldong_regn_cd VARCHAR(5) NOT NULL;
