-- Android QA seed for MySQL 8.
-- Keeps real users 1, 2, 3 intact and creates only [QA350]-marked data.
-- Re-running is idempotent while the QA350 markers remain on seeded rows.

SET NAMES utf8mb4;
SET @qa_now = NOW(6);
SET @qa_today = CURDATE();

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS qa350_guard;
CREATE TEMPORARY TABLE qa350_guard (
    guard_name VARCHAR(50) PRIMARY KEY,
    valid_value TINYINT NOT NULL,
    CONSTRAINT chk_qa350_guard CHECK (valid_value = 1)
);

INSERT INTO qa350_guard (guard_name, valid_value)
SELECT 'real-users-ready', IF(
        COUNT(DISTINCT u.id) = 3
        AND COUNT(DISTINCT p.user_id) = 3
        AND SUM(p.nickname IS NOT NULL AND p.gender IS NOT NULL AND p.birthday IS NOT NULL) = 3,
        1,
        0
    )
FROM users u
LEFT JOIN profiles p ON p.user_id = u.id
WHERE u.id IN (1, 2, 3);

DROP TEMPORARY TABLE IF EXISTS qa350_numbers;
CREATE TEMPORARY TABLE qa350_numbers (n INT PRIMARY KEY);
INSERT INTO qa350_numbers (n) VALUES
    (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
    (11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
    (21),(22),(23),(24),(25),(26),(27),(28),(29),(30),
    (31),(32),(33),(34),(35),(36),(37),(38),(39),(40),
    (41),(42),(43),(44),(45);

DROP TEMPORARY TABLE IF EXISTS qa350_destinations;
CREATE TEMPORARY TABLE qa350_destinations (
    slot INT PRIMARY KEY,
    destination_id BIGINT NOT NULL
);
INSERT INTO qa350_destinations (slot, destination_id)
SELECT 1, MIN(id) FROM destinations WHERE country_code = 'KR' AND city = '제주'
UNION ALL SELECT 2, MIN(id) FROM destinations WHERE country_code = 'KR' AND city = '부산'
UNION ALL SELECT 3, MIN(id) FROM destinations WHERE country_code = 'KR' AND city = '서울'
UNION ALL SELECT 4, MIN(id) FROM destinations WHERE country_code = 'KR' AND city = '강릉'
UNION ALL SELECT 5, MIN(id) FROM destinations WHERE country_code = 'JP' AND city = '오사카'
UNION ALL SELECT 6, MIN(id) FROM destinations WHERE country_code = 'JP' AND city = '도쿄'
UNION ALL SELECT 7, MIN(id) FROM destinations WHERE country_code = 'JP' AND city = '후쿠오카'
UNION ALL SELECT 8, MIN(id) FROM destinations WHERE country_code = 'JP' AND city = '교토'
UNION ALL SELECT 9, MIN(id) FROM destinations WHERE country_code = 'TH' AND city = '방콕'
UNION ALL SELECT 10, MIN(id) FROM destinations WHERE country_code = 'VN' AND city = '다낭';

INSERT INTO qa350_guard (guard_name, valid_value)
SELECT 'destinations-ready', IF(COUNT(*) = 10 AND COUNT(destination_id) = 10, 1, 0)
FROM qa350_destinations;

-- 27 virtual users. Odd indexes are female, even indexes are male (14:13).
INSERT INTO users (
    email, name, oauth_id, oauth_type, role, notification_enabled, created_at, modified_at
)
SELECT
    CONCAT('qa350-user-', LPAD(n, 2, '0'), '@dduru.test'),
    CONCAT('QA 가상 사용자 ', LPAD(n, 2, '0')),
    CONCAT('qa350:user:', LPAD(n, 2, '0')),
    'GOOGLE', 'USER', b'1', @qa_now, @qa_now
FROM qa350_numbers
WHERE n <= 27
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    oauth_id = VALUES(oauth_id),
    oauth_type = VALUES(oauth_type),
    role = VALUES(role),
    notification_enabled = VALUES(notification_enabled),
    modified_at = VALUES(modified_at);

DROP TEMPORARY TABLE IF EXISTS qa350_users;
CREATE TEMPORARY TABLE qa350_users (
    seq INT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE
);
INSERT INTO qa350_users (seq, user_id)
SELECT n.n, u.id
FROM qa350_numbers n
JOIN users u ON u.email = CONCAT('qa350-user-', LPAD(n.n, 2, '0'), '@dduru.test')
WHERE n.n <= 27;

INSERT INTO qa350_guard (guard_name, valid_value)
SELECT 'virtual-users-ready', IF(COUNT(*) = 27, 1, 0)
FROM qa350_users;

INSERT INTO qa350_guard (guard_name, valid_value)
SELECT 'profile-assets-ready', IF(
    (SELECT COUNT(DISTINCT avatar_type)
     FROM avatar_profiles
     WHERE avatar_type IN (
         'TTUR_POGUN','TTUR_POSEUL','TTUR_TTORANG','TTUR_DASOM',
         'TTUR_MUDI','TTUR_SODAM','TTUR_SWEET','TTUR_BANJJAK',
         'TTUR_SPARK','TTUR_LUNA','TTUR_GLIM','TTUR_HARAM',
         'TTUR_MALLANG','TTUR_BONGBONG','TTUR_BEOMI','TTUR_MARU'
     )) = 16
    AND (SELECT COUNT(DISTINCT display_order)
         FROM bg_colors
         WHERE display_order BETWEEN 1 AND 15) = 15,
    1,
    0
);

INSERT INTO qa350_guard (guard_name, valid_value)
SELECT 'qa-phone-numbers-available', IF(COUNT(*) = 0, 1, 0)
FROM profiles p
JOIN qa350_numbers n
  ON n.n <= 27
 AND p.phone_number = CONCAT('010000', LPAD(n.n, 5, '0'))
LEFT JOIN qa350_users qu ON qu.user_id = p.user_id
WHERE qu.user_id IS NULL;

INSERT INTO profiles (
    user_id, nickname, gender, birthday, profile_image_type, bio,
    avatar_id, bg_color_id, uploaded_image_url, phone_number, created_at, modified_at
)
SELECT
    qu.user_id,
    CONCAT('QA여행러', LPAD(qu.seq, 2, '0')),
    IF(MOD(qu.seq, 2) = 1, 'F', 'M'),
    CASE
        WHEN qu.seq BETWEEN 1 AND 3 THEN TIMESTAMPADD(YEAR,
            -GREATEST(21, LEAST(39,
                TIMESTAMPDIFF(YEAR, (SELECT birthday FROM profiles WHERE user_id = 1), CAST(@qa_today AS DATE))
                + (qu.seq - 2) * 2)), CAST(@qa_today AS DATE))
        WHEN qu.seq BETWEEN 4 AND 6 THEN TIMESTAMPADD(YEAR,
            -GREATEST(21, LEAST(39,
                TIMESTAMPDIFF(YEAR, (SELECT birthday FROM profiles WHERE user_id = 2), CAST(@qa_today AS DATE))
                + (qu.seq - 5) * 2)), CAST(@qa_today AS DATE))
        WHEN qu.seq BETWEEN 7 AND 9 THEN TIMESTAMPADD(YEAR,
            -GREATEST(21, LEAST(39,
                TIMESTAMPDIFF(YEAR, (SELECT birthday FROM profiles WHERE user_id = 3), CAST(@qa_today AS DATE))
                + (qu.seq - 8) * 2)), CAST(@qa_today AS DATE))
        ELSE DATE_SUB(CAST(@qa_today AS DATE), INTERVAL (21 + MOD(qu.seq - 10, 19)) YEAR)
    END,
    'AVATAR', '안드로이드 QA 통합 테스트용 가상 사용자입니다.',
    ap.id, bc.id, NULL, CONCAT('010000', LPAD(qu.seq, 5, '0')), @qa_now, @qa_now
FROM qa350_users qu
JOIN avatar_profiles ap
  ON ap.avatar_type = ELT(1 + MOD(qu.seq - 1, 16),
      'TTUR_POGUN','TTUR_POSEUL','TTUR_TTORANG','TTUR_DASOM',
      'TTUR_MUDI','TTUR_SODAM','TTUR_SWEET','TTUR_BANJJAK',
      'TTUR_SPARK','TTUR_LUNA','TTUR_GLIM','TTUR_HARAM',
      'TTUR_MALLANG','TTUR_BONGBONG','TTUR_BEOMI','TTUR_MARU')
JOIN bg_colors bc ON bc.display_order = 1 + MOD(qu.seq - 1, 15)
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    gender = VALUES(gender),
    birthday = VALUES(birthday),
    profile_image_type = VALUES(profile_image_type),
    bio = VALUES(bio),
    avatar_id = VALUES(avatar_id),
    bg_color_id = VALUES(bg_color_id),
    uploaded_image_url = NULL,
    phone_number = VALUES(phone_number),
    modified_at = VALUES(modified_at);

INSERT INTO qa350_guard (guard_name, valid_value)
SELECT 'virtual-profiles-ready', IF(
    COUNT(*) = 27
    AND SUM(p.gender = 'F') = 14
    AND SUM(p.gender = 'M') = 13
    AND SUM(p.phone_number IS NOT NULL) = 27
    AND COUNT(DISTINCT p.phone_number) = 27
    AND SUM(p.profile_image_type = 'AVATAR') = 27
    AND SUM(p.avatar_id IS NOT NULL) = 27
    AND SUM(p.bg_color_id IS NOT NULL) = 27
    AND MIN(TIMESTAMPDIFF(YEAR, p.birthday, CAST(@qa_today AS DATE))) >= 21
    AND MAX(TIMESTAMPDIFF(YEAR, p.birthday, CAST(@qa_today AS DATE))) <= 39,
    1,
    0
)
FROM qa350_users qu
JOIN profiles p ON p.user_id = qu.user_id;

INSERT INTO user_onboardings (
    user_id, is_onboarding_completed, is_profile_completed, survey_status, created_at, modified_at
)
SELECT user_id, b'1', b'1', 'COMPLETED', @qa_now, @qa_now
FROM qa350_users
ON DUPLICATE KEY UPDATE
    is_onboarding_completed = b'1',
    is_profile_completed = b'1',
    survey_status = 'COMPLETED',
    modified_at = VALUES(modified_at);

INSERT INTO surveys (
    user_id, rhythm_q1, rhythm_q2, rhythm_q3,
    energy_q1, energy_q2, energy_q3,
    consumption_q1, consumption_q2, consumption_q3,
    decision_q1, decision_q2, decision_q3,
    record_style, created_at, modified_at
)
SELECT
    user_id,
    IF(MOD(seq, 2) = 0, 'PLANNED_ROUTE', 'IMPULSE_SIDE_TRIP'),
    IF(MOD(seq, 3) = 0, 'PACK_EARLY', 'PACK_LAST_MINUTE'),
    IF(MOD(seq, 2) = 0, 'ROUTE_TIME_SET', 'ROUGH_LIST_ONLY'),
    IF(MOD(seq, 2) = 0, 'PACKED_DAY', 'RELAXED_DAY'),
    IF(MOD(seq, 3) = 0, 'BREAKFAST_SPRINT', 'BRUNCH_INSTEAD'),
    IF(MOD(seq, 2) = 0, 'FILL_WITH_SPOTS', 'DO_NOTHING_OK'),
    IF(MOD(seq, 2) = 0, 'ADJUST_BUDGET', 'FLEX_OK'),
    IF(MOD(seq, 3) = 0, 'SAVE_TIME_TAXI', 'VALUE_TRANSPORT'),
    IF(MOD(seq, 2) = 0, 'INVEST_EXPERIENCE', 'VALUE_CHOICE'),
    IF(MOD(seq, 2) = 0, 'LEAD_OR_ORGANIZE', 'DELEGATE_ROLE'),
    IF(MOD(seq, 3) = 0, 'PROPOSE_FIRST', 'FOLLOW_OTHERS'),
    IF(MOD(seq, 2) = 0, 'DRIVE_CONCLUSION', 'WAIT_AND_SEE'),
    IF(MOD(seq, 2) = 0, 'SHOOT_NOW', 'EYES_FIRST'),
    @qa_now, @qa_now
FROM qa350_users
ON DUPLICATE KEY UPDATE modified_at = VALUES(modified_at);

INSERT INTO travel_tendencies (
    user_id, avatar_type, rhythm_score, energy_score, consumption_score, decision_score,
    created_at, modified_at
)
SELECT
    user_id,
    ELT(1 + MOD(seq - 1, 16),
        'TTUR_POGUN','TTUR_POSEUL','TTUR_TTORANG','TTUR_DASOM',
        'TTUR_MUDI','TTUR_SODAM','TTUR_SWEET','TTUR_BANJJAK',
        'TTUR_SPARK','TTUR_LUNA','TTUR_GLIM','TTUR_HARAM',
        'TTUR_MALLANG','TTUR_BONGBONG','TTUR_BEOMI','TTUR_MARU'),
    1.0 + MOD(seq, 9) / 2.0,
    1.0 + MOD(seq + 2, 9) / 2.0,
    1.0 + MOD(seq + 4, 9) / 2.0,
    1.0 + MOD(seq + 6, 9) / 2.0,
    @qa_now, @qa_now
FROM qa350_users
ON DUPLICATE KEY UPDATE
    avatar_type = VALUES(avatar_type),
    rhythm_score = VALUES(rhythm_score),
    energy_score = VALUES(energy_score),
    consumption_score = VALUES(consumption_score),
    decision_score = VALUES(decision_score),
    modified_at = VALUES(modified_at);

INSERT INTO qa350_guard (guard_name, valid_value)
SELECT 'profile-avatar-consistent', IF(
    COUNT(*) = 27
    AND SUM(p.profile_image_type = 'AVATAR') = 27
    AND SUM(p.avatar_id IS NOT NULL AND p.bg_color_id IS NOT NULL) = 27
    AND SUM(ap.avatar_type = tt.avatar_type) = 27,
    1,
    0
)
FROM qa350_users qu
JOIN profiles p ON p.user_id = qu.user_id
JOIN travel_tendencies tt ON tt.user_id = qu.user_id
JOIN avatar_profiles ap ON ap.id = p.avatar_id;

INSERT INTO survey_activity_tags (survey_id, tag)
SELECT s.id, IF(MOD(qu.seq, 2) = 0, 'FOOD', 'SIGHTSEEING')
FROM qa350_users qu
JOIN surveys s ON s.user_id = qu.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM survey_activity_tags sat
    WHERE sat.survey_id = s.id
      AND sat.tag = IF(MOD(qu.seq, 2) = 0, 'FOOD', 'SIGHTSEEING')
);

-- Resolve each real user's current first preference without changing it.
DROP TEMPORARY TABLE IF EXISTS qa350_real_preferences;
CREATE TEMPORARY TABLE qa350_real_preferences (
    user_id BIGINT PRIMARY KEY,
    destination_id BIGINT NULL
);
INSERT INTO qa350_real_preferences (user_id, destination_id)
SELECT r.user_id,
       (
           SELECT CASE
               WHEN p.preference_type = 'CITY' THEN p.destination_id
               ELSE (SELECT MIN(d.id) FROM destinations d WHERE d.country_code = p.country_code)
           END
           FROM user_recommendation_destination_preferences p
           WHERE p.user_id = r.user_id
           ORDER BY p.preference_rank, p.id
           LIMIT 1
       )
FROM (SELECT 1 AS user_id UNION ALL SELECT 2 UNION ALL SELECT 3) r;

DROP TEMPORARY TABLE IF EXISTS qa350_post_specs;
CREATE TEMPORARY TABLE qa350_post_specs (
    seq INT PRIMARY KEY,
    post_key VARCHAR(8) NOT NULL UNIQUE,
    host_user_id BIGINT NOT NULL,
    destination_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    recruit_deadline DATE NULL,
    recruit_capacity INT NOT NULL,
    preferred_gender VARCHAR(1) NOT NULL,
    is_age_any BIT(1) NOT NULL,
    min_age INT NULL,
    max_age INT NULL,
    companion_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    photo_url TEXT NULL,
    tags JSON NOT NULL,
    view_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL
);

INSERT INTO qa350_post_specs
SELECT
    n.n,
    IF(n.n <= 3, CONCAT('R', LPAD(n.n, 2, '0')), CONCAT('D', LPAD(n.n - 3, 2, '0'))),
    IF(n.n <= 3, n.n, qu.user_id),
    COALESCE(rp.destination_id, qd.destination_id),
    CONCAT('[QA350] ',
        ELT(1 + MOD(n.n - 1, 10), '제주 바다 산책', '부산 맛집 투어', '서울 전시 나들이', '강릉 커피 여행',
            '오사카 먹방 여행', '도쿄 골목 탐방', '후쿠오카 온천 여행', '교토 사찰 산책', '방콕 야시장 투어', '다낭 휴양 여행'),
        ' ', LPAD(n.n, 2, '0')),
    CONCAT('[QA350:', IF(n.n <= 3, CONCAT('R', LPAD(n.n, 2, '0')), CONCAT('D', LPAD(n.n - 3, 2, '0'))),
        '] 안드로이드 홈, 모집글, 마이페이지 통합 테스트를 위한 여행 동행 모집글입니다.'),
    CASE
        WHEN n.n = 4 THEN CAST(@qa_today AS DATE)
        WHEN n.n <= 30 THEN DATE_ADD(CAST(@qa_today AS DATE), INTERVAL (4 + MOD(n.n * 3, 50)) DAY)
        WHEN n.n <= 40 THEN DATE_ADD(CAST(@qa_today AS DATE), INTERVAL (10 + MOD(n.n, 20)) DAY)
        ELSE DATE_SUB(CAST(@qa_today AS DATE), INTERVAL (20 + MOD(n.n, 20)) DAY)
    END,
    CASE
        WHEN n.n = 4 THEN DATE_ADD(CAST(@qa_today AS DATE), INTERVAL 2 DAY)
        WHEN n.n <= 30 THEN DATE_ADD(CAST(@qa_today AS DATE), INTERVAL (7 + MOD(n.n * 3, 50)) DAY)
        WHEN n.n <= 40 THEN DATE_ADD(CAST(@qa_today AS DATE), INTERVAL (13 + MOD(n.n, 20)) DAY)
        ELSE DATE_SUB(CAST(@qa_today AS DATE), INTERVAL (14 + MOD(n.n, 20)) DAY)
    END,
    CASE
        WHEN n.n BETWEEN 4 AND 8 THEN DATE_ADD(CAST(@qa_today AS DATE), INTERVAL MOD(n.n - 4, 4) DAY)
        WHEN n.n <= 40 THEN DATE_ADD(CAST(@qa_today AS DATE), INTERVAL (3 + MOD(n.n * 3, 45)) DAY)
        ELSE DATE_SUB(CAST(@qa_today AS DATE), INTERVAL 15 DAY)
    END,
    CASE WHEN n.n <= 3 THEN 6 WHEN n.n BETWEEN 31 AND 35 THEN 3 ELSE 3 + MOD(n.n, 5) END,
    ELT(1 + MOD(n.n - 1, 3), 'F', 'M', 'U'),
    IF(MOD(n.n, 3) = 0, b'0', b'1'),
    IF(MOD(n.n, 3) = 0, 20 + MOD(n.n, 8), NULL),
    IF(MOD(n.n, 3) = 0, 32 + MOD(n.n, 8), NULL),
    ELT(1 + MOD(n.n - 1, 4), 'FULL', 'PARTIAL', 'MEAL', 'UNSPECIFIED'),
    IF(n.n <= 35, 'OPEN', 'CLOSED'),
    IF(MOD(n.n, 7) = 0, NULL, d.image),
    JSON_ARRAY('QA350', CONCAT('QA350:', IF(n.n <= 3, CONCAT('R', LPAD(n.n, 2, '0')), CONCAT('D', LPAD(n.n - 3, 2, '0')))),
        ELT(1 + MOD(n.n - 1, 5), '맛집', '사진', '산책', '자연', '액티비티')),
    MOD(n.n * 37, 500),
    DATE_SUB(@qa_now, INTERVAL (46 - n.n) HOUR)
FROM qa350_numbers n
LEFT JOIN qa350_users qu ON qu.seq = 1 + MOD(n.n - 4, 27) AND n.n > 3
LEFT JOIN qa350_real_preferences rp ON rp.user_id = CASE
    WHEN n.n BETWEEN 4 AND 6 THEN 1
    WHEN n.n BETWEEN 7 AND 9 THEN 2
    WHEN n.n BETWEEN 10 AND 12 THEN 3
    ELSE NULL
END
JOIN qa350_destinations qd ON qd.slot = 1 + MOD(n.n - 1, 10)
JOIN destinations d ON d.id = COALESCE(rp.destination_id, qd.destination_id);

INSERT INTO posts (
    user_id, destination_id, title, content, start_date, end_date,
    recruit_capacity, recruit_count, recruit_deadline, preferred_gender,
    is_age_any, min_age, max_age, photo_url, tags, view_count, like_count,
    status, is_deleted, deleted_at, deleted_by, companion_type, created_at, modified_at
)
SELECT
    s.host_user_id, s.destination_id, s.title, s.content, s.start_date, s.end_date,
    s.recruit_capacity, 1, s.recruit_deadline, s.preferred_gender,
    s.is_age_any, s.min_age, s.max_age, s.photo_url, s.tags, s.view_count, 0,
    s.status, b'0', NULL, NULL, s.companion_type, s.created_at, @qa_now
FROM qa350_post_specs s
WHERE NOT EXISTS (
    SELECT 1 FROM posts p
    WHERE JSON_CONTAINS(p.tags, JSON_QUOTE(CONCAT('QA350:', s.post_key)))
);

UPDATE posts p
JOIN qa350_post_specs s
  ON JSON_CONTAINS(p.tags, JSON_QUOTE(CONCAT('QA350:', s.post_key)))
SET p.user_id = s.host_user_id,
    p.destination_id = s.destination_id,
    p.title = s.title,
    p.content = s.content,
    p.start_date = s.start_date,
    p.end_date = s.end_date,
    p.recruit_capacity = s.recruit_capacity,
    p.recruit_deadline = s.recruit_deadline,
    p.preferred_gender = s.preferred_gender,
    p.is_age_any = s.is_age_any,
    p.min_age = s.min_age,
    p.max_age = s.max_age,
    p.photo_url = s.photo_url,
    p.tags = s.tags,
    p.view_count = s.view_count,
    p.status = s.status,
    p.is_deleted = b'0',
    p.deleted_at = NULL,
    p.deleted_by = NULL,
    p.companion_type = s.companion_type,
    p.modified_at = @qa_now;

DROP TEMPORARY TABLE IF EXISTS qa350_posts;
CREATE TEMPORARY TABLE qa350_posts (
    seq INT PRIMARY KEY,
    post_key VARCHAR(8) NOT NULL UNIQUE,
    post_id BIGINT NOT NULL UNIQUE,
    host_user_id BIGINT NOT NULL
);
INSERT INTO qa350_posts (seq, post_key, post_id, host_user_id)
SELECT s.seq, s.post_key, MIN(p.id), s.host_user_id
FROM qa350_post_specs s
JOIN posts p ON JSON_CONTAINS(p.tags, JSON_QUOTE(CONCAT('QA350:', s.post_key)))
GROUP BY s.seq, s.post_key, s.host_user_id;

INSERT INTO qa350_guard (guard_name, valid_value)
SELECT 'qa-posts-ready', IF(COUNT(*) = 45, 1, 0)
FROM qa350_posts;

INSERT INTO journeys (post_id, title, photo_url, created_at, modified_at)
SELECT p.id, p.title, p.photo_url, p.created_at, @qa_now
FROM posts p
JOIN qa350_posts qp ON qp.post_id = p.id
WHERE NOT EXISTS (SELECT 1 FROM journeys j WHERE j.post_id = p.id);

UPDATE journeys j
JOIN posts p ON p.id = j.post_id
JOIN qa350_posts qp ON qp.post_id = p.id
SET j.title = p.title, j.photo_url = p.photo_url, j.modified_at = @qa_now;

DROP TEMPORARY TABLE IF EXISTS qa350_journeys;
CREATE TEMPORARY TABLE qa350_journeys (
    post_key VARCHAR(8) PRIMARY KEY,
    journey_id BIGINT NOT NULL UNIQUE,
    post_id BIGINT NOT NULL UNIQUE,
    host_user_id BIGINT NOT NULL
);
INSERT INTO qa350_journeys
SELECT qp.post_key, j.id, qp.post_id, qp.host_user_id
FROM qa350_posts qp
JOIN journeys j ON j.post_id = qp.post_id;

INSERT INTO journey_members (
    journey_id, user_id, role, status, joined_at, removed_at, created_at, modified_at
)
SELECT journey_id, host_user_id, 'HOST', 'ACTIVE', @qa_now, NULL, @qa_now, @qa_now
FROM qa350_journeys qj
WHERE NOT EXISTS (
    SELECT 1 FROM journey_members jm
    WHERE jm.journey_id = qj.journey_id AND jm.user_id = qj.host_user_id
);

UPDATE journey_members jm
JOIN qa350_journeys qj ON qj.journey_id = jm.journey_id AND qj.host_user_id = jm.user_id
SET jm.role = 'HOST', jm.status = 'ACTIVE', jm.removed_at = NULL, jm.modified_at = @qa_now;

INSERT INTO chat_rooms (journey_id, post_id, room_type, status, created_at, modified_at)
SELECT journey_id, NULL, 'GROUP', 'ACTIVE', @qa_now, @qa_now
FROM qa350_journeys qj
WHERE NOT EXISTS (SELECT 1 FROM chat_rooms cr WHERE cr.journey_id = qj.journey_id);

INSERT INTO chat_room_members (room_id, user_id, role, last_read_message_id, created_at, modified_at)
SELECT cr.id, qj.host_user_id, 'HOST', NULL, @qa_now, @qa_now
FROM qa350_journeys qj
JOIN chat_rooms cr ON cr.journey_id = qj.journey_id AND cr.room_type = 'GROUP'
WHERE NOT EXISTS (
    SELECT 1 FROM chat_room_members crm WHERE crm.room_id = cr.id AND crm.user_id = qj.host_user_id
);

-- Four received and four submitted states for each real account.
DROP TEMPORARY TABLE IF EXISTS qa350_participation_inputs;
CREATE TEMPORARY TABLE qa350_participation_inputs (
    part_key VARCHAR(30) PRIMARY KEY,
    post_key VARCHAR(8) NOT NULL,
    applicant_seq INT NULL,
    applicant_user_id BIGINT NULL,
    status VARCHAR(20) NOT NULL
);
INSERT INTO qa350_participation_inputs VALUES
    ('recv-u1-pending','R01',1,NULL,'PENDING'),
    ('recv-u1-contact','R01',2,NULL,'CONTACTING'),
    ('recv-u1-approved','R01',3,NULL,'APPROVED'),
    ('recv-u1-rejected','R01',4,NULL,'REJECTED'),
    ('recv-u2-pending','R02',5,NULL,'PENDING'),
    ('recv-u2-contact','R02',6,NULL,'CONTACTING'),
    ('recv-u2-approved','R02',7,NULL,'APPROVED'),
    ('recv-u2-rejected','R02',8,NULL,'REJECTED'),
    ('recv-u3-pending','R03',9,NULL,'PENDING'),
    ('recv-u3-contact','R03',10,NULL,'CONTACTING'),
    ('recv-u3-approved','R03',11,NULL,'APPROVED'),
    ('recv-u3-rejected','R03',12,NULL,'REJECTED'),
    ('sent-u1-pending','D01',NULL,1,'PENDING'),
    ('sent-u1-contact','D02',NULL,1,'CONTACTING'),
    ('sent-u1-approved','D03',NULL,1,'APPROVED'),
    ('sent-u1-rejected','D04',NULL,1,'REJECTED'),
    ('sent-u2-pending','D05',NULL,2,'PENDING'),
    ('sent-u2-contact','D06',NULL,2,'CONTACTING'),
    ('sent-u2-approved','D07',NULL,2,'APPROVED'),
    ('sent-u2-rejected','D08',NULL,2,'REJECTED'),
    ('sent-u3-pending','D09',NULL,3,'PENDING'),
    ('sent-u3-contact','D10',NULL,3,'CONTACTING'),
    ('sent-u3-approved','D11',NULL,3,'APPROVED'),
    ('sent-u3-rejected','D12',NULL,3,'REJECTED'),
    ('full-28-a','D28',20,NULL,'APPROVED'),
    ('full-28-b','D28',21,NULL,'APPROVED'),
    ('full-29-a','D29',22,NULL,'APPROVED'),
    ('full-29-b','D29',23,NULL,'APPROVED'),
    ('full-30-a','D30',24,NULL,'APPROVED'),
    ('full-30-b','D30',25,NULL,'APPROVED'),
    ('full-31-a','D31',26,NULL,'APPROVED'),
    ('full-31-b','D31',27,NULL,'APPROVED'),
    ('full-32-a','D32',18,NULL,'APPROVED'),
    ('full-32-b','D32',19,NULL,'APPROVED');

DROP TEMPORARY TABLE IF EXISTS qa350_participation_specs;
CREATE TEMPORARY TABLE qa350_participation_specs (
    part_key VARCHAR(30) PRIMARY KEY,
    post_key VARCHAR(8) NOT NULL,
    applicant_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL
);
INSERT INTO qa350_participation_specs (part_key, post_key, applicant_user_id, status)
SELECT input.part_key, input.post_key,
       COALESCE(input.applicant_user_id, qu.user_id), input.status
FROM qa350_participation_inputs input
LEFT JOIN qa350_users qu ON qu.seq = input.applicant_seq;

INSERT INTO participations (
    post_id, user_id, status, message, contacted_at, approved_at, rejected_at, created_at, modified_at
)
SELECT
    qp.post_id, ps.applicant_user_id, ps.status,
    CONCAT('[QA350:', ps.part_key, '] 안녕하세요. 함께 여행하고 싶어 신청합니다.'),
    IF(ps.status = 'CONTACTING', DATE_SUB(@qa_now, INTERVAL 2 HOUR), NULL),
    IF(ps.status = 'APPROVED', DATE_SUB(@qa_now, INTERVAL 1 DAY), NULL),
    IF(ps.status = 'REJECTED', DATE_SUB(@qa_now, INTERVAL 1 HOUR), NULL),
    DATE_SUB(@qa_now, INTERVAL 2 DAY), @qa_now
FROM qa350_participation_specs ps
JOIN qa350_posts qp ON qp.post_key = ps.post_key
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    message = VALUES(message),
    contacted_at = VALUES(contacted_at),
    approved_at = VALUES(approved_at),
    rejected_at = VALUES(rejected_at),
    modified_at = VALUES(modified_at);

DROP TEMPORARY TABLE IF EXISTS qa350_participations;
CREATE TEMPORARY TABLE qa350_participations (
    part_key VARCHAR(30) PRIMARY KEY,
    participation_id BIGINT NOT NULL UNIQUE,
    post_key VARCHAR(8) NOT NULL,
    post_id BIGINT NOT NULL,
    host_user_id BIGINT NOT NULL,
    applicant_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL
);
INSERT INTO qa350_participations
SELECT ps.part_key, p.id, ps.post_key, qp.post_id, qp.host_user_id, ps.applicant_user_id, ps.status
FROM qa350_participation_specs ps
JOIN qa350_posts qp ON qp.post_key = ps.post_key
JOIN participations p ON p.post_id = qp.post_id AND p.user_id = ps.applicant_user_id;

INSERT INTO journey_members (
    journey_id, user_id, role, status, joined_at, removed_at, created_at, modified_at
)
SELECT qj.journey_id, p.applicant_user_id, 'MEMBER', 'ACTIVE',
       COALESCE((SELECT approved_at FROM participations WHERE id = p.participation_id), @qa_now),
       NULL, @qa_now, @qa_now
FROM qa350_participations p
JOIN qa350_journeys qj ON qj.post_key = p.post_key
WHERE p.status = 'APPROVED'
ON DUPLICATE KEY UPDATE
    role = 'MEMBER', status = 'ACTIVE', removed_at = NULL, modified_at = VALUES(modified_at);

INSERT INTO chat_room_members (room_id, user_id, role, last_read_message_id, created_at, modified_at)
SELECT cr.id, p.applicant_user_id, 'GUEST', NULL, @qa_now, @qa_now
FROM qa350_participations p
JOIN qa350_journeys qj ON qj.post_key = p.post_key
JOIN chat_rooms cr ON cr.journey_id = qj.journey_id AND cr.room_type = 'GROUP'
WHERE p.status = 'APPROVED'
ON DUPLICATE KEY UPDATE role = 'GUEST', modified_at = VALUES(modified_at);

UPDATE posts p
JOIN qa350_posts qp ON qp.post_id = p.id
JOIN qa350_journeys qj ON qj.post_id = p.id
SET p.recruit_count = (
        SELECT COUNT(*) FROM journey_members jm
        WHERE jm.journey_id = qj.journey_id AND jm.status = 'ACTIVE'
    ),
    p.modified_at = @qa_now;

-- One private room for every CONTACTING application.
INSERT INTO chat_rooms (post_id, journey_id, room_type, status, created_at, modified_at)
SELECT p.post_id, NULL, 'PRIVATE', 'ACTIVE', @qa_now, @qa_now
FROM qa350_participations p
WHERE p.status = 'CONTACTING'
  AND NOT EXISTS (
      SELECT 1
      FROM chat_rooms cr
      JOIN chat_room_members host_member ON host_member.room_id = cr.id AND host_member.user_id = p.host_user_id
      JOIN chat_room_members guest_member ON guest_member.room_id = cr.id AND guest_member.user_id = p.applicant_user_id
      WHERE cr.post_id = p.post_id AND cr.room_type = 'PRIVATE'
  );

DROP TEMPORARY TABLE IF EXISTS qa350_private_rooms;
CREATE TEMPORARY TABLE qa350_private_rooms (
    part_key VARCHAR(30) PRIMARY KEY,
    room_id BIGINT NOT NULL UNIQUE,
    post_id BIGINT NOT NULL,
    host_user_id BIGINT NOT NULL,
    applicant_user_id BIGINT NOT NULL
);
INSERT INTO qa350_private_rooms
SELECT p.part_key, MIN(cr.id), p.post_id, p.host_user_id, p.applicant_user_id
FROM qa350_participations p
JOIN chat_rooms cr ON cr.post_id = p.post_id AND cr.room_type = 'PRIVATE'
LEFT JOIN chat_room_members hm ON hm.room_id = cr.id AND hm.user_id = p.host_user_id
LEFT JOIN chat_room_members gm ON gm.room_id = cr.id AND gm.user_id = p.applicant_user_id
WHERE p.status = 'CONTACTING'
  AND (hm.id IS NOT NULL AND gm.id IS NOT NULL
       OR NOT EXISTS (SELECT 1 FROM chat_room_members any_member WHERE any_member.room_id = cr.id))
GROUP BY p.part_key, p.post_id, p.host_user_id, p.applicant_user_id;

INSERT INTO chat_room_members (room_id, user_id, role, last_read_message_id, created_at, modified_at)
SELECT room_id, host_user_id, 'HOST', NULL, DATE_SUB(@qa_now, INTERVAL 2 HOUR), @qa_now
FROM qa350_private_rooms
ON DUPLICATE KEY UPDATE role = 'HOST', modified_at = VALUES(modified_at);

INSERT INTO chat_room_members (room_id, user_id, role, last_read_message_id, created_at, modified_at)
SELECT room_id, applicant_user_id, 'GUEST', NULL, DATE_SUB(@qa_now, INTERVAL 2 HOUR), @qa_now
FROM qa350_private_rooms
ON DUPLICATE KEY UPDATE role = 'GUEST', modified_at = VALUES(modified_at);

-- Real-user-visible private and group rooms, with deterministic message markers.
DROP TEMPORARY TABLE IF EXISTS qa350_chat_targets;
CREATE TEMPORARY TABLE qa350_chat_targets (
    room_key VARCHAR(30) PRIMARY KEY,
    room_id BIGINT NOT NULL UNIQUE,
    first_user_id BIGINT NOT NULL,
    second_user_id BIGINT NOT NULL,
    real_user_id BIGINT NOT NULL,
    message_count INT NOT NULL,
    read_mode VARCHAR(20) NOT NULL
);
INSERT INTO qa350_chat_targets
SELECT CONCAT('private-', part_key), room_id, host_user_id, applicant_user_id,
       CASE WHEN host_user_id IN (1,2,3) THEN host_user_id ELSE applicant_user_id END,
       12,
       CASE
           WHEN part_key LIKE 'recv-%' THEN 'ALL_READ'
           WHEN applicant_user_id = 1 THEN 'ONE_UNREAD'
           WHEN applicant_user_id = 2 THEN 'MANY_UNREAD'
           ELSE 'ALL_UNREAD'
       END
FROM qa350_private_rooms;

INSERT INTO qa350_chat_targets
SELECT CONCAT('group-', qj.post_key), cr.id, qj.host_user_id, approved.applicant_user_id,
       CASE WHEN qj.host_user_id IN (1,2,3) THEN qj.host_user_id ELSE approved.applicant_user_id END,
       15,
       CASE
           WHEN qj.post_key IN ('R01','D07') THEN 'ALL_READ'
           WHEN qj.post_key IN ('R02','D11') THEN 'ONE_UNREAD'
           ELSE 'MANY_UNREAD'
       END
FROM qa350_journeys qj
JOIN chat_rooms cr ON cr.journey_id = qj.journey_id AND cr.room_type = 'GROUP'
JOIN qa350_participations approved ON approved.post_key = qj.post_key AND approved.status = 'APPROVED'
WHERE qj.post_key IN ('R01','R02','R03','D03','D07','D11');

INSERT INTO chat_messages (room_id, sender_id, message_type, content, created_at, modified_at)
SELECT
    t.room_id,
    IF(MOD(n.n, 2) = 1, t.first_user_id, t.second_user_id),
    'TEXT',
    CONCAT('[QA350:', t.room_key, ':M', LPAD(n.n, 2, '0'), '] ',
        ELT(1 + MOD(n.n - 1, 4),
            '안녕하세요! 여행 일정을 같이 확인해요.',
            '네, 준비물과 만나는 장소도 확인했어요.',
            '맛집과 가고 싶은 장소 목록을 공유할게요.',
            '좋아요! 변경 사항은 이 채팅방에서 이야기해요.')),
    DATE_SUB(@qa_now, INTERVAL (t.message_count - n.n) MINUTE),
    DATE_SUB(@qa_now, INTERVAL (t.message_count - n.n) MINUTE)
FROM qa350_chat_targets t
JOIN qa350_numbers n ON n.n <= t.message_count
WHERE NOT EXISTS (
    SELECT 1 FROM chat_messages cm
    WHERE cm.room_id = t.room_id
      AND cm.content LIKE CONCAT('[QA350:', t.room_key, ':M', LPAD(n.n, 2, '0'), ']%')
);

UPDATE chat_room_members crm
JOIN qa350_chat_targets t ON t.room_id = crm.room_id AND t.real_user_id = crm.user_id
SET crm.last_read_message_id = CASE t.read_mode
        WHEN 'ALL_READ' THEN (SELECT MAX(id) FROM chat_messages WHERE room_id = t.room_id)
        WHEN 'ONE_UNREAD' THEN (SELECT id FROM chat_messages WHERE room_id = t.room_id AND message_type <> 'SYSTEM' ORDER BY id DESC LIMIT 1 OFFSET 1)
        WHEN 'MANY_UNREAD' THEN (SELECT id FROM chat_messages WHERE room_id = t.room_id AND message_type <> 'SYSTEM' ORDER BY id DESC LIMIT 1 OFFSET 5)
        ELSE NULL
    END,
    crm.modified_at = @qa_now;

UPDATE chat_room_members crm
JOIN qa350_chat_targets t ON t.room_id = crm.room_id AND t.real_user_id <> crm.user_id
SET crm.last_read_message_id = (SELECT MAX(id) FROM chat_messages WHERE room_id = t.room_id),
    crm.modified_at = @qa_now;

-- Detailed journey content for the six journeys visible to real users.
DROP TEMPORARY TABLE IF EXISTS qa350_detail_journeys;
CREATE TEMPORARY TABLE qa350_detail_journeys AS
SELECT qj.*
FROM qa350_journeys qj
WHERE qj.post_key IN ('R01','R02','R03','D03','D07','D11');

INSERT INTO journey_schedules (
    journey_id, title, category, day_offset, start_time, end_time,
    place_name, memo, is_deleted, deleted_at, deleted_by, created_at, modified_at
)
SELECT qj.journey_id,
       CONCAT('[QA350] ', ELT(n.n, '도착 후 점심 식사', '대표 관광지 둘러보기', '카페에서 자유 시간')),
       ELT(n.n, 'MEAL', 'SIGHTSEEING', 'CAFE'),
       n.n - 1,
       ELT(n.n, '12:00:00', '10:00:00', '15:00:00'),
       ELT(n.n, '13:30:00', '12:00:00', '16:30:00'),
       ELT(n.n, '현지 맛집', '대표 관광지', '전망 좋은 카페'),
       CONCAT('[QA350:', qj.post_key, ':S', n.n, '] 안드로이드 QA 일정'),
       b'0', NULL, NULL, @qa_now, @qa_now
FROM qa350_detail_journeys qj
JOIN qa350_numbers n ON n.n <= 3
WHERE NOT EXISTS (
    SELECT 1 FROM journey_schedules js
    WHERE js.journey_id = qj.journey_id
      AND js.memo LIKE CONCAT('[QA350:', qj.post_key, ':S', n.n, ']%')
);

INSERT INTO journey_posts (
    journey_id, author_user_id, content, is_notice, is_deleted,
    deleted_at, deleted_by, created_at, modified_at
)
SELECT
    qj.journey_id,
    IF(MOD(n.n, 2) = 1, qj.host_user_id,
       COALESCE((SELECT jm.user_id FROM journey_members jm
                 WHERE jm.journey_id = qj.journey_id AND jm.role = 'MEMBER' AND jm.status = 'ACTIVE'
                 ORDER BY jm.id LIMIT 1), qj.host_user_id)),
    CONCAT('[QA350:JP:', qj.post_key, ':', n.n, '] ',
        ELT(n.n,
            '모임 시간과 준비물을 확인해주세요.',
            '여행 전날 날씨와 교통편을 다시 확인해요.',
            '여행 일정과 각자 준비물을 공유합니다.',
            '함께 가고 싶은 장소와 맛집을 추천해주세요.',
            '여행 중 촬영한 사진을 모아 공유해요.',
            '정산할 비용과 개인 준비 항목을 확인해주세요.')),
    IF(n.n <= 2, b'1', b'0'), b'0', NULL, NULL,
    DATE_SUB(@qa_now, INTERVAL (7 - n.n) HOUR), @qa_now
FROM qa350_detail_journeys qj
JOIN qa350_numbers n ON n.n <= 6
WHERE NOT EXISTS (
    SELECT 1 FROM journey_posts jp
    WHERE jp.journey_id = qj.journey_id
      AND jp.content LIKE CONCAT('[QA350:JP:', qj.post_key, ':', n.n, ']%')
);

INSERT INTO journey_post_images (journey_post_id, image_url, sort_order, created_at, modified_at)
SELECT jp.id, d.image, n.n - 1, @qa_now, @qa_now
FROM qa350_detail_journeys qj
JOIN journeys j ON j.id = qj.journey_id
JOIN posts p ON p.id = j.post_id
JOIN destinations d ON d.id = p.destination_id AND d.image IS NOT NULL
JOIN journey_posts jp ON jp.journey_id = qj.journey_id
    AND jp.content LIKE CONCAT('[QA350:JP:', qj.post_key, ':%')
JOIN qa350_numbers n ON n.n <= CASE MOD(jp.id, 4) WHEN 0 THEN 3 WHEN 1 THEN 1 WHEN 2 THEN 2 ELSE 0 END
WHERE jp.is_notice = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM journey_post_images jpi
      WHERE jpi.journey_post_id = jp.id AND jpi.sort_order = n.n - 1
  );

