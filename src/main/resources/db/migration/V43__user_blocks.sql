CREATE TABLE user_blocks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    blocker_user_id BIGINT NOT NULL,
    blocked_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_blocks_blocker_blocked UNIQUE (blocker_user_id, blocked_user_id),
    CONSTRAINT chk_user_blocks_not_self CHECK (blocker_user_id <> blocked_user_id),
    CONSTRAINT fk_user_blocks_blocker FOREIGN KEY (blocker_user_id) REFERENCES users(id),
    CONSTRAINT fk_user_blocks_blocked FOREIGN KEY (blocked_user_id) REFERENCES users(id)
);

CREATE INDEX idx_user_blocks_blocker_id ON user_blocks (blocker_user_id, id);
CREATE INDEX idx_user_blocks_blocked_id ON user_blocks (blocked_user_id, blocker_user_id);
