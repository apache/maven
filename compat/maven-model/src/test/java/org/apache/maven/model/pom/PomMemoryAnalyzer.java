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
package org.apache.maven.model.pom;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;

/**
 * A utility class that analyzes Maven POM files to identify memory usage patterns and potential memory optimizations.
 * This analyzer focuses on identifying duplicate strings and their memory impact across different paths in the POM structure.
 *
 * <p>The analyzer processes POM files recursively, tracking string occurrences and their locations within the POM structure.
 * It can identify areas where string deduplication could provide significant memory savings.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * PomMemoryAnalyzer analyzer = new PomMemoryAnalyzer();
 * Model model = reader.read(Files.newInputStream(pomPath));
 * analyzer.analyzePom(model);
 * analyzer.printAnalysis();
 * </pre>
 *
 * <p>The analysis output includes:</p>
 * <ul>
 *   <li>Total memory usage per POM path</li>
 *   <li>Potential memory savings through string deduplication</li>
 *   <li>Most frequent string values and their occurrence counts</li>
 *   <li>Statistics grouped by POM element types</li>
 * </ul>
 *
 * <p>This tool is particularly useful for identifying memory optimization opportunities
 * in large Maven multi-module projects where POM files may contain significant
 * duplicate content.</p>
 */
public class PomMemoryAnalyzer {
    private final Map<String, Map<String, StringStats>> pathStats = new HashMap<>();
    private final Map<String, Integer> globalStringFrequency = new HashMap<>();
    private int totalPoms = 0;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: PomMemoryAnalyzer <directory-with-poms>");
            System.exit(1);
        }

        Path rootDir = Paths.get(args[0]);
        PomMemoryAnalyzer analyzer = new PomMemoryAnalyzer();
        MavenStaxReader reader = new MavenStaxReader();

        // Find all pom.xml files, excluding those under src/ or target/
        Files.walk(rootDir)
                .filter(path -> path.getFileName().toString().equals("pom.xml"))
                .filter(path -> !containsSrcOrTarget(path))
                .forEach(pomPath -> {
                    try {
                        Model model = reader.read(Files.newInputStream(pomPath));
                        analyzer.analyzePom(model);
                    } catch (Exception e) {
                        System.err.println("Error processing " + pomPath + ": " + e.getMessage());
                    }
                });

        // Print analysis
        analyzer.printAnalysis();
    }

    private static boolean containsSrcOrTarget(Path pomPath) {
        Path parent = pomPath.getParent();
        while (parent != null && parent.getFileName() != null) {
            String dirName = parent.getFileName().toString();
            if (dirName.equals("src") || dirName.equals("target")) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    public void analyzePom(Model model) {
        totalPoms++;
        Set<Object> visited = new HashSet<>();
        processModelNode(model, "/project", "project", visited);
    }

    private void processModelNode(Object node, String currentPath, String elementName, Set<Object> visited) {
        if (node == null || !visited.add(node)) {
            return;
        }

        Class<?> clazz = node.getClass();
        while (clazz != null && !clazz.equals(Object.class)) {
            for (Field field : clazz.getDeclaredFields()) {
                // Skip static fields and synthetic fields
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(node);
                    if (value == null) continue;

                    String fullPath = currentPath + "/" + field.getName();

                    if (value instanceof String) {
                        String strValue = (String) value;
                        recordString(fullPath, strValue);
                        globalStringFrequency.merge(strValue, 1, Integer::sum);
                    } else if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        for (Object item : list) {
                            if (item != null) {
                                String itemName = getSingular(field.getName());
                                processModelNode(item, fullPath + "/" + itemName, itemName, visited);
                            }
                        }
                    } else if (value instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) value;
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            if (entry.getValue() != null) {
                                processModelNode(
                                        entry.getValue(),
                                        fullPath + "/" + entry.getKey(),
                                        entry.getKey().toString(),
                                        visited);
                            }
                        }
                    } else if (!value.getClass().isPrimitive()
                            && !value.getClass().getName().startsWith("java.")) {
                        processModelNode(value, fullPath, field.getName(), visited);
                    }
                } catch (Exception e) {
                    // Skip inaccessible or problematic fields
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private String getSingular(String plural) {
        if (plural.endsWith("ies")) {
            return plural.substring(0, plural.length() - 3) + "y";
        }
        if (plural.endsWith("s")) {
            return plural.substring(0, plural.length() - 1);
        }
        return plural;
    }

    private void recordString(String path, String value) {
        pathStats
                .computeIfAbsent(path, k -> new HashMap<>())
                .computeIfAbsent(value, k -> new StringStats())
                .recordOccurrence(value);
    }

    public List<PathAnalysis> getPathAnalysisSorted() {
        List<PathAnalysis> analysis = new ArrayList<>();

        for (Map.Entry<String, Map<String, StringStats>> entry : pathStats.entrySet()) {
            String path = entry.getKey();
            Map<String, StringStats> stats = entry.getValue();

            long uniqueStrings = stats.size();
            long totalOccurrences = stats.values().stream()
                    .mapToLong(StringStats::getOccurrences)
                    .sum();
            long totalMemory = stats.entrySet().stream()
                    .mapToLong(e -> e.getKey().length() * e.getValue().getOccurrences() * 2L)
                    .sum();
            long potentialSavings = stats.entrySet().stream()
                    .mapToLong(e -> e.getKey().length() * 2L * (e.getValue().getOccurrences() - 1))
                    .sum();

            analysis.add(new PathAnalysis(
                    path,
                    uniqueStrings,
                    totalOccurrences,
                    totalMemory,
                    potentialSavings,
                    (double) totalOccurrences / uniqueStrings,
                    getMostFrequentValues(stats, 5)));
        }

        analysis.sort((a, b) -> Long.compare(b.potentialSavings, a.potentialSavings));
        return analysis;
    }

    private List<ValueFrequency> getMostFrequentValues(Map<String, StringStats> stats, int limit) {
        return stats.entrySet().stream()
                .map(e -> new ValueFrequency(e.getKey(), e.getValue().getOccurrences()))
                .sorted((a, b) -> Long.compare(b.frequency, a.frequency))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void printAnalysis() {
        System.out.printf("Analyzed %d POMs%n%n", totalPoms);

        // First, get all paths
        List<PathAnalysis> allPaths = getPathAnalysisSorted();

        // Create groups based on the final path component
        Map<String, List<PathAnalysis>> groupedPaths = new HashMap<>();
        Map<String, Map<String, Long>> groupValueFrequencies = new HashMap<>();

        for (PathAnalysis path : allPaths) {
            String finalComponent = path.path.substring(path.path.lastIndexOf('/') + 1);

            // Add path to its group
            groupedPaths.computeIfAbsent(finalComponent, k -> new ArrayList<>()).add(path);

            // Aggregate value frequencies for the group
            Map<String, Long> groupFreqs = groupValueFrequencies.computeIfAbsent(finalComponent, k -> new HashMap<>());
            for (ValueFrequency vf : path.mostFrequentValues) {
                groupFreqs.merge(vf.value, vf.frequency, Long::sum);
            }
        }

        // Create final group analyses and sort them by total savings
        List<GroupAnalysis> sortedGroups = groupedPaths.entrySet().stream()
                .map(entry -> {
                    String groupName = entry.getKey();
                    List<PathAnalysis> paths = entry.getValue();
                    Map<String, Long> valueFreqs = groupValueFrequencies.get(groupName);

                    long totalSavings =
                            paths.stream().mapToLong(p -> p.potentialSavings).sum();
                    long totalMemory =
                            paths.stream().mapToLong(p -> p.totalMemory).sum();
                    long totalUnique = valueFreqs.size();
                    long totalOccurrences =
                            valueFreqs.values().stream().mapToLong(l -> l).sum();

                    List<ValueFrequency> topValues = valueFreqs.entrySet().stream()
                            .map(e -> new ValueFrequency(e.getKey(), e.getValue()))
                            .sorted((a, b) -> Long.compare(b.frequency, a.frequency))
                            .limit(5)
                            .collect(Collectors.toList());

                    return new GroupAnalysis(
                            groupName, paths, totalUnique, totalOccurrences, totalMemory, totalSavings, topValues);
                })
                .sorted((a, b) -> Long.compare(b.totalSavings, a.totalSavings))
                .collect(Collectors.toList());

        // Print each group
        for (GroupAnalysis group : sortedGroups) {
            System.out.printf("%nPaths ending with '%s':%n", group.name);
            System.out.printf("Total potential savings: %dKB%n", group.totalSavings / 1024);
            System.out.printf("Total memory: %dKB%n", group.totalMemory / 1024);
            System.out.printf("Total unique values: %d%n", group.totalUnique);
            System.out.printf("Total occurrences: %d%n", group.totalOccurrences);
            System.out.printf("Duplication ratio: %.2f%n", (double) group.totalOccurrences / group.totalUnique);

            System.out.println("\nMost frequent values across all paths:");
            for (ValueFrequency v : group.mostFrequentValues) {
                System.out.printf("  %-70s %d times%n", v.value, v.frequency);
            }

            System.out.println("\nIndividual paths:");
            System.out.println("----------------------------------------");
            for (PathAnalysis path : group.paths.stream()
                    .sorted((a, b) -> Long.compare(b.potentialSavings, a.potentialSavings))
                    .collect(Collectors.toList())) {
                System.out.printf(
                        "%-90s %6dKB %6dKB%n", path.path, path.totalMemory / 1024, path.potentialSavings / 1024);
            }
            System.out.println();
        }
    }

    private static class GroupAnalysis {
        final String name;
        final List<PathAnalysis> paths;
        final long totalUnique;
        final long totalOccurrences;
        final long totalMemory;
        final long totalSavings;
        final List<ValueFrequency> mostFrequentValues;

        GroupAnalysis(
                String name,
                List<PathAnalysis> paths,
                long totalUnique,
                long totalOccurrences,
                long totalMemory,
                long totalSavings,
                List<ValueFrequency> mostFrequentValues) {
            this.name = name;
            this.paths = paths;
            this.totalUnique = totalUnique;
            this.totalOccurrences = totalOccurrences;
            this.totalMemory = totalMemory;
            this.totalSavings = totalSavings;
            this.mostFrequentValues = mostFrequentValues;
        }
    }

    private static class StringStats {
        private long occurrences = 0;

        public void recordOccurrence(String value) {
            occurrences++;
        }

        public long getOccurrences() {
            return occurrences;
        }
    }

    public static class PathAnalysis {
        public final String path;
        public final long uniqueStrings;
        public final long totalOccurrences;
        public final long totalMemory;
        public final long potentialSavings;
        public final double duplicationRatio;
        public final List<ValueFrequency> mostFrequentValues;

        public PathAnalysis(
                String path,
                long uniqueStrings,
                long totalOccurrences,
                long totalMemory,
                long potentialSavings,
                double duplicationRatio,
                List<ValueFrequency> mostFrequentValues) {
            this.path = path;
            this.uniqueStrings = uniqueStrings;
            this.totalOccurrences = totalOccurrences;
            this.totalMemory = totalMemory;
            this.potentialSavings = potentialSavings;
            this.duplicationRatio = duplicationRatio;
            this.mostFrequentValues = mostFrequentValues;
        }
    }

    public static class ValueFrequency {
        public final String value;
        public final long frequency;

        public ValueFrequency(String value, long frequency) {
            this.value = value;
            this.frequency = frequency;
        }
    }
}
