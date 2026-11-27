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
package org.apache.maven.cling.invoker.mvn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

/**
 * Writes a CycloneDX 1.6 JSON SBOM from the resolved artifacts of the build.
 * Hand-rolled encoder: maven-cli has no JSON library dependency, and adding one
 * for a single report is not justified.
 *
 * <p>Only fields Maven actually retains in the resolved artifact set are emitted:
 * component group/name/version and a synthesised purl, plus dependency edges.
 * Supplier, license and hash are omitted, never fabricated. Components and edges
 * are sorted and the timestamp honours {@code SOURCE_DATE_EPOCH}, so the output
 * is deterministic.</p>
 */
final class SbomReportWriter {
    private SbomReportWriter() {}

    static void write(Path file, MavenExecutionResult result) throws IOException {
        Map<String, String[]> components = new TreeMap<>();
        Map<String, Set<String>> dependencies = new TreeMap<>();
        for (MavenProject project : result.getTopologicallySortedProjects()) {
            String rootPurl = purl(project.getGroupId(), project.getArtifactId(), project.getVersion());
            if (rootPurl == null) {
                continue;
            }
            components.put(
                    rootPurl,
                    new String[] {project.getGroupId(), project.getArtifactId(), project.getVersion(), rootPurl});
            for (Artifact artifact : project.getArtifacts()) {
                String depPurl = purl(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                if (depPurl == null || depPurl.equals(rootPurl)) {
                    continue;
                }
                components.put(
                        depPurl,
                        new String[] {artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), depPurl});
                dependencies.computeIfAbsent(rootPurl, k -> new TreeSet<>()).add(depPurl);
            }
        }
        for (String purl : components.keySet()) {
            dependencies.putIfAbsent(purl, new TreeSet<>());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"bomFormat\": \"CycloneDX\",\n");
        sb.append("  \"specVersion\": \"1.6\",\n");
        sb.append("  \"version\": 1,\n");
        sb.append("  \"metadata\": {\n");
        sb.append("    \"timestamp\": \"").append(timestamp()).append("\",\n");
        sb.append("    \"tools\": {\"components\": [{\"type\": \"application\", \"name\": \"mvn\"}]},\n");
        sb.append("    \"authors\": [{\"name\": \"Apache Maven\"}]\n");
        sb.append("  },\n");
        sb.append("  \"components\": [\n");
        int i = 0;
        for (String[] c : components.values()) {
            sb.append("    {\"type\": \"library\", \"group\": \"")
                    .append(escape(c[0]))
                    .append("\", \"name\": \"")
                    .append(escape(c[1]))
                    .append("\", \"version\": \"")
                    .append(escape(c[2]))
                    .append("\", \"purl\": \"")
                    .append(escape(c[3]))
                    .append("\"}");
            sb.append(++i < components.size() ? ",\n" : "\n");
        }
        sb.append("  ],\n");
        sb.append("  \"dependencies\": [\n");
        int e = 0;
        for (Map.Entry<String, Set<String>> edge : dependencies.entrySet()) {
            sb.append("    {\"ref\": \"").append(escape(edge.getKey())).append("\", \"dependsOn\": [");
            int t = 0;
            for (String to : edge.getValue()) {
                sb.append(t++ > 0 ? ", " : "").append("\"").append(escape(to)).append("\"");
            }
            sb.append("]}");
            sb.append(++e < dependencies.size() ? ",\n" : "\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String purl(String groupId, String artifactId, String version) {
        if (groupId == null || artifactId == null || version == null) {
            return null;
        }
        return "pkg:maven/" + groupId + "/" + artifactId + "@" + version;
    }

    private static String timestamp() {
        String epoch = System.getenv("SOURCE_DATE_EPOCH");
        if (epoch != null && !epoch.isBlank()) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(epoch)).toString();
            } catch (NumberFormatException e) {
                // Invalid SOURCE_DATE_EPOCH: fall back to current time.
            }
        }
        return Instant.now().toString();
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '"') {
                out.append("\\\"");
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