INSERT INTO journey_post_comments (
    journey_post_id, author_user_id, content, is_deleted,
    deleted_at, deleted_by, created_at, modified_at
)
SELECT
    jp.id,
    IF(n.n = 1, qj.host_user_id,
       COALESCE((SELECT jm.user_id FROM journey_members jm
                 WHERE jm.journey_id = qj.journey_id AND jm.role = 'MEMBER' AND jm.status = 'ACTIVE'
                 ORDER BY jm.id LIMIT 1), qj.host_user_id)),
    CONCAT('[QA350:JC:', qj.post_key, ':', jp.id, ':', n.n, '] 일정 확인했습니다. 함께 즐거운 여행 준비해요!'),
    b'0', NULL, NULL, @qa_now, @qa_now
FROM qa350_detail_journeys qj
JOIN journey_posts jp ON jp.journey_id = qj.journey_id AND jp.is_notice = b'0'
JOIN qa350_numbers n ON n.n <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM journey_post_comments jpc
    WHERE jpc.journey_post_id = jp.id
      AND jpc.content LIKE CONCAT('[QA350:JC:', qj.post_key, ':', jp.id, ':', n.n, ']%')
);

-- Role labels for hosts and approved members on detailed journeys.
INSERT INTO journey_member_role_labels (
    journey_member_id, role_type, custom_role_label, created_at, modified_at
)
SELECT jm.id, IF(jm.role = 'HOST', 'SCHEDULE', 'PHOTO'), NULL, @qa_now, @qa_now
FROM journey_members jm
JOIN qa350_detail_journeys qj ON qj.journey_id = jm.journey_id
WHERE jm.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM journey_member_role_labels label
      WHERE label.journey_member_id = jm.id
        AND label.role_type = IF(jm.role = 'HOST', 'SCHEDULE', 'PHOTO')
  );

