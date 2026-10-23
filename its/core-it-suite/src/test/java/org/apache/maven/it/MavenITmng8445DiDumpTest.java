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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.regex.Pattern;

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
    private static final String MAVEN_DI_DEBUG = "maven.di.debug";
    private static final String SISU_DEBUG = "sisu.debug";
    private static final Pattern REVISION = Pattern.compile(" revision=([0-9]+)(?:->([0-9]+))?");
    private static final Pattern LOADER = Pattern.compile("    (L[0-9]+): (.+)");
    private static final String SISU_DUMP_MARKER = "-----[explicit bindings]";
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
                new BuilderCase("eight-threads", List.of("-T", "8"), true));
        // Exercise flag independence with both builders; output modes only need diagnostics enabled.
        Stream<DynamicTest> flags = builders.stream()
                .flatMap(builder -> Stream.of("false", "true")
                        .flatMap(nativeMode -> Stream.of(false, true)
                                .map(sisuEnabled -> dynamicTest(
                                        builder.name() + " native=" + nativeMode + " sisu=" + sisuEnabled,
                                        () -> verifyDump(
                                                directory.resolve("project"), builder, nativeMode, sisuEnabled)))));
        List<BuilderCase> outputModes = List.of(
                new BuilderCase("raw-streams", List.of("--raw-streams", "-T", "2"), true),
                new BuilderCase("quiet", List.of("-q", "-T", "2"), true));
        return Stream.concat(flags, outputModes.stream().map(builder -> dynamicTest(
                builder.name(), () -> verifyDump(directory.resolve("project"), builder, "true", true))));
    }

    private void verifyDump(Path project, BuilderCase builder, String nativeMode, boolean sisuEnabled)
            throws Exception {
        boolean nativeEnabled = !"false".equals(nativeMode);
        Verifier verifier = newVerifier(project);
        String logfile = "build-" + builder.name() + "-native-" + nativeMode + "-sisu-" + sisuEnabled + ".txt";
        verifier.setLogFileName(logfile);
        verifier.setForkJvm(true);
        verifier.addJvmArgument("-D" + MAVEN_DI_DEBUG + "=" + nativeMode);
        verifier.addJvmArgument("-D" + SISU_DEBUG + "=" + sisuEnabled);
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
        List<String> stdoutLines = verifier.getStdout().lines().toList();
        dumps += collectNativeRows(stdoutLines, nativeRows);
        List<String> outputLines = Stream.concat(lines.stream(), stdoutLines.stream()).toList();
        boolean sisuDump = outputLines.stream().anyMatch(line -> line.contains(SISU_DUMP_MARKER));
        assertEquals(sisuEnabled, sisuDump, "Sisu dump must depend only on sisu.debug");

        if (nativeEnabled) {
            // Each module creates its own Mojo injector.
            assertTrue(dumps >= MODULE_PATHS.size());
            assertTrue(nativeRows.stream().anyMatch(line -> line.contains("mng8445.CheckMojo")));
            assertTrue(nativeRows.stream().anyMatch(line -> line.contains("CheckMojo$Unused")));
            assertTrue(nativeRows.stream().anyMatch(line -> line.contains("Unused.provide")));
            assertTrue(hasInstanceBinding(nativeRows, "org.apache.maven.api.Session"));
            assertTrue(nativeRows.stream().anyMatch(line -> line.contains("field injector:") && line.contains(".session")));
        } else {
            assertEquals(0, dumps);
        }
    }

    private static boolean hasInstanceBinding(List<String> lines, String type) {
        boolean matchingKey = false;
        for (String original : lines) {
            String line = original.replaceFirst("^(  |    )[+~-] ", "$1");
            if (line.startsWith("  key: ")) {
                matchingKey = line.startsWith("  key: " + type + " [loader=");
            } else if (matchingKey && line.startsWith("      source: instance ")) {
                return true;
            }
        }
        return false;
    }

    private static int collectNativeRows(List<String> lines, List<String> rows) {
        String headerPrefix = "Maven DI native registry ";
        String footerPrefix = "} " + headerPrefix;
        String stdoutPrefix = "[stdout] ";
        Map<String, Long> revisions = new HashMap<>();
        Map<String, Map<String, String>> loaders = new HashMap<>();
        String header = null;
        int dumps = 0;
        for (String original : lines) {
            String plain = original;
            int decoration = plain.indexOf(stdoutPrefix);
            String line = decoration < 0 ? plain : plain.substring(decoration + stdoutPrefix.length());
            if (line.startsWith(headerPrefix)) {
                assertTrue(header == null, "Interleaved native dumps");
                header = line.split(" revision=")[0].substring(headerPrefix.length());
                var revision = REVISION.matcher(line);
                assertTrue(revision.find(), line);
                long before = Long.parseLong(revision.group(1));
                if (revision.group(2) == null) {
                    revisions.put(header, before);
                    loaders.put(header, new HashMap<>());
                } else {
                    assertEquals(revisions.get(header), Long.valueOf(before), "Delta must follow the preceding dump");
                    long after = Long.parseLong(revision.group(2));
                    assertTrue(after >= before, line);
                    revisions.put(header, after);
                }
                dumps++;
            } else if (line.startsWith(footerPrefix)) {
                assertEquals(header, line.substring(footerPrefix.length()));
                header = null;
            } else if (header != null) {
                rows.add(line);
                var loader = LOADER.matcher(line);
                if (loader.matches()) {
                    String previous = loaders.get(header).putIfAbsent(loader.group(1), loader.group(2));
                    if (previous != null) {
                        assertEquals(previous, loader.group(2), "Loader labels must retain their identity");
                    }
                }
            }
        }
        assertTrue(header == null, "Incomplete native dump");
        return dumps;
    }

    private record BuilderCase(String name, List<String> arguments, boolean parallel) {}
}
