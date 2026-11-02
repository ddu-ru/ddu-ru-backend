-- 기존 회원에게도 즉시 재테스트 1회를 제공하므로 기존 행은 NULL로 유지한다.
ALTER TABLE surveys ADD COLUMN last_retested_at DATE NULL;
