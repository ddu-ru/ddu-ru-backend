ALTER TABLE chat_room_members
    ADD COLUMN notification_enabled TINYINT(1) NOT NULL DEFAULT 1;
