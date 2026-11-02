-- 반짝 아바타를 두두로 변경한다.
-- 운영 DB에는 동일 변경이 수동 반영되어 있을 수 있으므로 구/신 타입을 먼저 모두 허용한다.
ALTER TABLE avatar_profiles
    MODIFY COLUMN avatar_type ENUM(
        'TTUR_BANJJAK',
        'TTUR_BEOMI',
        'TTUR_BONGBONG',
        'TTUR_DASOM',
        'TTUR_DUDU',
        'TTUR_GLIM',
        'TTUR_HARAM',
        'TTUR_LUNA',
        'TTUR_MALLANG',
        'TTUR_MARU',
        'TTUR_MUDI',
        'TTUR_POGUN',
        'TTUR_POSEUL',
        'TTUR_SODAM',
        'TTUR_SPARK',
        'TTUR_SWEET',
        'TTUR_TTORANG'
    ) NOT NULL;

ALTER TABLE travel_tendencies
    MODIFY COLUMN avatar_type ENUM(
        'TTUR_BANJJAK',
        'TTUR_BEOMI',
        'TTUR_BONGBONG',
        'TTUR_DASOM',
        'TTUR_DUDU',
        'TTUR_GLIM',
        'TTUR_HARAM',
        'TTUR_LUNA',
        'TTUR_MALLANG',
        'TTUR_MARU',
        'TTUR_MUDI',
        'TTUR_POGUN',
        'TTUR_POSEUL',
        'TTUR_SODAM',
        'TTUR_SPARK',
        'TTUR_SWEET',
        'TTUR_TTORANG'
    ) NOT NULL;

UPDATE avatar_profiles
SET image_url = REPLACE(image_url, '/avatar/ttur-', '/avatar/')
WHERE image_url LIKE '%/avatar/ttur-%';

UPDATE avatar_profiles
SET display_name = '두두',
    image_url = REPLACE(image_url, '/avatar/banjjak.png', '/avatar/dudu.png'),
    avatar_type = 'TTUR_DUDU'
WHERE avatar_type IN ('TTUR_BANJJAK', 'TTUR_DUDU');

UPDATE travel_tendencies
SET avatar_type = 'TTUR_DUDU'
WHERE avatar_type = 'TTUR_BANJJAK';

-- 데이터 전환이 끝난 뒤 구 타입을 제거한다.
ALTER TABLE avatar_profiles
    MODIFY COLUMN avatar_type ENUM(
        'TTUR_BEOMI',
        'TTUR_BONGBONG',
        'TTUR_DASOM',
        'TTUR_DUDU',
        'TTUR_GLIM',
        'TTUR_HARAM',
        'TTUR_LUNA',
        'TTUR_MALLANG',
        'TTUR_MARU',
        'TTUR_MUDI',
        'TTUR_POGUN',
        'TTUR_POSEUL',
        'TTUR_SODAM',
        'TTUR_SPARK',
        'TTUR_SWEET',
        'TTUR_TTORANG'
    ) NOT NULL;

ALTER TABLE travel_tendencies
    MODIFY COLUMN avatar_type ENUM(
        'TTUR_BEOMI',
        'TTUR_BONGBONG',
        'TTUR_DASOM',
        'TTUR_DUDU',
        'TTUR_GLIM',
        'TTUR_HARAM',
        'TTUR_LUNA',
        'TTUR_MALLANG',
        'TTUR_MARU',
        'TTUR_MUDI',
        'TTUR_POGUN',
        'TTUR_POSEUL',
        'TTUR_SODAM',
        'TTUR_SPARK',
        'TTUR_SWEET',
        'TTUR_TTORANG'
    ) NOT NULL;
