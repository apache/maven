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
     * maven-core's own {@code META-INF/maven/extension.xml} must not export an artifact that maven-core no longer
     * actually bundles: a stale {@code exportedArtifact} entry causes
     * {@link org.apache.maven.ArtifactFilterManager} to strip that dependency out of a plugin's own resolved
     * classpath even though Maven core does not provide it, breaking any plugin that declares it directly (see
     * #13726, jansi left exported as an artifact after the JAnsi-to-JLine migration removed the real dependency).
     * <p>
     * {@code exportedPackage} entries are a different, safer mechanism (a {@code ClassRealm} import with fallback
     * to the plugin's own classpath for anything the import doesn't resolve, see
     * {@code ClassRealm#loadClassFromImport}), so {@code org.fusesource.jansi.Ansi} staying there, named exactly
     * rather than as the {@code org.fusesource.jansi.*} package, is intentional: maven-jline ships only that one
     * compatibility class, forwarding to the already-initialized JLine terminal, and plugins that need other real
     * jansi classes still get them from their own classpath.
     */
    @Test
    public void testCoreExtensionDescriptorHasNoStaleArtifactExports() throws Exception {
        try (InputStream in =
                getClass().getClassLoader().getResourceAsStream(builder.getExtensionDescriptorLocation())) {
            assertNotNull(in, "maven-core's own extension descriptor must be on the test classpath");

            ExtensionDescriptor ed = builder.build(in);

            assertTrue(
                    ed.getExportedArtifacts().stream().noneMatch(a -> a.contains("jansi")),
                    "jansi is no longer a maven-core dependency and must not be exported as an artifact: "
                            + ed.getExportedArtifacts());
            assertTrue(
                    ed.getExportedPackages().contains("org.fusesource.jansi.Ansi"),
                    "the JAnsi compatibility shim export must stay, named exactly (not as the org.fusesource.jansi.*"
                            + " package, which maven-jline does not fully provide): " + ed.getExportedPackages());
        }
    }
}
