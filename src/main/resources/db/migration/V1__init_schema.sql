-- 갈곳 V1: 초기 스키마 (MySQL 8, utf8mb4)

CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nickname    VARCHAR(50)  NOT NULL,
    email       VARCHAR(255) NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE social_accounts (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_social_provider_user (provider, provider_user_id),
    CONSTRAINT fk_social_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 실내/야외 분류 규칙: rule_type = PLACE | CATEGORY | KEYWORD, env = INDOOR | OUTDOOR | BOTH
CREATE TABLE env_rules (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    rule_type   VARCHAR(20)  NOT NULL,
    match_value VARCHAR(100) NOT NULL,
    env         VARCHAR(10)  NOT NULL,
    priority    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_env_rule (rule_type, match_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TourAPI 장소. 분류 컬럼(cat1~3)은 신분류체계 적용 여부 확정 후 V2에서 조정 가능
CREATE TABLE places (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    content_id      VARCHAR(20)  NOT NULL,
    content_type_id INT          NOT NULL,
    cat1            VARCHAR(10)  NULL,
    cat2            VARCHAR(10)  NULL,
    cat3            VARCHAR(20)  NULL,
    title           VARCHAR(255) NOT NULL,
    addr            VARCHAR(500) NULL,
    lat             DOUBLE       NULL,
    lng             DOUBLE       NULL,
    image_url       VARCHAR(500) NULL,
    area_code       INT          NULL,
    sigungu_code    INT          NULL,
    env             VARCHAR(10)  NOT NULL DEFAULT 'BOTH',
    env_rule_id     BIGINT       NULL,
    synced_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_places_content_id (content_id),
    KEY idx_places_area_env (area_code, sigungu_code, env),
    KEY idx_places_lat_lng (lat, lng),
    CONSTRAINT fk_places_env_rule FOREIGN KEY (env_rule_id) REFERENCES env_rules (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE courses (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(100) NOT NULL,
    area_code   INT          NULL,
    travel_date DATE         NULL,
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    share_code  VARCHAR(36)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_courses_share_code (share_code),
    KEY idx_courses_user (user_id),
    CONSTRAINT fk_courses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE course_places (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    course_id   BIGINT       NOT NULL,
    place_id    BIGINT       NOT NULL,
    day_no      INT          NOT NULL DEFAULT 1,
    visit_order INT          NOT NULL,
    memo        VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_course_places_order (course_id, day_no, visit_order),
    CONSTRAINT fk_cp_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_place  FOREIGN KEY (place_id)  REFERENCES places (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 기상청 예보 캐시 (nx, ny는 기상청 격자 좌표)
CREATE TABLE weather_forecasts (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    area_code     INT         NOT NULL,
    nx            INT         NOT NULL,
    ny            INT         NOT NULL,
    forecast_date DATE        NOT NULL,
    sky           VARCHAR(20) NULL,
    precip_type   VARCHAR(20) NULL,
    temp_max      INT         NULL,
    temp_min      INT         NULL,
    pm_grade      VARCHAR(20) NULL,
    fetched_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_weather_area_date (area_code, forecast_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
