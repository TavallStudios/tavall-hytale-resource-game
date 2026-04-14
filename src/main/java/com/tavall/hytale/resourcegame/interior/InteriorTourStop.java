package com.tavall.hytale.resourcegame.interior;

import com.hypixel.hytale.math.vector.Vector3d;

/**
 * One visible onboarding stop inside the placeholder interior.
 */
public final class InteriorTourStop {
    private final int stepNumber;
    private final String label;
    private final String description;
    private final Vector3d position;

    public InteriorTourStop(int stepNumber, String label, String description, Vector3d position) {
        this.stepNumber = stepNumber;
        this.label = label;
        this.description = description;
        this.position = position;
    }

    public int stepNumber() {
        return stepNumber;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public Vector3d position() {
        return position;
    }

    public String displayLabel() {
        return "Step " + stepNumber + ": " + label;
    }
}
