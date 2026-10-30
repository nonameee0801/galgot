-- 갈곳 V3: 실내/야외 분류 규칙 초기 데이터 (서울 수집 결과의 lclsSystm 코드 기준)
-- 우선순위: PLACE 100 > 소분류(3단계) 30 > KEYWORD 25 > 중분류(2단계) 20 > 대분류(1단계) 10
-- 규칙이 없는 장소는 BOTH(기본값). 코드의 의미는 대표 장소명으로 추정했으므로 결과를 눈으로 검증할 것.

-- 대분류 (priority 10)
INSERT INTO env_rules (rule_type, match_value, env, priority) VALUES
('CATEGORY', 'NA', 'OUTDOOR', 10),   -- 자연 (산, 하천, 습지, 자연공원)
('CATEGORY', 'AC', 'OUTDOOR', 10);   -- 캠핑장, 글램핑

-- 중분류 (priority 20)
INSERT INTO env_rules (rule_type, match_value, env, priority) VALUES
('CATEGORY', 'VE06', 'INDOOR', 20),   -- 공연/영화 등 문화공간
('CATEGORY', 'VE09', 'INDOOR', 20),   -- 도서관, 문화원, 교육시설
('CATEGORY', 'VE12', 'INDOOR', 20),   -- 책방 등
('CATEGORY', 'EX02', 'INDOOR', 20),   -- 공예 체험
('CATEGORY', 'EX04', 'INDOOR', 20),   -- 음식 문화 체험
('CATEGORY', 'EX05', 'INDOOR', 20),   -- 스파, 온천, 한방
('CATEGORY', 'EX06', 'INDOOR', 20),   -- 산업/견학 시설
('CATEGORY', 'LS04', 'INDOOR', 20),   -- 실내 레저
('CATEGORY', 'VE01', 'OUTDOOR', 20),  -- 랜드마크, 다리, 동상, 벽화
('CATEGORY', 'VE03', 'OUTDOOR', 20),  -- 공원, 광장
('CATEGORY', 'VE04', 'OUTDOOR', 20),  -- 거리, 길, 마을
('CATEGORY', 'LS02', 'OUTDOOR', 20),  -- 수상 레저
('CATEGORY', 'LS03', 'OUTDOOR', 20),  -- 드론공원 등
('CATEGORY', 'HS02', 'OUTDOOR', 20);  -- 기념탑, 석불 등 야외 유적

-- 소분류 (priority 30): 중분류 규칙의 예외 포함
INSERT INTO env_rules (rule_type, match_value, env, priority) VALUES
('CATEGORY', 'VE070100', 'INDOOR', 30),   -- 박물관
('CATEGORY', 'VE070200', 'INDOOR', 30),   -- 기념관
('CATEGORY', 'VE070400', 'INDOOR', 30),
('CATEGORY', 'VE070500', 'INDOOR', 30),   -- 과학관
('CATEGORY', 'VE020400', 'INDOOR', 30),   -- 아쿠아리움
('CATEGORY', 'LS010900', 'INDOOR', 30),   -- 아이스링크
('CATEGORY', 'LS011300', 'INDOOR', 30),   -- 클라이밍
('CATEGORY', 'LS011200', 'INDOOR', 30),   -- 실탄사격장
('CATEGORY', 'LS010100', 'INDOOR', 30),   -- 롤러장
('CATEGORY', 'LS020700', 'INDOOR', 30),   -- 수영장 (LS02 야외 규칙의 예외)
('CATEGORY', 'LS011900', 'OUTDOOR', 30),  -- 산책로, 등산로
('CATEGORY', 'LS010200', 'OUTDOOR', 30),  -- 자전거도로, 둘레길
('CATEGORY', 'LS011600', 'OUTDOOR', 30),  -- 익스트림장
('CATEGORY', 'VE090600', 'BOTH', 30),     -- 대학교 캠퍼스 (VE09 실내 규칙의 예외)
('CATEGORY', 'LS020400', 'BOTH', 30);     -- 다이빙 (LS02 야외 규칙의 예외)

-- 제목 키워드 (priority 25)
INSERT INTO env_rules (rule_type, match_value, env, priority) VALUES
('KEYWORD', '박물관', 'INDOOR', 25),
('KEYWORD', '미술관', 'INDOOR', 25),
('KEYWORD', '도서관', 'INDOOR', 25),
('KEYWORD', '전시관', 'INDOOR', 25),
('KEYWORD', '과학관', 'INDOOR', 25),
('KEYWORD', '기념관', 'INDOOR', 25),
('KEYWORD', '아쿠아리움', 'INDOOR', 25),
('KEYWORD', '체험관', 'INDOOR', 25),
('KEYWORD', '갤러리', 'INDOOR', 25),
('KEYWORD', '공연장', 'INDOOR', 25),
('KEYWORD', '극장', 'INDOOR', 25),
('KEYWORD', '아이스링크', 'INDOOR', 25),
('KEYWORD', '둘레길', 'OUTDOOR', 25),
('KEYWORD', '산책로', 'OUTDOOR', 25),
('KEYWORD', '등산', 'OUTDOOR', 25),
('KEYWORD', '트레킹', 'OUTDOOR', 25),
('KEYWORD', '해수욕장', 'OUTDOOR', 25),
('KEYWORD', '캠핑장', 'OUTDOOR', 25),
('KEYWORD', '공원', 'OUTDOOR', 25),
('KEYWORD', '폭포', 'OUTDOOR', 25),
('KEYWORD', '계곡', 'OUTDOOR', 25);
