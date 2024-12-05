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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MavenITmng5768CliExecutionIdTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng5768CliExecutionIdTest() {
        super("(3.2.5,)");
    }

    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-5768-cli-execution-id");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-configuration:config@test-execution-id");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/config.properties");
        assertEquals("CONFIGURED", props.getProperty("beanParam.fieldParam"));
    }
}
