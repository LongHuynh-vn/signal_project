package com.alerts;

import java.util.List;

import com.data_management.PatientRecord;

/**
 * Strategy interface for alert detection algorithms.
 */
public interface AlertStrategy {

    List<Alert> checkAlert(List<PatientRecord> records);
}
