package com.alerts;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertDecoratorTest {

    @Test
    void testBaseDecoratorDelegatesAlertFields() {
        Alert baseAlert = new GenericAlert("1", AlertGenerator.TRIGGERED_ALERT, 1000L);
        AlertDecorator decorator = new AlertDecorator(baseAlert);

        assertEquals("1", decorator.getPatientId());
        assertEquals(AlertGenerator.TRIGGERED_ALERT, decorator.getCondition());
        assertEquals(1000L, decorator.getTimestamp());
        assertEquals(AlertPriority.NORMAL, decorator.getPriority());
        assertFalse(decorator.isRepeated());
    }

    @Test
    void testPriorityDecoratorMarksAlertHighPriority() {
        Alert baseAlert = new BloodPressureAlert("1", AlertGenerator.BP_CRITICAL, 1000L);
        Alert decoratedAlert = new PriorityAlertDecorator(baseAlert);

        assertEquals(AlertPriority.HIGH, decoratedAlert.getPriority());
        assertEquals(AlertGenerator.BP_CRITICAL, decoratedAlert.getCondition());
    }

    @Test
    void testRepeatedDecoratorMarksAlertRepeated() {
        Alert baseAlert = new GenericAlert("1", AlertGenerator.TRIGGERED_ALERT, 1000L);
        Alert decoratedAlert = new RepeatedAlertDecorator(baseAlert, 5000L);

        assertTrue(decoratedAlert.isRepeated());
        assertEquals(5000L, decoratedAlert.getRepeatIntervalMillis());
        assertEquals(AlertGenerator.TRIGGERED_ALERT, decoratedAlert.getCondition());
    }

    @Test
    void testAlertGeneratorAppliesPriorityAndRepeatedDecorators() {
        AlertGenerator generator = new AlertGenerator(null);

        generator.triggerAlert(new BloodPressureAlert("1", AlertGenerator.BP_CRITICAL, 1000L));
        generator.triggerAlert(new BloodPressureAlert("1", AlertGenerator.BP_CRITICAL, 2000L));

        List<Alert> alerts = generator.getAlerts();
        assertEquals(AlertPriority.HIGH, alerts.get(0).getPriority());
        assertFalse(alerts.get(0).isRepeated());
        assertEquals(AlertPriority.HIGH, alerts.get(1).getPriority());
        assertTrue(alerts.get(1).isRepeated());
        assertEquals(1000L, alerts.get(1).getRepeatIntervalMillis());
    }
}
