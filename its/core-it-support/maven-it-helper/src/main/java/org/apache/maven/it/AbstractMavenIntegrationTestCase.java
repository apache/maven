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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * @author Jason van Zyl
 * @author Kenney Westerhof
 */
public abstract class AbstractMavenIntegrationTestCase {
    /**
     * Save System.out for progress reports etc.
     */
    private static PrintStream out = System.out;

    private String testName;

    protected AbstractMavenIntegrationTestCase() {}

    @BeforeAll
    static void setupInputStream() {
        if (!(System.in instanceof NonCloseableInputStream)) {
            System.setIn(new NonCloseableInputStream(System.in));
        }
    }

    @BeforeEach
    void setupContext(TestInfo testInfo) {
        testName = testInfo.getTestMethod().get().getName();
    }

    protected String getName() {
        return testName;
    }



    private static class NonCloseableInputStream extends FilterInputStream {
        NonCloseableInputStream(InputStream delegate) {
            super(delegate);
        }

        @Override
        public void close() throws IOException {}
    }



    protected File extractResources(String resourcePath) throws IOException {
        return new File(
                        new File(System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"))),
                        resourcePath)
                .getAbsoluteFile();
    }

    protected Verifier newVerifier(String basedir) throws VerificationException {
        return newVerifier(basedir, true);
    }

    protected Verifier newVerifier(String basedir, String settings) throws VerificationException {
        return newVerifier(basedir, settings, true);
    }

    protected Verifier newVerifier(String basedir, boolean createDotMvn) throws VerificationException {
        return newVerifier(basedir, "remote", createDotMvn);
    }

    protected Verifier newVerifier(String basedir, String settings, boolean createDotMvn) throws VerificationException {
        Verifier verifier = new Verifier(basedir, createDotMvn);

        // try to get jacoco arg from command line if any then use it to start IT to populate jacoco data
        // we use a different file than the main one
        ProcessHandle.current()
                .info()
                .arguments()
                .flatMap(strings -> Arrays.stream(strings)
                        .filter(s -> s.contains("-javaagent:") && s.contains("org.jacoco.agent"))
                        .findFirst())
                .map(s -> s.replace("jacoco.exec", "jacoco-its.exec"))
                .ifPresent(verifier::addJvmArgument);

        verifier.setAutoclean(false);

        if (settings != null) {
            File settingsFile;
            if (!settings.isEmpty()) {
                settingsFile = new File("settings-" + settings + ".xml");
            } else {
                settingsFile = new File("settings.xml");
            }

            if (!settingsFile.isAbsolute()) {
                String settingsDir = System.getProperty("maven.it.global-settings.dir", "");
                if (!settingsDir.isEmpty()) {
                    settingsFile = new File(settingsDir, settingsFile.getPath());
                } else {
                    //
                    // Make is easier to run ITs from m2e in Maven IT mode without having to set any additional
                    // properties.
                    //
                    settingsFile = new File("target/test-classes", settingsFile.getPath());
                }
            }

            String path = settingsFile.getAbsolutePath();
            verifier.addCliArgument("--install-settings");
            if (path.indexOf(' ') < 0) {
                verifier.addCliArgument(path);
            } else {
                verifier.addCliArgument('"' + path + '"');
            }
        }

        // auto set source+target to lowest reasonable java version
        verifier.getSystemProperties().put("maven.compiler.source", "8");
        verifier.getSystemProperties().put("maven.compiler.target", "8");
        verifier.getSystemProperties().put("maven.compiler.release", "8");

        return verifier;
    }

    public static void fail(String message) {
        org.junit.jupiter.api.Assertions.fail(message);
    }
}
