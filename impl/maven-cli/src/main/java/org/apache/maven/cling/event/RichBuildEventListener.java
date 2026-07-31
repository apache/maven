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
package org.apache.maven.cling.event;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.LogLevel;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.logging.BuildEventListener;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.transfer.TransferEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.Display;

/**
 * A rich terminal build event listener using JLine's {@link Display} in
 * non-fullscreen mode — the same approach as mvnd.
 * <p>
 * The status area is rendered at the current cursor position using
 * {@link Display#updateAnsi}. When log output arrives, the display is
 * cleared (updated with empty lines), the log line is printed normally,
 * and then the status is redrawn below it. JLine handles all the cursor
 * math (moving up, erasing changed lines, etc.) and only repaints what
 * actually changed.
 * <p>
 * At the end of the build, the display is cleared and nothing remains
 * on screen — the summary then prints as normal scrolling text.
 * <p>
 * The status area has a fixed height based on the degree of concurrency,
 * so the separator and summary line stay anchored at the bottom. Active
 * projects are packed to the top of the slot area; empty lines fill the
 * gap between the last active project and the separator.
 * <p>
 * Falls back to simple log passthrough on dumb terminals.
 *
 * @since 4.1.0
 * @see PlainExecutionEventLogger
 * @see ExecutionEventLogger
 */
public class RichBuildEventListener implements BuildEventListener {

    // ---- ANSI colors ----

    private static final String ESC = "\033[";
    private static final String CYAN = ESC + "36m";
    private static final String YELLOW = ESC + "33m";
    private static final String BLUE = ESC + "34m";
    private static final String GREEN = ESC + "32m";
    private static final String RED = ESC + "31m";
    private static final String BOLD = ESC + "1m";
    private static final String DIM = ESC + "2m";
    private static final String RESET = ESC + "0m";

    // ---- Terminal & output ----

    private final Terminal terminal;
    private final PrintWriter writer;
    private final boolean supported;

    // ---- JLine Display ----

    /** JLine display in non-fullscreen mode — handles cursor math. */
    private volatile Display display;
    /** Whether the display is currently active. */
    private volatile boolean displayActive;
    /** Fixed number of lines in the status area (set once in initReactor). */
    private volatile int statusHeight;

    // ---- Reactor state ----

    private volatile int totalProjects;
    private volatile int completedProjects;
    private volatile Instant buildStartTime;
    /** One-line header shown at the top of the status area. */
    private volatile String headerLine;

    // ---- Project display ----

    private final Map<String, ProjectState> activeProjects = new ConcurrentHashMap<>();
    private final List<String> projectOrder = new ArrayList<>();
    private final Map<String, String> projectNames = new ConcurrentHashMap<>();

    // ---- Active downloads ----

    private final Map<String, TransferInfo> activeTransfers = new ConcurrentHashMap<>();

    // ---- Synchronization ----

    /** Guards all terminal output and slot mutations. */
    private final Object outputLock = new Object();

    // ---- Periodic refresh ----

    /** Scheduler for 1-second display refresh so elapsed timers stay live. */
    private volatile ScheduledExecutorService refreshScheduler;
    /** Handle for the periodic refresh task. */
    private volatile ScheduledFuture<?> refreshFuture;

    // ---- Warning tracking ----

    /** Number of WARN-level messages seen during the build. */
    private final AtomicInteger warningCount = new AtomicInteger();

    /** Number of ERROR-level messages seen during the build. */
    private final AtomicInteger errorCount = new AtomicInteger();

    // ---- Constructor ----

    /**
     * Creates a new RichBuildEventListener.
     *
     * @param terminal the JLine terminal for output
     * @param output   fallback output consumer (unused — kept for API compat)
     */
    public RichBuildEventListener(Terminal terminal, java.util.function.Consumer<String> output) {
        this.terminal = terminal;
        this.writer = terminal.writer();
        // Support ANSI if terminal type is not "dumb" and has reasonable size
        String type = terminal.getType();
        this.supported = type != null && !Terminal.TYPE_DUMB.equals(type) && terminal.getWidth() > 0;
    }

