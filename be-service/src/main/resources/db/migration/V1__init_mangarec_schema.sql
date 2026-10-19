CREATE SCHEMA IF NOT EXISTS mangarec;

SET search_path TO mangarec;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    subscription_status VARCHAR(20) NOT NULL DEFAULT 'FREE',
    subscription_expired_at TIMESTAMPTZ,
    stripe_customer_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ,
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED')),
    CONSTRAINT chk_users_subscription_status CHECK (subscription_status IN ('FREE', 'PRO')),
    CONSTRAINT uk_users_stripe_customer_id UNIQUE (stripe_customer_id)
);

CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));
CREATE INDEX idx_users_subscription_status ON users (subscription_status);
CREATE INDEX idx_users_created_at ON users (created_at);

CREATE TRIGGER trg_users_set_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_auth_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_auth_identities_provider CHECK (provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT chk_auth_identities_password CHECK (
        (provider = 'LOCAL' AND password_hash IS NOT NULL)
        OR
        (provider <> 'LOCAL' AND password_hash IS NULL)
    ),
    CONSTRAINT uk_auth_identities_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uk_auth_identities_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_auth_identities_user_id ON user_auth_identities (user_id);

CREATE TRIGGER trg_auth_identities_set_updated_at
BEFORE UPDATE ON user_auth_identities
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    device_id VARCHAR(100),
    ip_hash VARCHAR(128),
    user_agent_hash VARCHAR(128),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON user_refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON user_refresh_tokens (expires_at);

CREATE TABLE auth_action_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose VARCHAR(50) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_auth_action_tokens_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    CONSTRAINT uk_auth_action_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_auth_action_tokens_user_id ON auth_action_tokens (user_id);
CREATE INDEX idx_auth_action_tokens_expires_at ON auth_action_tokens (expires_at);

CREATE TABLE guest_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id VARCHAR(100) NOT NULL,
    first_ip_hash VARCHAR(128),
    last_ip_hash VARCHAR(128),
    user_agent_hash VARCHAR(128),
    claimed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    claimed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_guest_profiles_guest_id UNIQUE (guest_id)
);

CREATE INDEX idx_guest_profiles_claimed_by_user_id ON guest_profiles (claimed_by_user_id);
CREATE INDEX idx_guest_profiles_last_ip_hash ON guest_profiles (last_ip_hash);
CREATE INDEX idx_guest_profiles_last_seen_at ON guest_profiles (last_seen_at);

CREATE TABLE user_auth_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    guest_id VARCHAR(100),
    event_type VARCHAR(50) NOT NULL,
    ip_hash VARCHAR(128),
    user_agent_hash VARCHAR(128),
    success BOOLEAN NOT NULL DEFAULT TRUE,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_auth_events_event_type CHECK (
        event_type IN (
            'REGISTER',
            'LOGIN_SUCCESS',
            'LOGIN_FAILED',
            'LOGOUT',
            'GOOGLE_LOGIN',
            'TOKEN_REFRESH'
        )
    )
);

CREATE INDEX idx_auth_events_user_id ON user_auth_events (user_id);
CREATE INDEX idx_auth_events_guest_id ON user_auth_events (guest_id);
CREATE INDEX idx_auth_events_event_type ON user_auth_events (event_type);
CREATE INDEX idx_auth_events_created_at ON user_auth_events (created_at);
CREATE INDEX idx_auth_events_ip_hash ON user_auth_events (ip_hash);

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    plan_type VARCHAR(20) NOT NULL,
    stripe_price_id VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    amount_vnd BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_subscription_plans_type CHECK (plan_type IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_subscription_plans_currency CHECK (currency = 'VND'),
    CONSTRAINT chk_subscription_plans_amount CHECK (amount_vnd >= 0),
    CONSTRAINT uk_subscription_plans_code UNIQUE (code),
    CONSTRAINT uk_subscription_plans_stripe_price_id UNIQUE (stripe_price_id)
);

CREATE INDEX idx_subscription_plans_active ON subscription_plans (active);

CREATE TRIGGER trg_subscription_plans_set_updated_at
BEFORE UPDATE ON subscription_plans
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE RESTRICT,
    stripe_customer_id VARCHAR(100) NOT NULL,
    stripe_subscription_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_subscriptions_status CHECK (
        status IN (
            'INCOMPLETE',
            'INCOMPLETE_EXPIRED',
            'TRIALING',
            'ACTIVE',
            'PAST_DUE',
            'CANCELED',
            'UNPAID',
            'PAUSED'
        )
    ),
    CONSTRAINT uk_subscriptions_stripe_subscription_id UNIQUE (stripe_subscription_id)
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions (plan_id);
CREATE INDEX idx_subscriptions_stripe_customer_id ON subscriptions (stripe_customer_id);
CREATE INDEX idx_subscriptions_status ON subscriptions (status);
CREATE INDEX idx_subscriptions_current_period_end ON subscriptions (current_period_end);