-- Six liked posts per real user, plus varied virtual-user likes for sorting tests.
DROP TEMPORARY TABLE IF EXISTS qa350_real_like_specs;
CREATE TEMPORARY TABLE qa350_real_like_specs (user_id BIGINT, post_key VARCHAR(8), PRIMARY KEY (user_id, post_key));
INSERT INTO qa350_real_like_specs VALUES
    (1,'D13'),(1,'D19'),(1,'D25'),(1,'D28'),(1,'D33'),(1,'D38'),
    (2,'D13'),(2,'D19'),(2,'D25'),(2,'D28'),(2,'D33'),(2,'D38'),
    (3,'D13'),(3,'D19'),(3,'D25'),(3,'D28'),(3,'D33'),(3,'D38');

INSERT IGNORE INTO post_likes (post_id, user_id)
SELECT qp.post_id, s.user_id
FROM qa350_real_like_specs s
JOIN qa350_posts qp ON qp.post_key = s.post_key;

INSERT IGNORE INTO post_likes (post_id, user_id)
SELECT qp.post_id, qu.user_id
FROM qa350_posts qp
JOIN qa350_users qu ON qu.seq <= MOD(qp.seq, 8)
WHERE qu.user_id <> qp.host_user_id;

UPDATE posts p
JOIN qa350_posts qp ON qp.post_id = p.id
SET p.like_count = (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id),
    p.modified_at = @qa_now;

