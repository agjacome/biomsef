# --- !Ups

CREATE TABLE search_index (
    search_index_term  VARCHAR(255) NOT NULL,
    search_index_tf    FLOAT        NOT NULL,
    search_index_idf   FLOAT        NOT NULL,
    search_index_tfidf FLOAT        NOT NULL,
    article_id         BIGINT(20)   NOT NULL,
    keyword_id         BIGINT(20)   NOT NULL,

    CONSTRAINT search_index_pk PRIMARY KEY (search_index_term, keyword_id, article_id),

    CONSTRAINT search_index_article_fk FOREIGN KEY (article_id) REFERENCES articles(article_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT search_index_keyword_fk FOREIGN KEY (keyword_id) REFERENCES keywords(keyword_id) ON UPDATE CASCADE ON DELETE CASCADE
);

# --- !Downs

DROP TABLE search_index;
