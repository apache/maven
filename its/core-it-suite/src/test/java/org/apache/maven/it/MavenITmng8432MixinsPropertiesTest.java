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
package org.apache.maven.it;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8432">MNG-8432</a>.
 * <p>
 * Demonstrates that the {@code <mixins>} feature (Maven 4.2.0+) is the proper solution
 * for inheriting both dependency management and properties from a BOM-like project,
 * instead of extending BOM import semantics.
 * </p>
 */
public class MavenITmng8432MixinsPropertiesTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that a project using a mixin inherits both properties and
     * dependency management from the mixin POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testMixinProvidesDependencyManagementAndProperties() throws Exception {
        Path testDir = extractResources("/mng-8432-mixins-properties");

        // 1. Install the mixin-bom which provides properties and dependencyManagement
        Verifier verifier = newVerifier(testDir.resolve("mixin-bom"));
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng8432");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // 2. Build the consuming project and dump its effective POM
        Path projectDir = testDir.resolve("project");
        verifier = newVerifier(projectDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("help:effective-pom");
        verifier.addCliArgument("-Doutput=target/effective-pom.xml");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // 3. Verify the effective POM contains the mixin's property
        verifier.verifyFilePresent("target/effective-pom.xml");
        String effectivePom = Files.readString(projectDir.resolve("target/effective-pom.xml"));
        assertTrue(
                effectivePom.contains("<mixin.property>mixin-value</mixin.property>"),
                "Property from mixin BOM should be inherited by the consuming project");

        // 4. Verify the effective POM contains the managed dependency from the mixin
        assertTrue(
                effectivePom.contains("<artifactId>managed-dep</artifactId>"),
                "Dependency management from mixin BOM should be inherited by the consuming project");
    }
}
