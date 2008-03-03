Integration test.
The test creates two artifacts with a dependency to commons-collection.
artifact-fix requires version 3.2
artifact-range requires version (2.0,3.1.99]

artifact-combined has a dependency on the artifacts above.
In version 2.0.8 the test fails with a NPE