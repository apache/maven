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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8331">MNG-8331</a>.
 * <p>
 * When a project that uses modelVersion 4.1.0 had both dependencies with a <pre>version</pre> element and dependencies
 * without a <pre>version</pre> element, for which the version could be found elsewhere in the same aggregator project,
 * the <pre>DefaultModelBuilder</pre> would enrich the dependencies without a <pre>version</pre> element.
 * The dependencies that did have a <pre>version</pre> element would not be copied over into the new <pre>Model</pre>
 * instance.
 */
class MavenITmng8331VersionedAndUnversionedDependenciesTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8331VersionedAndUnversionedDependenciesTest() {
        super("[4.0.0-beta-5,)");
    }

    /**
     * Since the dependency on junit-jupiter-api had a version, it was added to the project. The dependency on module-a
     * did not have a version, but could be found in the same multi-module project. As a result, the dependency on
     * junit-jupiter-api was not present in the model (see class-level JavaDoc) which would cause the test-compile
     * to fail.
     */
    @Test
    void allDependenciesArePresentInTheProject() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-8331-versioned-and-unversioned-deps");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.setLogFileName("allDependenciesArePresentInTheProject.txt");
        verifier.executeGoal("test-compile");

        verifier.verifyErrorFreeLog();
    }
}
