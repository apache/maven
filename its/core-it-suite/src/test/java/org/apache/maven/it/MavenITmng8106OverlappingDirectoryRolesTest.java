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

import java.nio.file.Files;
import java.nio.file.Path;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenITmng8106OverlappingDirectoryRolesTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng8106OverlappingDirectoryRolesTest() {
        // Broken in: 3.9.0..3.9.6 && 4.0.0-alpha-1..4.0.0-alpha-13
        super();
    }

    @Test
    public void testDirectoryOverlap() throws Exception {
        Path testDir = extractResources("mng-8106");
        String repo = testDir.resolve("repo").toString();
        String tailRepo = Path.of(System.getProperty("user.home"), ".m2", "repository").toString();

        Verifier verifier = newVerifier(testDir.resolve("plugin"));
        verifier.addCliArgument("-X");
        verifier.addCliArgument("-Dmaven.repo.local=" + repo);
        verifier.addCliArgument("-Dmaven.repo.local.tail=" + tailRepo);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(testDir.resolve("jar"));
        verifier.addCliArgument("-X");
        verifier.addCliArgument("-Dmaven.repo.local=" + repo);
        verifier.addCliArgument("-Dmaven.repo.local.tail=" + tailRepo);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path metadataFile = Path.of(repo, "mng-8106/it/maven-metadata-local.xml");
        Xpp3Dom dom;
        try (var reader = Files.newBufferedReader(metadataFile)) {
            dom = Xpp3DomBuilder.build(reader);
        }
        assertTrue(dom.getChild("versioning") != null, "metadata missing A level data");
        assertTrue(dom.getChild("plugins") != null, "metadata missing G level data");
    }
}
