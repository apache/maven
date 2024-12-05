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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3183">MNG-3183</a>.
 *
 * @author Benjamin Bentmann
 */
@Disabled(
        "This IT is testing -l, while new Verifier uses same switch to make Maven4 log to file; in short, if that is broken, all ITs would be broken as well")
public class MavenITmng3183LoggingToFileTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3183LoggingToFileTest() {
        super("[3.0-alpha-1,)");
    }

    /**
     * Test that the CLI parameter -l can be used to direct logging to a file.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-3183");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("-l");
        verifier.addCliArgument("maven.log");
        verifier.setLogFileName("stdout.txt");
        new File(testDir, "stdout.txt").delete();
        new File(testDir, "maven.log").delete();
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> stdout = verifier.loadLines("stdout.txt");

        for (Iterator<String> it = stdout.iterator(); it.hasNext(); ) {
            String line = it.next();
            if (line.startsWith("+") || line.startsWith("EMMA")) {
                it.remove();
            }
        }

        assertEquals(Collections.EMPTY_LIST, stdout);

        List<String> log = verifier.loadLines("maven.log");

        assertFalse(log.isEmpty());
    }
}