    // ---- Reactor lifecycle ----

    /**
     * Initialize reactor state from the session. Called by {@link RichExecutionEventLogger}
     * during {@code sessionStarted}.
     */
    public void initReactor(MavenSession session) {
        List<MavenProject> allProjects = session.getAllProjects();
        List<MavenProject> projects = session.getProjects();

        this.totalProjects = allProjects.size();
        this.completedProjects = allProjects.size() - projects.size();
        this.buildStartTime = MonotonicClock.now();

        for (MavenProject project : allProjects) {
            projectOrder.add(project.getArtifactId());
            projectNames.put(project.getArtifactId(), project.getName());
        }

        // Build header line
        this.headerLine = buildHeaderLine(session);

        // Slot count = degree of concurrency (capped for sanity)
        int concurrency = 1;
        try {
            concurrency = Math.max(1, session.getRequest().getDegreeOfConcurrency());
        } catch (Exception e) {
            // fallback to 1
        }
        int slotCount = Math.min(concurrency, 8);
        // Fixed height: 1 header + N project slots + 1 separator + 1 summary
        this.statusHeight = slotCount + 3;

        if (supported) {
            setupDisplay();
        }
    }

    private String buildHeaderLine(MavenSession session) {
        StringBuilder h = new StringBuilder();
        h.append(' ').append(BOLD);

        // Maven version
        String mavenVersion = null;
        if (session.getSystemProperties() != null) {
            mavenVersion = session.getSystemProperties().getProperty("maven.version");
        }
        if (mavenVersion != null) {
            h.append("Maven ").append(mavenVersion);
        } else {
            h.append("Maven");
        }
        h.append(RESET);

        // Project name
        MavenProject top = session.getTopLevelProject();
        if (top != null) {
            h.append(DIM).append(" ─ ").append(RESET);
            h.append("building ");
            h.append(CYAN).append(top.getName()).append(RESET);
            if (top.getVersion() != null) {
                h.append(' ').append(DIM).append(top.getVersion()).append(RESET);
            }
        }

        // Goals
        List<String> goals = session.getGoals();
        if (goals != null && !goals.isEmpty()) {
            h.append(DIM).append(" ─ ").append(RESET);
            h.append(YELLOW).append(String.join(" ", goals)).append(RESET);
        }

        return h.toString();
    }

