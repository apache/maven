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
package org.apache.maven.shared.verifier;

import java.io.BufferedReader;
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
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.internal.HelperImpl;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import static java.util.Objects.requireNonNull;

/**
 * @author Jason van Zyl
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class Verifier {
    private static final String LOG_FILENAME = "log.txt";

    private static final List<String> DEFAULT_CLI_ARGUMENTS = Arrays.asList("--errors", "--batch-mode");

    private static final String AUTO_CLEAN_ARGUMENT = "org.apache.maven.plugins:maven-clean-plugin:clean";

    private final ExecutorHelper executorHelper;

    private final Path basedir;

    private final Path userHomeDirectory;

    private final List<String> defaultCliArguments;

    private final Properties systemProperties = new Properties();

    private final Map<String, String> environmentVariables = new HashMap<>();

    private final List<String> cliArguments = new ArrayList<>();

    private boolean autoClean = true;

    private boolean forkJvm = false;

    private String logFileName = LOG_FILENAME;

    private Path logFile;

    /**
     * Creates verifier instance.
     *
     * @param basedir The basedir, cannot be {@code null}
     * @param defaultCliArguments The defaultCliArguments override, may be {@code null}
     *
     * @see #DEFAULT_CLI_ARGUMENTS
     */
    public Verifier(String basedir, List<String> defaultCliArguments) throws VerificationException {
        requireNonNull(basedir);
        this.basedir = Paths.get(basedir);
        this.userHomeDirectory = Paths.get(System.getProperty("user.home"));
        this.executorHelper = new HelperImpl(Paths.get(System.getProperty("maven.home")));
        this.defaultCliArguments =
                new ArrayList<>(defaultCliArguments != null ? defaultCliArguments : DEFAULT_CLI_ARGUMENTS);

        this.logFile = this.basedir.resolve(logFileName);
    }

    public String getLogFileName() {
        return logFileName;
    }

    public Path getLogFile() {
        return logFile;
    }

    public void setLogFile(Path logFile) {
        this.logFile = logFile;
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
                throw new VerificationException(e);
            }
        }

        return lines;
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
            gav = gid + ":" + aid + ":" + classifier + ":" + ext + ":" + version;
        } else {
            gav = gid + ":" + aid + ":" + ext + ":" + version;
        }
        return executorHelper.artifactPath(executorHelper.executorRequest(), gav, null);
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
        return getArtifactMetadataPath(gid, aid, version, "maven-metadata-local.xml");
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
        return executorHelper.metadataPath(executorHelper.executorRequest(), gav, null);
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
        String mdPath = executorHelper.metadataPath(executorHelper.executorRequest(), gid, null);
        String localRepo = executorHelper.localRepository(executorHelper.executorRequest());
        Path dir = Paths.get(localRepo).resolve(mdPath).getParent();
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
                executorHelper.metadataPath(executorHelper.executorRequest(), gid + ":" + aid + ":" + version, null);
        String localRepo = executorHelper.localRepository(executorHelper.executorRequest());
        Path dir = Paths.get(localRepo).resolve(mdPath).getParent();
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

    public String getExecutable() {
        return ExecutorRequest.MVN;
    }

    public void execute() throws VerificationException {
        List<String> args = new ArrayList<>(defaultCliArguments);
        if (autoClean) {
            args.add(AUTO_CLEAN_ARGUMENT);
        }
        for (String cliArgument : cliArguments) {
            args.add(cliArgument.replace("${basedir}", getBasedir()));
        }
        try {
            ExecutorRequest.Builder builder =
                    executorHelper.executorRequest().cwd(basedir).userHomeDirectory(userHomeDirectory);
            if (!systemProperties.isEmpty()) {
                builder.jvmSystemProperties(new HashMap(systemProperties));
            }
            if (!environmentVariables.isEmpty()) {
                builder.environmentVariables(environmentVariables);
            }
            if (logFileName != null) {
                builder.argument("-l").argument(logFileName);
            }
            builder.arguments(args);

            ExecutorHelper.Mode mode = ExecutorHelper.Mode.AUTO;
            if (forkJvm) {
                mode = ExecutorHelper.Mode.FORKED;
            }
            int ret = executorHelper.execute(mode, builder.build());
            if (ret > 0) {
                throw new VerificationException("Exit code was non-zero: " + ret + "; command line and log = \n"
                        + getExecutable() + " "
                        + StringUtils.join(args.iterator(), " ") + "\n" + getLogContents(logFile));
            }
        } catch (ExecutorException e) {
            throw new VerificationException("Failed to execute Maven", e);
        }
    }

    public String getMavenVersion() throws VerificationException {
        return executorHelper.mavenVersion();
    }

    private String getLogContents(Path logFile) {
        try {
            return Files.readString(logFile);
        } catch (IOException e) {
            return "(Error reading log contents: " + e.getMessage() + ")";
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

    public void setEnvironmentVariable(String key, String value) {
        if (value != null) {
            environmentVariables.put(key, value);
        } else {
            environmentVariables.remove(key);
        }
    }

    public void setAutoClean(boolean autoClean) {
        this.autoClean = autoClean;
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

    public void setForkJvm(boolean forkJvm) {
        this.forkJvm = forkJvm;
    }

    public String getLocalRepository() {
        return executorHelper.localRepository(executorHelper.executorRequest());
    }
}
