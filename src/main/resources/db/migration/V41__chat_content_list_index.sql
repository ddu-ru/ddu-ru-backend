CREATE INDEX idx_chat_messages_room_type_id
    ON chat_messages (room_id, message_type, id);
