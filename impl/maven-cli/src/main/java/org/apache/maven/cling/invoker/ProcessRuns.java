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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Registry of the running Maven processes of the current user, kept as one descriptor file per process.
 * All the operations are best effort: a registry which cannot be read or written never fails a build.
 */
public final class ProcessRuns {

    private static final String RUNS_SUBDIR = ".m2/.maven/runs";
    private static final String FILE_PREFIX = "mvn-";
    private static final String FILE_SUFFIX = ".properties";

    private static final String KEY_PID = "pid";
    private static final String KEY_VERSION = "version";
    private static final String KEY_WORKDIR = "workDir";
    private static final String KEY_EXECROOT = "execRoot";
    private static final String KEY_STARTED = "started";

    /** The start time of a process is not reported with the same precision all the time. */
    private static final Duration START_TOLERANCE = Duration.ofSeconds(2);

    private ProcessRuns() {}

    private static Path defaultRunsDir() {
        return Paths.get(System.getProperty("user.home", ".")).resolve(RUNS_SUBDIR);
    }

    private static Path descriptor(Path runsDir, long pid) {
        return runsDir.resolve(FILE_PREFIX + pid + FILE_SUFFIX);
    }

    /**
     * Registers the current process.
     */
    public static void install(String version, Path workDir, Path execRoot) {
        ProcessHandle current = ProcessHandle.current();
        install(
                defaultRunsDir(),
                current.pid(),
                current.info().startInstant().orElseGet(Instant::now),
                version,
                workDir,
                execRoot);
    }

    /**
     * Removes the registration of the process.
     */
    public static void uninstall(long pid) {
        uninstall(defaultRunsDir(), pid);
    }

    /**
     * Describes the registered processes which are still running.
     *
     * @return a table of the processes, one line per process, without any trailing line separator
     */
    public static String listAlive() {
        return format(listAlive(defaultRunsDir()));
    }

    static void install(Path runsDir, long pid, Instant started, String version, Path workDir, Path execRoot) {
        try {
            Files.createDirectories(runsDir);
            Properties properties = new Properties();
            properties.setProperty(KEY_PID, Long.toString(pid));
            properties.setProperty(KEY_VERSION, version == null ? "-" : version);
            properties.setProperty(
                    KEY_WORKDIR,
                    workDir == null ? "?" : workDir.toAbsolutePath().toString());
            properties.setProperty(
                    KEY_EXECROOT,
                    execRoot == null ? "?" : execRoot.toAbsolutePath().toString());
            properties.setProperty(KEY_STARTED, started.toString());
            // written aside and moved in place, so a reader never sees a partial descriptor
            Path target = descriptor(runsDir, pid);
            Path temp = Files.createTempFile(runsDir, FILE_PREFIX + pid + "-", ".tmp");
            try {
                try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                    properties.store(writer, "mvn run");
                }
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException | RuntimeException ignored) {
            // best effort
        }
    }

    static void uninstall(Path runsDir, long pid) {
        try {
            Files.deleteIfExists(descriptor(runsDir, pid));
        } catch (IOException ignored) {
            // best effort
        }
    }

    static List<Run> listAlive(Path runsDir) {
        List<Run> runs = new ArrayList<>();
        if (!Files.isDirectory(runsDir)) {
            return runs;
        }
        try (DirectoryStream<Path> descriptors = Files.newDirectoryStream(runsDir, FILE_PREFIX + "*" + FILE_SUFFIX)) {
            for (Path file : descriptors) {
                Optional<Run> run = read(file);
                if (run.isPresent() && isRunning(run.get())) {
                    runs.add(run.get());
                } else {
                    // stale: the process is gone, or its pid was taken by another process since
                    Files.deleteIfExists(file);
                }
            }
        } catch (IOException ignored) {
            // return what was gathered
        }
        runs.sort(Comparator.comparing((Run run) -> run.started).thenComparingLong(run -> run.pid));
        return runs;
    }

    private static Optional<Run> read(Path file) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return Optional.of(new Run(
                    Long.parseLong(properties.getProperty(KEY_PID)),
                    properties.getProperty(KEY_VERSION, "-"),
                    properties.getProperty(KEY_WORKDIR, "?"),
                    properties.getProperty(KEY_EXECROOT, "?"),
                    properties.getProperty(KEY_STARTED, "?")));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Tells whether the process of the descriptor is running. A pid alone is not enough, as the system hands it over
     * to another process once the first one is gone, so the start time recorded has to be the one of the process too.
     */
    private static boolean isRunning(Run run) {
        Optional<ProcessHandle> process = ProcessHandle.of(run.pid).filter(ProcessHandle::isAlive);
        if (process.isEmpty()) {
            return false;
        }
        Optional<Instant> actualStart = process.get().info().startInstant();
        if (actualStart.isEmpty()) {
            // the system does not tell, nothing to compare with
            return true;
        }
        try {
            return Duration.between(Instant.parse(run.started), actualStart.get())
                            .abs()
                            .compareTo(START_TOLERANCE)
                    <= 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Formats the runs as a table with a header line. Every column but the last one is as wide as its widest
     * value, so that the output does not depend on a fixed width and no value is cut off.
     *
     * @param runs the runs to show
     * @return the table, or a message when there is no run
     */
    static String format(List<Run> runs) {
        if (runs.isEmpty()) {
            return "No running Maven processes.";
        }
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"PID", "VERSION", "STARTED", "WORKDIR", "EXEC_ROOT"});
        for (Run run : runs) {
            rows.add(new String[] {Long.toString(run.pid), run.version, run.started, run.workDir, run.execRoot});
        }
        int[] widths = new int[rows.get(0).length - 1];
        for (String[] row : rows) {
            for (int i = 0; i < widths.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            for (int i = 0; i < widths.length; i++) {
                sb.append(row[i]);
                for (int pad = row[i].length(); pad < widths[i] + 1; pad++) {
                    sb.append(' ');
                }
            }
            // the last column is not padded, so that lines carry no trailing white space
            sb.append(row[widths.length]);
        }
        return sb.toString();
    }

    static final class Run {
        final long pid;
        final String version;
        final String workDir;
        final String execRoot;
        final String started;

        Run(long pid, String version, String workDir, String execRoot, String started) {
            this.pid = pid;
            this.version = version;
            this.workDir = workDir;
            this.execRoot = execRoot;
            this.started = started;
        }
    }
}