-- Five currently visible super-host posts.
DROP TEMPORARY TABLE IF EXISTS qa350_superhost_targets;
CREATE TEMPORARY TABLE qa350_superhost_targets (
    post_key VARCHAR(8) PRIMARY KEY,
    post_id BIGINT NOT NULL,
    host_user_id BIGINT NOT NULL,
    ticket_id BIGINT NULL
);
INSERT INTO qa350_superhost_targets (post_key, post_id, host_user_id)
SELECT post_key, post_id, host_user_id
FROM qa350_posts
WHERE post_key IN ('D13','D14','D15','D16','D17');

INSERT INTO super_host_tickets (
    user_id, source, status, boost_duration_days, used_at, created_at, modified_at
)
SELECT t.host_user_id, 'ONBOARDING_SURVEY', 'USED', 14, @qa_now, @qa_now, @qa_now
FROM qa350_superhost_targets t
WHERE NOT EXISTS (SELECT 1 FROM super_host_exposures e WHERE e.post_id = t.post_id);

UPDATE qa350_superhost_targets t
SET t.ticket_id = COALESCE(
    (SELECT e.ticket_id FROM super_host_exposures e WHERE e.post_id = t.post_id ORDER BY e.id LIMIT 1),
    (SELECT MAX(ticket.id)
     FROM super_host_tickets ticket
     LEFT JOIN super_host_exposures exposure ON exposure.ticket_id = ticket.id
     WHERE ticket.user_id = t.host_user_id
       AND ticket.source = 'ONBOARDING_SURVEY'
       AND ticket.status = 'USED'
       AND exposure.id IS NULL)
);

