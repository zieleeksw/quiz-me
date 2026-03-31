--liquibase formatted sql
--changeset zieleeksw:5

CREATE TABLE categories
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_categories_course_id
        FOREIGN KEY (course_id)
            REFERENCES courses (id)
);

CREATE TABLE question_version_categories
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_version_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_question_version_categories_question_version_id
        FOREIGN KEY (question_version_id)
            REFERENCES question_versions (id),
    CONSTRAINT fk_question_version_categories_category_id
        FOREIGN KEY (category_id)
            REFERENCES categories (id),
    CONSTRAINT uq_question_version_categories_question_version_id_display_order
        UNIQUE (question_version_id, display_order)
);
