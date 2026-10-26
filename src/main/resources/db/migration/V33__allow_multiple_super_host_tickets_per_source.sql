-- 온보딩 완료 시 같은 출처의 슈퍼호스트 티켓을 여러 장 지급할 수 있도록 기존 유니크 인덱스를 제거한다.
-- B32 clean baseline에는 해당 인덱스가 없으므로 기존 DB에서만 제거한다.
SET @drop_super_host_ticket_source_unique = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'super_host_tickets'
              AND index_name = 'uk_super_host_tickets_user_source'
        ),
        'ALTER TABLE super_host_tickets DROP INDEX uk_super_host_tickets_user_source',
        'SELECT 1'
    )
);

PREPARE drop_super_host_ticket_source_unique_stmt FROM @drop_super_host_ticket_source_unique;
EXECUTE drop_super_host_ticket_source_unique_stmt;
DEALLOCATE PREPARE drop_super_host_ticket_source_unique_stmt;

-- 기존 온보딩 완료 사용자도 사용 여부와 관계없이 지급 이력이 총 3장이 되도록 부족분을 보충한다.
INSERT INTO super_host_tickets (
    user_id,
    source,
    status,
    boost_duration_days,
    used_at,
    created_at,
    modified_at
)
SELECT existing.user_id,
       'ONBOARDING_SURVEY',
       'UNUSED',
       3,
       NULL,
       NOW(6),
       NOW(6)
FROM (
    SELECT user_id, COUNT(*) AS ticket_count
    FROM super_host_tickets
    WHERE source = 'ONBOARDING_SURVEY'
    GROUP BY user_id
) existing
JOIN (
    SELECT 1 AS ticket_number
    UNION ALL SELECT 2
    UNION ALL SELECT 3
) target_tickets
  ON target_tickets.ticket_number > existing.ticket_count;