INSERT INTO super_host_exposures (
    ticket_id, user_id, post_id, status, started_at, ended_at, created_at, modified_at
)
SELECT ticket_id, host_user_id, post_id, 'ACTIVE', DATE_SUB(@qa_now, INTERVAL 1 DAY),
       DATE_ADD(@qa_now, INTERVAL 13 DAY), @qa_now, @qa_now
FROM qa350_superhost_targets t
WHERE ticket_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM super_host_exposures e WHERE e.ticket_id = t.ticket_id);

UPDATE super_host_exposures e
JOIN qa350_superhost_targets t ON t.post_id = e.post_id
SET e.status = 'ACTIVE',
    e.started_at = DATE_SUB(@qa_now, INTERVAL 1 DAY),
    e.ended_at = DATE_ADD(@qa_now, INTERVAL 13 DAY),
    e.modified_at = @qa_now;

-- Eight notifications per real user, half read and half unread.
DROP TEMPORARY TABLE IF EXISTS qa350_notification_resources;
CREATE TEMPORARY TABLE qa350_notification_resources (
    real_user_id BIGINT NOT NULL,
    notification_order INT NOT NULL,
    resource_id BIGINT NOT NULL,
    PRIMARY KEY (real_user_id, notification_order)
);

INSERT INTO qa350_notification_resources
SELECT real_user.user_id,
       CASE p.status WHEN 'PENDING' THEN 1 WHEN 'CONTACTING' THEN 2 WHEN 'APPROVED' THEN 3 ELSE 4 END,
       p.participation_id
