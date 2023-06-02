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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-187">MNG-187</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng0187CollectedProjectsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng0187CollectedProjectsTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that MavenProject.getCollectedProjects() provides access to the direct and indirect modules
     * of the current project.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-0187");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("sub-1/target");
        verifier.deleteDirectory("sub-1/sub-2/target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props;

        props = verifier.loadProperties("target/project.properties");
        assertEquals("2", props.getProperty("project.collectedProjects.size"));
        assertEquals(Arrays.asList(new String[] {"sub-1", "sub-2"}), getProjects(props));

        props = verifier.loadProperties("sub-1/target/project.properties");
        assertEquals("1", props.getProperty("project.collectedProjects.size"));
        assertEquals(Arrays.asList(new String[] {"sub-2"}), getProjects(props));

        props = verifier.loadProperties("sub-1/sub-2/target/project.properties");
        assertEquals("0", props.getProperty("project.collectedProjects.size"));
        assertEquals(Arrays.asList(new String[] {}), getProjects(props));
    }

    private List<String> getProjects(Properties props) {
        List<String> projects = new ArrayList<>();

        for (Object o : props.keySet()) {
            String key = o.toString();
            if (key.startsWith("project.collectedProjects.") && !key.endsWith(".size")) {
                projects.add(props.getProperty(key));
            }
        }

        Collections.sort(projects);

        return projects;
    }
}
