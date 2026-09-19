CREATE INDEX idx_posts_deleted_created_destination
    ON posts (is_deleted, created_at, destination_id);

CREATE INDEX idx_posts_destination_deleted_status
    ON posts (destination_id, is_deleted, status);
