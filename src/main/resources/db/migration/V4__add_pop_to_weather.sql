-- 강수확률(%) 컬럼 추가. 하루 중 가장 높은 값을 저장한다.
ALTER TABLE weather_forecasts
    ADD COLUMN pop INT NULL AFTER precip_type;
