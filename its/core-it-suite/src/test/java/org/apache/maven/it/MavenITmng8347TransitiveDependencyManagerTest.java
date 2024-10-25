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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MRESOLVER-614">MRESOLVER-614</a> that is
 * fixed in Resolver release 2.0.3.
 * <p>
 * Maven 3 was not transitive regarding dependency management. Maven4 started to be, but beta-5 discovered a nasty
 * bug where Resolver was applying dependency management onto node itself it contributed (basically overriding
 * same node direct dependencies).
 */
class MavenITmng8347TransitiveDependencyManagerTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8347TransitiveDependencyManagerTest() {
        super("[3.9.0,)"); // since we have chained local repository
    }

    /**
     * We run same command with various Maven versions and based on their version assert (Maven3 was not transitive,
     * Maven4 before beta-6 was broken, post Maven 4 beta-6 all should be OK).
     */
    @Test
    void transitiveDependencyManager() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-8347-transitive-dependency-manager");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-V");
        verifier.addCliArgument("dependency:3.8.0:tree");
        verifier.addCliArgument("-Dmaven.repo.local.tail=" + testDir + "/local-repo");
        verifier.addCliArgument("-Dmaven.repo.local.tail.ignoreAvailability");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> l = verifier.loadLines(verifier.getLogFileName(), "UTF-8");
        if (matchesVersionRange("[3.9.0,4.0.0-alpha-12)")) {
            // Maven3 is not transitive
            a(l, "[INFO] org.apache.maven.it.mresolver614:root:jar:1.0.0");
            a(l, "[INFO] \\- org.apache.maven.it.mresolver614:level1:jar:1.0.0:compile");
            a(l, "[INFO]    \\- org.apache.maven.it.mresolver614:level2:jar:1.0.0:compile");
            a(l, "[INFO]       \\- org.apache.maven.it.mresolver614:level3:jar:1.0.0:compile");
            a(l, "[INFO]          \\- org.apache.maven.it.mresolver614:level4:jar:1.0.0:compile");
            a(l, "[INFO]             \\- org.apache.maven.it.mresolver614:level5:jar:1.0.0:compile");
            a(l, "[INFO]                \\- org.apache.maven.it.mresolver614:level6:jar:1.0.2:compile");
        } else if (matchesVersionRange("[4.0.0-alpha-12,4.0.0-beta-5]")) {
            // Maven 4 is transitive (added in 4.0.0-alpha-12, but was broken up to beta-6)
            a(l, "[INFO] org.apache.maven.it.mresolver614:root:jar:1.0.0");
            a(l, "[INFO] \\- org.apache.maven.it.mresolver614:level1:jar:1.0.0:compile");
            a(l, "[INFO]    \\- org.apache.maven.it.mresolver614:level2:jar:1.0.0:compile");
            a(l, "[INFO]       \\- org.apache.maven.it.mresolver614:level3:jar:1.0.1:compile");
            a(l, "[INFO]          \\- org.apache.maven.it.mresolver614:level4:jar:1.0.1:compile");
            a(l, "[INFO]             \\- org.apache.maven.it.mresolver614:level5:jar:1.0.2:compile");
            a(l, "[INFO]                \\- org.apache.maven.it.mresolver614:level6:jar:1.0.2:compile");
        } else if (matchesVersionRange("[4.0.0-beta-6,)")) {
            // Maven 4 is transitive and should produce expected results
            a(l, "[INFO] org.apache.maven.it.mresolver614:root:jar:1.0.0");
            a(l, "[INFO] \\- org.apache.maven.it.mresolver614:level1:jar:1.0.0:compile");
            a(l, "[INFO]    \\- org.apache.maven.it.mresolver614:level2:jar:1.0.0:compile");
            a(l, "[INFO]       \\- org.apache.maven.it.mresolver614:level3:jar:1.0.0:compile");
            a(l, "[INFO]          \\- org.apache.maven.it.mresolver614:level4:jar:1.0.1:compile");
            a(l, "[INFO]             \\- org.apache.maven.it.mresolver614:level5:jar:1.0.2:compile");
            a(l, "[INFO]                \\- org.apache.maven.it.mresolver614:level6:jar:1.0.2:compile");
        }
    }

    /**
     * Assert true, log lines contains string...
     */
    protected void a(List<String> logLines, String string) {
        assertTrue("missing " + string, logLines.contains(string));
    }
}
