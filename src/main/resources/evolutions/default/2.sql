# --- !Ups

CREATE TABLE keywords (
    keyword_id         BIGINT(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    keyword_normalized VARCHAR(255) NOT NULL,
    keyword_category   TINYINT      NOT NULL,

    UNIQUE KEY idx_keywords_unique_normalized_category (keyword_normalized, keyword_category)
);

# --- !Downs

DROP TABLE keywords;
