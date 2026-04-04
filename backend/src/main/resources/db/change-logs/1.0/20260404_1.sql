--liquibase formatted sql
--changeset zieleeksw:6

CREATE TABLE quizzes
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id BIGINT NOT NULL,
    current_version_number INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_quizzes_course_id
        FOREIGN KEY (course_id)
            REFERENCES courses (id)
);

CREATE TABLE quiz_versions
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    title VARCHAR(120) NOT NULL,
    mode VARCHAR(16) NOT NULL,
    random_count INTEGER,
    question_order VARCHAR(16) NOT NULL,
    answer_order VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_quiz_versions_quiz_id
        FOREIGN KEY (quiz_id)
            REFERENCES quizzes (id)
            ON DELETE CASCADE,
    CONSTRAINT uq_quiz_versions_quiz_id_version_number
        UNIQUE (quiz_id, version_number)
);

CREATE TABLE quiz_version_questions
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quiz_version_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_quiz_version_questions_quiz_version_id
        FOREIGN KEY (quiz_version_id)
            REFERENCES quiz_versions (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_quiz_version_questions_question_id
        FOREIGN KEY (question_id)
            REFERENCES questions (id),
    CONSTRAINT uq_quiz_version_questions_quiz_version_id_display_order
        UNIQUE (quiz_version_id, display_order)
);

CREATE TABLE quiz_version_categories
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quiz_version_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_quiz_version_categories_quiz_version_id
        FOREIGN KEY (quiz_version_id)
            REFERENCES quiz_versions (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_quiz_version_categories_category_id
        FOREIGN KEY (category_id)
            REFERENCES categories (id),
    CONSTRAINT uq_quiz_version_categories_quiz_version_id_display_order
        UNIQUE (quiz_version_id, display_order)
);
