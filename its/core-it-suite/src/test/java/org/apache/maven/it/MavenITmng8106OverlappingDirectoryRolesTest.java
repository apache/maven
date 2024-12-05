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
import java.io.FileReader;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenITmng8106OverlappingDirectoryRolesTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng8106OverlappingDirectoryRolesTest() {
        // Broken in: 3.9.0..3.9.6 && 4.0.0-alpha-1..4.0.0-alpha-13
        super("(3.9.6,3.999.999],[4.0.0-beta-1,)");
    }

    @Test
    public void testDirectoryOverlap() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-8106");
        String repo = new File(testDir, "repo").getAbsolutePath();
        String tailRepo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";

        Verifier verifier = newVerifier(new File(testDir, "plugin").getAbsolutePath());
        verifier.addCliArgument("-X");
        verifier.addCliArgument("-Dmaven.repo.local=" + repo);
        verifier.addCliArgument("-Dmaven.repo.local.tail=" + tailRepo);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "jar").getAbsolutePath());
        verifier.addCliArgument("-X");
        verifier.addCliArgument("-Dmaven.repo.local=" + repo);
        verifier.addCliArgument("-Dmaven.repo.local.tail=" + tailRepo);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File metadataFile = new File(new File(repo), "mng-8106/it/maven-metadata-local.xml");
        Xpp3Dom dom;
        try (FileReader reader = new FileReader(metadataFile)) {
            dom = Xpp3DomBuilder.build(reader);
        }
        assertTrue(dom.getChild("versioning") != null, "metadata missing A level data");
        assertTrue(dom.getChild("plugins") != null, "metadata missing G level data");
    }
}
