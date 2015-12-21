# --- !Ups

CREATE TABLE users (
    user_id       BIGINT(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_email    VARCHAR(255) NOT NULL UNIQUE,
    user_password CHAR(60)     NOT NULL
);

-- Default user is:
-- email: admin@biomsef.sing.ei.uvigo.es
-- pass:  biomsef-admin
INSERT INTO users (user_email, user_password) VALUES('admin@biomsef.sing.ei.uvigo.es', '$2a$10$ctPrJX1k/iTG6WPJa2k9fegxpmhyJg/XZXeo14vpQB/dlpIxKMTWS');

# --- !Downs

DROP TABLE users;
