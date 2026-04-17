# Postgres Schema

Apply files in order:
1. 001_player_profile.sql
2. 002_player_game_state.sql
3. 003_player_game_state_evolution.sql

These are the canonical tables for player identity and game state. They are intentionally narrow and
paired with a JSONB metadata field for future citizen/troop expansion.

The evolution script is intentionally idempotent. It keeps existing local or remote development
databases compatible as gameplay fields move from placeholder metadata into first-class columns.
