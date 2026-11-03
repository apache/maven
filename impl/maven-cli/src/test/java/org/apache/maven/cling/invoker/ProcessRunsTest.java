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
package org.apache.maven.cling.invoker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessRunsTest {

    private final long pid = ProcessHandle.current().pid();

    private final Instant started =
            ProcessHandle.current().info().startInstant().orElseGet(Instant::now);

    @Test
    void listsTheRunningProcess(@TempDir Path runsDir) {
        ProcessRuns.install(runsDir, pid, started, "4.1.0", Path.of("work"), Path.of("root"));

        List<ProcessRuns.Run> runs = ProcessRuns.listAlive(runsDir);

        assertEquals(1, runs.size());
        assertEquals(pid, runs.get(0).pid);
        assertEquals("4.1.0", runs.get(0).version);
        assertTrue(ProcessRuns.format(runs).contains(Long.toString(pid)));
    }

    @Test
    void forgetsAProcessWhichIsGone(@TempDir Path runsDir) throws Exception {
        ProcessRuns.install(runsDir, pid, started, "4.1.0", null, null);
        ProcessRuns.uninstall(runsDir, pid);

        assertTrue(ProcessRuns.listAlive(runsDir).isEmpty());
        try (var files = Files.list(runsDir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void dropsTheDescriptorOfAPidTakenByAnotherProcess(@TempDir Path runsDir) throws Exception {
        // the pid is alive, but it was not started when the descriptor says
        ProcessRuns.install(runsDir, pid, Instant.parse("2000-01-01T00:00:00Z"), "4.1.0", null, null);

        assertTrue(ProcessRuns.listAlive(runsDir).isEmpty());
        try (var files = Files.list(runsDir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void dropsTheDescriptorOfAnUnknownPid(@TempDir Path runsDir) {
        ProcessRuns.install(runsDir, Long.MAX_VALUE, started, "4.1.0", null, null);

        assertTrue(ProcessRuns.listAlive(runsDir).isEmpty());
    }

    @Test
    void ignoresAnUnreadableDescriptor(@TempDir Path runsDir) throws Exception {
        Files.writeString(runsDir.resolve("mvn-1.properties"), "pid=not-a-number");

        assertTrue(ProcessRuns.listAlive(runsDir).isEmpty());
    }

    @Test
    void ignoresAMissingDirectory(@TempDir Path runsDir) {
        assertTrue(ProcessRuns.listAlive(runsDir.resolve("missing")).isEmpty());
    }

    @Test
    void formatsColumnsAsWideAsTheirValues() {
        String longDir = "/very/long/working/directory/which/is/well/over/forty-eight/characters/long";
        String table = ProcessRuns.format(List.of(
                new ProcessRuns.Run(7, "4.1.0", longDir, "/root", "2026-10-14T10:00:00Z"),
                new ProcessRuns.Run(123456, "4.1.0-SNAPSHOT", "/w", "/root2", "2026-10-14T10:00:00Z")));

        String[] lines = table.split(System.lineSeparator());

        assertEquals(3, lines.length);
        assertTrue(lines[1].contains(longDir), "a long value is not cut off");
        int execRoot = lines[0].indexOf("EXEC_ROOT");
        assertEquals(execRoot, lines[1].indexOf("/root"));
        assertEquals(execRoot, lines[2].indexOf("/root2"));
        for (String line : lines) {
            assertEquals(line.trim(), line, "no leading or trailing white space");
        }
    }

    @Test
    void formatsAnEmptyList() {
        String text = ProcessRuns.format(List.of());

        assertEquals("No running Maven processes.", text);
        assertFalse(text.endsWith(System.lineSeparator()));
    }
}
