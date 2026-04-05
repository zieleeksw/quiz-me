--liquibase formatted sql
--changeset zieleeksw:8

CREATE TABLE quiz_attempts
(
    id              BIGSERIAL PRIMARY KEY,
    course_id       BIGINT                   NOT NULL,
    quiz_id         BIGINT                   NOT NULL,
    user_id         BIGINT                   NOT NULL,
    quiz_title      VARCHAR(120)             NOT NULL,
    correct_answers INTEGER                  NOT NULL,
    total_questions INTEGER                  NOT NULL,
    finished_at     TIMESTAMP WITH TIME ZONE NOT NULL
);
