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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4026">MNG-4026</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4026ReactorDependenciesOrderTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4026ReactorDependenciesOrderTest() {
        // This feature depends on MNG-1412
        super("(2.0.8,)");
    }

    /**
     * Verify that the project class path is properly ordered during a reactor build, i.e. when dependencies are
     * resolved as active project artifacts from the reactor.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG4026() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4026");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("consumer/target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath1 = verifier.loadLines("consumer/target/classpath-1.txt", "UTF-8");
        assertEquals(5, classpath1.size());
        assertEquals("consumer/classes", classpath1.get(0));
        assertEquals("dep-1/pom.xml", classpath1.get(1));
        assertEquals("dep-3/pom.xml", classpath1.get(2));
        assertEquals("dep-2/pom.xml", classpath1.get(3));
        assertEquals("dep-4/pom.xml", classpath1.get(4));

        List<String> classpath2 = verifier.loadLines("consumer/target/classpath-2.txt", "UTF-8");
        assertEquals(classpath1, classpath2);
    }
}
