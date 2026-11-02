-- 현재 display_order와 무관하게 질문 ID별 이미지 번호를 고정한다.
-- NULL 또는 다른 URL만 갱신하며, S3 경로의 대소문자도 구분한다.
UPDATE survey_questions AS question
JOIN (
    SELECT 'rhythm_q1' AS question_id, 1 AS image_number
    UNION ALL SELECT 'rhythm_q2', 2
    UNION ALL SELECT 'rhythm_q3', 3
    UNION ALL SELECT 'consumption_q1', 4
    UNION ALL SELECT 'consumption_q2', 5
    UNION ALL SELECT 'consumption_q3', 6
    UNION ALL SELECT 'energy_q1', 7
    UNION ALL SELECT 'energy_q2', 8
    UNION ALL SELECT 'energy_q3', 9
    UNION ALL SELECT 'decision_q1', 10
    UNION ALL SELECT 'decision_q2', 11
    UNION ALL SELECT 'decision_q3', 12
    UNION ALL SELECT 'record_style', 13
    UNION ALL SELECT 'activity_tags', 14
) AS target ON target.question_id = question.question_id
SET question.image_url = CONCAT(
        'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/survey/q', target.image_number, '.png'
    ),
    question.modified_at = NOW(6)
WHERE NOT (
    CAST(question.image_url AS BINARY) <=> CAST(CONCAT(
        'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/survey/q', target.image_number, '.png'
    ) AS BINARY)
);
