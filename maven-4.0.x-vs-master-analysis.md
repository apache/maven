# Maven 4.0.x vs Master Branch Analysis

**Version Numbers:**
- **origin/maven-4.0.x**: `4.0.0-SNAPSHOT` (intended as final Maven 4.0 release)
- **origin/master**: `4.1.0-SNAPSHOT` (development toward Maven 4.1.0/4.2.0)

---

## Overview Statistics

| Metric | Value |
|--------|-------|
| Total files changed | 3,146 |
| Total additions | 18,755 lines |
| Total deletions | 16,216 lines |
| Files modified | 2,920 |
| Files added | 86 |
| Files deleted | 43 |
| Files renamed | ~68 |
| Commits unique to master | 329 |
| Commits unique to maven-4.0.x | 250 |

---

## 1. API Changes (`org.apache.maven.api`)

### Breaking Changes

| File | Change | Impact |
|------|--------|--------|
| `api/maven-api-model/src/main/java/org/apache/maven/api/model/InputLocation.java` | **DELETED** | Replaced by `InputLocation` in `compat/maven-model` package |
| `api/maven-api-model/src/main/java/org/apache/maven/api/model/InputLocationTracker.java` | **DELETED** | Replaced by compat layer |
| `api/maven-api-model/src/main/java/org/apache/maven/api/model/InputSource.java` | **DELETED** | Replaced by compat layer |

**Note**: These classes moved to `compat/maven-model` package as they're now considered legacy/compatibility APIs, not part of the experimental 4.0 API.

### New API Features

| Class | New Methods/Features | Since |
|-------|---------------------|-------|
| `ModelSource` | `String getModelId()` | 4.0.0 |
| `ProblemCollector` | `create(int, Predicate<P>)` with filtering | 4.0.0 |
| `Sources.resolvedSource()` | Parameter renamed `location` → `modelId` | 4.0.0 |
| `ModelObjectProcessor` | **NEW FILE** in api/maven-api-model | 4.1.0 |

### Significant API Modifications

**`Constants.java` (api/maven-api-core)**:
- Added `MAVEN_DEPLOY_BUILD_POM` property (4.1.0)
- Added cache configuration APIs (4.1.0):
  - `MAVEN_CACHE_CONFIG_PROPERTY`
  - `MAVEN_CACHE_STATS`
  - `MAVEN_CACHE_KEY_REFS`
  - `MAVEN_CACHE_VALUE_REFS`
- Documentation clarifications (Maven 2 vs 3 references)

**Exception Classes** (all in `api/maven-api-core/services`):
- Removed `@Serial` fields and imports from ALL service exception classes:
  - `ArtifactDeployerException`
  - `ArtifactInstallerException`
  - `ArtifactResolverException`
  - `ChecksumAlgorithmServiceException`
  - `DependencyResolverException`
  - `InterpolatorException`
  - `LookupException`
  - `MavenException`
  - `ModelBuilderException`
  - `ProjectBuilderException`
  - `PrompterException`
  - `SettingsBuilderException`
  - `SuperPomProviderException`
  - `ToolchainManagerException`
  - `ToolchainsBuilderException`
  - `TransportProviderException`
  - `VersionParserException`
  - `VersionRangeResolverException`
  - `VersionResolverException`

---

## 2. Implementation Changes (`impl/`)

### 2.1 Cache Architecture Overhaul

**New Files Added** (`impl/maven-impl/src/main/java/org/apache/maven/impl/cache/`):
- `Cache.java` - New cache interface
- `CacheConfig.java` - Configuration with key/value reference types
- `CacheConfigurationResolver.java` - Settings-based configuration
- `CacheSelector.java` - Cache implementation selector
- `CacheSelectorParser.java` - Configuration parser
- `CacheStatistics.java` - Statistics tracking
- `PartialCacheConfig.java` - Partial configuration support

**Files Deleted**:
- `SoftIdentityMap.java` - Replaced by new architecture

**Key Improvements**:
- Multiple reference types: `NONE`, `SOFT`, `WEAK`, `HARD`
- Separate configuration for keys and values
- Comprehensive statistics (evictions by policy, hit/miss ratios)
- Shutdown hook to display cache statistics
- Better memory management with `ReferenceQueue` cleanup

