ALTER TABLE users
    ADD COLUMN nickname VARCHAR(10);

ALTER TABLE users
    ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);