CREATE TRIGGER trg_subscriptions_set_updated_at
BEFORE UPDATE ON subscriptions
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    subscription_id UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    stripe_invoice_id VARCHAR(100),
    stripe_payment_intent_id VARCHAR(100),
    amount_vnd BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL,
    paid_at TIMESTAMPTZ,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payments_currency CHECK (currency = 'VND'),
    CONSTRAINT chk_payments_amount CHECK (amount_vnd >= 0),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT uk_payments_stripe_invoice_id UNIQUE (stripe_invoice_id)
);

CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_subscription_id ON payments (subscription_id);
CREATE INDEX idx_payments_stripe_payment_intent_id ON payments (stripe_payment_intent_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at);

CREATE TRIGGER trg_payments_set_updated_at
BEFORE UPDATE ON payments
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE stripe_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stripe_event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    processed_at TIMESTAMPTZ,
    processing_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stripe_webhook_events_event_id UNIQUE (stripe_event_id)
);

CREATE INDEX idx_stripe_webhook_events_event_type ON stripe_webhook_events (event_type);
CREATE INDEX idx_stripe_webhook_events_processed_at ON stripe_webhook_events (processed_at);
CREATE INDEX idx_stripe_webhook_events_created_at ON stripe_webhook_events (created_at);

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    preferred_genres_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    excluded_tags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    preferred_languages_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_preferences_preferred_genres_array CHECK (jsonb_typeof(preferred_genres_json) = 'array'),
    CONSTRAINT chk_preferences_excluded_tags_array CHECK (jsonb_typeof(excluded_tags_json) = 'array'),
    CONSTRAINT chk_preferences_preferred_languages_array CHECK (jsonb_typeof(preferred_languages_json) = 'array')
);

CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    guest_profile_id UUID REFERENCES guest_profiles(id) ON DELETE RESTRICT,
    title VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_chat_sessions_owner CHECK (user_id IS NOT NULL OR guest_profile_id IS NOT NULL)
);

CREATE INDEX idx_chat_sessions_user_id ON chat_sessions (user_id);
CREATE INDEX idx_chat_sessions_guest_profile_id ON chat_sessions (guest_profile_id);
CREATE INDEX idx_chat_sessions_is_active ON chat_sessions (is_active);
CREATE INDEX idx_chat_sessions_created_at ON chat_sessions (created_at);

CREATE TRIGGER trg_chat_sessions_set_updated_at
BEFORE UPDATE ON chat_sessions
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    token_usage INT,
    response_time_ms INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_chat_messages_sender_type CHECK (sender_type IN ('USER', 'AGENT')),
    CONSTRAINT chk_chat_messages_token_usage CHECK (token_usage IS NULL OR token_usage >= 0),
    CONSTRAINT chk_chat_messages_response_time CHECK (response_time_ms IS NULL OR response_time_ms >= 0)
);

CREATE INDEX idx_chat_messages_session_id ON chat_messages (session_id);
CREATE INDEX idx_chat_messages_sender_type ON chat_messages (sender_type);
CREATE INDEX idx_chat_messages_created_at ON chat_messages (created_at);

CREATE TABLE manga_result_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manga_dex_id VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_url VARCHAR(1000),
    mangadex_url VARCHAR(1000),
    genres_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    tags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    raw_payload JSONB,
    source VARCHAR(30) NOT NULL DEFAULT 'AI_SERVICE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_manga_snapshots_genres_array CHECK (jsonb_typeof(genres_json) = 'array'),
    CONSTRAINT chk_manga_snapshots_tags_array CHECK (jsonb_typeof(tags_json) = 'array')
);

CREATE INDEX idx_manga_snapshots_manga_dex_id ON manga_result_snapshots (manga_dex_id);
CREATE INDEX idx_manga_snapshots_created_at ON manga_result_snapshots (created_at);

CREATE TABLE recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    manga_snapshot_id UUID REFERENCES manga_result_snapshots(id) ON DELETE SET NULL,
    manga_dex_id VARCHAR(100) NOT NULL,
    rank INT NOT NULL,
    score NUMERIC(8, 5),
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_recommendations_rank CHECK (rank > 0),
    CONSTRAINT uk_recommendations_message_rank UNIQUE (message_id, rank)
);

CREATE INDEX idx_recommendations_message_id ON recommendations (message_id);
CREATE INDEX idx_recommendations_manga_snapshot_id ON recommendations (manga_snapshot_id);
CREATE INDEX idx_recommendations_manga_dex_id ON recommendations (manga_dex_id);
