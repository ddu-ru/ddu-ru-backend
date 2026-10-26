CREATE TABLE journey_polls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    journey_id BIGINT NOT NULL,
    author_user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    allow_multiple_choices BOOLEAN NOT NULL,
    anonymous BOOLEAN NOT NULL,
    allow_option_addition BOOLEAN NOT NULL,
    closes_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    CONSTRAINT fk_journey_polls_journey FOREIGN KEY (journey_id) REFERENCES journeys(id),
    CONSTRAINT fk_journey_polls_author FOREIGN KEY (author_user_id) REFERENCES users(id)
);
CREATE INDEX idx_journey_polls_journey_id_id ON journey_polls(journey_id, id);

CREATE TABLE journey_poll_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id BIGINT NOT NULL,
    content VARCHAR(100) NOT NULL,
    display_order INT NOT NULL,
    CONSTRAINT fk_journey_poll_options_poll FOREIGN KEY (poll_id) REFERENCES journey_polls(id),
    CONSTRAINT uk_journey_poll_options_order UNIQUE (poll_id, display_order)
);

CREATE TABLE journey_poll_votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    option_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    CONSTRAINT fk_journey_poll_votes_option FOREIGN KEY (option_id) REFERENCES journey_poll_options(id),
    CONSTRAINT fk_journey_poll_votes_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_journey_poll_votes_option_user UNIQUE (option_id, user_id)
);

ALTER TABLE chat_messages MODIFY COLUMN message_type ENUM('IMAGE', 'POLL', 'SYSTEM', 'TEXT') NOT NULL;
ALTER TABLE chat_messages ADD COLUMN poll_id BIGINT NULL;
ALTER TABLE chat_messages ADD CONSTRAINT uk_chat_messages_poll UNIQUE (poll_id);
ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_messages_poll FOREIGN KEY (poll_id) REFERENCES journey_polls(id);
