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

import java.io.File;
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

public class MavenITmng5783PluginDependencyFiltering extends AbstractMavenIntegrationTestCase {

    public MavenITmng5783PluginDependencyFiltering() {
        super("[3.0,)");
    }

    @Test
    public void testSLF4j() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-5783-plugin-dependency-filtering");
        Verifier verifier = newVerifier(new File(testDir, "plugin").getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "slf4j").getAbsolutePath(), "remote");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Note that plugin dependencies always include plugin itself and plexus-utils

        List<String> dependencies = verifier.loadLines("target/dependencies.txt", "UTF-8");
        if (matchesVersionRange("(,3.9.0)")) {
            assertEquals(3, dependencies.size());
        } else {
            assertEquals(2, dependencies.size());
        }
        assertEquals(
                "mng-5783-plugin-dependency-filtering:mng-5783-plugin-dependency-filtering-plugin:maven-plugin:0.1",
                dependencies.get(0));
        assertEquals("org.slf4j:slf4j-api:jar:1.7.5", dependencies.get(1));
        if (matchesVersionRange("(,3.9.0)")) {
            assertEquals("org.codehaus.plexus:plexus-utils:jar:1.1", dependencies.get(2));
        }
    }
}