Related commits:
- `731700abc6` - Improve Maven cache architecture (#2506)
- `304791ec1c` - Consolidate caches (#11354)
- `9bc69624fc` - Rename cache keys (#11222)

### 2.2 Project Builder Enhancements

**New Files** (`impl/maven-core/src/main/java/org/apache/maven/project/`):
- `SourceHandlingContext.java` - Modular source tracking
- `SmartProjectComparator.java` - Critical path-based project ordering

**Major Features**:

1. **Modular Project Support** (SourceHandlingContext):
   - Automatic detection of modular projects from `<sources>` configuration
   - Duplicate source validation with warnings
   - Mixed modular/classic source detection (ERROR)
   - Module-aware resource injection for modular projects
   - Legacy directory handling with appropriate warnings

2. **Smart Build Ordering** (SmartProjectComparator):
   - Critical path analysis for parallel builds
   - Project weight calculation: `weight = 1 + max(downstream_weights)`
   - Improves parallel build efficiency (similar to Takari Smart Builder)

3. **Enhanced Error Reporting**:
   - `ProjectBuildingException` now provides detailed problem summaries
   - Per-project problem listings with source locations
   - Fixed `"[unknown project]"` messages

Related commits:
- `74ef127617` - Add module-aware resource handling (#11505)
- `f97bac3111` - Unify source tracking with SourceHandlingContext (#11632)
- `405e2e10fd` - Fix profile source tracking (#11440)

### 2.3 CLI/mvnup Changes

**XML Library Migration**: JDOM2 → DomTrip

**Deleted Files** (`impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/mvnup/goals/`):
- `JDomUtils.java` (544 lines) - Replaced with DomUtils
- `GAVUtils.java` - Consolidated
- `UpgradeConstants.java` - Constants moved to library

**New Files**:
- `DomUtils.java` - DomTrip-based XML utilities

**Key Changes**:
- Pattern changes:
  - `.getChild(name)` → `.child(name)` (returns `Optional`)
  - `.getAttribute(name)` → `.attributeObject(name)`
  - `.getRootElement()` → `.root()`
- Constants now from `eu.maveniverse.domtrip.maven.MavenPomElements`
- Maven 4.2.0 model support for upgrade tool

Related commits:
- `a336a2c579` - Switch mvnup to domtrip (#11432)

---

## 3. Model Version Updates

| Version | POM Template | Location |
|---------|--------------|----------|
| 4.1.0 | `pom-4.1.0.xml` | Modified - adds xmlns namespace, modelVersion 4.1.0 |
| 4.2.0 | `pom-4.2.0.xml` | **NEW** - added template |

**Schema Changes**:
- All versions (4.0.0+) use the same stable namespace: `http://maven.apache.org/POM/4.0.0`
- Version is specified in the `<modelVersion>` element
- XSD validation uses version-specific schema locations (e.g., `https://maven.apache.org/xsd/maven-4.1.0.xsd`)

---

## 4. Dependency Changes (pom.xml)

### New Dependencies:
```xml
<eu.maveniverse.maven.domtrip>
  <domtrip.core.version>0.4.0</domtrip.core.version>
  <domtrip.maven.version>0.4.0</domtrip.maven.version>
</eu.maveniverse.maven.domtrip>
```

### Version Upgrades:
- `junit` 5.13.4 → 6.0.2
- `slf4j` integration added (SLF4J JDK Platform Logging)

### Removed Dependencies:
- `org.jdom:jdom2` - Replaced by DomTrip
- `org.hamcrest:hamcrest` - Removed from dependencyManagement
- `org.assertj:assertj-core` - Removed from dependencyManagement
- `org.xmlunit:xmlunit-assertj` - Removed

### Build Changes:
- Removed `maven-antrun-plugin` pre-site workaround
- Output timestamps updated
- Parent version changes tracked

---

## 5. Functional Changes

### New Features:

1. **Cache Statistics**:
   - Display cache statistics at build end via `maven.cache.stats=true`
   - Track hits/misses by reference type
   - Track evictions by retention policy

2. **Source Handling**:
   - Modular project detection
   - Automatic resource injection for modular sources
   - Duplicate source warnings
   - Mixed modular/classic source errors

3. **Build Optimization**:
   - Smart project comparator for parallel builds
   - Critical path-based project ordering

4. **Logging**:
   - SLF4J JDK Platform Logging Integration (JEP 264 support)

5. **Build POM Deployment**:
   - `maven.deploy.buildPom` property to control build POM deployment

### Bug Fixes:

- Profile source tracking in multi-module projects (#11440)
- Multiple SLF4J providers warning in tests (#11480)
- ConcurrentModificationException fix (#11428)
- NullPointerException when clearing project properties
- FileSelector.matches(Path) issues for files/directories (#11551)
- Special characters in `.mvn/jvm.config` (fix #11363, #11485, #11486)

### Documentation:

- README.md updates:
  - Branch badges updated for 4.1.0, 4.0.x, and 3.9.x
  - Distribution target path updated to 4.1.x-SNAPSHOT
  - Jenkins and GitHub Actions badges for all branches

- `.asf.yaml`: Added protected branches configuration

- Added `cache-configuration.md` documentation

---

## 6. Compatibility Layer Changes (`compat/`)

| Package | Key Changes |
|---------|-------------|
| `maven-builder-support` | `DefaultProblem` updates, new factory test |
| `maven-compat` | Removed `maven-parent` dependency references, various test updates |
| `maven-model` | Added `InputLocation`, `InputSource` (moved from api layer) |
| `maven-model-builder` | Added `pom-4.2.0.xml`, updated `pom-4.1.0.xml` |
| `maven-model-builder` | Default settings validator updates |

---

## 7. Test Infrastructure

### New Test Projects (`impl/maven-core/src/test/projects/project-builder/`):
- `duplicate-enabled-sources/pom.xml` - Tests duplicate source warnings
- `mixed-sources/pom.xml` - Tests mixed modular/classic handling
- `modular-sources/pom.xml` - Tests modular source auto-detection
- `modular-sources-with-explicit-resources/pom.xml` - Tests resource injection
- `multiple-directories-same-module/pom.xml` - Tests module validation
- `sources-mixed-modules/pom.xml` - Tests multiple module sources

### New Test Files:
- Java 17+ features tests (Stream.toList usage)
- Cache configuration tests
- Reference types tests
- SmartProjectComparator tests

---

## 8. Risk Assessment for Renaming Strategy

**Plan: Rename master → 4.x, maven-4.0.x → master**

| Risk Area | Details | Mitigation |
|-----------|---------|------------|
| **API Stability** | Model classes moved from `api` to `compat` | API users should have been using compat layer since 4.0.0 RC |
| **Breaking Changes** | `SoftIdentityMap` removed in master | No breaking if 4.0.x released as is |
| **Version Difference** | 4.0.x → 4.1.0-rc1 (or 4.1.0) | Clear version bump signals new features |
| **Cache Architecture** | Complete redesign | Internal change, mostly transparent to users |
| **mvnup Tool** | XML library change (JDOM2→DomTrip) | Only affects upgrade tool users |
| **Model Versions** | 4.1.0 and 4.2.0 added | POM validation may need updates |

**Recommendation**: The rename strategy is viable because:
1. Master has significant new features (4.1.0) that shouldn't block 4.0.0 release
2. All changes are additive or internal refactoring
3. API breaking changes are documented via version bump
4. Test coverage is comprehensive

---

## 9. Key Commits Summary

### Master → 4.0.x (329 commits):
- Cache architecture redesign (#2506, #11354, #11222)
- Modular sources support (#11505, #11632)
- Smart build ordering (#11632)
- SLF4J JDK integration (#11684)
- DomTrip migration (#11432)
- Model 4.2.0 support

### 4.0.x → Master (250 commits):
- Dependency updates (logback, pmd, byte-buddy)
- Maven parent 46/47 updates
- Hard links for local repository (#11550)
- Mimir mirror features (both branches)

---

## 10. Summary Diagram

```
maven-4.0.x (4.0.0-SNAPSHOT)
├── 4.0.0 release candidates stabilized
├── 250 unique commits (mostly dependency updates, backported features)
├── POM model 4.0.0 support
├── Cache: SoftIdentityMap based
├── mvnup: JDOM2 based
└── Goal: Final Maven 4.0 release

         ↓ (diverged from X where master is now)

master (4.1.0-SNAPSHOT)
├── 329 unique commits (new features)
├── POM model 4.1.0 and 4.2.0 support
├── Cache: New architecture with reference types, statistics
├── Modularity: SourceHandlingContext, SmartProjectComparator
├── mvnup: DomTrip based
├── Dependencies: DomTrip added, JDOM2 removed
├── New features:
│   - Modular source handling
│   - Module-aware resources
│   - Smart build ordering
│   - Cache statistics
│   - SLF4J JDK integration
│   - Deploy build POM control
└── Goal: Maven 4.1.0/4.2.0 development
```

---

## Appendix: Full File Change List

### Deleted API Files:
```
D	api/maven-api-model/src/main/java/org/apache/maven/api/model/InputLocation.java
D	api/maven-api-model/src/main/java/org/apache/maven/api/model/InputLocationTracker.java
D	api/maven-api-model/src/main/java/org/apache/maven/api/model/InputSource.java
```

### Added API Files:
```
A	api/maven-api-core/src/test/java/org/apache/maven/api/JavaPathTypeTest.java
A	api/maven-api-core/src/test/java/org/apache/maven/api/feature/FeaturesTest.java
A	api/maven-api-core/src/test/java/org/apache/maven/api/services/ModelSourceTest.java
A	api/maven-api-model/src/main/java/org/apache/maven/api/model/ModelObjectProcessor.java
```

### Added Cache Files:
```
A	impl/maven-impl/src/main/java/org/apache/maven/impl/cache/Cache.java
A	impl/maven-impl/src/main/java/org/apache/maven/impl/cache/CacheConfig.java
A	impl/maven-impl/src/main/java/org/apache/maven/impl/cache/CacheConfigurationResolver.java
A	impl/maven-impl/src/main/java/org/apache/maven/impl/cache/CacheSelector.java
A	impl/maven-impl/src/main/java/org/apache/maven/impl/cache/CacheSelectorParser.java
A	impl/maven-impl/src/main/java/org/apache/maven/impl/cache/CacheStatistics.java
A	impl/maven-impl/src/main/java/org/apache/maven/impl/cache/PartialCacheConfig.java
```

### Deleted Implementation Files:
```
D	impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/mvnup/goals/GAVUtils.java
D	impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/mvnup/goals/JDomUtils.java
D	impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/mvnup/goals/UpgradeConstants.java
D	impl/maven-cli/src/test/java/org/apache/maven/cling/invoker/mvnup/goals/GAVTest.java
D	impl/maven-cli/src/test/java/org/apache/maven/cling/invoker/mvnup/goals/JDomUtilsTest.java
D	impl/maven-core/src/test/java/org/apache/maven/project/ProjectBuildingResultWithLocationMatcher.java
D	impl/maven-core/src/test/java/org/apache/maven/project/ProjectBuildingResultWithProblemMessageMatcher.java
D	impl/maven-impl/src/main/java/org/apache/maven/impl/cache/SoftIdentityMap.java
D	impl/maven-impl/src/main/java/org/apache/maven/impl/model/FileToRawModelMerger.java
D	impl/maven-impl/src/main/java/org/apache/maven/impl/resolver/ArtifactDescriptorReaderDelegate.java
```

### Added Implementation Files:
```
A	impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/mvnup/goals/DomUtils.java
A	impl/maven-cli/src/test/java/org/apache/maven/cling/invoker/mvnup/goals/DomUtilsTest.java
A	impl/maven-core/src/main/java/org/apache/maven/lifecycle/internal/builder/multithreaded/SmartProjectComparator.java
A	impl/maven-core/src/main/java/org/apache/maven/project/SourceHandlingContext.java
A	src/site/markdown/cache-configuration.md
```

---

**Generated on**: February 6, 2026
**Git branches analyzed**: origin/maven-4.0.x vs origin/master