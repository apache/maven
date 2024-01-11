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
package org.apache.maven.model.io;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalXmlResolverTest {

    @Test
    void testAbsoluteUriWithRelativePath() {
        XMLStreamException exception =
                assertThrows(XMLStreamException.class, () -> new LocalXmlResolver(Paths.get("/users/base"))
                        .resolveEntity(null, "file:foo/bar.xml", "file:/users/base/pom.xml", null));
        assertTrue(exception.getMessage().contains("systemID must be a relative URI"), exception.getMessage());
    }

    @Test
    void testAbsoluteUriWithAbsolutePath() {
        XMLStreamException exception =
                assertThrows(XMLStreamException.class, () -> new LocalXmlResolver(Paths.get("/users/base"))
                        .resolveEntity(null, "file:/foo/bar.xml", "file:/users/base/pom.xml", null));
        assertTrue(exception.getMessage().contains("systemID must be a relative URI"), exception.getMessage());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testRelativeUriWithDifferentAbsolutePath() {
        XMLStreamException exception =
                assertThrows(XMLStreamException.class, () -> new LocalXmlResolver(Paths.get("/users/base"))
                        .resolveEntity(null, "/foo/bar.xml", "file:/users/base/pom.xml", null));
        assertTrue(exception.getMessage().contains("systemID must be a relative path"), exception.getMessage());
    }

    @Test
    void testRelativeUriWithDifferentAbsolutePathWin() {
        // `C:/users` is parsed as a `C` scheme !
        XMLStreamException exception =
                assertThrows(XMLStreamException.class, () -> new LocalXmlResolver(Paths.get("/users/base"))
                        .resolveEntity(null, "C:/foo/bar.xml", "file:/users/base/pom.xml", null));
        assertTrue(exception.getMessage().contains("systemID must be a relative URI"), exception.getMessage());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testRelativeUriWithSameAbsolutePath() {
        XMLStreamException exception =
                assertThrows(XMLStreamException.class, () -> new LocalXmlResolver(Paths.get("/users/base"))
                        .resolveEntity(null, "/users/base/foo/bar.xml", "file:/users/base/pom.xml", null));
        assertTrue(exception.getMessage().contains("systemID must be a relative path"), exception.getMessage());
    }

    @Test
    void testRelativeUriWithSameAbsolutePathWin() {
        // `C:/users` is parsed as a `C` scheme !
        XMLStreamException exception =
                assertThrows(XMLStreamException.class, () -> new LocalXmlResolver(Paths.get("/users/base"))
                        .resolveEntity(null, "C:/users/base/foo/bar.xml", "file:/users/base/pom.xml", null));
        assertTrue(exception.getMessage().contains("systemID must be a relative URI"), exception.getMessage());
    }

    @Test
    void testRelativeUriWithRelativeUriToParentOutsideTree() {
        XMLStreamException exception =
                assertThrows(XMLStreamException.class, () -> new LocalXmlResolver(Paths.get("/users/base"))
                        .resolveEntity(null, "../../bar.xml", "file:/users/base/foo/pom.xml", null));
        assertTrue(
                exception.getMessage().contains("systemID cannot refer to a path outside rootDirectory"),
                exception.getMessage());
    }

    @Test
    void testRelativeUriWithRelativeUriToParentInsideTree() {
        XMLStreamException exception =
                assertThrows(XMLStreamException.class, () -> new LocalXmlResolver(Paths.get("/users/base"))
                        .resolveEntity(null, "../bar.xml", "file:/users/base/foo/pom.xml", null));
        assertTrue(
                exception.getMessage().contains("Unable to create Source")
                        && exception.getMessage().contains("/users/base/bar.xml".replace('/', File.separatorChar)),
                exception.getMessage());
    }

    @Test
    void testRelativeUriWithRelativePath() {
        XMLStreamException exception =
                assertThrows(XMLStreamException.class, () -> new LocalXmlResolver(Paths.get("/users/base"))
                        .resolveEntity(null, "foo/bar.xml", "file:/users/base/pom.xml", null));
        assertTrue(
                exception.getMessage().contains("Unable to create Source")
                        && exception.getMessage().contains("/users/base/foo/bar.xml".replace('/', File.separatorChar)),
                exception.getMessage());
    }
}
