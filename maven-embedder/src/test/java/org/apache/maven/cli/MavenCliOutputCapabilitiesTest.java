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
package org.apache.maven.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.logging.OutputCapabilities.Destination;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenCliOutputCapabilitiesTest {
    @TempDir
    Path directory;

    private PrintStream out;
    private PrintStream err;
    private Properties properties;

    @BeforeEach
    void save() {
        out = System.out;
        err = System.err;
        properties = (Properties) System.getProperties().clone();
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
    }

    @AfterEach
    void restore() {
        if (System.out != out) {
            System.out.close();
        }
        System.setOut(out);
        System.setErr(err);
        System.setProperties(properties);
        org.slf4j.simple.MavenSlf4jSimpleFriend.init();
    }

    @Test
    void logFileWithForcedColorStillReportsFile() throws Exception {
        MavenCli cli = new MavenCli();
        CliRequest request = new CliRequest(
                new String[] {"-l", directory.resolve("build.log").toString(), "--color=always"}, null);
        cli.cli(request);
        cli.logging(request);
        assertEquals(Destination.FILE, request.outputCapabilities.get().getDestination());
        assertEquals(
                Optional.of(Charset.defaultCharset()),
                request.outputCapabilities.get().getEncoding());
    }

    @Test
    void failedLogFileDoesNotClaimFile() throws Exception {
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        MavenCli cli = new MavenCli();
        CliRequest request = new CliRequest(new String[] {"-l", directory.toString()}, null);
        cli.cli(request);
        cli.logging(request);
        assertEquals(Destination.UNKNOWN, request.outputCapabilities.get().getDestination());
        assertEquals(Optional.empty(), request.outputCapabilities.get().getEncoding());
    }
}
