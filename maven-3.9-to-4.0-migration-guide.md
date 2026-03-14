# Maven 3.9 → 4.0 Migration Guide

**Analysis Date**: February 6, 2026
**Baseline**: `maven-3.9.12` (latest 3.9 release)
**Target**: `origin/maven-4.0.x` (planned 4.0.0 release)

---

## Executive Summary

Maven 4.0 represents a **major architectural overhaul** of Apache Maven, introducing:

- **New public API layer** (`org.apache.maven.api`)
- **Java 17 minimum requirement**
- **Complete rewrite of internal implementation**
- **Modernized dependency injection with Sisu**
- **SLF4J-based logging framework**
- **Performance improvements** (hard links, better caching)
- **New POM model features** (modular sources, profile conditions)
- **Maven upgrade tool** (`mvnup`)

| Metric | Value |
|--------|-------|
| Total commits | 5,293 |
| Files changed | ~10,787 |
| Lines added | +483,686 |
| Lines deleted | -100,895 |
| Net change | +382,791 lines |
| Development time | ~3+ years |

---

## 1. Platform Requirements

### 1.1 Java Version

| Version | Java Version | Notes |
|---------|--------------|-------|
| Maven 3.9 | Java 8+ | Minimum Java 8 |
| **Maven 4.0** | **Java 17+** | **Minimum Java 17** |

**Impact**: Projects and plugins built with Java < 17 will require:
- Update JDK to 17+ for building
- Possible plugin compatibility updates

### 1.2 Build Requirements

Maven 4.0 itself now requires:
- **Java 17** to build
- **Maven 3.6.3+** to bootstrap

```xml
<!-- Maven 4.0 pom.xml -->
<javaVersion>17</javaVersion>
<maven.compiler.source>${javaVersion}</maven.compiler.source>
<maven.compiler.target>${javaVersion}</maven.compiler.target>
<maven.compiler.release>${javaVersion}</maven.compiler.release>
```

---

## 2. Major Architectural Changes

### 2.1 New Public API Layer

**New Package Structure** in Maven 4.0:
```
api/
├── maven-api-annotations      # Custom annotations (@Experimental, @Nonnull, etc.)
├── maven-api-cli              # Command-line interface API
├── maven-api-core             # Core API (Artifact, Dependency, Source, etc.)
├── maven-api-di               # Dependency Injection API
├── maven-api-model            # Model API
├── maven-api-metadata        # Metadata API
├── maven-api-plugin          # Plugin API
├── maven-api-settings        # Settings API
├── maven-api-spi             # Service Provider Interface
├── maven-api-toolchain       # Toolchain API
└── maven-api-xml             # XML handling API
```

**Key API Classes**:
```java
// Core domain objects
org.apache.maven.api.Artifact
org.apache.maven.api.Dependency
org.apache.maven.api.ArtifactCoordinates
org.apache.maven.api.Service
org.apache.maven.api.Source
org.apache.maven.api.SourceRoot
org.apache.maven.api.Project

// Services (SPI)
org.apache.maven.api.services.ArtifactResolver
org.apache.maven.api.services.DependencyResolver
org.apache.maven.api.services.ModelBuilder
org.apache.maven.api.services.ProjectBuilder
org.apache.maven.api.services.ToolchainManager

// DI
org.apache.maven.api.di.Inject
org.apache.maven.api.di.Named
org.apache.maven.api.di.Singleton
org.apache.maven.api.di.Provider
```

**Impact**:
- Extensions/plugins encouraged to use new `api` layer
- Legacy `org.apache.maven` classes remain in `compat` layer
- New features only available in `api` layer

### 2.2 Compatibility Layer

**Purpose**: Provides backward compatibility for existing plugins and extensions.

**Structure**:
```
compat/
├── maven-artifact           # Legacy artifact handling
├── maven-builder-support    # Builder utilities
├── maven-compat            # Shim for Maven 3.x classes
├── maven-embedder          # Embed API
├── maven-model             # Model v3 compatibility
├── maven-model-builder     # Model builder compat
├── maven-plugin-api        # Plugin API compat
├── maven-repository-metadata
├── maven-resolver-provider
├── maven-settings
├── maven-settings-builder
├── maven-toolchain-builder
└── maven-toolchain-model
```

**Note**: `compat` layer will be removed in future major versions.

### 2.3 Implementation Layer

**New Implementation** (`impl/`):
- Modern, clean implementation
- Uses new `api` layer internally
- Sisu-based dependency injection
- SLF4J logging

