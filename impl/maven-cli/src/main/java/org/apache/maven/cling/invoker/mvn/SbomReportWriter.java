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
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

/**
 * Writes an SBOM report in a protobom-style JSON document from the resolved
 * artifacts of the build. Hand-rolled encoder: maven-cli has no JSON library
 * dependency, and adding one for a single report is not justified.
 *
 * <p>Only fields Maven actually retains in the resolved artifact set are emitted:
 * component name, version and a synthesised purl. Supplier, license and hash are
 * omitted, never fabricated. Components are sorted by purl and the timestamp
 * honours {@code SOURCE_DATE_EPOCH}, so the output is deterministic.</p>
 */
final class SbomReportWriter {
    private SbomReportWriter() {}

    static void write(Path file, MavenExecutionResult result) throws IOException {
        Map<String, String[]> components = new TreeMap<>();
        for (MavenProject project : result.getTopologicallySortedProjects()) {
            addComponent(components, project.getGroupId(), project.getArtifactId(), project.getVersion());
            for (Artifact artifact : project.getArtifacts()) {
                addComponent(components, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
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
        sb.append("  \"components\": [\n");
        int i = 0;
        for (String[] c : components.values()) {
            sb.append("    {\"name\": \"").append(escape(c[0]))
                    .append("\", \"version\": \"").append(escape(c[1]))
                    .append("\", \"purl\": \"").append(escape(c[2])).append("\"}");
            sb.append(++i < components.size() ? ",\n" : "\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void addComponent(Map<String, String[]> components, String groupId, String artifactId, String version) {
        if (groupId == null || artifactId == null || version == null) {
            return;
        }
        String purl = "pkg:maven/" + groupId + "/" + artifactId + "@" + version;
        components.putIfAbsent(purl, new String[] {artifactId, version, purl});
    }

    private static String timestamp() {
        String epoch = System.getenv("SOURCE_DATE_EPOCH");
        Instant instant = epoch != null ? Instant.ofEpochSecond(Long.parseLong(epoch)) : Instant.now();
        return instant.atOffset(ZoneOffset.UTC).toInstant().toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
