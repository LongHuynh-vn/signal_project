package com.alerts;

/**
 * Decorator that marks an alert as a repeated occurrence.
 */
public class RepeatedAlertDecorator extends AlertDecorator {
    private final long repeatIntervalMillis;

    public RepeatedAlertDecorator(Alert wrappedAlert, long repeatIntervalMillis) {
        super(wrappedAlert);
        this.repeatIntervalMillis = repeatIntervalMillis;
    }

    @Override
    public boolean isRepeated() {
        return true;
    }

    @Override
    public long getRepeatIntervalMillis() {
        return repeatIntervalMillis;
    }
}
