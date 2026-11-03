-- 갈곳 V8: 코스 일정(기획서 18장 D)
-- 코스가 며칠짜리인지(당일치기 1, 1박2일 2, 2박3일 3)를 저장한다.
-- 지금까지는 장소가 든 일차 수만 있어, 2박3일로 만들고 1일차만 채우면 당일치기와 구분되지 않았다.
-- 기존 코스는 담긴 장소의 가장 큰 일차로 채우고, 장소가 없는 코스는 1로 둔다.
-- 적용 전에 3일차를 넘는 course_places 행이 없음을 확인했다(1~3 규칙).

ALTER TABLE courses
    ADD COLUMN trip_days TINYINT NOT NULL DEFAULT 1 AFTER travel_date;

UPDATE courses c
SET c.trip_days = (
    SELECT GREATEST(1, COALESCE(MAX(cp.day_no), 1))
    FROM course_places cp
    WHERE cp.course_id = c.id
);
