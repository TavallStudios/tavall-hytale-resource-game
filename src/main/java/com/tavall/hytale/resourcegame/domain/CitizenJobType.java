package com.tavall.hytale.resourcegame.domain;

/**
 * Role assignments for citizens in the aggregated population continuum.
 */
public enum CitizenJobType {
    IDLE,
    GATHERER,
    HUNTER,
    COOK,
    MINER,
    BLACKSMITH,
    ARCHITECT,
    GRUNT_BUILDER,
    /**
     * Legacy aggregate bucket retained for old metadata. New planning uses explicit builder roles.
     */
    BUILDER,
    TRAINEE,
    SOLDIER
}
