# --- !Ups

CREATE TABLE authors (
    author_id         BIGINT(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    author_last_name  VARCHAR(255) NOT NULL,
    author_first_name VARCHAR(255) NOT NULL,
    author_initials   VARCHAR(10)  NOT NULL,

    UNIQUE KEY idx_authors_unique_name (author_last_name, author_first_name, author_initials)
);

CREATE TABLE article_authors (
    author_id  BIGINT(20) NOT NULL,
    article_id BIGINT(20) NOT NULL,
    position   INT        NOT NULL,

    CONSTRAINT article_authors_primary_key PRIMARY KEY (author_id, article_id),

    CONSTRAINT article_authors_author_fk  FOREIGN KEY (author_id)  REFERENCES authors(author_id)   ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT article_authors_article_fk FOREIGN KEY (article_id) REFERENCES articles(article_id) ON UPDATE CASCADE ON DELETE CASCADE
);


# --- !Downs

DROP TABLE article_authors;
DROP TABLE authors;
