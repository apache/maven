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

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8653">MNG-8653</a>.
 */
class MavenITmng8653AfterAndEachPhasesWithConcurrentBuilderTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8653AfterAndEachPhasesWithConcurrentBuilderTest() {
        super("(4.0.0-rc-3,)");
    }

    /**
     *  Verify the dependency management of the consumer POM is computed correctly
     */
    @Test
    void testIt() throws Exception {
        Path basedir = extractResources("/mng-8653").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArguments("compile", "-b", "concurrent", "-T8");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLogLines();
        List<String> hallo = lines.stream().filter(l -> l.contains("Hallo")).toList();

        // Verify parent's before:all is first
        assertTrue(
                hallo.get(0).contains("'before:all' phase from 'parent'"),
                "First line should be parent's before:all but was: " + hallo.get(0));

        // Verify parent's after:all is last
        assertTrue(
                hallo.get(hallo.size() - 1).contains("'after:all' phase from 'parent'"),
                "Last line should be parent's after:all but was: " + hallo.get(hallo.size() - 1));
    }
}
