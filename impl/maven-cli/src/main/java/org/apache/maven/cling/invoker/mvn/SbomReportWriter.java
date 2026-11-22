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
 * Writes an SBOM report in a protobom-style JSON document from the resolved
 * artifacts of the build. Hand-rolled encoder: maven-cli has no JSON library
 * dependency, and adding one for a single report is not justified.
 *
 * <p>Only fields Maven actually retains in the resolved artifact set are emitted:
 * node id/name/version and a synthesised purl, plus dependency edges and root
 * elements. Supplier, license and hash are omitted, never fabricated. Nodes,
 * edges and roots are sorted and the timestamp honours {@code SOURCE_DATE_EPOCH},
 * so the output is deterministic.</p>
 */
final class SbomReportWriter {
    private SbomReportWriter() {}

    static void write(Path file, MavenExecutionResult result) throws IOException {
        Map<String, String[]> nodes = new TreeMap<>();
        Map<String, Set<String>> edges = new TreeMap<>();
        Set<String> roots = new TreeSet<>();
        for (MavenProject project : result.getTopologicallySortedProjects()) {
            String rootPurl = purl(project.getGroupId(), project.getArtifactId(), project.getVersion());
            if (rootPurl == null) {
                continue;
            }
            nodes.put(rootPurl, new String[] {project.getArtifactId(), project.getVersion(), rootPurl});
            roots.add(rootPurl);
            for (Artifact artifact : project.getArtifacts()) {
                String depPurl = purl(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                if (depPurl == null || depPurl.equals(rootPurl)) {
                    continue;
                }
                nodes.put(depPurl, new String[] {artifact.getArtifactId(), artifact.getVersion(), depPurl});
                edges.computeIfAbsent(rootPurl, k -> new TreeSet<>()).add(depPurl);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"sbomFormat\": \"protobom-json\",\n");
        sb.append("  \"metadata\": {\n");
        sb.append("    \"authors\": [\"Apache Maven\"],\n");
        sb.append("    \"tools\": [\"mvn\"],\n");
        sb.append("    \"date\": \"").append(timestamp()).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"nodeList\": {\n");
        sb.append("    \"nodes\": [\n");
        int i = 0;
        for (String[] c : nodes.values()) {
            sb.append("      {\"id\": \"").append(escape(c[2]))
                    .append("\", \"name\": \"").append(escape(c[0]))
                    .append("\", \"version\": \"").append(escape(c[1])).append("\"}");
            sb.append(++i < nodes.size() ? ",\n" : "\n");
        }
        sb.append("    ],\n");
        sb.append("    \"edges\": [\n");
        int e = 0;
        for (Map.Entry<String, Set<String>> edge : edges.entrySet()) {
            sb.append("      {\"type\": \"dependsOn\", \"from\": \"").append(escape(edge.getKey())).append("\", \"to\": [");
            int t = 0;
            for (String to : edge.getValue()) {
                sb.append(t++ > 0 ? ", " : "").append("\"").append(escape(to)).append("\"");
            }
            sb.append("]}");
            sb.append(++e < edges.size() ? ",\n" : "\n");
        }
        sb.append("    ],\n");
        sb.append("    \"rootElements\": [\n");
        int r = 0;
        for (String root : roots) {
            sb.append("      \"").append(escape(root)).append("\"");
            sb.append(++r < roots.size() ? ",\n" : "\n");
        }
        sb.append("    ]\n");
        sb.append("  }\n");
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
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
