CREATE TABLE subscription_plan_retirement_operations (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_subscription_plan_retirement_plan
        FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
    CONSTRAINT chk_subscription_plan_retirement_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_subscription_plan_retirement_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_subscription_plan_retirement_status_updated
    ON subscription_plan_retirement_operations (status, updated_at);
