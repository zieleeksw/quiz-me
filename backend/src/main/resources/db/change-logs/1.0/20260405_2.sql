--liquibase formatted sql
--changeset zieleeksw:10

ALTER TABLE quiz_attempts
    ADD COLUMN review_snapshot_json TEXT NOT NULL DEFAULT '[]';
