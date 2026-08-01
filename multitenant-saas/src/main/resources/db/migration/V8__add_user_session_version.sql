ALTER TABLE app_users
    ADD COLUMN session_version BIGINT DEFAULT 0 NOT NULL;
