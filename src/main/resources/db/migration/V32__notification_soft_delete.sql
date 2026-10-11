ALTER TABLE notifications
    ADD COLUMN deleted_at DATETIME(6) NULL;

CREATE INDEX idx_notifications_recipient_visible_created
    ON notifications (recipient_user_id, deleted_at, created_at);
