# MNG-8572 Custom Type Handler with Maven API DI

This integration test demonstrates how to create a Maven plugin with `extensions=true` that provides a custom artifact type handler using the Maven API DI system.

## Structure

- `extension/`: The Maven plugin with extensions=true that provides a custom artifact type handler
  - Uses `org.apache.maven.api.di.Named` annotation
  - Implements `org.apache.maven.api.spi.TypeProvider` interface
  - Provides a custom type with ID "custom-type"
  - Uses the Maven API DI system for component discovery
  - The annotation processor automatically generates the DI index

- `test/`: A test project that uses the custom type handler
  - References the plugin in the build section with `<extensions>true</extensions>`
  - Has a dependency with `type="custom-type"`
  - Verifies that the custom type handler is used

## Key Points

1. **Maven Plugin with extensions=true**:
   - The plugin is configured with `<extensions>true</extensions>`
   - This allows it to participate in the build process and provide custom components

2. **TypeProvider Implementation**:
   - Implements the `org.apache.maven.api.spi.TypeProvider` interface
   - Annotated with `@Named` from `org.apache.maven.api.di`
   - Returns a custom implementation of the `Type` interface

3. **Annotation Processing**:
   - The Maven API DI annotation processor automatically generates the index file
   - No need to manually create the `META-INF/maven/org.apache.maven.api.di.Inject` file

## Running the Test

1. Build and install the plugin:
   ```
   cd extension
   mvn install
   ```

2. Install the dummy artifact:
   ```
   cd test
   ./install-dummy.sh
   ```

3. Run the test project:
   ```
   cd test
   mvn validate
   ```

4. Verify that the custom type handler is used:
   ```
   [INFO] Using custom type handler for type: custom-type
   ```
