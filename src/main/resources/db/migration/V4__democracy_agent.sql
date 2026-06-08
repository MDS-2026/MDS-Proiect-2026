-- Democracy Agent tables

ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS message_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS metadata TEXT;

CREATE TABLE IF NOT EXISTS democracy_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    goal_text TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COLLECTING',
    proposals_json TEXT,
    member_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_democracy_sessions_group_status
    ON democracy_sessions (group_id, status);

CREATE TABLE IF NOT EXISTS member_constraints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES democracy_sessions(id) ON DELETE CASCADE,
    member_email VARCHAR(255) NOT NULL,
    start_date VARCHAR(20),
    end_date VARCHAR(20),
    deal_breakers TEXT,
    max_contribution NUMERIC(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (session_id, member_email)
);

CREATE TABLE IF NOT EXISTS democracy_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES democracy_sessions(id) ON DELETE CASCADE,
    voter_email VARCHAR(255) NOT NULL,
    option_index INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (session_id, voter_email)
);
