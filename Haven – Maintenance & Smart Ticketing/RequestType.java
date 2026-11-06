package com.apartment.maintenance.model;

/**
 * The whole "prioritizer" lives here.
 * priority   -> lower number = handle first
 * hoursToFix -> used to calculate estimatedCompletionTime
 */
public enum RequestType {

    GAS_LEAK(1, 2),
    NO_HEAT(2, 8),
    WATER_LEAK(3, 24),
    BROKEN_APPLIANCE(4, 72),
    OTHER(5, 120);

    private final int priority;
    private final int hoursToFix;

    RequestType(int priority, int hoursToFix) {
        this.priority = priority;
        this.hoursToFix = hoursToFix;
    }

    public int getPriority() {
        return priority;
    }

    public int getHoursToFix() {
        return hoursToFix;
    }
}
