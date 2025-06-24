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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8465">MNG-8465</a>.
 */
class MavenITmng8465RepositoryWithProjectDirTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8465RepositoryWithProjectDirTest() {
        super("[4.0.0-rc-3-SNAPSHOT,)");
    }

    /**
     *  Verify various supported repository URLs.
     */
    @Test
    void testProjectDir() throws Exception {
        Path basedir = extractResources("/mng-8465").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("help:effective-pom");
        verifier.execute();
        List<String> urls = verifier.loadLogLines().stream()
                .filter(s -> s.trim().contains("<url>file://"))
                .toList();
        assertEquals(4, urls.size());
    }
}
