ALTER TABLE user_recommendation_destination_preferences
    ADD COLUMN preference_rank INT NOT NULL;

ALTER TABLE user_recommendation_destination_preferences
    ADD CONSTRAINT chk_recommendation_destination_rank CHECK (preference_rank BETWEEN 1 AND 3);

ALTER TABLE user_recommendation_destination_preferences
    ADD CONSTRAINT uk_recommendation_destination_rank UNIQUE (user_id, preference_rank);
