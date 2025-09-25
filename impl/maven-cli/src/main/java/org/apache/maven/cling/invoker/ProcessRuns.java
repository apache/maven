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

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.Properties;

public final class ProcessRuns {

    private static final String RUNS_SUBDIR = ".m2/.maven/runs";
    private static final String FILE_PREFIX = "mvn-";
    private static final String FILE_SUFFIX = ".properties";

    private static final String KEY_PID = "pid";
    private static final String KEY_VERSION = "version";
    private static final String KEY_WORKDIR = "workDir";
    private static final String KEY_EXECROOT = "execRoot";
    private static final String KEY_STARTED = "started";

    private static Path runsDir() {
        final String home = System.getProperty("user.home", ".");
        return Paths.get(home).resolve(RUNS_SUBDIR);
    }

    private static Path desc(final long pid) {
        return runsDir().resolve(FILE_PREFIX + pid + FILE_SUFFIX);
    }

    public static void install(final long pid, final String version, final Path workDir, final Path execRoot) {
        try {
            Files.createDirectories(runsDir());
            final Properties p = new Properties();
            p.setProperty(KEY_PID, Long.toString(pid));
            p.setProperty(KEY_VERSION, version == null ? "-" : version);
            p.setProperty(
                    KEY_WORKDIR,
                    workDir == null ? "?" : workDir.toAbsolutePath().toString());
            p.setProperty(
                    KEY_EXECROOT,
                    execRoot == null ? "?" : execRoot.toAbsolutePath().toString());
            p.setProperty(KEY_STARTED, Instant.now().toString());
            try (Writer w = Files.newBufferedWriter(desc(pid), StandardCharsets.UTF_8)) {
                p.store(w, "mvn run");
            }
        } catch (final Exception ignored) {
            // best-effort
        }
    }

    public static void uninstall(final long pid) {
        try {
            Files.deleteIfExists(desc(pid));
        } catch (final Exception ignored) {
            // best-effort
        }
    }

    private static final class Run {
        final long pid;
        final String version;
        final String workDir;
        final String execRoot;
        final String started;

        Run(final long pid, final String version, final String workDir, final String execRoot, final String started) {
            this.pid = pid;
            this.version = version;
            this.workDir = workDir;
            this.execRoot = execRoot;
            this.started = started;
        }
    }

    public static java.util.List<Run> listAlive() {
        final Path dir = runsDir();
        if (!Files.isDirectory(dir)) {
            return java.util.List.of();
        }
        final java.util.List<Run> out = new java.util.ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, FILE_PREFIX + "*" + FILE_SUFFIX)) {
            for (final Path f : ds) {
                final Properties p = new Properties();
                try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    p.load(r);
                }
                final long pid = parseLong(p.getProperty(KEY_PID), -1L);
                final boolean alive = pid > 0
                        && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
                if (alive) {
                    out.add(new Run(
                            pid,
                            p.getProperty(KEY_VERSION, "-"),
                            p.getProperty(KEY_WORKDIR, "?"),
                            p.getProperty(KEY_EXECROOT, "?"),
                            p.getProperty(KEY_STARTED, "?")));
                } else {
                    try {
                        Files.deleteIfExists(f);
                    } catch (final Exception ignored) {
                    }
                }
            }
        } catch (final Exception ignored) {
            // return what we gathered
        }
        out.sort(Comparator.comparing((Run r) -> r.started).thenComparingLong(r -> r.pid));
        return out;
    }

    public static String format(final java.util.List<Run> runs) {
        final String nl = System.lineSeparator();
        if (runs.isEmpty()) {
            return "No running Maven processes." + nl;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "%-10s %-12s %-24s %-48s %-48s%s", "PID", "VERSION", "STARTED", "WORKDIR", "EXEC_ROOT", nl));
        for (final Run r : runs) {
            sb.append(String.format(
                    "%-10d %-12s %-24s %-48s %-48s%s",
                    r.pid, r.version, r.started, truncateEnd(r.workDir, 48), truncateEnd(r.execRoot, 48), nl));
        }
        return sb.toString();
    }

    private static long parseLong(final String s, final long dflt) {
        try {
            return Long.parseLong(s);
        } catch (final Exception e) {
            return dflt;
        }
    }

    private static String truncateEnd(final String s, final int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "?" : s;
        }
        return "…" + s.substring(Math.max(0, s.length() - (max - 1)));
    }
}