    /**
     * Set up the JLine Display in non-fullscreen mode.
     */
    private void setupDisplay() {
        synchronized (outputLock) {
            display = new Display(terminal, false);
            display.resize(statusHeight, terminal.getWidth());
            displayActive = true;
            display.updateAnsi(buildStatusLines(), 0);
        }

        // Start a 1-second periodic refresh so that elapsed-time counters
        // stay live even when no build events are arriving (e.g. during
        // a slow mojo execution with no log output).
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "maven-rich-display-refresh");
            t.setDaemon(true);
            return t;
        });
        executor.setRemoveOnCancelPolicy(true);
        refreshScheduler = executor;
        refreshFuture = refreshScheduler.scheduleAtFixedRate(this::redraw, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Tear down the status display. Called by {@link RichExecutionEventLogger}
     * during {@code sessionEnded} before printing the summary.
     * <p>
     * The flush at the end is critical: {@link Display} writes through
     * {@link Terminal#writer()} (a {@code PrintWriter} that does not auto-flush),
     * while subsequent log output from SLF4J goes through {@code System.out}
     * (which <em>does</em> auto-flush on {@code println}). Without the flush,
     * the clear sequences sit in the writer's buffer while the summary text
     * reaches the terminal first via {@code System.out} — then the belated
     * clear erases the summary the user was supposed to see.
     */
    public void tearDown() {
        // Stop the periodic refresh first (outside outputLock to avoid deadlock)
        if (refreshFuture != null) {
            refreshFuture.cancel(false);
            refreshFuture = null;
        }
        if (refreshScheduler != null) {
            refreshScheduler.shutdownNow();
            refreshScheduler = null;
        }

        synchronized (outputLock) {
            if (!displayActive) {
                return;
            }
            displayActive = false;

            // Clear the display area: update with empty lines, cursor at top
            display.updateAnsi(Collections.nCopies(statusHeight, ""), 0);
            // Erase from cursor to end of screen — removes any leftover artifacts
            writer.print("\033[J");
            // Flush immediately so the clear reaches the terminal BEFORE
            // any subsequent log output that goes through System.out
            writer.flush();
        }
    }

    // ---- BuildEventListener interface ----

    @Override
    public void sessionStarted(ExecutionEvent event) {
        // Reactor init is handled via initReactor() called from RichExecutionEventLogger
    }

    @Override
    public void projectStarted(String projectId) {
        activeProjects.put(projectId, new ProjectState(projectId, MonotonicClock.now()));
        redraw();
    }

    @Override
    public void projectFinished(String projectId) {
        activeProjects.remove(projectId);
        completedProjects++;
        redraw();
    }

    @Override
    public void projectLogMessage(String projectId, LogEvent event) {
        // In rich mode, suppress INFO/DEBUG/TRACE/WARN — the status bar provides
        // live progress and warnings are summarized at the end of the build.
        // Only ERROR passes through to the terminal immediately.
        if (event.level() == LogLevel.WARN) {
            warningCount.incrementAndGet();
            return;
        }
        if (event.level() == LogLevel.INFO || event.level() == LogLevel.DEBUG || event.level() == LogLevel.TRACE) {
            return;
        }
        if (event.level() == LogLevel.ERROR) {
            errorCount.incrementAndGet();
        }
        String output = event.formattedMessage();
        if (output == null) {
            output = event.message();
        }
        printAboveStatus(output);
    }

    /**
     * Returns the number of WARN-level log messages seen during the build.
     */
    public int getWarningCount() {
        return warningCount.get();
    }

    /**
     * Returns the number of ERROR-level log messages seen during the build.
     */
    public int getErrorCount() {
        return errorCount.get();
    }

    @Override
    public void log(String msg) {
        printAboveStatus(msg);
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        String projectId = event.getProject().getArtifactId();
        ProjectState state = activeProjects.get(projectId);
        if (state != null) {
            state.currentMojo = event.getMojoExecution().getArtifactId() + ":"
                    + event.getMojoExecution().getGoal();
        }
        synchronized (outputLock) {
            if (displayActive) {
                display.updateAnsi(buildStatusLines(), 0);
            }
        }
    }

    @Override
    public void executionFailure(String projectId, boolean halted, String exception) {
        ProjectState state = activeProjects.get(projectId);
        if (state != null) {
            state.failed = true;
        }
        synchronized (outputLock) {
            if (displayActive) {
                display.updateAnsi(buildStatusLines(), 0);
            }
        }
    }

    @Override
    public void transfer(String projectId, TransferEvent event) {
        String resource = event.getResource().getResourceName();

        switch (event.getType()) {
            case INITIATED:
            case STARTED:
                String artifactName = extractArtifactName(resource);
                activeTransfers.put(
                        resource,
                        new TransferInfo(artifactName, 0, event.getResource().getContentLength()));
                redraw();
                break;
            case PROGRESSED:
                TransferInfo info = activeTransfers.get(resource);
                if (info != null) {
                    info.transferred = event.getTransferredBytes();
                    // Only update every ~50KB to avoid too-frequent redraws
                    if (info.transferred - info.lastUpdateBytes > 51200) {
                        info.lastUpdateBytes = info.transferred;
                        redraw();
                    }
                }
                break;
            case SUCCEEDED:
            case FAILED:
                activeTransfers.remove(resource);
                redraw();
                break;
            default:
                break;
        }
    }

    @Override
    public void finish(int exitCode) throws Exception {
        tearDown();
    }

    @Override
    public void fail(Throwable t) throws Exception {
        tearDown();
    }

    // ---- Display helpers ----

    /**
     * Print a message above the status area: clear the display, print
     * the message as normal scrolling text, then redraw the status below.
     */
    private void printAboveStatus(String msg) {
        synchronized (outputLock) {
            if (displayActive) {
                // Clear status area so the message prints where it was
                display.updateAnsi(Collections.nCopies(statusHeight, ""), 0);
                display.reset();
                // Print the log message (scrolls normally)
                writer.println(msg);
                writer.flush();
                // Redraw status below the new output
                display.updateAnsi(buildStatusLines(), 0);
            } else {
                writer.println(msg);
                writer.flush();
            }
        }
    }

    private void redraw() {
        synchronized (outputLock) {
            if (displayActive) {
                display.updateAnsi(buildStatusLines(), 0);
            }
        }
    }

    // ---- Status line building ----

    /**
     * Build exactly {@link #statusHeight} status lines.
     * Layout: 1 header + active projects (packed top) + empty padding + 1 separator + 1 summary.
     * The separator and summary are always at the bottom — they never move.
     */
    private List<String> buildStatusLines() {
        int termWidth = Math.max(terminal.getWidth(), 40);

        // Collect active projects sorted by start time for visual stability
        List<ProjectState> active = new ArrayList<>(activeProjects.values());
        active.sort((a, b) -> a.startTime.compareTo(b.startTime));

        // Number of project slot lines = statusHeight - 3 (header, separator, summary)
        int slotCount = statusHeight - 3;

        List<String> lines = new ArrayList<>(statusHeight);

        // Header line
        lines.add(headerLine != null ? headerLine : "");

        // Active projects packed to the top (up to slotCount)
        int projectsShown = 0;
        for (ProjectState state : active) {
            if (projectsShown >= slotCount) {
                break;
            }
            lines.add(formatProjectSlot(state));
            projectsShown++;
        }

        // Empty padding lines at the bottom of the slot area
        for (int i = projectsShown; i < slotCount; i++) {
            lines.add("");
        }

        // Separator line (always at the same position)
        lines.add(DIM + "─".repeat(Math.min(termWidth, 120)) + RESET);

        // Summary line (progress indicators + counter + elapsed + downloads)
        lines.add(buildSummaryLine(termWidth));

        return lines;
    }

    private String formatProjectSlot(ProjectState state) {
        StringBuilder b = new StringBuilder();
        if (state.failed) {
            b.append(RED).append(" ✗ ").append(RESET);
        } else {
            b.append(CYAN).append(" ● ").append(RESET);
        }
        b.append(BOLD);
        b.append(projectNames.getOrDefault(state.projectId, state.projectId));
        b.append(RESET);
        if (state.currentMojo != null) {
            b.append("  ").append(YELLOW).append(state.currentMojo).append(RESET);
        }
        Duration elapsed = Duration.between(state.startTime, MonotonicClock.now());
        b.append("  ").append(DIM).append(formatCompactDuration(elapsed)).append(RESET);
        return b.toString();
    }

    private String buildSummaryLine(int termWidth) {
        StringBuilder s = new StringBuilder(" ");

        // Module status indicators (✓ done, ● active, ○ pending)
        int maxIndicators = Math.min(projectOrder.size(), (termWidth - 40) / 3);
        if (totalProjects > 1 && maxIndicators > 0) {
            int shown = 0;
            for (String pid : projectOrder) {
                if (shown >= maxIndicators) {
                    s.append("… ");
                    break;
                }
                if (activeProjects.containsKey(pid)) {
                    ProjectState ps = activeProjects.get(pid);
                    if (ps != null && ps.failed) {
                        s.append(RED).append("✗ ").append(RESET);
                    } else {
                        s.append(YELLOW).append("● ").append(RESET);
                    }
                } else if (projectOrder.indexOf(pid) < completedProjects) {
                    s.append(GREEN).append("✓ ").append(RESET);
                } else {
                    s.append(DIM).append("○ ").append(RESET);
                }
                shown++;
            }
        }

        // Progress counter
        s.append('[');
        s.append(BOLD)
                .append(completedProjects)
                .append('/')
                .append(totalProjects)
                .append(RESET);
        s.append(']');

        // Elapsed time
        if (buildStartTime != null) {
            Duration elapsed = Duration.between(buildStartTime, MonotonicClock.now());
            s.append("  ").append(DIM).append(formatCompactDuration(elapsed)).append(RESET);
        }

        // Downloads (merged into summary line to keep height fixed)
        if (!activeTransfers.isEmpty()) {
            s.append("  ").append(BLUE).append("↓ ").append(RESET);
            if (activeTransfers.size() == 1) {
                TransferInfo ti = activeTransfers.values().iterator().next();
                s.append(ti.artifactName);
                if (ti.totalBytes > 0) {
                    s.append(' ')
                            .append(DIM)
                            .append(formatBytes(ti.transferred))
                            .append('/')
                            .append(formatBytes(ti.totalBytes))
                            .append(RESET);
                }
            } else {
                s.append(activeTransfers.size()).append(" artifacts");
            }
        }

        return s.toString();
    }

    // ---- Helpers ----

    /**
     * Truncate a string containing ANSI escape sequences to
     * {@code maxVisible} visible characters. If truncation occurs,
     * a RESET is appended to close any open styling.
     */
    static String truncateAnsi(String s, int maxVisible) {
        StringBuilder out = new StringBuilder(s.length());
        int visible = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\033') {
                // Start of escape sequence — copy through without counting
                out.append(c);
                i++;
                if (i < s.length()) {
                    char next = s.charAt(i);
                    if (next == '[') {
                        // CSI sequence: ESC [ ... <letter>
                        out.append(next);
                        i++;
                        while (i < s.length()) {
                            char cc = s.charAt(i);
                            out.append(cc);
                            i++;
                            if (Character.isLetter(cc)) {
                                break;
                            }
                        }
                    } else {
                        // Two-char escape (DECSC, DECRC, etc.)
                        out.append(next);
                        i++;
                    }
                }
            } else {
                if (visible >= maxVisible) {
                    out.append(RESET);
                    break;
                }
                out.append(c);
                visible++;
                i++;
            }
        }
        return out.toString();
    }

    private static String extractArtifactName(String resourceName) {
        if (resourceName == null) {
            return "unknown";
        }
        int lastSlash = resourceName.lastIndexOf('/');
        return lastSlash >= 0 ? resourceName.substring(lastSlash + 1) : resourceName;
    }

    private static String formatCompactDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        } else if (totalSeconds < 3600) {
            return (totalSeconds / 60) + "m " + (totalSeconds % 60) + "s";
        } else {
            return (totalSeconds / 3600) + "h " + ((totalSeconds % 3600) / 60) + "m";
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.0f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    // ---- Inner state classes ----

    private static class ProjectState {
        final String projectId;
        final Instant startTime;
        volatile String currentMojo;
        volatile boolean failed;

        ProjectState(String projectId, Instant startTime) {
            this.projectId = projectId;
            this.startTime = startTime;
        }
    }

    private static class TransferInfo {
        final String artifactName;
        final long totalBytes;
        volatile long transferred;
        volatile long lastUpdateBytes;

        TransferInfo(String artifactName, long transferred, long totalBytes) {
            this.artifactName = artifactName;
            this.transferred = transferred;
            this.totalBytes = totalBytes;
        }
    }
}
