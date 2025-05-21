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
package org.apache.maven.artifact.handler;

import javax.inject.Inject;

import java.nio.file.Files;
import java.util.List;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static java.nio.file.Files.readAllLines;
import static org.assertj.core.api.Assertions.assertThat;
import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

@PlexusTest
class ArtifactHandlerTest {
    private static final String[] EXPECTED_COLUMN_HEADERS = new String[] {
        "", "type", "classifier", "extension", "packaging", "language", "added to classpath", "includesDependencies"
    };
    private static final List<String> VALID_PACKAGING_TYPES = List.of(
            "aar",
            "apk",
            "bundle",
            "ear",
            "eclipse-plugin",
            "eclipse-test-plugin",
            "ejb",
            "hpi",
            "jar",
            "java-source",
            "javadoc",
            "jpi",
            "kar",
            "lpkg",
            "maven-archetype",
            "maven-plugin",
            "nar",
            "par",
            "pom",
            "rar",
            "sar",
            "swc",
            "swf",
            "test-jar",
            "war",
            "zip");
    private static final String ARTIFACT_HANDLERS_APT = "src/site/apt/artifact-handlers.apt";

    @Inject
    PlexusContainer container;

    @Test
    void testAptConsistency() throws Exception {
        for (String line : readAllLines(getTestFile(ARTIFACT_HANDLERS_APT).toPath())) {
            if (line.startsWith("||")) {
                int i = 0;
                for (String col : line.split("\\|\\|")) {
                    assertEquals(EXPECTED_COLUMN_HEADERS[i++], col.trim(), "Wrong column header");
                }
            } else if (line.startsWith("|")) {
                String[] cols = line.split("\\|");
                String type = trimApt(cols[1]);
                assertHeader(
                        type,
                        trimApt(cols[3], type),
                        trimApt(cols[4], type),
                        trimApt(cols[2]),
                        trimApt(cols[5]),
                        trimApt(cols[6]),
                        trimApt(cols[7]));
            }
        }
    }

    private void assertHeader(
            String type,
            String extension,
            String packaging,
            String classifier,
            String language,
            String addedToClasspath,
            String includesDependencies)
            throws ComponentLookupException {
        ArtifactHandler handler = container.lookup(ArtifactHandlerManager.class).getArtifactHandler(type);
        assertEquals(handler.getExtension(), extension, type + " extension");
        // Packaging/Directory is Maven1 remnant!!!
        // assertEquals(handler.getPackaging(), packaging, type + " packaging");
        assertThat(handler.getPackaging()).isNotEmpty();
        assertThat(VALID_PACKAGING_TYPES).contains(packaging);
        assertEquals(handler.getClassifier(), classifier, type + " classifier");
        assertEquals(handler.getLanguage(), language, type + " language");
        assertEquals(handler.isAddedToClasspath() ? "true" : null, addedToClasspath, type + " addedToClasspath");
        assertEquals(
                handler.isIncludesDependencies() ? "true" : null, includesDependencies, type + " includesDependencies");
    }

    private String trimApt(String content, String type) {
        String value = trimApt(content);
        return "= type".equals(value) ? type : value;
    }

    private String trimApt(String content) {
        String value = content.replace('<', ' ').replace('>', ' ').trim();
        return value.isEmpty() ? null : value;
    }
}
