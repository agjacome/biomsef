# --- !Ups

CREATE TABLE annotations (
    annotation_id    BIGINT(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id       BIGINT(20)   NOT NULL,
    keyword_id       BIGINT(20)   NOT NULL,
    annotation_text  VARCHAR(255) NOT NULL,
    annotation_start INT          NOT NULL,
    annotation_end   INT          NOT NULL,

    CONSTRAINT annotation_article_fk FOREIGN KEY (article_id) REFERENCES articles(article_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT annotation_keyword_fk FOREIGN KEY (keyword_id) REFERENCES keywords(keyword_id) ON UPDATE CASCADE ON DELETE CASCADE
);

# --- !Downs

DROP TABLE annotations;
