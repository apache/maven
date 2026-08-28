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
package org.apache.maven.cling.invoker.mvnlog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Applies structural filters to a parsed build report (JSON as {@code Map<String, Object>}).
 * <p>
 * Filters are applied in order:
 * <ol>
 *   <li>{@code --module}: keep only modules whose {@code artifactId} contains the pattern</li>
 *   <li>{@code --mojo}: within each module, keep only mojos whose {@code goal} contains the pattern</li>
 *   <li>{@code --level}: within each mojo and module, keep only log events at or above the level</li>
 *   <li>{@code --grep}: within each mojo and module, keep only log events whose message matches</li>
 * </ol>
 * All string matching is case-insensitive.
 *
 * @since 4.1.0
 */
final class BuildReportFilter {

    /**
     * Log level ordinals for severity comparison.
     * Higher value = more severe.
     */
    private static final Map<String, Integer> LEVEL_ORDINALS = Map.of(
            "TRACE", 0,
            "DEBUG", 1,
            "INFO", 2,
            "WARN", 3,
            "WARNING", 3,
            "ERROR", 4);

    private final String modulePattern;
    private final String mojoPattern;
    private final String levelFilter;
    private final String grepPattern;

    BuildReportFilter(String modulePattern, String mojoPattern, String levelFilter, String grepPattern) {
        this.modulePattern = modulePattern != null ? modulePattern.toLowerCase(Locale.ROOT) : null;
        this.mojoPattern = mojoPattern != null ? mojoPattern.toLowerCase(Locale.ROOT) : null;
        this.levelFilter = levelFilter != null ? levelFilter.toUpperCase(Locale.ROOT) : null;
        this.grepPattern = grepPattern != null ? grepPattern.toLowerCase(Locale.ROOT) : null;
    }

    /**
     * Returns {@code true} if any filter is active.
     */
    boolean hasFilters() {
        return modulePattern != null || mojoPattern != null || levelFilter != null || grepPattern != null;
    }

    /**
     * Returns {@code true} if log-event-level filters are active
     * ({@code --level} or {@code --grep}).
     */
    boolean hasLogFilters() {
        return levelFilter != null || grepPattern != null;
    }

    /**
     * Apply all active filters to the report, returning a new report map
     * with only the matching entries. The original map is not modified.
     */
    @SuppressWarnings("unchecked")
    Map<String, Object> apply(Map<String, Object> report) {
        if (!hasFilters()) {
            return report;
        }

        Map<String, Object> result = new LinkedHashMap<>(report);

        // Filter modules
        Object modulesObj = result.get("modules");
        if (modulesObj instanceof List) {
            List<Map<String, Object>> modules = (List<Map<String, Object>>) modulesObj;
            List<Map<String, Object>> filtered = new ArrayList<>();

            for (Map<String, Object> module : modules) {
                // --module filter: match on artifactId
                if (modulePattern != null) {
                    String artifactId = getString(module, "artifactId");
                    if (artifactId == null
                            || !artifactId.toLowerCase(Locale.ROOT).contains(modulePattern)) {
                        continue;
                    }
                }

                Map<String, Object> filteredModule = new LinkedHashMap<>(module);

                // --mojo filter: keep only matching mojos
                if (mojoPattern != null) {
                    Object mojosObj = filteredModule.get("mojos");
                    if (mojosObj instanceof List) {
                        List<Map<String, Object>> mojos = (List<Map<String, Object>>) mojosObj;
                        List<Map<String, Object>> filteredMojos = new ArrayList<>();
                        for (Map<String, Object> mojo : mojos) {
                            String goal = getString(mojo, "goal");
                            if (goal != null && goal.toLowerCase(Locale.ROOT).contains(mojoPattern)) {
                                filteredMojos.add(filterMojoLogEvents(mojo));
                            }
                        }
                        filteredModule.put("mojos", filteredMojos);
                    }
                } else if (hasLogFilters()) {
                    // Apply log filters to mojos even without --mojo
                    Object mojosObj = filteredModule.get("mojos");
                    if (mojosObj instanceof List) {
                        List<Map<String, Object>> mojos = (List<Map<String, Object>>) mojosObj;
                        List<Map<String, Object>> filteredMojos = new ArrayList<>();
                        for (Map<String, Object> mojo : mojos) {
                            filteredMojos.add(filterMojoLogEvents(mojo));
                        }
                        filteredModule.put("mojos", filteredMojos);
                    }
                }

                // Apply log filters to module-level output
                if (hasLogFilters()) {
                    filteredModule.put("output", filterLogEvents(getLogEvents(filteredModule, "output")));
                }

                filtered.add(filteredModule);
            }

            result.put("modules", filtered);
        }

        // Apply log filters to build-level output
        if (hasLogFilters()) {
            result.put("output", filterLogEvents(getLogEvents(result, "output")));
        }

        return result;
    }

