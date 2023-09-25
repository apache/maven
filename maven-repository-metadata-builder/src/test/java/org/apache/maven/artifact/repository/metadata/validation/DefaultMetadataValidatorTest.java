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
package org.apache.maven.artifact.repository.metadata.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.validator.DefaultMetadataValidator;
import org.apache.maven.artifact.repository.metadata.validator.MetadataProblemCollector;
import org.apache.maven.artifact.repository.metadata.validator.MetadataValidator.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultMetadataValidatorTest {

    private DefaultMetadataValidator validator;

    @BeforeEach
    public void setUp() throws Exception {
        validator = new DefaultMetadataValidator();
    }

    @AfterEach
    public void tearDown() throws Exception {
        validator = null;
    }

    private void assertContains(String msg, String substring) {
        assertTrue(msg.contains(substring), "\"" + substring + "\" was not found in: " + msg);
    }

    @Test
    void testValidateArtifactMetadata() {
        Metadata metadata = new Metadata();
        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(3, problems.messages.size());
        assertContains(problems.messages.get(0), "'groupId' on repository metadata level artifactId is missing");
        assertContains(problems.messages.get(1), "'artifactId' on repository metadata level artifactId is missing");
        assertContains(problems.messages.get(2), "'versioning' on repository metadata level artifactId is missing");

        metadata.setArtifactId("myArtifactId");
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(2, problems.messages.size());

        metadata.setGroupId("myGroupId");
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(1, problems.messages.size());

        Versioning versioning = new Versioning();
        metadata.setVersioning(versioning);
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(1, problems.messages.size());

        versioning.addVersion("1.2.3");
        ;
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(0, problems.messages.size());
    }

    @Test
    void testValidateGroupIdMetadata() {
        Metadata metadata = new Metadata();
        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate(metadata, Level.GROUP_ID, null, problems);
        assertEquals(0, problems.messages.size());

        Plugin plugin = new Plugin();
        plugin.setArtifactId("myArtifactId");
        plugin.setName("myPluginName");
        plugin.setPrefix("mypluginprefix");
        metadata.getPlugins().add(plugin);
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(0, problems.messages.size());

        plugin = new Plugin();
        metadata.getPlugins().add(plugin);
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(3, problems.messages.size());
        assertContains(
                problems.messages.get(0), "'plugins.plugin.name' on repository metadata level groupId is missing");
        assertContains(
                problems.messages.get(1), "'plugins.plugin.prefix' on repository metadata level groupId is missing");
        assertContains(
                problems.messages.get(2),
                "'plugins.plugin.artifactId' on repository metadata level groupId is missing");

        plugin = metadata.getPlugins().get(1);
        plugin.setArtifactId("myOtherArtifactId");
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(2, problems.messages.size());
        assertContains(
                problems.messages.get(0),
                "'plugins.plugin.name' for myOtherArtifactId on repository metadata level groupId is missing");
        assertContains(
                problems.messages.get(1), "'plugins.plugin.prefix' on repository metadata level groupId is missing");

        plugin.setName("myOtherPluginName");
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(1, problems.messages.size());
        assertContains(
                problems.messages.get(0),
                "'plugins.plugin.prefix' for myOtherPluginName on repository metadata level groupId is missing");

        plugin.setPrefix("myotherpluginprefix");
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        validator.validate(metadata, Level.GROUP_ID, null, problems);
        assertEquals(0, problems.messages.size());

        metadata.setGroupId("myGroupId");
        problems = new SimpleProblemCollector();
        validator.validate(metadata, problems);
        assertEquals(1, problems.messages.size());
    }

    @Test
    void testValidateVersionMetadata() {
        // TODO: add some tests
    }

    private static class SimpleProblemCollector implements MetadataProblemCollector {

        public List<String> messages = new ArrayList<>();

        public void add(Severity severity, String message, int line, int column, Exception cause) {
            messages.add(message);
        }
    }
}
