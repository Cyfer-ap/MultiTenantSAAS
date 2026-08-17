CREATE TABLE email_verification_challenges (
    id UUID NOT NULL,
    email VARCHAR(150) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_email_verification_challenges PRIMARY KEY (id)
);

CREATE INDEX idx_email_verification_email
    ON email_verification_challenges (email);

CREATE INDEX idx_email_verification_expires_at
    ON email_verification_challenges (expires_at);

CREATE TABLE trusted_email_browsers (
    id UUID NOT NULL,
    email VARCHAR(150) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_trusted_email_browsers PRIMARY KEY (id),
    CONSTRAINT uk_trusted_email_browser_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_trusted_email_browser_email
    ON trusted_email_browsers (email);

CREATE INDEX idx_trusted_email_browser_expires_at
    ON trusted_email_browsers (expires_at);
