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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2124">MNG-2124</a>.
 */
public class MavenITmng2124PomInterpolationWithParentValuesTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng2124PomInterpolationWithParentValuesTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that ${parent.artifactId} resolves correctly. [MNG-2124]
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2124() throws Exception {
        File testDir = extractResources("/mng-2124");
        File child = new File(testDir, "parent/child");

        Verifier verifier = newVerifier(child.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/parent.properties");
        assertEquals("parent, child", props.getProperty("project.description"));
    }
}
