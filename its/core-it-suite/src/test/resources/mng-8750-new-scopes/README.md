# Maven 4 New Scopes Integration Test (MNG-8750)

This integration test verifies the correct behavior of the new dependency scopes introduced in Maven 4:

## New Scopes

### 1. `compile-only`
- **Purpose**: Dependencies needed only during compilation, not at runtime
- **Behavior**: 
  - Available in compile classpath
  - NOT available in runtime classpath
  - NOT transitive
- **Use Case**: Annotation processors, code generation tools, compile-time-only libraries

### 2. `test-only` 
- **Purpose**: Dependencies needed only during test compilation, not at test runtime
- **Behavior**:
  - Available in test compile classpath
  - NOT available in test runtime classpath
  - NOT transitive
- **Use Case**: Test annotation processors, test code generation tools

### 3. `test-runtime`
- **Purpose**: Dependencies needed only during test runtime, not during test compilation
- **Behavior**:
  - NOT available in test compile classpath
  - Available in test runtime classpath
  - Transitive
- **Use Case**: Test runtime libraries, database drivers for integration tests

## Test Structure

### Test Modules

1. **compile-only-test**: Tests `compile-only` scope behavior
2. **test-only-test**: Tests `test-only` scope behavior  
3. **test-runtime-test**: Tests `test-runtime` scope behavior
4. **consumer-pom-test**: Tests consumer POM generation excludes new scopes
5. **comprehensive-test**: Tests all new scopes working together

### Test Dependencies

Located in `repo/org/apache/maven/its/mng8750/`:

- `compile-only-dep-1.0.jar`: Test dependency for compile-only scope
- `test-only-dep-1.0.jar`: Test dependency for test-only scope
- `test-runtime-dep-1.0.jar`: Test dependency for test-runtime scope
- `compile-dep-1.0.jar`: Regular compile dependency for comparison
- `test-dep-1.0.jar`: Regular test dependency for comparison

## Verification Points

### Classpath Inclusion
- ✅ `compile-only` dependencies appear in compile classpath
- ❌ `compile-only` dependencies do NOT appear in runtime classpath
- ✅ `test-only` dependencies appear in test compile classpath
- ❌ `test-only` dependencies do NOT appear in test runtime classpath
- ❌ `test-runtime` dependencies do NOT appear in test compile classpath
- ✅ `test-runtime` dependencies appear in test runtime classpath

### Consumer POM Compatibility
- ❌ New scopes (`compile-only`, `test-only`, `test-runtime`) do NOT appear in consumer POMs
- ✅ Only Maven 3 compatible scopes (`compile`, `provided`, `runtime`, `test`, `system`) appear in consumer POMs
- ✅ Dependencies with new scopes are either excluded or transformed to compatible scopes

### Runtime Behavior
- `compile-only` dependencies cause `NoClassDefFoundError` at runtime
- `test-only` dependencies cause `NoClassDefFoundError` at test runtime
- `test-runtime` dependencies are accessible via reflection at test runtime

## Running the Test

```bash
# Run the integration test
mvn test -Dtest=MavenITmng8750NewScopesTest

# Run individual test methods
mvn test -Dtest=MavenITmng8750NewScopesTest#testCompileOnlyScope
mvn test -Dtest=MavenITmng8750NewScopesTest#testTestOnlyScope
mvn test -Dtest=MavenITmng8750NewScopesTest#testTestRuntimeScope
mvn test -Dtest=MavenITmng8750NewScopesTest#testConsumerPomExcludesNewScopes
mvn test -Dtest=MavenITmng8750NewScopesTest#testAllNewScopesTogether
```

## Expected Outcomes

All tests should pass, demonstrating that:

1. New scopes work as designed for classpath inclusion/exclusion
2. Consumer POMs maintain Maven 3 compatibility by excluding new scopes
3. All new scopes can work together in the same project
4. Existing Maven 3 scopes continue to work as expected

## Maven Version Requirement

This test requires Maven 4.0.0 or later, as specified in the test class:

```java
public MavenITmng8750NewScopesTest() {
    super("[4.0.0,)");
}
```
