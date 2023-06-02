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

/**
 * An integration test which proves that the bug of MNG-7310 is fixed.
 * The bug is about loading an extension in a sibling submodule, which ends up failing the build.
 *
 * <a href="https://issues.apache.org/jira/browse/MNG-7310">MNG-7310</a>.
 *
 */
public class MavenITmng7310LifecycleActivatedInSpecifiedModuleTest extends AbstractMavenIntegrationTestCase {

    public static final String BASE_TEST_DIR = "/mng-7310-lifecycle-activated-in-specified-module";

    public MavenITmng7310LifecycleActivatedInSpecifiedModuleTest() {
        super("[4.0.0-alpha-1,)");
    }

    public void testItShouldNotLoadAnExtensionInASiblingSubmodule() throws Exception {
        File extensionTestDir = ResourceExtractor.simpleExtractResources(getClass(), BASE_TEST_DIR + "/extension");
        File projectTestDir = ResourceExtractor.simpleExtractResources(getClass(), BASE_TEST_DIR + "/project");

        Verifier verifier = newVerifier(extensionTestDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();

        Verifier verifier2 = newVerifier(projectTestDir.getAbsolutePath());
        verifier2.addCliArgument("compile");
        verifier2.execute();
    }
}
