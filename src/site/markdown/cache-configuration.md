<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
# Maven Cache Configuration Enhancement

This document describes the enhanced cache configuration functionality in Maven's DefaultRequestCache.

## Overview

The DefaultRequestCache has been enhanced to support configurable reference types and cache scopes through user-defined selectors. This allows fine-grained control over caching behavior for different request types.

## Key Features

### 1. Early Return for ProtoSession
- The `doCache` method now returns early when the request session is not a `Session` instance (e.g., `ProtoSession`)
- This prevents caching attempts for non-session contexts

### 2. Configurable Cache Behavior
- Cache scope and reference type can be configured via session user properties
- Configuration uses CSS-like selectors to match request types
- Supports parent-child request relationships

### 3. Backward Compatibility
- Existing `CacheMetadata` interface still supported
- Default behavior unchanged when no configuration provided
- Legacy hardcoded behavior maintained as fallback

## Configuration Syntax

### User Property
```
maven.cache.config
```

### Selector Syntax
```
[ParentRequestType] RequestType { scope: <scope>, ref: <reference> }
```

Where:
- `RequestType`: Short class name of the request (e.g., `ModelBuilderRequest`)
- `ParentRequestType`: Optional parent request type or `*` for any parent
- `scope`: Cache retention scope (optional)
- `ref`: Reference type for cache entries (optional)

**Note**: You can specify only `scope` or only `ref` - missing values will be merged from less specific selectors or use defaults.

### Available Values

#### Scopes
- `session`: SESSION_SCOPED - retained for Maven session duration
- `request`: REQUEST_SCOPED - retained for current build request
- `persistent`: PERSISTENT - persisted across Maven invocations
- `disabled`: DISABLED - no caching performed

#### Reference Types
- `soft`: SOFT - cleared before OutOfMemoryError
- `hard`: HARD - never cleared by GC
- `weak`: WEAK - cleared more aggressively
- `none`: NONE - no caching (always compute)

## Examples

### Basic Configuration
```bash
mvn clean install -Dmaven.cache.config="ModelBuilderRequest { scope: session, ref: hard }"
```

### Multiple Selectors with Merging
```bash
mvn clean install -Dmaven.cache.config="
ArtifactResolutionRequest { scope: session, ref: soft }
ModelBuildRequest { scope: request, ref: soft }
ModelBuilderRequest VersionRangeRequest { ref: hard }
ModelBuildRequest * { ref: hard }
"
```

### Partial Configuration and Merging
```bash
# Base configuration for all ModelBuilderRequest
# More specific selectors can override individual properties
mvn clean install -Dmaven.cache.config="
ModelBuilderRequest { scope: session }
* ModelBuilderRequest { ref: hard }
ModelBuildRequest ModelBuilderRequest { ref: soft }
"
```

### Parent-Child Relationships
```bash
# VersionRangeRequest with ModelBuilderRequest parent uses hard references
mvn clean install -Dmaven.cache.config="ModelBuilderRequest VersionRangeRequest { ref: hard }"

# Any request with ModelBuildRequest parent uses hard references
mvn clean install -Dmaven.cache.config="ModelBuildRequest * { ref: hard }"
```

## Selector Priority and Merging

Selectors are ordered by specificity (most specific first):
1. Parent + Request type (e.g., `ModelBuildRequest ModelBuilderRequest`)
2. Request type only (e.g., `ModelBuilderRequest`)
3. Wildcard patterns (e.g., `* ModelBuilderRequest`)

### Configuration Merging
- Multiple selectors can match the same request
- More specific selectors override properties from less specific ones
- Only non-null properties are merged (allows partial configuration)
- Processing stops when a complete configuration is found

Example:
```
ModelBuilderRequest { scope: session }        # Base: sets scope
* ModelBuilderRequest { ref: hard }           # Adds ref type
ModelBuildRequest ModelBuilderRequest { ref: soft }  # Overrides ref for specific parent
```

For a `ModelBuilderRequest` with `ModelBuildRequest` parent:
- Final config: `scope: session, ref: soft`

## Implementation Details

### New Classes
- `CacheConfig`: Record holding complete scope and reference type configuration
- `PartialCacheConfig`: Record holding partial configuration (allows null values)
- `CacheSelector`: Represents a selector rule with matching logic
- `CacheSelectorParser`: Parses configuration strings into selectors
- `CacheConfigurationResolver`: Resolves and merges configuration for requests

### Modified Classes
- `DefaultRequestCache.doCache()`: Enhanced with configurable behavior
- Early return for non-Session requests
- Removed hardcoded reference types
- Integrated configuration resolution

## Migration Guide

### For Users
- No changes required for existing builds
- New configuration is opt-in via user properties
- Existing behavior preserved when no configuration provided

### For Developers
- `CacheMetadata` interface still supported for backward compatibility
- New configuration takes precedence over `CacheMetadata`
- Default reference types changed to SOFT for consistency

## Testing

The implementation includes comprehensive tests:
- `CacheConfigurationTest`: Unit tests for configuration parsing and resolution
- Integration tests for selector matching and priority
- Backward compatibility tests

## Performance Considerations

- Configuration parsing is cached per session to avoid re-parsing
- Selector matching is optimized for common cases
- Memory usage improved with configurable reference types
- Early return for ProtoSession reduces overhead

## Future Enhancements

Potential future improvements:
- Support for more complex selector patterns
- Configuration validation and error reporting
- Runtime configuration updates
- Performance metrics and monitoring
