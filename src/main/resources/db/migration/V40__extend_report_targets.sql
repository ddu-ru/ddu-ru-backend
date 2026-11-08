ALTER TABLE reports
    MODIFY COLUMN post_id BIGINT NULL,
    ADD COLUMN target_type VARCHAR(20) NOT NULL DEFAULT 'POST',
    ADD COLUMN reported_user_id BIGINT NULL,
    ADD COLUMN chat_room_id BIGINT NULL,
    ADD COLUMN message_id BIGINT NULL,
    ADD COLUMN target_snapshot TEXT NULL,
    ADD COLUMN evidence_key VARCHAR(512) NULL,
    ADD CONSTRAINT fk_reports_reported_user FOREIGN KEY (reported_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_reports_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id),
    ADD CONSTRAINT fk_reports_message FOREIGN KEY (message_id) REFERENCES chat_messages(id),
    ADD CONSTRAINT uk_reports_message_reporter UNIQUE (message_id, user_id),
    ADD CONSTRAINT chk_reports_target CHECK (
        (target_type = 'POST' AND post_id IS NOT NULL AND reported_user_id IS NULL AND chat_room_id IS NULL AND message_id IS NULL)
        OR (target_type = 'USER' AND post_id IS NULL AND reported_user_id IS NOT NULL AND chat_room_id IS NULL AND message_id IS NULL)
        OR (target_type = 'CHAT_MESSAGE' AND post_id IS NULL AND reported_user_id IS NOT NULL AND chat_room_id IS NOT NULL AND message_id IS NOT NULL)
    );

CREATE INDEX idx_reports_user_target_status ON reports (user_id, target_type, reported_user_id, status);

UPDATE reports r JOIN posts p ON r.post_id = p.id
SET r.target_snapshot = CONCAT(p.title, '\n', p.content)
WHERE r.target_type = 'POST';

ALTER TABLE notifications
    MODIFY COLUMN resource_type ENUM('CHAT_ROOM','JOURNEY','JOURNEY_POST','MATCH','POST','SCHEDULE','REPORT') NOT NULL,
    MODIFY COLUMN type ENUM('JOURNEY_NOTICE','MATCH_APPLIED','MATCH_APPROVED','POST_UPDATED','SCHEDULE_CANCELED','SCHEDULE_CREATED','SCHEDULE_UPDATED','TRIP_UPCOMING','REPORT_REVIEWED') NOT NULL;
