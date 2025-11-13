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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.ExecutorTool;
import org.apache.maven.cling.executor.embedded.EmbeddedMavenExecutor;
import org.apache.maven.cling.executor.forked.ForkedMavenExecutor;
import org.apache.maven.cling.executor.internal.HelperImpl;
import org.apache.maven.cling.executor.internal.ToolboxTool;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import static java.util.Objects.requireNonNull;

/**
 *
 */
public class Verifier {
    /**
     * Keep executor alive, as long as Verifier is in classloader. Embedded classloader keeps embedded Maven
     * ClassWorld alive, instead to re-create it per invocation, making embedded execution fast(er).
     */
    private static final EmbeddedMavenExecutor EMBEDDED_MAVEN_EXECUTOR = new EmbeddedMavenExecutor();
    /**
     * Keep executor alive, as long as Verifier is in classloader. For forked this means nothing, but is
     * at least "handled the same" as embedded counterpart. Later on, we could have some similar solution like
     * mvnd has, and keep pool of "hot" processes maybe?
     */
    private static final ForkedMavenExecutor FORKED_MAVEN_EXECUTOR = new ForkedMavenExecutor();

    /**
     * The "preferred" fork mode of Verifier, defaults to "auto". In fact, am unsure is any other fork mode usable,
     * maybe "forked" that is slow, but offers maximum isolation?
     *
     * @see ExecutorHelper.Mode
     */
    private static final ExecutorHelper.Mode VERIFIER_FORK_MODE =
            ExecutorHelper.Mode.valueOf(System.getProperty("verifier.forkMode", ExecutorHelper.Mode.AUTO.toString())
                    .toUpperCase(Locale.ROOT));

    private static final List<String> DEFAULT_CLI_ARGUMENTS = Arrays.asList("--errors", "--batch-mode");

    private static final String AUTO_CLEAN_ARGUMENT = "org.apache.maven.plugins:maven-clean-plugin:clean";

    private final ExecutorHelper executorHelper;

    private final ExecutorTool executorTool;

    private final Path basedir; // the basedir of IT

    private final Path tempBasedir; // empty basedir for queries

    private final Path outerLocalRepository; // this is the "outer" build effective local repo

    private final List<String> defaultCliArguments;

    private final Properties systemProperties = new Properties();

    private final Map<String, String> environmentVariables = new HashMap<>();

    private final List<String> cliArguments = new ArrayList<>();

    private final List<String> jvmArguments = new ArrayList<>();

    // TestSuiteOrdering creates Verifier in non-forked JVM as well, and there no prop set is set (so use default)
    private final String toolboxVersion = System.getProperty("version.toolbox", "0.14.1");

    private Path userHomeDirectory; // the user home

    private String executable = ExecutorRequest.MVN;

    private boolean autoClean = true;

    private boolean forkJvm = false;

    private boolean handleLocalRepoTail = true; // if false: IT will become fully isolated

    private String logFileName = "log.txt";

    private Path logFile;

    private boolean skipMavenRc = true;

    private ByteArrayOutputStream stdout;

    private ByteArrayOutputStream stderr;

    public Verifier(String basedir) throws VerificationException {
        this(basedir, null);
    }

    public Verifier(String basedir, List<String> defaultCliArguments) throws VerificationException {
        this(basedir, defaultCliArguments, true);
    }

    public Verifier(String basedir, boolean createDotMvn) throws VerificationException {
        this(basedir, null, createDotMvn);
    }

    /**
     * Creates verifier instance using passed in basedir as "cwd" and passed in default CLI arguments (if not null).
     * The discovery of user home and Maven installation directory is performed as well.
     *
     * @param basedir The basedir, cannot be {@code null}
     * @param defaultCliArguments The defaultCliArguments override, may be {@code null}
     * @param createDotMvn If {@code true}, Verifier will create {@code .mvn} in passed basedir.
     *
     * @see #DEFAULT_CLI_ARGUMENTS
     */
    public Verifier(String basedir, List<String> defaultCliArguments, boolean createDotMvn) throws VerificationException {
        requireNonNull(basedir);
        try {
            this.basedir = Paths.get(basedir).toAbsolutePath();
            if (createDotMvn) {
                Files.createDirectories(this.basedir.resolve(".mvn"));
            }
            this.tempBasedir = Files.createTempDirectory("verifier");
            this.userHomeDirectory = Paths.get(System.getProperty("maven.test.user.home", "user.home"));
            Files.createDirectories(this.userHomeDirectory);
            this.outerLocalRepository = Paths.get(System.getProperty("maven.test.repo.outer", ".m2/repository"));
            this.executorHelper = new HelperImpl(
                    VERIFIER_FORK_MODE,
                    Paths.get(System.getProperty("maven.home")),
                    this.userHomeDirectory,
                    EMBEDDED_MAVEN_EXECUTOR,
                    FORKED_MAVEN_EXECUTOR);
            this.executorTool = new ToolboxTool(executorHelper, toolboxVersion);
            this.defaultCliArguments =
                    new ArrayList<>(defaultCliArguments != null ? defaultCliArguments : DEFAULT_CLI_ARGUMENTS);
            this.logFile = this.basedir.resolve(logFileName);
        } catch (IOException e) {
            throw new VerificationException("Could not create verifier", e);
        }
    }

