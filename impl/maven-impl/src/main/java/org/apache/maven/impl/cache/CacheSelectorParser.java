/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.impl.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.api.cache.CacheRetention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for cache selector configuration strings.
 *
 * Supports syntax like:
 * <pre>
 * ArtifactResolutionRequest { scope: session, ref: soft }
 * ModelBuildRequest { scope: request, ref: soft }
 * ModelBuilderRequest VersionRangeRequest { ref: hard }
 * ModelBuildRequest * { ref: hard }
 * VersionRangeRequest { scope: session }
 * * { ref: weak }
 * </pre>
 */
public class CacheSelectorParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheSelectorParser.class);
    
    // Pattern to match selector rules: "[ParentType] RequestType { properties }"
    private static final Pattern RULE_PATTERN = Pattern.compile(
        "([\\w*]+)(?:\\s+([\\w*]+))?\\s*\\{([^}]+)\\}",
        Pattern.MULTILINE
    );
    
    // Pattern to match properties within braces: "key: value"
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "(\\w+)\\s*:\\s*([\\w]+)"
    );
    
    /**
     * Parses a cache configuration string into a list of cache selectors.
     * 
     * @param configString the configuration string to parse
     * @return list of parsed cache selectors, ordered by specificity (most specific first)
     */
    public static List<CacheSelector> parse(String configString) {
        List<CacheSelector> selectors = new ArrayList<>();
        
        if (configString == null || configString.trim().isEmpty()) {
            return selectors;
        }
        
        Matcher ruleMatcher = RULE_PATTERN.matcher(configString);
        while (ruleMatcher.find()) {
            try {
                CacheSelector selector = parseRule(ruleMatcher);
                if (selector != null) {
                    selectors.add(selector);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to parse cache selector rule: {}", ruleMatcher.group(), e);
            }
        }
        
        // Sort by specificity (most specific first)
        selectors.sort((a, b) -> compareSpecificity(b, a));
        
        return selectors;
    }
    
    /**
     * Parses a single rule from a regex matcher.
     */
    private static CacheSelector parseRule(Matcher ruleMatcher) {
        String firstType = ruleMatcher.group(1);
        String secondType = ruleMatcher.group(2);
        String properties = ruleMatcher.group(3);

        // Determine parent and request types
        String parentType = null;
        String requestType = firstType;

        if (secondType != null) {
            parentType = firstType;
            requestType = secondType;
        }

        // Parse properties
        PartialCacheConfig config = parseProperties(properties);
        if (config == null) {
            return null;
        }

        return new CacheSelector(parentType, requestType, config);
    }
    
    /**
     * Parses properties string into a PartialCacheConfig.
     */
    private static PartialCacheConfig parseProperties(String properties) {
        CacheRetention scope = null;
        Cache.ReferenceType referenceType = null;

        Matcher propMatcher = PROPERTY_PATTERN.matcher(properties);
        while (propMatcher.find()) {
            String key = propMatcher.group(1);
            String value = propMatcher.group(2);

            switch (key.toLowerCase()) {
                case "scope":
                    scope = parseScope(value);
                    break;
                case "ref":
                case "reference":
                    referenceType = parseReferenceType(value);
                    break;
                default:
                    LOGGER.warn("Unknown cache configuration property: {}", key);
            }
        }

        // Return partial configuration (null values are allowed)
        return new PartialCacheConfig(scope, referenceType);
    }
    
    /**
     * Parses a scope string into CacheRetention.
     */
    private static CacheRetention parseScope(String value) {
        return switch (value.toLowerCase()) {
            case "session" -> CacheRetention.SESSION_SCOPED;
            case "request" -> CacheRetention.REQUEST_SCOPED;
            case "persistent" -> CacheRetention.PERSISTENT;
            case "disabled", "none" -> CacheRetention.DISABLED;
            default -> {
                LOGGER.warn("Unknown cache scope: {}, using default REQUEST_SCOPED", value);
                yield CacheRetention.REQUEST_SCOPED;
            }
        };
    }
    
    /**
     * Parses a reference type string into Cache.ReferenceType.
     */
    private static Cache.ReferenceType parseReferenceType(String value) {
        return switch (value.toLowerCase()) {
            case "soft" -> Cache.ReferenceType.SOFT;
            case "hard" -> Cache.ReferenceType.HARD;
            case "weak" -> Cache.ReferenceType.WEAK;
            case "none" -> Cache.ReferenceType.NONE;
            default -> {
                LOGGER.warn("Unknown reference type: {}, using default SOFT", value);
                yield Cache.ReferenceType.SOFT;
            }
        };
    }
    
    /**
     * Compares specificity of two selectors. More specific selectors should be checked first.
     * Specificity order: parent + request > request only > wildcard
     */
    private static int compareSpecificity(CacheSelector a, CacheSelector b) {
        int aScore = getSpecificityScore(a);
        int bScore = getSpecificityScore(b);
        return Integer.compare(aScore, bScore);
    }
    
    private static int getSpecificityScore(CacheSelector selector) {
        int score = 0;
        
        // Parent type specificity
        if (selector.parentRequestType() != null) {
            if (!"*".equals(selector.parentRequestType())) {
                score += 100; // Specific parent type
            } else {
                score += 50; // Wildcard parent type
            }
        }
        
        // Request type specificity
        if (!"*".equals(selector.requestType())) {
            score += 10; // Specific request type
        } else {
            score += 1; // Wildcard request type
        }
        
        return score;
    }
}
