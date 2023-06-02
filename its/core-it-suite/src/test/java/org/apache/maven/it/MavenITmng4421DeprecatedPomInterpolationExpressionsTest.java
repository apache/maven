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
import java.util.Properties;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4421">MNG-4421</a>.
 *
 * But <a href="https://issues.apache.org/jira/browse/MNG-7244">MNG-7244</a> removes the deprecation of
 * <code>pom.X</code>.
 * See {@link MavenITmng7244IgnorePomPrefixInExpressions}.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4421DeprecatedPomInterpolationExpressionsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4421DeprecatedPomInterpolationExpressionsTest() {
        super("[3.0-alpha-3,4.0.0-alpha-1)");
    }

    /**
     * Test that expressions of the form ${pom.*} and {*} referring to the model cause build warnings.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4421");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("0.1", props.getProperty("project.properties.property1"));
        assertEquals("0.1", props.getProperty("project.properties.property2"));

        List<String> lines = verifier.loadLines("log.txt", null);

        boolean warnedPomPrefix = false;
        boolean warnedEmptyPrefix = false;

        for (String line : lines) {
            if (line.startsWith("[WARN")) {
                if (line.contains("${pom.version}")) {
                    warnedPomPrefix = true;
                }
                if (line.contains("${version}")) {
                    warnedEmptyPrefix = true;
                }
            }
        }

        assertTrue(warnedPomPrefix);
        assertTrue(warnedEmptyPrefix);
    }
}
