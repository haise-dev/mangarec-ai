SET search_path TO mangarec;

ALTER TABLE users RENAME COLUMN stripe_customer_id TO payment_customer_id;
ALTER TABLE users RENAME CONSTRAINT uk_users_stripe_customer_id TO uk_users_payment_customer_id;

ALTER TABLE subscription_plans RENAME COLUMN stripe_price_id TO provider_plan_code;
ALTER TABLE subscription_plans RENAME CONSTRAINT uk_subscription_plans_stripe_price_id TO uk_subscription_plans_provider_plan_code;
ALTER TABLE subscription_plans
    ADD COLUMN provider VARCHAR(30) NOT NULL DEFAULT 'PAYOS';

ALTER TABLE subscriptions RENAME COLUMN stripe_customer_id TO provider_customer_id;
ALTER TABLE subscriptions RENAME COLUMN stripe_subscription_id TO provider_subscription_id;
ALTER TABLE subscriptions RENAME CONSTRAINT uk_subscriptions_stripe_subscription_id TO uk_subscriptions_provider_subscription_id;
ALTER INDEX idx_subscriptions_stripe_customer_id RENAME TO idx_subscriptions_provider_customer_id;
ALTER TABLE subscriptions
    ADD COLUMN provider VARCHAR(30) NOT NULL DEFAULT 'PAYOS',
    ALTER COLUMN provider_customer_id DROP NOT NULL,
    ALTER COLUMN provider_subscription_id DROP NOT NULL;

ALTER TABLE payments RENAME COLUMN stripe_invoice_id TO provider_invoice_id;
ALTER TABLE payments RENAME COLUMN stripe_payment_intent_id TO provider_transaction_id;
ALTER TABLE payments RENAME CONSTRAINT uk_payments_stripe_invoice_id TO uk_payments_provider_invoice_id;
ALTER INDEX idx_payments_stripe_payment_intent_id RENAME TO idx_payments_provider_transaction_id;
ALTER TABLE payments
    ADD COLUMN provider VARCHAR(30) NOT NULL DEFAULT 'PAYOS',
    ADD COLUMN payos_order_code BIGINT,
    ADD COLUMN payos_payment_link_id VARCHAR(100),
    ADD COLUMN checkout_url VARCHAR(1000),
    ADD COLUMN qr_code TEXT;

CREATE UNIQUE INDEX uk_payments_payos_order_code
    ON payments (payos_order_code)
    WHERE payos_order_code IS NOT NULL;

CREATE UNIQUE INDEX uk_payments_payos_payment_link_id
    ON payments (payos_payment_link_id)
    WHERE payos_payment_link_id IS NOT NULL;

CREATE SEQUENCE payos_order_code_seq
    START WITH 100000000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE stripe_webhook_events RENAME TO payment_webhook_events;
ALTER TABLE payment_webhook_events RENAME COLUMN stripe_event_id TO provider_event_id;
ALTER TABLE payment_webhook_events RENAME CONSTRAINT uk_stripe_webhook_events_event_id TO uk_payment_webhook_events_provider_event_id;
ALTER INDEX idx_stripe_webhook_events_event_type RENAME TO idx_payment_webhook_events_event_type;
ALTER INDEX idx_stripe_webhook_events_processed_at RENAME TO idx_payment_webhook_events_processed_at;
ALTER INDEX idx_stripe_webhook_events_created_at RENAME TO idx_payment_webhook_events_created_at;

INSERT INTO subscription_plans (code, name, plan_type, provider, provider_plan_code, currency, amount_vnd, active)
VALUES
    ('PRO_MONTHLY', 'MangaRec Pro Monthly', 'MONTHLY', 'PAYOS', 'PAYOS_PRO_MONTHLY', 'VND', 49000, TRUE),
    ('PRO_YEARLY', 'MangaRec Pro Yearly', 'YEARLY', 'PAYOS', 'PAYOS_PRO_YEARLY', 'VND', 490000, TRUE)
ON CONFLICT (code) DO NOTHING;
