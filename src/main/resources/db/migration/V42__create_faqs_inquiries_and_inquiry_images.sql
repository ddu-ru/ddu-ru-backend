CREATE TABLE faqs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category VARCHAR(20) NOT NULL,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    visible BIT(1) NOT NULL DEFAULT b'0',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6),
    modified_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_faqs_visible_order (visible, sort_order, id)
);

CREATE TABLE inquiries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(20) NOT NULL,
    content VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    answer_content VARCHAR(5000),
    answered_by BIGINT,
    answered_at DATETIME(6),
    answer_modified_at DATETIME(6),
    created_at DATETIME(6),
    modified_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_inquiries_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_inquiries_answered_by FOREIGN KEY (answered_by) REFERENCES users (id),
    INDEX idx_inquiries_user_id (user_id, id),
    INDEX idx_inquiries_status_id (status, id)
);

CREATE TABLE inquiry_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inquiry_images_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries (id),
    INDEX idx_inquiry_images_inquiry_order (inquiry_id, sort_order)
);
