package com.alerts;

/**
 * Decorator that raises an alert's priority.
 */
public class PriorityAlertDecorator extends AlertDecorator {
    private final AlertPriority priority;

    public PriorityAlertDecorator(Alert wrappedAlert) {
        this(wrappedAlert, AlertPriority.HIGH);
    }

    public PriorityAlertDecorator(Alert wrappedAlert, AlertPriority priority) {
        super(wrappedAlert);
        this.priority = priority;
    }

    @Override
    public AlertPriority getPriority() {
        return priority;
    }
}