```
impl/
├── maven-cli          # New CLI (CLIng)
├── maven-core         # Core operations
├── maven-di           # DI setup
├── maven-executor     # Plugin execution
├── maven-impl         # Service implementations
├── maven-jline        # JLine integration
└── maven-logging      # Logging setup
```

---

## 3. New Features

### 3.1 Maven Upgrade Tool (`mvnup`)

**Purpose**: Automated upgrade of Maven projects from version 3.x to 4.x.

**Location**: `impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/mvnup/`

**Features**:
- Detect and upgrade POM format
- Update plugin versions
- Fix dependency issues
- Apply Maven 4 best practices
- Generate backup of original POMs

**Usage**:
```bash
# Check what would be changed (dry-run)
mvn mvnup:help
mvn mvnup:upgrade

# Run upgrade
mvn mvnup:upgrade --dry-run  # Preview changes
mvn mvnup:upgrade           # Apply changes
```

**Related Issues**: MNG-8765, #11001, #7934-#7938

### 3.2 Modular Sources

**New POM Feature**: Support for JPMS-style modular project layouts.

**Example**:
```xml
<project>
  <sources>
    <module>org.example.myapp</module>
    <source>
      <module>org.example.myapp</module>
      <language>java</language>
      <scope>main</scope>
      <directory>src/${module}/${scope}/${language}</directory>
    </source>
  </sources>
</project>
```

**Automatically handles**:
- Module-aware source directories (`src/org.example.module/main/java`)
- Module-aware resource directories
- Duplicate detection with warnings
- Mixed modular/classic validation

**Related Issue**: #11505

### 3.3 Profile Activation with Conditions

**New Feature**: `<activation.condition>` element allows complex profile activation logic.

**Example**:
```xml
<profile>
  <activation>
    <condition>
      exists("${project.basedir}/src/main/java") &&
      System.getProperty("os.name").startsWith("Windows")
    </condition>
  </activation>
</profile>
```

**Features**:
- Boolean expressions
- Support for `${project.basedir}`
- Conditional operators (`&&`, `||`, `!`)
- Function calls (`exists()`, `missing()`, etc.)

**Related Issue**: #11528

### 3.4 Enhanced Cache Architecture

**New Features**:
1. **Hard Links**: Local repository uses hard links instead of copying files
2. **Configurable Reference Types**: `NONE`, `SOFT`, `WEAK`, `HARD`
3. **Cache Statistics**: Built-in tracking of hits/misses
4. **Per-entry reference types**

**Configuration**:
```properties
# Enable cache statistics
maven.cache.stats=true

# Configure reference types
maven.cache.keyValueRefs=SOFT
maven.cache.config=<?xml version="1.0"...>
```

**Related Issues**: #2506, #11354, #11222

### 3.5 SLF4J-based Logging

**Change**: Complete logging rewrite using SLF4J.

**Configuration**:
```properties
# Use new property names (not org.slf4j.simpleLogger.*)
maven.logger.level=INFO
maven.logger.io.level=DEBUG
maven.logger.log.prefix=/path/to/log.txt
```

**Features**:
- Flexible configuration
- Multiple logger implementations
- Better multi-threaded logging support
- JDK Platform Logging Integration (JEP 264)

**Related Issues**: MNG-8503, MNG-8421, #11684

### 3.6 Dependency Management Transitivity

**New Behavior**:
- Maven 4.0 **enables** transitivity for dependency management by default
- Maven 3.x ignored dependency management in transitive dependencies

**Configuration**:
```properties
# Disable transitivity (Maven 3 behavior)
maven.dependency.transitivity=false

# Enable transitivity (Maven 4 default, can be omitted)
maven.dependency.transitivity=true
```

**Impact**: Projects relying on Maven 3's transitive dependency management behavior may need adjustment.

### 3.7 Consumer POMs

**New Feature**: Ability to control consumer POM generation and flattening.

**Configuration**:
```properties
# Disable flattening by default
maven.consumer.pom.flatten=false

# Control whether to include consumer POMs in reactor
maven.consumer.pom.include=true
```

**Features**:
- Disabled by default (Maven 3 included them by default)
- Opt-in flattening
- Better control over what gets published

### 3.8 BOM Packaging

**New Feature**: Dedicated `bom` packaging type.

**Example**:
```xml
<project>
  <packaging>bom</packaging>
</project>
```

