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
package org.apache.maven.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleAccessTest {
    @TempDir
    Path directory;

    @Test
    void mavenConsumersCanCompileAndRunOnTheModulePath() throws Exception {
        for (String module : List.of(
                "org.apache.maven.impl",
                "org.apache.maven.cling",
                "org.apache.maven.core",
                "maven.compat",
                "maven.embedder",
                "maven.model.builder")) {
            Path source = createConsumer(module);
            Path output = directory.resolve(module).resolve("classes");
            ToolResult compilation = compile(source, output);
            assertEquals(0, compilation.exitCode(), compilation.output());

            ToolResult execution = runTool(
                    "java", "--module-path", utilities() + File.pathSeparator + output, "-m", module + "/probe.Probe");
            assertEquals(0, execution.exitCode(), execution.output());
            assertEquals(Os.OS_FAMILY, execution.output().strip());
        }
    }

    @Test
    void unrelatedModulesCannotAccessUtilities() throws Exception {
        Path source = createConsumer("org.apache.maven.unrelated");
        ToolResult compilation = compile(source, directory.resolve("unrelated-classes"));
        assertNotEquals(0, compilation.exitCode(), "An unrelated module must not be able to access Maven utilities");
        assertTrue(compilation.output().contains("org.apache.maven.utils"), compilation.output());
    }

    private Path createConsumer(String module) throws Exception {
        Path source = Files.createDirectories(directory.resolve(module).resolve("src"));
        Files.writeString(
                source.resolve("module-info.java"), "module " + module + " { requires org.apache.maven.utils; }");
        Path probe = Files.createDirectories(source.resolve("probe"));
        Files.writeString(probe.resolve("Probe.java"), """
                package probe;
                public class Probe {
                    public static void main(String[] args) {
                        System.out.print(org.apache.maven.utils.Os.OS_FAMILY);
                    }
                }
                """);
        return source;
    }

    private ToolResult compile(Path source, Path output) throws Exception {
        return runTool(
                "javac",
                "--release",
                "17",
                "--module-path",
                utilities().toString(),
                "-d",
                output.toString(),
                source.resolve("module-info.java").toString(),
                source.resolve("probe/Probe.java").toString());
    }

    private Path utilities() throws Exception {
        return Path.of(
                Os.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private ToolResult runTool(String tool, String... arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", tool).toString());
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), tool + " timed out");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ToolResult(process.exitValue(), output);
        } finally {
            process.destroyForcibly();
        }
    }

    private record ToolResult(int exitCode, String output) {}
}
