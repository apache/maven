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
package org.apache.maven.internal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.Sources;
import org.apache.maven.internal.impl.DefaultProjectBuilder.SourceWrapper;
import org.apache.maven.model.building.ModelSource2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks that the related POM, which is how the parent POM is found, is resolved against the directory of the POM
 * of the source, and not against the POM itself.
 */
class DefaultProjectBuilderSourceWrapperTest {

    @TempDir
    Path dir;

    private Path pom(String relative) throws IOException {
        Path pom = dir.resolve(relative);
        Files.createDirectories(pom.getParent());
        Files.writeString(pom, "<project/>");
        return pom;
    }

    @Test
    void testParentPomIsNotTheSamePom() throws IOException {
        Path parent = pom("pom.xml");
        Path child = pom("child/pom.xml");

        ModelSource2 related = new SourceWrapper(Sources.fromPath(child)).getRelatedSource("../pom.xml");

        assertNotNull(related);
        assertEquals(parent.toUri(), related.getLocationURI());
    }

    @Test
    void testDirectoryDesignatesItsPom() throws IOException {
        Path parent = pom("pom.xml");
        Path child = pom("child/pom.xml");

        ModelSource2 related = new SourceWrapper(Sources.fromPath(child)).getRelatedSource("..");

        assertNotNull(related);
        assertEquals(parent.toUri(), related.getLocationURI());
    }

    @Test
    void testBackslashesAreSeparators() throws IOException {
        pom("a/b/pom.xml");
        Path child = pom("c/pom.xml");

        ModelSource2 related = new SourceWrapper(Sources.fromPath(child)).getRelatedSource("..\\a\\b\\pom.xml");

        assertNotNull(related);
        assertEquals(dir.resolve("a/b/pom.xml").toUri(), related.getLocationURI());
    }

    @Test
    void testMissingPomIsNull() throws IOException {
        Path child = pom("child/pom.xml");

        assertNull(new SourceWrapper(Sources.fromPath(child)).getRelatedSource("../pom.xml"));
    }

    @Test
    void testSourceWithoutFileIsNull() {
        Source inMemory = new Source() {
            @Override
            public Path getPath() {
                return null;
            }

            @Override
            public InputStream openStream() {
                return InputStream.nullInputStream();
            }

            @Override
            public String getLocation() {
                return "<memory>";
            }

            @Override
            public Source resolve(String relative) {
                return null;
            }
        };

        assertNull(new SourceWrapper(inMemory).getRelatedSource("../pom.xml"));
    }
}
