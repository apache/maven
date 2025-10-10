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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/11055">gh-11055</a>.
 *
 * It reproduces the behavior difference between using Session::getService and field injection via @Inject
 * for some core services.
 * @since 4.0.0-rc-4
 *
 */
class MavenITgh11055DIServiceInjectionTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testGetServiceSucceeds() throws Exception {
        File testDir = extractResources("/gh-11055-di-service-injection");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
