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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3482">MNG-3482</a>.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 */
public class MavenITmng3482DependencyPomInterpolationTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3482DependencyPomInterpolationTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    @Test
    public void testitMNG3482() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3482");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng3482");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines("target/classpath.txt", "UTF-8");
        assertTrue(classpath.toString(), classpath.contains("dep2-1.jar"));
    }
}
