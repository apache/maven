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
package org.apache.maven.model.transform;

import javax.xml.stream.XMLStreamReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParentXMLFilterTest extends AbstractXMLFilterTests {
    private Function<XMLStreamReader, XMLStreamReader> filterCreator;
    private Path projectPath;

    @BeforeEach
    void reset() throws IOException {
        filterCreator = null;
        projectPath = Paths.get("target/test-classes/" + getClass().getSimpleName() + "/child");
        Files.createDirectories(projectPath);
        if (!Files.isRegularFile(projectPath.resolve("../pom.xml"))) {
            Files.createFile(projectPath.resolve("../pom.xml"));
        }
        if (!Files.isRegularFile(projectPath.resolve("pom.xml"))) {
            Files.createFile(projectPath.resolve("pom.xml"));
        }
        Path relPath = projectPath.resolve("RELATIVEPATH");
        Files.createDirectories(relPath);
        if (!Files.isRegularFile(relPath.resolve("pom.xml"))) {
            Files.createFile(relPath.resolve("pom.xml"));
        }
    }

    @Override
    protected XMLStreamReader getFilter(XMLStreamReader parser) {
        Function<XMLStreamReader, XMLStreamReader> filterCreator =
                (this.filterCreator != null ? this.filterCreator : this::createFilter);
        return filterCreator.apply(parser);
    }

    protected XMLStreamReader createFilter(XMLStreamReader parser) {
        return createFilter(
                parser, x -> Optional.of(new RelativeProject("GROUPID", "ARTIFACTID", "1.0.0")), projectPath);
    }

    protected XMLStreamReader createFilter(
            XMLStreamReader parser, Function<Path, Optional<RelativeProject>> pathMapper, Path projectPath) {
        Function<Path, Path> locator = p -> p.resolve("pom.xml");
        return new ParentXMLFilter(new FastForwardFilter(parser), pathMapper, locator, projectPath);
    }

    @Test
    void testWithFastForward() throws Exception {
        String input = "<project>"
                + "<build>"
                + "<plugins>"
                + "<plugin>"
                + "<configuration>"
                + "<parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "</parent>"
                + "</configuration>"
                + "</plugin>"
                + "</plugins>"
                + "</build>"
                + "</project>";
        String expected = input;

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    @Test
    void testWithFastForwardAfterByPass() throws Exception {
        String input = "<project>"
                + "<build>"
                + "<plugins>"
                + "<plugin>"
                + "<configuration>"
                + "<parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "</parent>"
                + "</configuration>"
                + "</plugin>"
                + "</plugins>"
                + "</build>"
                + "<parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "</parent>"
                + "</project>";
        String expected = "<project>"
                + "<build>"
                + "<plugins>"
                + "<plugin>"
                + "<configuration>"
                + "<parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "</parent>"
                + "</configuration>"
                + "</plugin>"
                + "</plugins>"
                + "</build>"
                + "<parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<version>1.0.0</version>"
                + "</parent>"
                + "</project>";

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    @Test
    void testMinimum() throws Exception {
        String input = "<project><parent /></project>";
        String expected = input;
        String actual = transform(input);
        assertEquals(expected, actual);
    }

    @Test
    void testNoRelativePath() throws Exception {
        String input = "<project><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<version>VERSION</version>"
                + "</parent></project>";
        String expected = input;

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    @Test
    void testDefaultRelativePath() throws Exception {
        String input = "<project>\n"
                + "  <parent>\n"
                + "    <groupId>GROUPID</groupId>\n"
                + "    <artifactId>ARTIFACTID</artifactId>\n"
                + "  </parent>\n"
                + "</project>";
        String expected = "<project>\n"
                + "  <parent>\n"
                + "    <groupId>GROUPID</groupId>\n"
                + "    <artifactId>ARTIFACTID</artifactId>\n"
                + "    <version>1.0.0</version>\n"
                + "  </parent>\n"
                + "</project>";

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    /**
     * An empty relative path means it must be downloaded from a repository.
     * That implies that the version cannot be solved (if missing, Maven should complain)
     *
     * @throws Exception
     */
    @Test
    void testEmptyRelativePathNoVersion() throws Exception {
        String input = "<project><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<relativePath></relativePath>"
                + "</parent></project>";
        String expected = "<project><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<relativePath />" // SAX optimization, however "" != null ...
                + "</parent></project>";

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    @Test
    void testNoVersion() throws Exception {
        String input = "<project><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<relativePath>RELATIVEPATH</relativePath>"
                + "</parent></project>";
        String expected = "<project><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<relativePath>RELATIVEPATH</relativePath>"
                + "<version>1.0.0</version>"
                + "</parent></project>";

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    @Test
    void testInvalidRelativePath() throws Exception {
        filterCreator = parser -> createFilter(
                parser, x -> Optional.ofNullable(null), Paths.get("pom.xml").toAbsolutePath());

        String input = "<project><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<relativePath>RELATIVEPATH</relativePath>"
                + "</parent></project>";
        String expected = input;

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    @Test
    void testRelativePathAndVersion() throws Exception {
        String input = "<project><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<relativePath>RELATIVEPATH</relativePath>"
                + "<version>1.0.0</version>"
                + "</parent></project>";
        String expected = "<project><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<relativePath>RELATIVEPATH</relativePath>"
                + "<version>1.0.0</version>"
                + "</parent></project>";

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    @Test
    void testWithWeirdNamespace() throws Exception {
        String input = "<relativePath:project xmlns:relativePath=\"relativePath\">"
                + "<relativePath:parent>"
                + "<relativePath:groupId>GROUPID</relativePath:groupId>"
                + "<relativePath:artifactId>ARTIFACTID</relativePath:artifactId>"
                + "<relativePath:relativePath>RELATIVEPATH</relativePath:relativePath>"
                + "</relativePath:parent></relativePath:project>";
        String expected = "<relativePath:project xmlns:relativePath=\"relativePath\">"
                + "<relativePath:parent>"
                + "<relativePath:groupId>GROUPID</relativePath:groupId>"
                + "<relativePath:artifactId>ARTIFACTID</relativePath:artifactId>"
                + "<relativePath:relativePath>RELATIVEPATH</relativePath:relativePath>"
                + "<relativePath:version>1.0.0</relativePath:version>"
                + "</relativePath:parent>"
                + "</relativePath:project>";

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    @Test
    void comment() throws Exception {
        String input = "<project><!--before--><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<!--version here-->"
                + "</parent>"
                + "</project>";
        String expected = "<project><!--before--><parent>"
                + "<groupId>GROUPID</groupId>"
                + "<artifactId>ARTIFACTID</artifactId>"
                + "<!--version here-->"
                + "<version>1.0.0</version>"
                + "</parent>"
                + "</project>";

        String actual = transform(input);

        assertEquals(expected, actual);
    }

    @Test
    void testIndent() throws Exception {
        String input = "<project>\n"
                + "  <parent>\n"
                + "    <groupId>GROUPID</groupId>\n"
                + "    <artifactId>ARTIFACTID</artifactId>\n"
                + "    <!--version here-->\n"
                + "  </parent>\n"
                + "</project>";
        String expected = "<project>\n"
                + "  <parent>\n"
                + "    <groupId>GROUPID</groupId>\n"
                + "    <artifactId>ARTIFACTID</artifactId>\n"
                + "    <!--version here-->\n"
                + "    <version>1.0.0</version>\n"
                + "  </parent>\n"
                + "</project>";

        String actual = transform(input);

        assertEquals(expected, actual);
    }
}