**Features**:
- Automatically transforms to `pom` in consumer POMs
- Preserves dependency versions in dependencyManagement
- Simplifies BOM project definitions

**Related Issue**: #11427

---

## 4. Breaking Changes

### 4.1 Java Version

❌ **Breaking**: Minimum Java 17 (up from Java 8)

**Migration**:
- Update build JDK to 17+
- Update CI/CD configurations
- Check plugin compatibility with Java 17

### 4.2 Removed/Deprecated Classes

**Removed** (from Maven 3):
- `org.apache.maven.model.mergeId` - deprecated in Maven 3.9
- Various legacy interfaces (MNG-8750)

**Deprecated** in Maven 4.0.0-rc-6:
- `InputLocation` constructors (use factory methods)
- `InputSource` constructors (use factory methods)

**Example**:
```java
// OLD (deprecated in Maven 4.0.0-rc-6)
InputLocation loc = new InputLocation(1, 1, source);

// NEW (recommended)
InputLocation loc = InputLocation.of(1, 1, source);
```

### 4.3 Logging Configuration

❌ **Breaking**: Property names changed.

```properties
# Maven 3 (no longer works)
org.slf4j.simpleLogger.log.org.apache.maven=INFO

# Maven 4.0 (new syntax)
maven.logger.level=INFO
maven.logger.io.level=DEBUG
```

### 4.4 Dependency Management Transitivity

⚠️ **Behavior Change**: Dependency management now works transitively.

```properties
# To get Maven 3 behavior in Maven 4.0:
maven.dependency.transitivity=false
```

### 4.5 Consumer POMs

⚠️ **Behavior Change**: Consumer POMs not included by default.

```properties
# To get Maven 3 behavior (include consumer POMs):
maven.consumer.pom.include=true

# To enable flattening (Maven 3 behavior):
maven.consumer.pom.flatten=true
```

---

## 5. New Configuration Properties

Maven 4.0 introduces many new configuration properties:

### 5.1 System Properties

| Property | Default | Description |
|----------|---------|-------------|
| `maven.dependency.transitivity` | `true` | Enable dependency management transitivity |
| `maven.consumer.pom.flatten` | `false` | Flatten consumer POMs |
| `maven.consumer.pom.include` | `true` | Include consumer POMs |
| `maven.deploy.buildPom` | `true` | Deploy build POM alongside consumer POM |
| `maven.builder.maxProblems` | `100` | Maximum problems to collect |

### 5.2 Logging Properties

| Property | Default | Description |
|----------|---------|-------------|
| `maven.logger.level` | `INFO` | Root logger level |
| `maven.logger.io.level` | `DEBUG` | I/O logger level |
| `maven.logger.log.prefix` | - | Log output prefix |
| `maven.logger.stdout.level` | `INFO` | Stdout logger level |
| `maven.logger.stderr.level` | `ERROR` | Stderr logger level |

### 5.3 Cache Properties

| Property | Default | Description |
|----------|---------|-------------|
| `maven.cache.stats` | `false` | Display cache statistics at build end |
| `maven.cache.config` | - | Cache configuration file |
| `maven.cache.keyValueRefs` | `SOFT` | Key reference type |
| `maven.cache.cacheValueRefs` | `SOFT` | Value reference type |

### 5.4 Resolver Properties

| Property | Default | Description |
|----------|---------|-------------|
| `maven.resolver.transport` | `default` | Transport provider |
| `maven.resolver.localRepository` | `${user.home}/.m2/repository` | Local repo location |

---

## 6. Compatibility Considerations

### 6.1 Maven 3.9 Plugins

**Good News**: Most Maven 3.9 plugins should work with Maven 4.0 due to:
- `compat` layer providing legacy APIs
- Maven 3.9 toolchain support explicitly registered

**Potential Issues**:
- Plugins using removed internal classes
- Plugins with specific Maven 3 assumptions
- Plugins with broken dependency management usage

### 6.2 Maven 4.0-Ready Plugins

Plugins updated for Maven 4.0 will:
- Use `api` layer classes
- Support Java 17
- Use SLF4J-based logging
- Leverage new dependency transitivity

### 6.3 Extension Compatibility

**Maven 3.X Extensions**:
- Continue to work via `compat` layer
- Benefit from Maven 4 features automatically

**Maven 4.0 Extensions**:
- Should use new `maven-api` classes
- Can use Sisu annotations (`@Inject`, `@Named`)
- Can access new features (modular sources, etc.)

