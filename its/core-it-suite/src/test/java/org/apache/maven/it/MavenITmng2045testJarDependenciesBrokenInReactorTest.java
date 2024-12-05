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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2045">MNG-2045</a>:
 * Simple IT test invoking maven in a reactor with 2 projects --
 * first project produces a test-jar, which is required to
 * compile second project.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author mikko.koponen@ri.fi
 */
public class MavenITmng2045testJarDependenciesBrokenInReactorTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2045testJarDependenciesBrokenInReactorTest() {
        super("(2.0.7,)"); // 2.0.8+
    }

    @Test
    public void testitMNG2045() throws Exception {
        File testDir = extractResources("/mng-2045");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("test-user/target");
        verifier.deleteArtifacts("org.apache.maven.its.mng2045");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compile = verifier.loadLines("test-user/target/compile.txt");
        assertTestClasses(compile);
        assertNotMainClasses(compile);

        List<String> runtime = verifier.loadLines("test-user/target/runtime.txt");
        assertTestClasses(runtime);
        assertNotMainClasses(runtime);

        List<String> test = verifier.loadLines("test-user/target/test.txt");
        assertTestClasses(test);
        assertNotMainClasses(test);
    }

    private void assertTestClasses(List<String> classpath) {
        /*
         * Different Maven versions use the test-classes directory or the assembled test JAR but all that matters here
         * is merely that we have the test classes on the classpath.
         */
        assertTrue(
                classpath.contains("test")
                        || classpath.contains("test.jar")
                        || classpath.contains("test-jar-0.1-SNAPSHOT-tests.jar"),
                "test classes missing in " + classpath);
    }

    private void assertNotMainClasses(List<String> classpath) {
        // When depending on the test JAR of some module, we shouldn't get its main classes
        assertFalse(classpath.contains("main"), "main classes present in " + classpath);
    }
}
