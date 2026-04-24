package com.tavall.hytale.resourcegame.services;

@FunctionalInterface
interface CheckedBooleanSupplier {
    boolean getAsBoolean() throws Exception;
}
