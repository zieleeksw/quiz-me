--liquibase formatted sql
--changeset zieleeksw:3

ALTER TABLE courses
    ADD COLUMN owner_user_id BIGINT;

UPDATE courses
SET owner_user_id = (
    SELECT id
    FROM users
    ORDER BY id
    FETCH FIRST ROW ONLY
)
WHERE owner_user_id IS NULL;

ALTER TABLE courses
    ADD CONSTRAINT fk_courses_owner_user_id
        FOREIGN KEY (owner_user_id)
            REFERENCES users (id);

ALTER TABLE courses
    ALTER COLUMN owner_user_id SET NOT NULL;
