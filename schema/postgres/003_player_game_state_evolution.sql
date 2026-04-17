-- Idempotent schema evolution for existing development databases.
ALTER TABLE IF EXISTS player_game_state
    ADD COLUMN IF NOT EXISTS castle_asset_type TEXT NOT NULL DEFAULT 'stone_column_castle';

ALTER TABLE IF EXISTS player_game_state
    ADD COLUMN IF NOT EXISTS metadata_json JSONB;
