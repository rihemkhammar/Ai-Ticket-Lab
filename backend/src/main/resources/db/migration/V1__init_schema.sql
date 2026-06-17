CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
                       id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       username      VARCHAR(50)  NOT NULL UNIQUE,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(50)  NOT NULL DEFAULT 'TECHNICIAN',
                       created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE tickets (
                         id          BIGSERIAL    PRIMARY KEY,
                         created_by  UUID         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                         title       VARCHAR(255) NOT NULL,
                         description TEXT         NOT NULL,
                         status      VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
                         created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tickets_created_by ON tickets(created_by);