FROM (SELECT 1 user_id UNION ALL SELECT 2 UNION ALL SELECT 3) real_user
JOIN qa350_participations p
  ON p.part_key LIKE CONCAT('recv-u', real_user.user_id, '-%');

INSERT INTO qa350_notification_resources
SELECT real_user.user_id, 5, p.participation_id
FROM (SELECT 1 user_id UNION ALL SELECT 2 UNION ALL SELECT 3) real_user
JOIN qa350_participations p
  ON p.part_key = CONCAT('sent-u', real_user.user_id, '-approved');

INSERT INTO qa350_notification_resources
SELECT real_user.user_id, 6, MIN(jp.id)
FROM (SELECT 1 user_id UNION ALL SELECT 2 UNION ALL SELECT 3) real_user
JOIN qa350_journeys qj ON qj.post_key = CONCAT('R0', real_user.user_id)
JOIN journey_posts jp ON jp.journey_id = qj.journey_id AND jp.is_notice = b'1'
GROUP BY real_user.user_id;

INSERT INTO qa350_notification_resources
SELECT real_user.user_id, 7, MIN(js.id)
FROM (SELECT 1 user_id UNION ALL SELECT 2 UNION ALL SELECT 3) real_user
JOIN qa350_journeys qj ON qj.post_key = CONCAT('R0', real_user.user_id)
JOIN journey_schedules js ON js.journey_id = qj.journey_id
GROUP BY real_user.user_id;

