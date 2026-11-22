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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SbomReportWriterTest {

    @Test
    void writesCycloneDxWithGroupsAndDependencies() throws Exception {
        MavenProject project = new MavenProject();
        project.setGroupId("com.example");
        project.setArtifactId("app");
        project.setVersion("1.0");

        Set<Artifact> artifacts = new LinkedHashSet<>();
        artifacts.add(new DefaultArtifact(
                "org.example", "lib", "2.0", "compile", "jar", null, new DefaultArtifactHandler("jar")));
        project.setArtifacts(artifacts);

        DefaultMavenExecutionResult result = new DefaultMavenExecutionResult();
        result.setTopologicallySortedProjects(List.of(project));

        Path file = Files.createTempFile("sbom", ".json");
        SbomReportWriter.write(file, result);
        String json = Files.readString(file);

        assertTrue(json.contains("\"bomFormat\": \"CycloneDX\""), "must be CycloneDX JSON");
        assertTrue(json.contains("\"group\": \"org.example\""), "group must be emitted");
        assertTrue(json.contains("\"name\": \"lib\""), "artifact name must be emitted");
        assertTrue(json.contains("\"ref\": \"pkg:maven/com.example/app@1.0\""), "root ref must be emitted");
        assertTrue(json.contains("\"dependsOn\": [\"pkg:maven/org.example/lib@2.0\"]"), "edge must be emitted");

        Files.delete(file);
    }
}
