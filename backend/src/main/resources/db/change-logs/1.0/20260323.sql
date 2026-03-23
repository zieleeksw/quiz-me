--liquibase formatted sql
--changeset zieleeksw:1

CREATE TABLE roles
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
);

CREATE TABLE users
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_users_role_id
        FOREIGN KEY (role_id)
            REFERENCES roles (id)
);