INSERT INTO qa350_notification_resources
SELECT real_user.user_id, 8, qj.journey_id
FROM (SELECT 1 user_id UNION ALL SELECT 2 UNION ALL SELECT 3) real_user
JOIN qa350_journeys qj ON qj.post_key = CONCAT('R0', real_user.user_id);

INSERT INTO notifications (
    recipient_user_id, type, body, resource_type, resource_id,
    is_read, read_at, deleted_at, created_at, modified_at
)
SELECT
    real_user.user_id,
    spec.type,
    CONCAT('[QA350:N:', real_user.user_id, ':', spec.ord, '] ', spec.body),
    spec.resource_type,
    resource.resource_id,
    IF(MOD(spec.ord, 2) = 0, b'1', b'0'),
    IF(MOD(spec.ord, 2) = 0, DATE_SUB(@qa_now, INTERVAL spec.ord MINUTE), NULL),
    NULL,
    DATE_SUB(@qa_now, INTERVAL (9 - spec.ord) HOUR), @qa_now
FROM (SELECT 1 user_id UNION ALL SELECT 2 UNION ALL SELECT 3) real_user
JOIN (
    SELECT 1 ord, 'MATCH_APPLIED' type, '새로운 동행 신청이 도착했어요.' body, 'MATCH' resource_type, 'RECEIVED_PART' resource_kind, 'pending' resource_key
    UNION ALL SELECT 2, 'MATCH_APPLIED', '연락 중인 동행 신청을 확인해주세요.', 'MATCH', 'RECEIVED_PART', 'contact'
    UNION ALL SELECT 3, 'MATCH_APPLIED', '승인할 수 있는 동행 신청이 있어요.', 'MATCH', 'RECEIVED_PART', 'approved'
    UNION ALL SELECT 4, 'MATCH_APPLIED', '거절된 신청 이력을 확인해주세요.', 'MATCH', 'RECEIVED_PART', 'rejected'
    UNION ALL SELECT 5, 'MATCH_APPROVED', '동행 신청이 승인되었어요.', 'MATCH', 'SENT_APPROVED', 'approved'
    UNION ALL SELECT 6, 'JOURNEY_NOTICE', '여행 공지가 등록되었어요.', 'JOURNEY_POST', 'NOTICE', 'notice'
    UNION ALL SELECT 7, 'SCHEDULE_CREATED', '새로운 여행 일정이 등록되었어요.', 'SCHEDULE', 'SCHEDULE', 'schedule'
    UNION ALL SELECT 8, 'TRIP_UPCOMING', '여행이 곧 시작돼요.', 'JOURNEY', 'JOURNEY', 'journey'
) spec
JOIN qa350_notification_resources resource
  ON resource.real_user_id = real_user.user_id
 AND resource.notification_order = spec.ord
