--liquibase formatted sql
--changeset zieleeksw:9

CREATE TABLE quiz_sessions
(
    id                BIGSERIAL PRIMARY KEY,
    course_id         BIGINT                   NOT NULL,
    quiz_id           BIGINT                   NOT NULL,
    user_id           BIGINT                   NOT NULL,
    quiz_title        VARCHAR(120)             NOT NULL,
    question_ids_json TEXT                     NOT NULL,
    answers_json      TEXT                     NOT NULL,
    current_index     INTEGER                  NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_quiz_sessions_course_quiz_user UNIQUE (course_id, quiz_id, user_id)
);
