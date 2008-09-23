This verifies that -Dversion=2 overrides the normal result of the ${version} expression in the pom, which should otherwise resolve to the POM's own version (1). The test build runs twice:

1. run: `mvn clean initialize` and verify that target/touch-1.txt exists.
2. run: `mvn -Dversion=2 clean initialize` and verify that target/touch-2.txt exists.
