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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3716">MNG-3716</a>.
 *
 * todo Fill in a better description of what this test verifies!
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 *
 */
public class MavenITmng3716AggregatorForkingTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3716AggregatorForkingTest() {
        super("(2.0.8,)"); // only test in 2.0.9+
    }

    @Test
    public void testitMNG3716() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3716");
        File pluginDir = new File(testDir, "maven-mng3716-plugin");
        File projectsDir = new File(testDir, "projects");

        Verifier verifier;

        verifier = newVerifier(pluginDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectsDir.getAbsolutePath());
        verifier.addCliArgument("org.apache.maven.its.mng3716:mavenit-mng3716-plugin:1:run");
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