WHERE NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.recipient_user_id = real_user.user_id
      AND n.body LIKE CONCAT('[QA350:N:', real_user.user_id, ':', spec.ord, ']%')
);

-- Force today's recommendation data for real users to be recalculated against the new posts.
DELETE mr
FROM mate_recommendations mr
JOIN mate_recommendation_batches batch ON batch.id = mr.batch_id
WHERE batch.user_id IN (1,2,3) AND batch.recommendation_date = @qa_today;

DELETE FROM mate_recommendation_batches
WHERE user_id IN (1,2,3) AND recommendation_date = @qa_today;

COMMIT;

-- Execution summary.
SELECT 'virtual_users' AS metric, COUNT(*) AS value
FROM users WHERE email LIKE 'qa350-user-%@dduru.test'
UNION ALL
SELECT 'complete_virtual_profiles', COUNT(*)
FROM users u
JOIN profiles p ON p.user_id = u.id
JOIN travel_tendencies tt ON tt.user_id = u.id
JOIN avatar_profiles ap ON ap.id = p.avatar_id AND ap.avatar_type = tt.avatar_type
WHERE u.email LIKE 'qa350-user-%@dduru.test'
  AND p.phone_number IS NOT NULL
  AND p.profile_image_type = 'AVATAR'
  AND p.bg_color_id IS NOT NULL
UNION ALL
SELECT 'qa_posts', COUNT(*) FROM posts WHERE JSON_CONTAINS(tags, JSON_QUOTE('QA350'))
UNION ALL
SELECT 'qa_participations', COUNT(*)
FROM participations WHERE message LIKE '[QA350:%'
UNION ALL
SELECT 'qa_private_rooms', COUNT(DISTINCT cr.id)
FROM chat_rooms cr JOIN posts p ON p.id = cr.post_id
WHERE cr.room_type = 'PRIVATE' AND JSON_CONTAINS(p.tags, JSON_QUOTE('QA350'))
UNION ALL
SELECT 'real_visible_journeys', COUNT(DISTINCT jm.journey_id)
FROM journey_members jm WHERE jm.user_id IN (1,2,3) AND jm.status = 'ACTIVE'
  AND jm.journey_id IN (SELECT j.id FROM journeys j JOIN posts p ON p.id=j.post_id WHERE JSON_CONTAINS(p.tags, JSON_QUOTE('QA350')))
UNION ALL
SELECT 'qa_notifications', COUNT(*) FROM notifications WHERE body LIKE '[QA350:N:%';

SELECT host.id AS real_user_id, p.status, COUNT(*) AS received_count
FROM (SELECT 1 id UNION ALL SELECT 2 UNION ALL SELECT 3) host
JOIN posts post ON post.user_id = host.id AND JSON_CONTAINS(post.tags, JSON_QUOTE('QA350'))
JOIN participations p ON p.post_id = post.id AND p.message LIKE '[QA350:%'
GROUP BY host.id, p.status
ORDER BY host.id, p.status;

SELECT p.user_id AS real_user_id, p.status, COUNT(*) AS submitted_count
FROM participations p
JOIN posts post ON post.id = p.post_id AND JSON_CONTAINS(post.tags, JSON_QUOTE('QA350'))
WHERE p.user_id IN (1,2,3) AND p.message LIKE '[QA350:%'
GROUP BY p.user_id, p.status
ORDER BY p.user_id, p.status;
