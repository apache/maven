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
import java.nio.file.Files;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for MNG-4559:
 * - Verifies that multiple JVM arguments in .mvn/jvm.config are properly handled
 * - Ensures arguments are correctly split and passed to the JVM
 */
public class MavenITmng4559MultipleJvmArgsTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng4559MultipleJvmArgsTest() {
        super("[4.0.0-rc-4,)");
    }

    @Test
    void testMultipleJvmArgs() throws Exception {
        File testDir = extractResources("/mng-4559-multiple-jvm-args");
        File mvnDir = new File(testDir, ".mvn");
        File jvmConfig = new File(mvnDir, "jvm.config");

        mvnDir.mkdirs();
        Files.writeString(
                jvmConfig.toPath(),
                "# This is a comment\n" + "-Xmx2048m -Xms1024m -Dtest.prop1=value1 # end of line comment\n"
                        + "# Another comment\n"
                        + "-Dtest.prop2=\"value 2\"");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/jvm.properties");
        assertEquals("value1", props.getProperty("project.properties.pom.test.prop1"));
        assertEquals("value 2", props.getProperty("project.properties.pom.test.prop2"));
    }
}
