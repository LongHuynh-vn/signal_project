package com.alerts;

/**
 * Base decorator that wraps an alert and delegates all behavior by default.
 */
public class AlertDecorator implements Alert {
    private final Alert wrappedAlert;

    public AlertDecorator(Alert wrappedAlert) {
        if (wrappedAlert == null) {
            throw new IllegalArgumentException("Wrapped alert must not be null");
        }
        this.wrappedAlert = wrappedAlert;
    }

    protected Alert getWrappedAlert() {
        return wrappedAlert;
    }

    @Override
    public String getPatientId() {
        return wrappedAlert.getPatientId();
    }

    @Override
    public String getCondition() {
        return wrappedAlert.getCondition();
    }

    @Override
    public long getTimestamp() {
        return wrappedAlert.getTimestamp();
    }

    @Override
    public AlertPriority getPriority() {
        return wrappedAlert.getPriority();
    }

    @Override
    public boolean isRepeated() {
        return wrappedAlert.isRepeated();
    }

    @Override
    public long getRepeatIntervalMillis() {
        return wrappedAlert.getRepeatIntervalMillis();
    }

    @Override
    public String toString() {
        return wrappedAlert.toString();
    }
}
