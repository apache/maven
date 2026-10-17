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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Execution(ExecutionMode.SAME_THREAD)
class MavenITmng8445DiDumpTest extends AbstractMavenIntegrationTestCase {
    private static final List<String> MODULE_PATHS = List.of("", "a/", "b/");

    @TestFactory
    Stream<DynamicTest> dumpIsPassiveWithSequentialAndParallelBuilders() throws Exception {
        Path directory = extractResources("mng-8445-di-dump");
        Verifier plugin = newVerifier(directory.resolve("plugin"));
        plugin.addCliArgument("install");
        plugin.execute();
        plugin.verifyErrorFreeLog();

        List<BuilderCase> builders = List.of(
                new BuilderCase("default", List.of(), false),
                new BuilderCase("eight-threads", List.of("-T", "8"), true),
                new BuilderCase("raw-streams", List.of("--raw-streams", "-T", "2"), true),
                new BuilderCase("quiet", List.of("-q", "-T", "2"), true));
        return builders.stream()
                .flatMap(builder -> Stream.of(false, true)
                        .map(enabled -> dynamicTest(
                                builder.name() + " diagnostics=" + enabled,
                                () -> verifyDump(directory.resolve("project"), builder, enabled))));
    }

    private void verifyDump(Path project, BuilderCase builder, boolean enabled) throws Exception {
        Verifier verifier = newVerifier(project);
        String logfile = "build-" + builder.name() + "-" + enabled + ".txt";
        verifier.setLogFileName(logfile);
        verifier.setForkJvm(true);
        verifier.addJvmArgument("-Dsisu.debug=" + enabled);
        verifier.addCliArguments(builder.arguments().toArray(String[]::new));
        verifier.addCliArgument("validate");
        for (String module : MODULE_PATHS) {
            Files.deleteIfExists(project.resolve(module + "target/di-check.txt"));
        }
        verifier.execute();
        verifier.verifyErrorFreeLog();
        List<String> lines = verifier.loadLines(logfile);
        List<String> workerIds = new ArrayList<>();
        for (String module : MODULE_PATHS) {
            workerIds.addAll(verifier.loadLines(module + "target/di-check.txt"));
        }
        assertEquals(MODULE_PATHS.size(), workerIds.size());
        if (builder.parallel()) {
            assertTrue(
                    workerIds.subList(1, workerIds.size()).stream().distinct().count() > 1,
                    "Expected more than one Maven worker");
        }
        List<String> nativeRows = new ArrayList<>();
        int dumps = collectNativeRows(lines, nativeRows);
        Path stdout = project.resolve(logfile.replace(".txt", "-stdout.txt"));
        if (Files.isRegularFile(stdout)) {
            dumps += collectNativeRows(Files.readAllLines(stdout), nativeRows);
        }
        if (enabled) {
            // Each module creates its own Mojo injector.
            assertTrue(dumps >= MODULE_PATHS.size());
            assertTrue(nativeRows.stream().anyMatch(line -> line.contains("mng8445.CheckMojo")));
            assertTrue(nativeRows.stream().anyMatch(line -> line.contains("CheckMojo$Unused")));
            assertTrue(nativeRows.stream().anyMatch(line -> line.contains("Unused.provide")));
            assertTrue(nativeRows.stream()
                    .anyMatch(line ->
                            line.contains("source=instance ") && line.contains("org.apache.maven.api.Session")));
        } else {
            assertEquals(0, dumps);
            assertFalse(lines.stream().anyMatch(line -> line.startsWith("  scope ")));
        }
    }

    private static int collectNativeRows(List<String> lines, List<String> rows) {
        String headerPrefix = "Maven DI native registry ";
        String footerPrefix = "} " + headerPrefix;
        String stdoutPrefix = "[stdout] ";
        String header = null;
        int dumps = 0;
        for (String original : lines) {
            int decoration = original.indexOf(stdoutPrefix);
            String line = decoration < 0 ? original : original.substring(decoration + stdoutPrefix.length());
            if (line.startsWith(headerPrefix)) {
                assertTrue(header == null, "Interleaved native dumps");
                header = line.split(" revision=")[0].substring(headerPrefix.length());
                dumps++;
            } else if (line.startsWith(footerPrefix)) {
                assertEquals(header, line.substring(footerPrefix.length()));
                header = null;
            } else if (header != null) {
                rows.add(line);
            }
        }
        assertTrue(header == null, "Incomplete native dump");
        return dumps;
    }

    private record BuilderCase(String name, List<String> arguments, boolean parallel) {}
}
