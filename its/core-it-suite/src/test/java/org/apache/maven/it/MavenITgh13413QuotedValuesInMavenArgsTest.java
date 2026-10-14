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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/13413">gh-13413</a>:
 * a value with white space which is quoted in {@code MAVEN_ARGS}, like a path with a space for
 * {@code -Dmaven.repo.local}, must reach Maven as a single argument.
 */
class MavenITgh13413QuotedValuesInMavenArgsTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testQuotedValueWithSpace() throws Exception {
        Path basedir = extractResources("gh-13413-maven-args-quoting");

        Verifier verifier = newVerifier(basedir);
        verifier.setForkJvm(true); // the launcher script is the one which splits MAVEN_ARGS
        verifier.setEnvironmentVariable("MAVEN_ARGS", "\"-Dprop.spaced=a value with spaces\" -Dprop.plain=plain");
        verifier.setEnvironmentVariable("MAVEN_DEBUG_SCRIPT", "1");
        verifier.addCliArguments("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("a value with spaces", props.getProperty("project.properties.pom.prop.spaced"));
        assertEquals("plain", props.getProperty("project.properties.pom.prop.plain"));
    }
}