---

## 7. Performance Improvements

### 7.1 Hard Links in Local Repository

**Improvement**: Project local repository uses hard links instead of copying.

**Benefits**:
- Faster multi-module builds
- Reduced disk I/O
- Less disk space usage
- Graceful fallback to copy if unsupported

**Impact Speed**: Especially noticeable in large multi-module builds.

### 7.2 Enhanced Caching

**Improvement**: Configurable cache with reference types.

**Benefits**:
- Better memory management
- Optional statistics for tuning
- Configurable retention policies (PERSISTENT, SESSION_SCOPED, REQUEST_SCOPED)

### 7.3 Dependency Resolution Performance

**Improvement**: Maven Resolver 2.0.14 (vs 1.9.x in Maven 3.9).

| Version | Resolver | Notes |
|---------|----------|-------|
| Maven 3.9 | Resolver 1.9.x | - |
| Maven 4.0 | Resolver 2.0.14 | New API, improved performance |

---

## 8. Directory Structure Changes

### 8.1 Source Layout Changes

```
Maven 3.9                                  Maven 4.0
├── maven-model               ──────────────┬──> compat/maven-model (compat)
├── maven-core                ──────────────┼──> impl/maven-core (impl)
├── maven-plugins             ──────────────┤
├── maven-settings            ──────────────┼──> api/maven-api-settings (NEW)
├── maven-embedder            ──────────────┼──> compat/maven-embedder (compat)
└── ...                        ──────────────┘

                                        ┌──> api/ (NEW - Public API)
                                        ├──> compat/ (NEW - Compatibility)
                                        └──> impl/ (NEW - Implementation)
```

### 8.2 New Modules in Maven 4.0

**API Modules**:
- `maven-api-annotations`
- `maven-api-cli`
- `maven-api-core`
- `maven-api-di`
- `maven-api-model`
- `maven-api-metadata`
- `maven-api-plugin`
- `maven-api-settings`
- `maven-api-spi`
- `maven-api-toolchain`
- `maven-api-xml`

**Impl Modules**:
- `maven-cli` (NEW CLIng)
- `maven-core` (refactored)
- `maven-di`
- `maven-executor`
- `maven-impl`
- `maven-jline`
- `maven-logging`

**Compat Modules** (Maven 3.9 compatibility):
- `maven-artifact`
- `maven-builder-support`
- `maven-compat`
- `maven-embedder`
- `maven-model`
- `maven-model-builder`
- `maven-plugin-api`
- `maven-repository-metadata`
- `maven-resolver-provider`
- `maven-settings`
- `maven-settings-builder`
- `maven-toolchain-builder`
- `maven-toolchain-model`

---

## 9. Migration Guide

### 9.1 Pre-Migration Checklist

- [ ] Ensure Java 17+ available
- [ ] Review build configuration for removed features
- [ ] Test critical plugins with Maven 4.0 RC
- [ ] Review dependency management usage
- [ ] Check custom logging configuration
- [ ] Test with sample project

### 9.2 Step-by-Step Migration

**Step 1: Test with Maven 4.0 RC**

```bash
# Download Maven 4.0.0-rc-5+
curl -O https://downloads.apache.org/maven/maven-4/4.0.0-rc-5/binaries/apache-maven-4.0.0-rc-5-bin.tar.gz
tar -xzf apache-maven-4.0.0-rc-5-bin.tar.gz
export PATH="$PWD/apache-maven-4.0.0-rc-5/bin:$PATH"

# Test with your project
mvn clean verify
```

**Step 2: Fix Java Version Issues**

```xml
<!-- Update pom.xml if needed -->
<properties>
  <maven.compiler.source>17</maven.compiler.source>
  <maven.compiler.target>17</maven.compiler.target>
</properties>
```

**Step 3: Update Logging Configuration**

Convert from:
```properties
org.slf4j.simpleLogger.log.org.apache.maven.cli=INFO
```

To:
```properties
maven.logger.level=INFO
```

**Step 4: Review Dependency Management**

If you relied on non-transitive dependency management:

```properties
# Add to Maven configuration
maven.dependency.transitivity=false
```

**Step 5: Run Maven Upgrade Tool**

```bash
# Preview changes
mvn mvnup:upgrade --dry-run

# Apply changes
mvn mvnup:upgrade
```

**Step 6: Fix Deprecation Warnings**

