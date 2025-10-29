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
import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6189">MNG-6189</a>:
 * using maven-site-plugin reportPlugins parameter must issue a warning.
 *
 * @author Hervé Boutemy
 */
@Disabled("Bounds: (3.5-alpha-1,4.0.0-alpha-2]")
public class MavenITmng6189SiteReportPluginsWarningTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testit() throws Exception {
        Path testDir = extractResourcesAsPath("/mng-6189-site-reportPlugins-warning");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyTextInLog("[WARNING] Reporting configuration should be done in <reporting> section");
    }
}
