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
package org.apache.maven.testing.plugin;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// swaps the global basedir system property that every @MojoTest reads
@Isolated
class MojoExtensionCoordinatesTest {

    @TempDir
    Path pluginBasedir;

    @Test
    void coordinatesInterpolateCiFriendlyVersion() throws Exception {
        Files.writeString(
                pluginBasedir.resolve("pom.xml"),
                "<project>"
                        + "<parent><groupId>org.example</groupId><artifactId>parent</artifactId>"
                        + "<version>${revision}</version></parent>"
                        + "<artifactId>my-plugin</artifactId>"
                        + "<properties><revision>1.2.3</revision></properties>"
                        + "</project>");
        String previous = System.getProperty("basedir");
        System.setProperty("basedir", pluginBasedir.toString());
        try {
            String[] coord = new MojoExtension().mojoCoordinates("goal");
            assertArrayEquals(new String[] {"org.example", "my-plugin", "1.2.3", "goal"}, coord);
        } finally {
            if (previous != null) {
                System.setProperty("basedir", previous);
            } else {
                System.clearProperty("basedir");
            }
        }
    }

    @Test
    void coordinatesThrowsOnUnresolvableExpression() throws Exception {
        Files.writeString(
                pluginBasedir.resolve("pom.xml"),
                "<project><parent><groupId>org.example</groupId><artifactId>parent</artifactId>"
                        + "<version>${unresolved}</version></parent>"
                        + "<artifactId>my-plugin</artifactId></project>");
        String previous = System.getProperty("basedir");
        System.setProperty("basedir", pluginBasedir.toString());
        try {
            assertThrows(Exception.class, () -> new MojoExtension().mojoCoordinates("goal"));
        } finally {
            if (previous != null) {
                System.setProperty("basedir", previous);
            } else {
                System.clearProperty("basedir");
            }
        }
    }
}
