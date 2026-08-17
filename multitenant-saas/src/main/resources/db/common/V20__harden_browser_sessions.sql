ALTER TABLE refresh_tokens
    ADD COLUMN csrf_token_hash VARCHAR(64);

ALTER TABLE refresh_tokens
    ADD COLUMN persistent_session BOOLEAN NOT NULL DEFAULT FALSE;