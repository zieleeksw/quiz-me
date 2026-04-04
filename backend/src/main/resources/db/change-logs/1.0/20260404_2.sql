--liquibase formatted sql
--changeset zieleeksw:7

ALTER TABLE quizzes
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