    /**
     * Collect all log events matching the current filters from a report.
     * Returns a flat list of log event maps, each annotated with a
     * {@code "context"} field indicating where the event came from
     * (module name, mojo goal, or build-level).
     */
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> collectMatchingLogEvents(Map<String, Object> report) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Build-level log events (skip when filtering by module — build-level
        // events don't belong to any module)
        if (modulePattern == null) {
            for (Map<String, Object> event : getLogEvents(report, "output")) {
                if (matchesLogFilters(event)) {
                    Map<String, Object> annotated = new LinkedHashMap<>(event);
                    annotated.put("context", "build");
                    results.add(annotated);
                }
            }
        }

        // Module and mojo-level log events
        Object modulesObj = report.get("modules");
        if (modulesObj instanceof List) {
            for (Map<String, Object> module : (List<Map<String, Object>>) modulesObj) {
                String artifactId = getString(module, "artifactId");

                // Check module filter
                if (modulePattern != null) {
                    if (artifactId == null
                            || !artifactId.toLowerCase(Locale.ROOT).contains(modulePattern)) {
                        continue;
                    }
                }

                // Module-level output
                for (Map<String, Object> event : getLogEvents(module, "output")) {
                    if (matchesLogFilters(event)) {
                        Map<String, Object> annotated = new LinkedHashMap<>(event);
                        annotated.put("context", artifactId != null ? artifactId : "unknown");
                        results.add(annotated);
                    }
                }

                // Mojo-level output
                Object mojosObj = module.get("mojos");
                if (mojosObj instanceof List) {
                    for (Map<String, Object> mojo : (List<Map<String, Object>>) mojosObj) {
                        String goal = getString(mojo, "goal");

                        // Check mojo filter
                        if (mojoPattern != null) {
                            if (goal == null || !goal.toLowerCase(Locale.ROOT).contains(mojoPattern)) {
                                continue;
                            }
                        }

                        String mojoLabel = (artifactId != null ? artifactId : "") + ":" + (goal != null ? goal : "");
                        for (Map<String, Object> event : getLogEvents(mojo, "output")) {
                            if (matchesLogFilters(event)) {
                                Map<String, Object> annotated = new LinkedHashMap<>(event);
                                annotated.put("context", mojoLabel);
                                results.add(annotated);
                            }
                        }
                    }
                }
            }
        }

        return results;
    }

    /**
     * Filter the log events within a mojo entry.
     */
    private Map<String, Object> filterMojoLogEvents(Map<String, Object> mojo) {
        if (!hasLogFilters()) {
            return mojo;
        }
        Map<String, Object> filtered = new LinkedHashMap<>(mojo);
        filtered.put("output", filterLogEvents(getLogEvents(mojo, "output")));
        return filtered;
    }

    /**
     * Filter a list of log events by level and grep pattern.
     */
    private List<Map<String, Object>> filterLogEvents(List<Map<String, Object>> events) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> event : events) {
            if (matchesLogFilters(event)) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * Check if a single log event matches the active level and grep filters.
     */
    private boolean matchesLogFilters(Map<String, Object> event) {
        // Level filter
        if (levelFilter != null) {
            String eventLevel = getString(event, "level");
            if (eventLevel == null || !isAtOrAbove(eventLevel, levelFilter)) {
                return false;
            }
        }

        // Grep filter
        if (grepPattern != null) {
            String message = getString(event, "message");
            if (message == null || !message.toLowerCase(Locale.ROOT).contains(grepPattern)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns {@code true} if {@code eventLevel} is at or above {@code minLevel}
     * in severity.
     */
    static boolean isAtOrAbove(String eventLevel, String minLevel) {
        Integer eventOrd = LEVEL_ORDINALS.get(eventLevel.toUpperCase(Locale.ROOT));
        Integer minOrd = LEVEL_ORDINALS.get(minLevel.toUpperCase(Locale.ROOT));
        if (eventOrd == null || minOrd == null) {
            return true; // unknown levels pass through
        }
        return eventOrd >= minOrd;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getLogEvents(Map<String, Object> container, String key) {
        Object value = container.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return List.of();
    }

    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
