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
package org.apache.maven.project;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link ExtensionDescriptorBuilder}.
 *
 * @author Benjamin Bentmann
 */
public class ExtensionDescriptorBuilderTest {

    private ExtensionDescriptorBuilder builder;

    @BeforeEach
    protected void setUp() throws Exception {

        builder = new ExtensionDescriptorBuilder();
    }

    private InputStream toStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testEmptyDescriptor() throws Exception {
        String xml = "<extension></extension>";

        ExtensionDescriptor ed = builder.build(toStream(xml));

        assertNotNull(ed);
        assertNotNull(ed.getExportedPackages());
        assertTrue(ed.getExportedPackages().isEmpty());
        assertNotNull(ed.getExportedArtifacts());
        assertTrue(ed.getExportedArtifacts().isEmpty());
    }

    @Test
    public void testCompleteDescriptor() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>" + "<extension>" + "<exportedPackages>"
                + "<exportedPackage>a</exportedPackage>" + "<exportedPackage>b</exportedPackage>"
                + "<exportedPackage>c</exportedPackage>" + "</exportedPackages>" + "<exportedArtifacts>"
                + "<exportedArtifact>x</exportedArtifact>" + "<exportedArtifact>y</exportedArtifact>"
                + "<exportedArtifact> z </exportedArtifact>" + "</exportedArtifacts>" + "</extension>";

        ExtensionDescriptor ed = builder.build(toStream(xml));

        assertNotNull(ed);
        assertEquals(Arrays.asList("a", "b", "c"), ed.getExportedPackages());
        assertEquals(Arrays.asList("x", "y", "z"), ed.getExportedArtifacts());
    }

    /**
     * maven-core's own {@code META-INF/maven/extension.xml} must keep excluding {@code org.fusesource.jansi:jansi}
     * as an {@code exportedArtifact} even though jansi is no longer a maven-core dependency (replaced by JLine, see
     * #11874): maven-jline exports forwarding classes for the JAnsi API (see
     * {@code org.apache.maven.jline}{@code .*} and {@code org.fusesource.jansi.*} in maven-jline), and it is the
     * {@code exportedArtifact} exclusion that makes a plugin's own {@code org.fusesource.jansi:jansi} dependency
     * resolve to those forwarding classes instead of shadowing them with the real {@code AnsiConsole}, which would
     * install its own {@code System.out}/{@code System.err} independently of the terminal Maven already installed
     * (see #13726, #13727).
     * <p>
     * The four {@code exportedPackage} entries are named exactly, not as the {@code org.fusesource.jansi.*}
     * wildcard, since {@code Ansi}, {@code AnsiConsole}, {@code AnsiPrintStream} and {@code AnsiMode} are the only
     * classes maven-jline provides; any other class in that package a plugin needs falls back to its own classpath
     * (see {@code ClassRealm#loadClassFromImport}).
     */
    @Test
    public void testCoreExtensionDescriptorHasNoStaleArtifactExports() throws Exception {
        try (InputStream in =
                getClass().getClassLoader().getResourceAsStream(builder.getExtensionDescriptorLocation())) {
            assertNotNull(in, "maven-core's own extension descriptor must be on the test classpath");

            ExtensionDescriptor ed = builder.build(in);

            assertTrue(
                    ed.getExportedArtifacts().contains("org.fusesource.jansi:jansi"),
                    "jansi must stay excluded so a plugin's own jansi dependency resolves to maven-jline's"
                            + " forwarding classes instead of shadowing them: " + ed.getExportedArtifacts());
            assertEquals(
                    Arrays.asList(
                            "org.fusesource.jansi.Ansi",
                            "org.fusesource.jansi.AnsiConsole",
                            "org.fusesource.jansi.AnsiPrintStream",
                            "org.fusesource.jansi.AnsiMode"),
                    ed.getExportedPackages().stream()
                            .filter(p -> p.startsWith("org.fusesource.jansi"))
                            .collect(Collectors.toList()),
                    "the JAnsi compatibility shim exports must stay, named exactly (not as the"
                            + " org.fusesource.jansi.* package, which maven-jline does not fully provide): "
                            + ed.getExportedPackages());
        }
    }
}
