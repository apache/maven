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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case for <a href="https://issues.apache.org/jira/browse/MNG-6173">MNG-6173</a>.
 */
public class MavenITmng6173GetAllProjectsInReactorTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng6173GetAllProjectsInReactorTest() {
        super("[3.2.1,3.3.1),[3.5.0-alpha-2,)");
    }

    /**
     * Verifies that {@code MavenSession#getAllProjects()} returns all projects in the reactor
     * not only the ones being built.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitShouldReturnAllProjectsInReactor() throws Exception {

        File testDir = extractResources("/mng-6173-get-all-projects-in-reactor");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("module-1/target");
        verifier.deleteDirectory("module-2/target");
        verifier.addCliArgument("-pl");
        verifier.addCliArgument("module-1");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties properties = verifier.loadProperties("module-1/target/session.properties");
        assertEquals("3", properties.getProperty("session.allProjects.size"));
        assertEquals(Arrays.asList(new String[] {"base-project", "module-1", "module-2"}), getProjects(properties));
    }

    private List<String> getProjects(Properties properties) {
        List<String> projects = new ArrayList<>();

        for (Object o : properties.keySet()) {
            String key = o.toString();
            if (key.startsWith("session.allProjects.") && !key.endsWith(".size")) {
                projects.add(properties.getProperty(key));
            }
        }

        Collections.sort(projects);

        return projects;
    }
}
