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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IT for <a href="https://github.com/apache/maven/issues/13522">gh-13522</a>.
 *
 * <p>Verifies that when a DI-injected {@code Logger} is used inside a mojo,
 * the {@link org.apache.maven.api.logging.LogEvent} it emits carries the
 * correct {@code projectId} and {@code mojoId} as set by
 * {@code DefaultBuildPluginManager}.</p>
 */
class MavenITgh13522LoggerContextTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testLoggerContextFieldsArePopulated() throws Exception {
        // Step 1: install the IT plugin into the local repo
        Path pluginDir = extractResources("gh-13522-logger-context");
        Verifier installer = newVerifier(pluginDir);
        installer.setAutoclean(false);
        installer.deleteDirectory("target");
        installer.deleteArtifacts("org.apache.maven.its.gh13522");
        installer.addCliArgument("install");
        installer.addCliArgument("-DskipTests");
        installer.execute();
        installer.verifyErrorFreeLog();

        // Step 2: run the consumer project that invokes check-context
        Path consumerDir = pluginDir.resolve("src/it/simple-project");
        Verifier consumer = newVerifier(consumerDir);
        consumer.setAutoclean(false);
        consumer.addCliArgument("validate");
        consumer.execute();
        consumer.verifyErrorFreeLog();

        // Step 3: read and assert the captured log-context.properties
        Path propsFile = consumerDir.resolve("target/log-context.properties");
        assertTrue(Files.exists(propsFile), "log-context.properties must exist");

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(propsFile)) {
            props.load(is);
        }

        // projectId: artifactId of the consumer project (set by LoggingExecutionListener)
        assertEquals(
                "simple-project",
                props.getProperty("projectId", ""),
                "projectId must match the consumer project artifactId");

        // mojoId: fullGoalName@executionId — e.g. "logger-context:check-context@check-logger-context"
        String mojoId = props.getProperty("mojoId", "");
        assertTrue(mojoId.contains("check-context"), "mojoId must contain the goal name: " + mojoId);
        assertTrue(mojoId.contains("check-logger-context"), "mojoId must contain the execution id: " + mojoId);

        // loggerName: the mojo class or its DI-generated factory wrapper
        String loggerName = props.getProperty("loggerName", "");
        assertTrue(
                loggerName.startsWith("org.apache.maven.its.gh13522.CheckContextMojo"),
                "loggerName must start with the mojo implementation class name: " + loggerName);

        // pluginId: resolved via SLF4J StackWalker + registered plugin classloaders
        assertEquals(
                "org.apache.maven.its.gh13522:logger-context-maven-plugin:0.1.0-SNAPSHOT",
                props.getProperty("pluginId", ""),
                "pluginId must match the plugin GAV");
    }
}
