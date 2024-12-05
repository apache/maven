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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4403">MNG-4403</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4403LenientDependencyPomParsingTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4403LenientDependencyPomParsingTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-beta-2,)");
    }

    /**
     * Test that dependency POMs are only subject to minimal validation during metadata retrieval, i.e. Maven should
     * ignore most kinds of badness and make a best effort at getting the metadata. Of particular interest is also,
     * how Maven deals with duplicate dependency declarations.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4403");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4403");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> artifacts = verifier.loadLines("target/artifacts.txt");
        Collections.sort(artifacts);

        List<String> expected = new ArrayList<>();
        expected.add("org.apache.maven.its.mng4403:a:jar:0.1");
        expected.add("org.apache.maven.its.mng4403:b:jar:0.1");
        expected.add("org.apache.maven.its.mng4403:c:jar:0.1");

        assertEquals(expected, artifacts);
    }
}
