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
import java.util.regex.Pattern;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for
 * <a href="https://issues.apache.org/jira/browse/MNG-7965">MNG-7965</a>.
 * Allow to exclude plugins from validation. Affected ones as Maven 4.0.0-alpha-8 and Maven 4.0.0-alpha-9.
 */
class MavenITmng7965PomDuplicateTagsTest extends AbstractMavenIntegrationTestCase {

    protected MavenITmng7965PomDuplicateTagsTest() {
        super("(,4.0.0-alpha-8),(4.0.0-alpha-9,)");
    }

    @Test
    void javadocIsExecutedAndFailed() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7965-pom-duplicate-tags");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");

        boolean invocationFailed = false;
        // whatever outcome we do not fail here, but later
        try {
            verifier.execute();
        } catch (VerificationException e) {
            invocationFailed = true;
        }

        List<String> logs = verifier.loadLogLines();

        // the POM is not parseable
        verifyRegexInLog(logs, "\\[ERROR\\]\\s+Non-parseable POM");

        assertTrue("The Maven invocation should have failed: the POM is non-parseable", invocationFailed);
    }

    private void verifyRegexInLog(List<String> logs, String text) {
        Pattern p = Pattern.compile(text);
        assertTrue("Log file not contains: " + text, logs.stream().anyMatch(p.asPredicate()));
    }
}
