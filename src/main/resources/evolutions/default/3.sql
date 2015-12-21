# --- !Ups

CREATE TABLE articles (
    article_id            BIGINT(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_pmid          BIGINT(20)   UNIQUE,
    article_title         VARCHAR(255) NOT NULL,
    article_content       TEXT         NOT NULL,
    article_year          INT          NOT NULL,
    article_is_annotated  BOOLEAN      NOT NULL,
    article_is_processing BOOLEAN      NOT NULL
);

# --- !Downs

DROP TABLE articles;
