-- 테스트 DB에는 수동 반영된 순서다. 다른 환경과 신규 DB에도 같은 순서를 적용한다.
-- 이미 목표 순서인 문항은 modified_at을 포함해 변경하지 않는다.
UPDATE survey_questions AS question
JOIN (
    SELECT 'rhythm_q1' AS question_id, 1 AS display_order
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
SET question.display_order = target.display_order,
    question.modified_at = NOW(6)
WHERE question.display_order <> target.display_order;
