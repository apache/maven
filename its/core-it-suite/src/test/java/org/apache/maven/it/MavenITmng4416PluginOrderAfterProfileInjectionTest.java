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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4416">MNG-4416</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4416PluginOrderAfterProfileInjectionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4416PluginOrderAfterProfileInjectionTest() {
        super("[2.0.5,)");
    }

    /**
     * Test that merging of plugins during profile injection follows these rules regarding ordering:
     * {@code model:   X ->      A -> B ->      D -> E
     * profile:      Y -> A ->      C -> D ->      F
     * result:  X -> Y -> A -> B -> C -> D -> E -> F}
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4416");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/it.properties");
        assertNotNull(props.getProperty("project.build.plugins"));

        List<String> expected = new ArrayList<>();
        expected.add("maven-it-plugin-error");
        expected.add("maven-it-plugin-configuration");
        expected.add("maven-it-plugin-dependency-resolution");
        expected.add("maven-it-plugin-packaging");
        expected.add("maven-it-plugin-log-file");
        expected.add("maven-it-plugin-expression");
        expected.add("maven-it-plugin-fork");
        expected.add("maven-it-plugin-touch");

        List<String> actual = new ArrayList<>();

        int count = Integer.parseInt(props.getProperty("project.build.plugins"));
        for (int i = 0; i < count; i++) {
            actual.add(props.getProperty("project.build.plugins." + i + ".artifactId"));
        }

        actual.retainAll(expected);

        assertEquals(actual, expected);
    }
}
