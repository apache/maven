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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3719">MNG-3719</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng3719PomExecutionOrderingTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3719PomExecutionOrderingTest() {
        super("[2.0.11,2.1.0-M1),[2.1.0-M2,4.0.0-alpha-1)");
    }

    /**
     * Test that 3 executions are run in the correct order.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3719() throws Exception {
        File testDir = extractResources("/mng-3719");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Pattern pattern = Pattern.compile(".*step-([0-9])\\.properties.*");

        int[] stepLines = new int[3];
        List<String> content = verifier.loadLogLines();
        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);

            Matcher m = pattern.matcher(line);
            if (m.matches()) {
                int step = Integer.valueOf(m.group(1));
                stepLines[step - 1] = i + 1;
            }
        }

        // check order - note it is not in sequence as the plugin definitions are merged
        assertTrue(stepLines[0] > 0, "Step 1 should be found");
        assertTrue(stepLines[0] < stepLines[2], "Step 3 should be second");
        assertTrue(stepLines[2] < stepLines[1], "Step 2 should be third");
    }
}
