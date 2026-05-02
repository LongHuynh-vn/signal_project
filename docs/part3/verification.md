# Part 3 Verification

## Test Verification

Run the Part 3 test suite with:

```sh
mvn -Dmaven.repo.local=target/m2repo test
```

Expected result:

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The tests cover patient record retrieval, simulator file parsing, blood pressure alerts, blood saturation alerts, combined hypotensive hypoxemia, ECG peak alerts, triggered alert records, and the data-storage-backed alert evaluation path.

## Coverage Verification

Generate the JaCoCo coverage report with:

```sh
mvn -Dmaven.repo.local=target/m2repo package
```

The HTML report is generated at:

```text
target/site/jacoco/index.html
```

Latest coverage summary:

```text
Overall line coverage: 190/478 = 39.7%
com.alerts line coverage: 109/114 = 95.6%
com.data_management line coverage: 81/109 = 74.3%
```

The primary untested areas are long-running simulator scheduling and live TCP/WebSocket output behavior. Those parts require timing or network clients and are outside the Part 3 storage and alert requirements.

## UML Verification

The Part 3 implementation diagram is:

```text
uml_models/part3_patient_storage_alerts.png
```

It reflects the implemented file reader, data storage, patient records, alert generator, stored alerts, and main-class routing.