Update deprecated API usage:
```java
// Before
InputLocation loc = new InputLocation(1, 1, source);

// After
InputLocation loc = InputLocation.of(1, 1, source);
```

**Step 7: Test Thoroughly**

```bash
# Full build test
mvn clean install -Prun-its

# Verify generated artifacts
mvn verify
```

**Step 8: Update CI/CD**

- Update Maven version in CI config
- Update Java version to 17 or 21
- Update any hardcoded paths
- Test in CI environment

**Step 9: Update Team Documentation**

- Update build instructions
- Document new features being used
- Update dependency management docs
- Note any Maven 4-specific configurations

**Step 10: Rollout**

- Update local development environments
- Update CI/CD pipelines
- Update deployment scripts
- Monitor for issues

---

## 10. Common Issues and Solutions

### 10.1 Java Version Errors

**Error**: `Unsupported class file major version 61 (Java 17)`

**Solution**: Ensure you're using Java 17 or higher:
```bash
java -version  # Should show 17+
```

### 10.2 Plugin Not Found

**Error**: Plugin fails with `NoSuchMethodError` or `ClassNotFoundException`

**Solution**:
1. Check plugin version compatibility
2. Update to latest plugin version
3. Report issue if needed

### 10.3 Logging Configuration Ignored

**Issue**: Old property names don't work

**Solution**: Convert to new property names:
```properties
# Remove old properties
# org.slf4j.simpleLogger.log.org.apache.maven=INFO

# Add new properties
maven.logger.level=INFO
```

### 10.4 Dependency Management Differences

**Issue**: Different dependency resolution or transitive dependencies

**Solution**:
1. Review dependency management usage
2. If Maven 3 behavior needed: `maven.dependency.transitivity=false`
3. Adjust dependency scopes if needed

### 10.5 Consumer POM Issues

**Issue**: Missing or different consumer POMs

**Solution**:
```properties
# To include consumer POMs
maven.consumer.pom.include=true

# To flatten consumer POMs
maven.consumer.pom.flatten=true
```

---

## 11. Major Features Deep Dive

### 11.1 Dependency Injection with Sisu

**Maven 3.9**: Plexus-based DI
**Maven 4.0**: Sisu-based DI (JSR-330 compliant)

**Example**:
```java
// Maven 4.0 Extension
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.maven.api.di.Named;  // Use Maven's annotations

@Singleton
@Named("myService")
public class MyService {

    @Inject
    public MyService(ArtifactResolver resolver) {
        this.resolver = resolver;
    }
}
```

### 11.2 New Model API

**Key Classes**:

```java
// Artifact
Artifact artifact = Artifact.builder()
    .groupId("org.example")
    .artifactId("myapp")
    .version("1.0.0")
    .type("jar")
    .build();

// Dependency
Dependency dependency = Dependency.builder()
    .artifact(artifact)
    .scope(Scope.COMPILE)
    .build();

// SourceRoot
SourceRoot sourceRoot = SourceRoot.builder()
    .language(Language.JAVA)
    .scope(ProjectScope.MAIN)
    .directory(Paths.get("src/main/java"))
    .build();
```

### 11.3 Service API

**Using Services**:

```java
// In extension or plugin
@Inject
 private ArtifactResolver artifactResolver;

public void myMethod() {
    ArtifactResolverRequest request = ArtifactResolverRequest.builder()
        .artifact(Artifact.builder()
            .groupId("org.example")
            .artifactId("myapp")
            .version("1.0.0")
            .build())
        .repositories(repositories)
        .build();

    ArtifactResolverResult result = artifactResolver.resolve(request);
}
```

---

## 12. Testing for Maven 4.0 Compatibility

### 12.1 Integration Test Tips

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-verifier-plugin</artifactId>
  <version>...</version>
  <configuration>
    <!-- Test with both Maven 3.9 and 4.0 -->
    <mavenVersion>[3.9.12,4.0.0)</mavenVersion>
  </configuration>
