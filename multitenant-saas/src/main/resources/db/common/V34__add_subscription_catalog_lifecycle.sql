CREATE TABLE IF NOT EXISTS subscription_plan_provider_mappings (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    environment VARCHAR(16) NOT NULL,
    provider_product_id VARCHAR(255),
    provider_price_id VARCHAR(255),
    provider_plan_id VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_plan_provider_mapping_plan
        FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
    CONSTRAINT uk_plan_provider_price_ref
        UNIQUE (provider, environment, provider_price_id),
    CONSTRAINT uk_plan_provider_plan_ref
        UNIQUE (provider, environment, provider_plan_id)
);

CREATE INDEX IF NOT EXISTS idx_plan_provider_mapping_lookup
    ON subscription_plan_provider_mappings(plan_id, provider, environment, status);

ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS plan_code_snapshot VARCHAR(60);
ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS plan_name_snapshot VARCHAR(150);
ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS plan_description_snapshot VARCHAR(500);
ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS billing_interval_snapshot VARCHAR(20);
ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS price_snapshot NUMERIC(19, 2);
ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS currency_snapshot VARCHAR(3);
ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS max_users_snapshot INTEGER;
ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS max_projects_snapshot INTEGER;
ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS max_storage_mb_snapshot BIGINT;

UPDATE tenant_subscriptions
SET plan_code_snapshot = (
    SELECT p.code FROM subscription_plans p WHERE p.id = tenant_subscriptions.plan_id
)
WHERE plan_code_snapshot IS NULL;

UPDATE tenant_subscriptions
SET plan_name_snapshot = (
    SELECT p.name FROM subscription_plans p WHERE p.id = tenant_subscriptions.plan_id
)
WHERE plan_name_snapshot IS NULL;

UPDATE tenant_subscriptions
SET plan_description_snapshot = (
    SELECT p.description FROM subscription_plans p WHERE p.id = tenant_subscriptions.plan_id
)
WHERE plan_description_snapshot IS NULL;

UPDATE tenant_subscriptions
SET billing_interval_snapshot = (
    SELECT p.billing_interval FROM subscription_plans p WHERE p.id = tenant_subscriptions.plan_id
)
WHERE billing_interval_snapshot IS NULL;

UPDATE tenant_subscriptions
SET price_snapshot = (
    SELECT p.price FROM subscription_plans p WHERE p.id = tenant_subscriptions.plan_id
)
WHERE price_snapshot IS NULL;

UPDATE tenant_subscriptions
SET currency_snapshot = (
    SELECT p.currency FROM subscription_plans p WHERE p.id = tenant_subscriptions.plan_id
)
WHERE currency_snapshot IS NULL;

UPDATE tenant_subscriptions
SET max_users_snapshot = (
    SELECT p.max_users FROM subscription_plans p WHERE p.id = tenant_subscriptions.plan_id
)
WHERE max_users_snapshot IS NULL;

UPDATE tenant_subscriptions
SET max_projects_snapshot = (
    SELECT p.max_projects FROM subscription_plans p WHERE p.id = tenant_subscriptions.plan_id
)
WHERE max_projects_snapshot IS NULL;

UPDATE tenant_subscriptions
SET max_storage_mb_snapshot = (
    SELECT p.max_storage_mb FROM subscription_plans p WHERE p.id = tenant_subscriptions.plan_id
)
WHERE max_storage_mb_snapshot IS NULL;

ALTER TABLE tenant_subscriptions ALTER COLUMN plan_code_snapshot SET NOT NULL;
ALTER TABLE tenant_subscriptions ALTER COLUMN plan_name_snapshot SET NOT NULL;
ALTER TABLE tenant_subscriptions ALTER COLUMN billing_interval_snapshot SET NOT NULL;
ALTER TABLE tenant_subscriptions ALTER COLUMN price_snapshot SET NOT NULL;
ALTER TABLE tenant_subscriptions ALTER COLUMN currency_snapshot SET NOT NULL;
