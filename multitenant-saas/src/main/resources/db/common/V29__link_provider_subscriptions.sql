ALTER TABLE tenant_subscriptions
    ADD COLUMN billing_provider VARCHAR(32);

ALTER TABLE tenant_subscriptions
    ADD COLUMN provider_subscription_id VARCHAR(255);

ALTER TABLE tenant_subscriptions
    ADD COLUMN provider_event_created_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX uk_tenant_subscription_provider_ref
    ON tenant_subscriptions(billing_provider, provider_subscription_id);
