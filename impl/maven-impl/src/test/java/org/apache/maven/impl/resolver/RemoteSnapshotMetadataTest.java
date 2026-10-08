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
package org.apache.maven.impl.resolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.apache.maven.api.metadata.Metadata;
import org.apache.maven.api.metadata.SnapshotVersion;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Merging new snapshot artifacts into the version-level metadata already in the repository.
 */
class RemoteSnapshotMetadataTest {

    @TempDir
    Path temp;

    /**
     * Existing metadata that lists the main artifact without a {@code <classifier>} element, as
     * Maven 3 and repository managers that regenerate metadata write it.
     */
    private static final String WITHOUT_CLASSIFIER_ELEMENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId>test</groupId>
              <artifactId>repro</artifactId>
              <versioning>
                <lastUpdated>20260901000000</lastUpdated>
                <snapshot>
                  <timestamp>20260901.000000</timestamp>
                  <buildNumber>1</buildNumber>
                </snapshot>
                <snapshotVersions>
                  <snapshotVersion>
                    <extension>pom</extension>
                    <value>1-20260901.000000-1</value>
                    <updated>20260901000000</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>sources</classifier>
                    <extension>jar</extension>
                    <value>1-20260901.000000-1</value>
                    <updated>20260901000000</updated>
                  </snapshotVersion>
                </snapshotVersions>
              </versioning>
              <version>1-SNAPSHOT</version>
            </metadata>
            """;

    @Test
    void newBuildReplacesAnEntryReadWithoutAClassifierElement() throws Exception {
        List<SnapshotVersion> versions = mergeOnto(WITHOUT_CLASSIFIER_ELEMENT);

        List<SnapshotVersion> poms =
                versions.stream().filter(v -> "pom".equals(v.getExtension())).toList();
        assertEquals(1, poms.size(), "one pom entry, not the new one plus the previous build's: " + poms);
        assertTrue(poms.get(0).getVersion().endsWith("-2"), "the pom entry is the new build: " + poms);
    }

    @Test
    void entriesForOtherClassifiersAreKept() throws Exception {
        List<SnapshotVersion> versions = mergeOnto(WITHOUT_CLASSIFIER_ELEMENT);

        List<SnapshotVersion> sources = versions.stream()
                .filter(v -> "sources".equals(v.getClassifier()))
                .toList();
        assertEquals(1, sources.size(), "the sources jar was not redeployed and stays listed: " + versions);
        assertEquals("1-20260901.000000-1", sources.get(0).getVersion());
    }

    @Test
    void newBuildReplacesAnEntryWithAnEmptyClassifierElement() throws Exception {
        String emptyClassifier = WITHOUT_CLASSIFIER_ELEMENT.replace(
                "<extension>pom</extension>", "<classifier></classifier>\n        <extension>pom</extension>");

        List<SnapshotVersion> poms = mergeOnto(emptyClassifier).stream()
                .filter(v -> "pom".equals(v.getExtension()))
                .toList();

        assertEquals(1, poms.size(), "one pom entry: " + poms);
        assertTrue(poms.get(0).getVersion().endsWith("-2"), "the pom entry is the new build: " + poms);
    }

    private List<SnapshotVersion> mergeOnto(String existing) throws Exception {
        Path file = temp.resolve("maven-metadata.xml");
        Files.writeString(file, existing, StandardCharsets.UTF_8);
        Metadata recessive = MavenMetadata.read(file);

        DefaultArtifact pom = new DefaultArtifact("test:repro:pom:1-SNAPSHOT");
        RemoteSnapshotMetadata metadata = new RemoteSnapshotMetadata(pom, Instant.now(), null);
        metadata.bind(pom);
        metadata.merge(recessive);

        return metadata.metadata.getVersioning().getSnapshotVersions();
    }
}
