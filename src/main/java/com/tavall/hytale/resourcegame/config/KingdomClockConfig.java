package com.tavall.hytale.resourcegame.config;

/**
 * Kingdom clock settings.
 */
public final class KingdomClockConfig {
    private final String timezone;
    private final int dayStartHour;
    private final int dayEndHour;

    public KingdomClockConfig(String timezone, int dayStartHour, int dayEndHour) {
        this.timezone = timezone;
        this.dayStartHour = dayStartHour;
        this.dayEndHour = dayEndHour;
    }

    public String timezone() {
        return timezone;
    }

    public int dayStartHour() {
        return dayStartHour;
    }

    public int dayEndHour() {
        return dayEndHour;
    }

    public static KingdomClockConfig fromEnv() {
        return new KingdomClockConfig(
                EnvironmentConfig.get("TAVALL_KINGDOM_TIMEZONE", "UTC"),
                6,
                18
        );
    }
}
