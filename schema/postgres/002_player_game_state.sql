-- Player game state table
CREATE TABLE IF NOT EXISTS player_game_state (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES player_profile(id) ON DELETE CASCADE,
    castle_id UUID,
    castle_world TEXT,
    castle_x DOUBLE PRECISION,
    castle_y DOUBLE PRECISION,
    castle_z DOUBLE PRECISION,
    citizen_count INT NOT NULL DEFAULT 0,
    troop_count INT NOT NULL DEFAULT 0,
    food INT NOT NULL DEFAULT 0,
    wood INT NOT NULL DEFAULT 0,
    iron INT NOT NULL DEFAULT 0,
    interior_world TEXT,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(profile_id)
);
