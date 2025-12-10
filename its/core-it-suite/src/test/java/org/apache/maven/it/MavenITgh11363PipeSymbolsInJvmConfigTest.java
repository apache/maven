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
 * This is a test set for <a href="https://github.com/apache/maven/issues/11363">gh-11363</a>:
 * Verify that pipe symbols in .mvn/jvm.config are properly handled and don't cause shell command parsing errors.
 */
public class MavenITgh11363PipeSymbolsInJvmConfigTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that pipe symbols in .mvn/jvm.config are properly handled
     */
    @Test
    void testPipeSymbolsInJvmConfig() throws Exception {
        Path basedir = extractResources("/gh-11363-pipe-symbols-jvm-config")
                .getAbsoluteFile()
                .toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.setForkJvm(true); // Use forked JVM to test .mvn/jvm.config processing
        // Enable debug logging for launcher script to diagnose jvm.config parsing issues
        verifier.setEnvironmentVariable("MAVEN_DEBUG_SCRIPT", "1");
        verifier.addCliArguments("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("de|*.de|my.company.mirror.de", props.getProperty("project.properties.pom.prop.nonProxyHosts"));
        assertEquals("value|with|pipes", props.getProperty("project.properties.pom.prop.with.pipes"));
    }
}