</plugin>
```

### 12.2 Checklist for Plugin Developers

- [ ] Update to Java 17
- [ ] Test with Maven 4.0 RC
- [ ] Check for deprecated API usage
- [ ] Update dependency injection (if using Plexus directly)
- [ ] Support new logging configuration
- [ ] Handle dependency management changes
- [ ] Add CI tests for both Maven 3.9 and 4.0
- [ ] Update documentation

---

## 13. Release History

### 13.1 Maven 4.0 Roadmap

| Milestone | Date | Notes |
|-----------|------|-------|
| Alpha 1-13 | 2023-2024 | Early development |
| Beta 2-5 | 2024 | Feature complete |
| RC-1 | 2025-10 | First release candidate |
| RC-2 | 2025-10 | Bug fixes |
| RC-3 | 2025-10 | Additional fixes |
| RC-4 | 2025-10 | Logging fixes |
| RC-5 | 2025-11-07 | Stability release |
| **RC-6 (planned)** | **2026-Q1** | **Final release candidate** |
| **4.0.0 (final)** | **TBD** | **Pending RC-6 validation** |

### 13.2 Key Milestones in Development

- ~2022: Maven 4 development started
- 2022-2023: API layer design
- 2023-2024: Implementation rewrite
- 2024: Feature freeze
- 2025: Release candidates
- 2026: Final release

---

## 14. Resources

### 14.1 Documentation

- [Maven 4.0 Documentation](https://maven.apache.org/ref/4.0.0/)
- [Migration Guide](https://maven.apache.org/docs/4.0.0/release-notes.html)
- [API Javadoc](https://maven.apache.org/ref/4.0.0/apidocs/)

### 14.2 Source Code

- Apache Maven Repository: https://github.com/apache/maven
- Maven 4.0.x branch: https://github.com/apache/maven/tree/maven-4.0.x
- Maven 3.9.x branch: https://github.com/apache/maven/tree/maven-3.9.x

### 14.3 Issue Tracking

- Issue Tracker: https://github.com/apache/maven/issues
- Mailing Lists: https://maven.apache.org/mailing-lists.html

---

## 15. Quick Reference

### 15.1 Version Comparison

| Feature | Maven 3.9 | Maven 4.0 |
|---------|-----------|----------|
| Java | 8+ | 17+ |
| API | org.apache.maven | org.apache.maven.api (new) |
| Logging | SLF4J with simple config | Enhanced SLF4J API |
| DI | Plexus | Sisu (JSR-330) |
| Modular Sources | ❌ | ✅ |
| Profile Conditions | ❌ | ✅ |
| mvnup tool | ❌ | ✅ |
| Hard links | ❌ | ✅ |
| BOM packaging | ❌ | ✅ |
| Dependency Mgmt Transitivity | ❌ | ✅ |
| Cache Reference Types | Limited | Configurable (SOFT/WEAK/HARD) |
| Consumer POM Flattening | ✅ default | ⚠️ opt-in |

### 15.2 Property Migration

| Maven 3.9 Property | Maven 4.0 Property | Notes |
|--------------------|--------------------|-------|
| `org.slf4j.simpleLogger.*` | `maven.logger.*` | Name prefix changed |
| N/A | `maven.dependency.transitivity` | New feature |
| N/A | `maven.consumer.pom.*` | New feature |
| N/A | `maven.cache.*` | New feature |

### 15.3 Package Migration

| Maven 3.9 | Maven 4.0 | Status |
|-----------|----------|--------|
| `org.apache.maven.*` | Same | ✅ compat layer |
| `org.apache.maven.api.*` | New | ✅ added |
| `org.apache.maven.cli.*` | `org.apache.maven.cling.*` | ⚠️ changed |
| SLF4J simple config | Enhanced SLF4J | ⚠️ config changes |

---

## 16. Summary

Maven 4.0 represents a **major evolution** of Apache Maven with:

### Advantages
- ✅ Modern, clean architecture
- ✅ Public API for extensions
- ✅ Java 17 support for modern features
- ✅ Performance improvements (hard links, caching)
- ✅ New features (modular sources, profile conditions)
- ✅ Automated upgrade tool (`mvnup`)
- ✅ SLF4J integration
- ✅ Better dependency management

### Considerations
- ⚠️ Java 17 minimum requirement
- ⚠️ Some behavior changes (dependency transitivity, consumer POMs)
- ⚠️ New property names for configuration
- ⚠️ Some deprecated APIs

### Recommendations
1. **Test first** with Maven 4.0 RC
2. **Use mvnup** for automated project upgrades
3. **Review** dependency management usage
4. **Update** logging configuration
5. **Plan** for Java 17 upgrade
6. **Monitor** for plugin compatibility issues

---

**Generated on**: February 6, 2026
**Baseline Maven**: 3.9.12
**Target Maven**: 4.0.0-SNAPSHOT (maven-4.0.x branch)
**Total Analysis**: ~5,293 commits, 10,787 changed files, +483K/-100K lines