    public void setUserHomeDirectory(Path userHomeDirectory) {
        this.userHomeDirectory = requireNonNull(userHomeDirectory, "userHomeDirectory");
    }

    public String getToolboxVersion() {
        return toolboxVersion;
    }

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = requireNonNull(executable);
    }

    public ExecutorHelper.Mode getDefaultMode() {
        return executorHelper.getDefaultMode();
    }

    public void execute() throws VerificationException {
        List<String> args = new ArrayList<>(defaultCliArguments);
        for (String cliArgument : cliArguments) {
            args.add(cliArgument.replace("${basedir}", getBasedir()));
        }

        if (handleLocalRepoTail) {
            // note: all used Strings are non-null/empty if "not present" for simpler handling
            // "outer" build pass these in, check are they present or not
            // Important: here we do "string ops" only, and no path ops, as it will be Maven
            // (based on user.home and other) that will unravel these strings to paths!
            String outerTail = System.getProperty("maven.repo.local.tail", "").trim();
            String outerHead = outerLocalRepository.toString();

            String itTail = args.stream()
                    .filter(s -> s.startsWith("-Dmaven.repo.local.tail="))
                    .findFirst()
                    .map(s -> s.substring(24).trim())
                    .orElse("");
            if (!itTail.isEmpty()) {
                // remove it
                args = args.stream()
                        .filter(s -> !s.startsWith("-Dmaven.repo.local.tail="))
                        .collect(Collectors.toList());
            }

            // push things to tail
            itTail = Stream.of(itTail, outerHead, outerTail)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
            if (!itTail.isEmpty()) {
                args.add("-Dmaven.repo.local.tail=" + itTail);
            }
        }

        // make sure these are first
        if (autoClean) {
            args.add(0, AUTO_CLEAN_ARGUMENT);
        }
        if (logFileName != null) {
            args.add(0, logFileName);
            args.add(0, "-l");
        }

        // TODO: disable RRF for now until https://github.com/apache/maven-resolver/issues/1641 can be fixed
        args.add("-Daether.remoteRepositoryFilter.groupId=false");
        args.add("-Daether.remoteRepositoryFilter.prefixes=false");

        try {
            ExecutorRequest.Builder builder = executorHelper
                    .executorRequest()
                    .command(executable)
                    .cwd(basedir)
                    .userHomeDirectory(userHomeDirectory)
                    .jvmArguments(jvmArguments)
                    .arguments(args)
                    .skipMavenRc(skipMavenRc);
            if (!systemProperties.isEmpty()) {
                builder.jvmSystemProperties(new HashMap(systemProperties));
            }
            if (!environmentVariables.isEmpty()) {
                builder.environmentVariables(environmentVariables);
            }

            ExecutorHelper.Mode mode = executorHelper.getDefaultMode();
            if (forkJvm) {
                mode = ExecutorHelper.Mode.FORKED;
            }
            stdout = new ByteArrayOutputStream();
            stderr = new ByteArrayOutputStream();
            ExecutorRequest request = builder.stdOut(stdout).stdErr(stderr).build();

            // Store the command line to prepend to log file after execution
            // Skip adding command line info if quiet logging is enabled (respects -q flag)
            String commandLineHeader = null;
            if (logFileName != null && !isQuietLogging(args)) {
                try {
                    commandLineHeader = formatCommandLine(request, mode);
                } catch (Exception e) {
                    // Don't fail the execution if we can't format the command line, just log it
                    System.err.println("Warning: Could not format command line: " + e.getMessage());
                }
            }

            int ret = executorHelper.execute(mode, request);

            // After execution, prepend the command line to the log file
            if (commandLineHeader != null && Files.exists(logFile)) {
                try {
                    String existingContent = Files.readString(logFile, StandardCharsets.UTF_8);
                    String newContent = commandLineHeader + existingContent;
                    Files.writeString(logFile, newContent, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    // Don't fail the execution if we can't write the command line, just log it
                    System.err.println("Warning: Could not prepend command line to log file: " + e.getMessage());
                }
            }
            if (ret > 0) {
                String dump;
                try {
                    dump = executorTool.dump(request.toBuilder()).toString();
                } catch (Exception e) {
                    dump = "FAILED: " + e.getMessage();
                }
                throw new VerificationException("Exit code was non-zero: " + ret + "; command line and log = \n"
                        + getExecutable() + " "
                        + "\nstdout: " + stdout
                        + "\nstderr: " + stderr
                        + "\nreq: " + request
                        + "\ndump: " + dump
                        + "\n" + getLogContents(logFile));
            }
        } catch (ExecutorException e) {
            throw new VerificationException("Failed to execute Maven", e);
        }
    }

    public String getMavenVersion() throws VerificationException {
        return executorHelper.mavenVersion();
    }

    /**
     * Add a command line argument, each argument must be set separately one by one.
     * <p>
     * <code>${basedir}</code> in argument will be replaced by value of {@link #getBasedir()} during execution.
     *
     * @param cliArgument an argument to add
     */
    public void addCliArgument(String cliArgument) {
        cliArguments.add(cliArgument);
    }

    /**
     * Add a jvm argument, each argument must be set separately one by one.
     *
     * @param jvmArgument an argument to add
     */
    public void addJvmArgument(String jvmArgument) {
        jvmArguments.add(jvmArgument);
    }

    /**
     * Add a command line arguments, each argument must be set separately one by one.
     * <p>
     * <code>${basedir}</code> in argument will be replaced by value of {@link #getBasedir()} during execution.
     *
     * @param cliArguments an arguments list to add
     */
    public void addCliArguments(String... cliArguments) {
        Collections.addAll(this.cliArguments, cliArguments);
    }

    public Properties getSystemProperties() {
        return systemProperties;
    }

    /**
     * This method renders all env variables that are used for CI detection (by all known detector) to not trigger.
     */
    public void removeCIEnvironmentVariables() {
        environmentVariables.putAll(Map.of(
                "CIRCLECI", "",
                "CI", "false",
                "GITHUB_ACTIONS", "",
                "WORKSPACE", "",
                "TEAMCITY_VERSION", "",
                "TRAVIS", ""));
    }

    public void setEnvironmentVariable(String key, String value) {
        if (value != null) {
            environmentVariables.put(key, value);
        } else {
            environmentVariables.remove(key);
        }
    }

    public String getBasedir() {
        return basedir.toString();
    }

    public void setLogFileName(String logFileName) {
        if (logFileName == null || logFileName.isEmpty()) {
            throw new IllegalArgumentException("log file name unspecified");
        }
        this.logFileName = logFileName;
        this.logFile = this.basedir.resolve(this.logFileName);
    }

    public void setAutoclean(boolean autoClean) {
        this.autoClean = autoClean;
    }

    public void setForkJvm(boolean forkJvm) {
        this.forkJvm = forkJvm;
    }

    public void setSkipMavenRc(boolean skipMavenRc) {
        this.skipMavenRc = skipMavenRc;
    }

    public void setHandleLocalRepoTail(boolean handleLocalRepoTail) {
        this.handleLocalRepoTail = handleLocalRepoTail;
    }

    public String getLocalRepository() {
        return getLocalRepositoryWithSettings(null);
    }

    public String getLocalRepositoryWithSettings(String settingsXml) {
        if (settingsXml != null) {
            // when invoked with settings.xml, the file must be resolved from basedir (as Maven does)
            // but we should not use basedir, as it may contain extensions.xml or a project, that Maven will eagerly
            // load, and may fail, as it would need more (like CI friendly versioning, etc).
            // if given, it must exist
            Path settingsFile = basedir.resolve(settingsXml).toAbsolutePath().normalize();
            if (!Files.isRegularFile(settingsFile)) {
                throw new IllegalArgumentException("settings xml does not exist: " + settingsXml);
            }
            return executorTool.localRepository(executorHelper
                    .executorRequest()
                    .cwd(tempBasedir)
                    .userHomeDirectory(userHomeDirectory)
                    .argument("-s")
                    .argument(settingsFile.toString()));
        } else {
            String outerHead = System.getProperty("maven.test.repo.local", "").trim();
            if (!outerHead.isEmpty()) {
                return outerHead;
            } else {
                return executorTool.localRepository(
                        executorHelper.executorRequest().cwd(tempBasedir).userHomeDirectory(userHomeDirectory));
            }
        }
    }

    private String getLogContents(Path logFile) {
        try {
            return Files.readString(logFile);
        } catch (IOException e) {
            return "(Error reading log contents: " + e.getMessage() + ")";
        }
    }

    /**
     * Formats the command line that would be executed for the given ExecutorRequest and mode.
     * This provides a human-readable representation of the Maven command for debugging purposes.
     */
    private String formatCommandLine(ExecutorRequest request, ExecutorHelper.Mode mode) {
        StringBuilder cmdLine = new StringBuilder();

        cmdLine.append("# Command line: ");
        // Add the Maven executable path
        Path mavenExecutable = request.installationDirectory()
                .resolve("bin")
                .resolve(System.getProperty("os.name").toLowerCase().contains("windows")
                    ? request.command() + ".cmd"
                    : request.command());
        cmdLine.append(mavenExecutable.toString());

        // Add MAVEN_ARGS if they would be used (only for forked mode)
        if (mode == ExecutorHelper.Mode.FORKED || mode == ExecutorHelper.Mode.AUTO) {
            String mavenArgsEnv = System.getenv("MAVEN_ARGS");
            if (mavenArgsEnv != null && !mavenArgsEnv.isEmpty()) {
                cmdLine.append(" ").append(mavenArgsEnv);
            }
        }

        // Add the arguments
        for (String arg : request.arguments()) {
            cmdLine.append(" ");
            // Quote arguments that contain spaces
            if (arg.contains(" ")) {
                cmdLine.append("\"").append(arg).append("\"");
            } else {
                cmdLine.append(arg);
            }
        }

        // Add environment variables that would be set
        if (request.environmentVariables().isPresent() && !request.environmentVariables().get().isEmpty()) {
            cmdLine.append("\n# Environment variables:");
            for (Map.Entry<String, String> entry : request.environmentVariables().get().entrySet()) {
                cmdLine.append("\n# ").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        // Add JVM arguments that would be set via MAVEN_OPTS
        List<String> jvmArgs = new ArrayList<>();
        if (!request.userHomeDirectory().equals(ExecutorRequest.getCanonicalPath(Paths.get(System.getProperty("user.home"))))) {
            jvmArgs.add("-Duser.home=" + request.userHomeDirectory().toString());
        }
        if (request.jvmArguments().isPresent()) {
            jvmArgs.addAll(request.jvmArguments().get());
        }
        if (request.jvmSystemProperties().isPresent()) {
            jvmArgs.addAll(request.jvmSystemProperties().get().entrySet().stream()
                    .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                    .toList());
        }

        if (!jvmArgs.isEmpty()) {
            cmdLine.append("\n# MAVEN_OPTS=").append(String.join(" ", jvmArgs));
        }

        if (request.skipMavenRc()) {
            cmdLine.append("\n# MAVEN_SKIP_RC=true");
        }

        cmdLine.append("\n# Working directory: ").append(request.cwd().toString());
        cmdLine.append("\n# Execution mode: ").append(mode.toString());

        cmdLine.append("\n");
        return cmdLine.toString();
    }

    /**
     * Checks if quiet logging is enabled by looking for the -q or --quiet flag in the arguments.
     */
    private boolean isQuietLogging(List<String> args) {
        return args.contains("-q") || args.contains("--quiet");
    }

    public String getLogFileName() {
        return logFileName;
    }

    public Path getLogFile() {
        return logFile;
    }

    public void verifyErrorFreeLog() throws VerificationException {
        List<String> lines = loadFile(logFile.toFile(), false);

        for (String line : lines) {
            // A hack to keep stupid velocity resource loader errors from triggering failure
            if (stripAnsi(line).contains("[ERROR]") && !isVelocityError(line)) {
                throw new VerificationException("Error in execution: " + line);
            }
        }
    }

    /**
     * Throws an exception if the text <strong>is</strong> present in the log.
     *
     * @param text the text to assert present
     * @throws VerificationException if text is not found in log
     */
    public void verifyTextNotInLog(String text) throws VerificationException, IOException {
        verifyTextNotInLog(loadLogLines(), text);
    }

    public static void verifyTextNotInLog(List<String> lines, String text) throws VerificationException {
        if (textOccurencesInLog(lines, text) > 0) {
            throw new VerificationException("Text found in log: " + text);
        }
    }

    public static void verifyTextInLog(List<String> lines, String text) throws VerificationException {
        if (textOccurencesInLog(lines, text) <= 0) {
            throw new VerificationException("Text not found in log: " + text);
        }
    }

    public long textOccurrencesInLog(String text) throws IOException {
        return textOccurencesInLog(loadLogLines(), text);
    }

    public static long textOccurencesInLog(List<String> lines, String text) {
        return lines.stream().filter(line -> stripAnsi(line).contains(text)).count();
    }

    /**
     * Checks whether the specified line is just an error message from Velocity. Especially old versions of Doxia employ
     * a very noisy Velocity instance.
     *
     * @param line The log line to check, must not be <code>null</code>.
     * @return <code>true</code> if the line appears to be a Velocity error, <code>false</code> otherwise.
     */
    private static boolean isVelocityError(String line) {
        return line.contains("VM_global_library.vm") || line.contains("VM #") && line.contains("macro");
    }

    /**
     * Throws an exception if the text is not present in the log.
     *
     * @param text the text to assert present
     * @throws VerificationException if text is not found in log
     */
    public void verifyTextInLog(String text) throws VerificationException {
        List<String> lines = loadFile(logFile.toFile(), false);

        boolean result = false;
        for (String line : lines) {
            if (stripAnsi(line).contains(text)) {
                result = true;
                break;
            }
        }
        if (!result) {
            throw new VerificationException("Text not found in log: " + text);
        }
    }

    public String getStdout() {
        return stdout != null ? stdout.toString(StandardCharsets.UTF_8) : "";
    }

    public String getStderr() {
        return stderr != null ? stderr.toString(StandardCharsets.UTF_8) : "";
    }

    public static String stripAnsi(String msg) {
        return msg.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
    }

    public Properties loadProperties(String filename) throws VerificationException {
        Properties properties = new Properties();

        File propertiesFile = new File(getBasedir(), filename);
        try (FileInputStream fis = new FileInputStream(propertiesFile)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new VerificationException("Error reading properties file", e);
        }

        return properties;
    }

    /**
     * Loads the (non-empty) lines of the specified text file.
     *
     * @param filename The path to the text file to load, relative to the base directory, must not be <code>null</code>.
     * @param encoding The character encoding of the file, may be <code>null</code> or empty to use the platform default
     *                 encoding.
     * @return The list of (non-empty) lines from the text file, can be empty but never <code>null</code>.
     * @throws IOException If the file could not be loaded.
     * @since 1.2
     */
    public List<String> loadLines(String filename, String encoding) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = getReader(filename, encoding)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private BufferedReader getReader(String filename, String encoding) throws IOException {
        File file = new File(getBasedir(), filename);
        if (encoding != null && !encoding.isEmpty()) {
            return Files.newBufferedReader(file.toPath(), Charset.forName(encoding));
        } else {
            return Files.newBufferedReader(file.toPath());
        }
    }

    public List<String> loadFile(String basedir, String filename, boolean hasCommand) throws VerificationException {
        return loadFile(new File(basedir, filename), hasCommand);
    }

    public List<String> loadFile(File file, boolean hasCommand) throws VerificationException {
        List<String> lines = new ArrayList<>();

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();

                while (line != null) {
                    line = line.trim();

                    if (!line.startsWith("#") && !line.isEmpty()) {
                        lines.addAll(replaceArtifacts(line, hasCommand));
                    }
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new VerificationException("Verifier loadFile failure", e);
            }
        }

        return lines;
    }

    public String loadLogContent() throws IOException {
        return Files.readString(getLogFile());
    }

    public List<String> loadLogLines() throws IOException {
        return loadLines(getLogFileName());
    }

    public List<String> loadLines(String filename) throws IOException {
        return loadLines(filename, null);
    }

    private static final String MARKER = "${artifact:";

    private List<String> replaceArtifacts(String line, boolean hasCommand) {
        int index = line.indexOf(MARKER);
        if (index >= 0) {
            String newLine = line.substring(0, index);
            index = line.indexOf("}", index);
            if (index < 0) {
                throw new IllegalArgumentException("line does not contain ending artifact marker: '" + line + "'");
            }
            String artifact = line.substring(newLine.length() + MARKER.length(), index);

            newLine += getArtifactPath(artifact);
            newLine += line.substring(index + 1);

            List<String> l = new ArrayList<>();
            l.add(newLine);

            int endIndex = newLine.lastIndexOf('/');

            String command = null;
            String filespec;
            if (hasCommand) {
                int startIndex = newLine.indexOf(' ');

                command = newLine.substring(0, startIndex);

                filespec = newLine.substring(startIndex + 1, endIndex);
            } else {
                filespec = newLine;
            }

            File dir = new File(filespec);
            addMetadataToList(dir, hasCommand, l, command);
            addMetadataToList(dir.getParentFile(), hasCommand, l, command);

            return l;
        } else {
            return Collections.singletonList(line);
        }
    }

    private static void addMetadataToList(File dir, boolean hasCommand, List<String> l, String command) {
        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("maven-metadata") && name.endsWith(".xml");
                }
            });

            for (String file : files) {
                if (hasCommand) {
                    l.add(command + " " + new File(dir, file).getPath());
                } else {
                    l.add(new File(dir, file).getPath());
                }
            }
        }
    }

    private String getArtifactPath(String artifact) {
        StringTokenizer tok = new StringTokenizer(artifact, ":");
        if (tok.countTokens() != 4) {
            throw new IllegalArgumentException("Artifact must have 4 tokens: '" + artifact + "'");
        }

        String[] a = new String[4];
        for (int i = 0; i < 4; i++) {
            a[i] = tok.nextToken();
        }

        String groupId = a[0];
        String artifactId = a[1];
        String version = a[2];
        String ext = a[3];
        return getArtifactPath(groupId, artifactId, version, ext);
    }

    public String getArtifactPath(String groupId, String artifactId, String version, String ext) {
        return getArtifactPath(groupId, artifactId, version, ext, null);
    }

    /**
     * Returns the absolute path to the artifact denoted by groupId, artifactId, version, extension and classifier.
     *
     * @param gid        The groupId, must not be null.
     * @param aid        The artifactId, must not be null.
     * @param version    The version, must not be null.
     * @param ext        The extension, must not be null.
     * @param classifier The classifier, may be null to be omitted.
     * @return the absolute path to the artifact denoted by groupId, artifactId, version, extension and classifier,
     *         never null.
     */
    public String getArtifactPath(String gid, String aid, String version, String ext, String classifier) {
        if (classifier != null && classifier.isEmpty()) {
            classifier = null;
        }
        if ("maven-plugin".equals(ext)) {
            ext = "jar";
        } else if ("coreit-artifact".equals(ext)) {
            ext = "jar";
            classifier = "it";
        } else if ("test-jar".equals(ext)) {
            ext = "jar";
            classifier = "tests";
        }

        String gav;
        if (classifier != null) {
            gav = gid + ":" + aid + ":" + ext + ":" + classifier + ":" + version;
        } else {
            gav = gid + ":" + aid + ":" + ext + ":" + version;
        }
        return getLocalRepository()
                + File.separator
                + executorTool.artifactPath(executorHelper.executorRequest(), gav, null);
    }

    private String getSupportArtifactPath(String artifact) {
        StringTokenizer tok = new StringTokenizer(artifact, ":");
        if (tok.countTokens() != 4) {
            throw new IllegalArgumentException("Artifact must have 4 tokens: '" + artifact + "'");
        }

        String[] a = new String[4];
        for (int i = 0; i < 4; i++) {
            a[i] = tok.nextToken();
        }

        String groupId = a[0];
        String artifactId = a[1];
        String version = a[2];
        String ext = a[3];
        return getSupportArtifactPath(groupId, artifactId, version, ext);
    }

    public String getSupportArtifactPath(String groupId, String artifactId, String version, String ext) {
        return getSupportArtifactPath(groupId, artifactId, version, ext, null);
    }

    /**
     * Returns the absolute path to the artifact denoted by groupId, artifactId, version, extension and classifier.
     *
     * @param gid        The groupId, must not be null.
     * @param aid        The artifactId, must not be null.
     * @param version    The version, must not be null.
     * @param ext        The extension, must not be null.
     * @param classifier The classifier, may be null to be omitted.
     * @return the absolute path to the artifact denoted by groupId, artifactId, version, extension and classifier,
     *         never null.
     */
    public String getSupportArtifactPath(String gid, String aid, String version, String ext, String classifier) {
        if (classifier != null && classifier.isEmpty()) {
            classifier = null;
        }
        if ("maven-plugin".equals(ext)) {
            ext = "jar";
        } else if ("coreit-artifact".equals(ext)) {
            ext = "jar";
            classifier = "it";
        } else if ("test-jar".equals(ext)) {
            ext = "jar";
            classifier = "tests";
        }

        String gav;
        if (classifier != null) {
            gav = gid + ":" + aid + ":" + ext + ":" + classifier + ":" + version;
        } else {
            gav = gid + ":" + aid + ":" + ext + ":" + version;
        }
        return outerLocalRepository
                .resolve(executorTool.artifactPath(
                        executorHelper.executorRequest().argument("-Dmaven.repo.local=" + outerLocalRepository),
                        gav,
                        null))
                .toString();
    }

    public List<String> getArtifactFileNameList(String org, String name, String version, String ext) {
        List<String> files = new ArrayList<>();
        String artifactPath = getArtifactPath(org, name, version, ext);
        File dir = new File(artifactPath);
        files.add(artifactPath);
        addMetadataToList(dir, false, files, null);
        addMetadataToList(dir.getParentFile(), false, files, null);
        return files;
    }

    /**
     * Gets the path to the local artifact metadata. Note that the method does not check whether the returned path
     * actually points to existing metadata.
     *
     * @param gid     The group id, must not be <code>null</code>.
     * @param aid     The artifact id, must not be <code>null</code>.
     * @param version The artifact version, may be <code>null</code>.
     * @return The (absolute) path to the local artifact metadata, never <code>null</code>.
     */
    public String getArtifactMetadataPath(String gid, String aid, String version) {
        return getArtifactMetadataPath(gid, aid, version, "maven-metadata.xml");
    }

    /**
     * Gets the path to a file in the local artifact directory. Note that the method does not check whether the returned
     * path actually points to an existing file.
     *
     * @param gid      The group id, must not be <code>null</code>.
     * @param aid      The artifact id, may be <code>null</code>.
     * @param version  The artifact version, may be <code>null</code>.
     * @param filename The filename to use, must not be <code>null</code>.
     * @return The (absolute) path to the local artifact metadata, never <code>null</code>.
     */
    public String getArtifactMetadataPath(String gid, String aid, String version, String filename) {
        return getArtifactMetadataPath(gid, aid, version, filename, null);
    }

    /**
     * Gets the path to a file in the local artifact directory. Note that the method does not check whether the returned
     * path actually points to an existing file.
     *
     * @param gid      The group id, must not be <code>null</code>.
     * @param aid      The artifact id, may be <code>null</code>.
     * @param version  The artifact version, may be <code>null</code>.
     * @param filename The filename to use, must not be <code>null</code>.
     * @param repoId   The remote repository ID from where metadata originate, may be <code>null</code>.
     * @return The (absolute) path to the local artifact metadata, never <code>null</code>.
     */
    public String getArtifactMetadataPath(String gid, String aid, String version, String filename, String repoId) {
        String gav;
        if (gid != null) {
            gav = gid + ":";
        } else {
            gav = ":";
        }
        if (aid != null) {
            gav += aid + ":";
        } else {
            gav += ":";
        }
        if (version != null) {
            gav += version + ":";
        } else {
            gav += ":";
        }
        gav += filename;
        return getLocalRepository()
                + File.separator
                + executorTool.metadataPath(executorHelper.executorRequest(), gav, repoId);
    }

    /**
     * Gets the path to the local artifact metadata. Note that the method does not check whether the returned path
     * actually points to existing metadata.
     *
     * @param gid The group id, must not be <code>null</code>.
     * @param aid The artifact id, must not be <code>null</code>.
     * @return The (absolute) path to the local artifact metadata, never <code>null</code>.
     */
    public String getArtifactMetadataPath(String gid, String aid) {
        return getArtifactMetadataPath(gid, aid, null);
    }

    public void deleteArtifact(String org, String name, String version, String ext) throws IOException {
        List<String> files = getArtifactFileNameList(org, name, version, ext);
        for (String fileName : files) {
            FileUtils.forceDelete(new File(fileName));
        }
    }

    /**
     * Deletes all artifacts in the specified group id from the local repository.
     *
     * @param gid The group id whose artifacts should be deleted, must not be <code>null</code>.
     * @throws IOException If the artifacts could not be deleted.
     * @since 1.2
     */
    public void deleteArtifacts(String gid) throws IOException {
        String mdPath = executorTool.metadataPath(executorHelper.executorRequest(), gid, null);
        Path dir = Paths.get(getLocalRepository()).resolve(mdPath).getParent();
        FileUtils.deleteDirectory(dir.toFile());
    }

    /**
     * Deletes all artifacts in the specified g:a:v from the local repository.
     *
     * @param gid     The group id whose artifacts should be deleted, must not be <code>null</code>.
     * @param aid     The artifact id whose artifacts should be deleted, must not be <code>null</code>.
     * @param version The (base) version whose artifacts should be deleted, must not be <code>null</code>.
     * @throws IOException If the artifacts could not be deleted.
     * @since 1.3
     */
    public void deleteArtifacts(String gid, String aid, String version) throws IOException {
        requireNonNull(gid, "gid is null");
        requireNonNull(aid, "aid is null");
        requireNonNull(version, "version is null");

        String mdPath =
                executorTool.metadataPath(executorHelper.executorRequest(), gid + ":" + aid + ":" + version, null);
        Path dir = Paths.get(getLocalRepository()).resolve(mdPath).getParent();
        FileUtils.deleteDirectory(dir.toFile());
    }

    /**
     * Deletes the specified directory.
     *
     * @param path The path to the directory to delete, relative to the base directory, must not be <code>null</code>.
     * @throws IOException If the directory could not be deleted.
     * @since 1.2
     */
    public void deleteDirectory(String path) throws IOException {
        FileUtils.deleteDirectory(new File(getBasedir(), path));
    }

    public File filterFile(String srcPath, String dstPath) throws IOException {
        return filterFile(srcPath, dstPath, (String) null);
    }

    public File filterFile(String srcPath, String dstPath, Map<String, String> filterMap) throws IOException {
        return filterFile(srcPath, dstPath, null, filterMap);
    }

    /**
     * Filters a text file by replacing some user-defined tokens.
     * This method is equivalent to:
     *
     * <pre>
     *     filterFile( srcPath, dstPath, fileEncoding, verifier.newDefaultFilterMap() )
     * </pre>
     *
     * @param srcPath          The path to the input file, relative to the base directory, must not be
     *                         <code>null</code>.
     * @param dstPath          The path to the output file, relative to the base directory and possibly equal to the
     *                         input file, must not be <code>null</code>.
     * @param fileEncoding     The file encoding to use, may be <code>null</code> or empty to use the platform's default
     *                         encoding.
     * @return The path to the filtered output file, never <code>null</code>.
     * @throws IOException If the file could not be filtered.
     * @since 2.0
     */
    public File filterFile(String srcPath, String dstPath, String fileEncoding) throws IOException {
        return filterFile(srcPath, dstPath, fileEncoding, newDefaultFilterMap());
    }

    /**
     * Filters a text file by replacing some user-defined tokens.
     *
     * @param srcPath      The path to the input file, relative to the base directory, must not be
     *                     <code>null</code>.
     * @param dstPath      The path to the output file, relative to the base directory and possibly equal to the
     *                     input file, must not be <code>null</code>.
     * @param fileEncoding The file encoding to use, may be <code>null</code> or empty to use the platform's default
     *                     encoding.
     * @param filterMap    The mapping from tokens to replacement values, must not be <code>null</code>.
     * @return The path to the filtered output file, never <code>null</code>.
     * @throws IOException If the file could not be filtered.
     * @since 1.2
     */
    public File filterFile(String srcPath, String dstPath, String fileEncoding, Map<String, String> filterMap)
            throws IOException {
        Charset charset = fileEncoding != null ? Charset.forName(fileEncoding) : StandardCharsets.UTF_8;
        File srcFile = new File(getBasedir(), srcPath);
        String data = Files.readString(srcFile.toPath(), charset);

        for (Map.Entry<String, String> entry : filterMap.entrySet()) {
            data = StringUtils.replace(data, entry.getKey(), entry.getValue());
        }

        File dstFile = new File(getBasedir(), dstPath);
        //noinspection ResultOfMethodCallIgnored
        dstFile.getParentFile().mkdirs();
        Files.writeString(dstFile.toPath(), data, charset);

        return dstFile;
    }

    /**
     * Gets a new copy of the default filter map. These default filter map, contains the tokens "@basedir@" and
     * "@baseurl@" to the test's base directory and its base <code>file:</code> URL, respectively.
     *
     * @return The (modifiable) map with the default filter map, never <code>null</code>.
     * @since 2.0
     */
    public Map<String, String> newDefaultFilterMap() {
        Map<String, String> filterMap = new HashMap<>();

        Path basedir = Paths.get(getBasedir()).toAbsolutePath();
        filterMap.put("@basedir@", basedir.toString());
        filterMap.put("@baseurl@", basedir.toUri().toASCIIString());

        return filterMap;
    }

    /**
     * Verifies that the given file exists.
     *
     * @param file the path of the file to check
     * @throws VerificationException in case the given file does not exist
     */
    public void verifyFilePresent(String file) throws VerificationException {
        verifyFilePresence(file, true);
    }

    /**
     * Verifies that the given file does not exist.
     *
     * @param file the path of the file to check
     * @throws VerificationException if the given file exists
     */
    public void verifyFileNotPresent(String file) throws VerificationException {
        verifyFilePresence(file, false);
    }

    private void verifyArtifactPresence(boolean wanted, String groupId, String artifactId, String version, String ext)
            throws VerificationException {
        List<String> files = getArtifactFileNameList(groupId, artifactId, version, ext);
        for (String fileName : files) {
            verifyFilePresence(fileName, wanted);
        }
    }

    /**
     * Verifies that the artifact given through its Maven coordinates exists.
     *
     * @param groupId the groupId of the artifact (must not be null)
     * @param artifactId the artifactId of the artifact (must not be null)
     * @param version the version of the artifact (must not be null)
     * @param ext the extension of the artifact (must not be null)
     * @throws VerificationException if the given artifact does not exist
     */
    public void verifyArtifactPresent(String groupId, String artifactId, String version, String ext)
            throws VerificationException {
        verifyArtifactPresence(true, groupId, artifactId, version, ext);
    }

    /**
     * Verifies that the artifact given through its Maven coordinates does not exist.
     *
     * @param groupId the groupId of the artifact (must not be null)
     * @param artifactId the artifactId of the artifact (must not be null)
     * @param version the version of the artifact (must not be null)
     * @param ext the extension of the artifact (must not be null)
     * @throws VerificationException if the given artifact exists
     */
    public void verifyArtifactNotPresent(String groupId, String artifactId, String version, String ext)
            throws VerificationException {
        verifyArtifactPresence(false, groupId, artifactId, version, ext);
    }

    private void verifyFilePresence(String filePath, boolean wanted) throws VerificationException {
        if (filePath.contains("!/")) {
            Path basedir = Paths.get(getBasedir()).toAbsolutePath();
            String urlString = "jar:" + basedir.toUri().toASCIIString() + "/" + filePath;

            InputStream is = null;
            try {
                URL url = new URL(urlString);

                is = url.openStream();

                if (is == null) {
                    if (wanted) {
                        throw new VerificationException("Expected JAR resource was not found: " + filePath);
                    }
                } else {
                    if (!wanted) {
                        throw new VerificationException("Unwanted JAR resource was found: " + filePath);
                    }
                }
            } catch (MalformedURLException e) {
                throw new VerificationException("Error looking for JAR resource", e);
            } catch (IOException e) {
                if (wanted) {
                    throw new VerificationException("Error looking for JAR resource: " + filePath);
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } else {
            File expectedFile = new File(filePath);

            // NOTE: On Windows, a path with a leading (back-)slash is relative to the current drive
            if (!expectedFile.isAbsolute() && !expectedFile.getPath().startsWith(File.separator)) {
                expectedFile = new File(getBasedir(), filePath);
            }

            if (filePath.indexOf('*') > -1) {
                File parent = expectedFile.getParentFile();

                if (!parent.exists()) {
                    if (wanted) {
                        throw new VerificationException(
                                "Expected file pattern was not found: " + expectedFile.getPath());
                    }
                } else {
                    String shortNamePattern = expectedFile.getName().replaceAll("\\*", ".*");

                    String[] candidates = parent.list();

                    boolean found = false;

                    if (candidates != null) {
                        for (String candidate : candidates) {
                            if (candidate.matches(shortNamePattern)) {
                                found = true;
                                break;
                            }
                        }
                    }

                    if (!found && wanted) {
                        throw new VerificationException(
                                "Expected file pattern was not found: " + expectedFile.getPath());
                    } else if (found && !wanted) {
                        throw new VerificationException("Unwanted file pattern was found: " + expectedFile.getPath());
                    }
                }
            } else {
                if (!expectedFile.exists()) {
                    if (wanted) {
                        throw new VerificationException("Expected file was not found: " + expectedFile.getPath());
                    }
                } else {
                    if (!wanted) {
                        throw new VerificationException("Unwanted file was found: " + expectedFile.getPath());
                    }
                }
            }
        }
    }

    /**
     * Verifies that the artifact given by its Maven coordinates exists and contains the given content.
     *
     * @param groupId the groupId of the artifact (must not be null)
     * @param artifactId the artifactId of the artifact (must not be null)
     * @param version the version of the artifact (must not be null)
     * @param ext the extension of the artifact (must not be null)
     * @param content the expected content
     * @throws IOException if reading from the artifact fails
     * @throws VerificationException if the content of the artifact differs
     */
    public void verifyArtifactContent(String groupId, String artifactId, String version, String ext, String content)
            throws IOException, VerificationException {
        String fileName = getArtifactPath(groupId, artifactId, version, ext);
        if (!content.equals(FileUtils.fileRead(fileName))) {
            throw new VerificationException("Content of " + fileName + " does not equal " + content);
        }
    }
}
