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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Jason van Zyl
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class Verifier {
    private static final String LOG_FILENAME = "log.txt";

    private static final String[] DEFAULT_CLI_ARGUMENTS = {"--errors", "--batch-mode"};

    /**
     * Command used to clean project before execution.
     * <p
     * NOTE: Neither test lifecycle binding nor prefix resolution here but call the goal directly.
     */
    private static final String CLEAN_CLI_ARGUMENT = "org.apache.maven.plugins:maven-clean-plugin:clean";

    public static final String USER_HOME = System.getProperty("user.home");

    public static final File USER_MAVEN_CONFIGURATION_HOME = new File(USER_HOME, ".m2");

    public static final File DEFAULT_USER_SETTINGS_FILE = new File(USER_MAVEN_CONFIGURATION_HOME, "settings.xml");

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE = new File(System.getProperty("maven.conf"), "settings.xml");

    private String localRepo;

    private final String basedir;

    private String[] defaultCliArguments;

    private List<String> cliArguments = new ArrayList<>();

    private Properties systemProperties = new Properties();

    private Map<String, String> environmentVariables = new HashMap<>();

    private Properties verifierProperties = new Properties();

    private boolean autoclean = true;

    private String localRepoLayout = "default";

    /**
     * If {@code true} uses {@link ForkedLauncher}, if {@code false} uses {@link Embedded3xLauncher},
     * otherwise considers the value {@link #forkMode}.
     */
    private Boolean forkJvm;

    private String logFileName = LOG_FILENAME;

    private File logFile;

    private String mavenHome;

    // will launch mvn with -X
    private boolean mavenDebug = false;

    /**
     * Either "auto" (use {@link ForkedLauncher} when {@link #environmentVariables} is not empty,
     * otherwise use {@link Embedded3xLauncher}) , "embedder" (always use {@link Embedded3xLauncher})
     * or something else (always use {@link ForkedLauncher}).
     * Set through system property {@code verifier.forkMode}.
     * Only relevant if {@link #forkJvm} is {@code null}.
     */
    private String forkMode;

    private boolean debugJvm = false;

    private boolean useWrapper;

    private static MavenLauncher embeddedLauncher;

    private String settingsFile;

    public Verifier(String basedir) throws VerificationException {
        this.basedir = basedir;

        this.forkMode = System.getProperty("verifier.forkMode");

        findLocalRepo(settingsFile);

        this.mavenHome = System.getProperty("maven.home");

        setForkMode();

        useWrapper = Files.exists(Paths.get(getBasedir(), "mvnw"));

        this.defaultCliArguments = DEFAULT_CLI_ARGUMENTS.clone();
    }

    /**
     * @deprecated to be removed
     * use <ul>
     * <li>{@link #Verifier(String basedir)}</li>
     * </ul>
     */
    @Deprecated
    public Verifier(String basedir, boolean debug) throws VerificationException {
        this(basedir);
    }

    /**
     * @deprecated to be removed
     * use <ul>
     * <li>{@link #Verifier(String basedir)}</li>
     * <li>{@link #setSettingsFile(String settingsFile)} to set settings file</li>
     * </ul>
     */
    @Deprecated
    public Verifier(String basedir, String settingsFile) throws VerificationException {
        this(basedir);
        setSettingsFile(settingsFile);
    }

    /**
     * @deprecated to be removed
     * use <ul>
     * <li>{@link #Verifier(String basedir)}</li>
     * <li>{@link #setSettingsFile(String settingsFile)} to set settings file</li>
     * </ul>
     */
    @Deprecated
    public Verifier(String basedir, String settingsFile, boolean debug) throws VerificationException {
        this(basedir);
        setSettingsFile(settingsFile);
    }

    /**
     * @deprecated to be removed
     * use <uL>
     * <li>{@link #Verifier(String basedir)}</li>
     * <li>{@link #setSettingsFile(String settingsFile)} to set settings file</li>
     * <li>{@link #setDefaultCliArguments(String[] defaultCliArguments)} to set default cliArguments</li>
     * </ul>
     */
    @Deprecated
    public Verifier(String basedir, String settingsFile, boolean debug, String[] defaultCliArguments)
            throws VerificationException {
        this(basedir);
        setSettingsFile(settingsFile);
        setDefaultCliArguments(defaultCliArguments);
    }

    /**
     * @deprecated to be removed
     * use <ul>
     * <li>{@link #Verifier(String)}</li>
     * <li>{@link #setSettingsFile(String)} to set settings file</li>
     * <li>{@link #setForkJvm(boolean)} to set forkJvm status</li>
     * </ul>
     */
    @Deprecated
    public Verifier(String basedir, String settingsFile, boolean debug, boolean forkJvm) throws VerificationException {
        this(basedir);
        setSettingsFile(settingsFile);
        setForkJvm(forkJvm);
    }

    /**
     * @deprecated to be removed
     * use <ul>
     * <li>{@link #Verifier(String basedir)}</li>
     * <li>{@link #setSettingsFile(String settingsFile)} to set settings file</li>
     * <li>{@link #setForkJvm(boolean)} to set forkJvm status and</li>
     * <li>{@link #setDefaultCliArguments(String[] defaultCliArguments)} to set settings file</li>
     * </ul>
     */
    @Deprecated
    public Verifier(String basedir, String settingsFile, boolean debug, boolean forkJvm, String[] defaultCliArguments)
            throws VerificationException {
        this(basedir);
        setSettingsFile(settingsFile);
        setForkJvm(forkJvm);
        setDefaultCliArguments(defaultCliArguments);
    }

    /**
     * @deprecated to be removed
     * use <ul>
     * <li>{@link #Verifier(String basedir)}</li>
     * <li>{@link #setSettingsFile(String settingsFile)} to set settings file</li>
     * <li>{@link #setMavenHome(String mavenHome)}  to set maven home</li>
     * </ul>
     */
    @Deprecated
    public Verifier(String basedir, String settingsFile, boolean debug, String mavenHome) throws VerificationException {
        this(basedir);
        setSettingsFile(settingsFile);
        setMavenHome(mavenHome);
    }

    /**
     * @deprecated to be removed
     * use <ul>
     * <li>{@link #Verifier(String basedir)}</li>
     * <li>{@link #setSettingsFile(String settingsFile)} to set settings file</li>
     * <li>{@link #setMavenHome(String mavenHome)}  to set maven home</li>
     * <li>{@link #setDefaultCliArguments(String[] defaultCliArguments)} to set settings file</li>
     * </ul>
     */
    @Deprecated
    public Verifier(String basedir, String settingsFile, boolean debug, String mavenHome, String[] defaultCliArguments)
            throws VerificationException {
        this(basedir);
        setSettingsFile(settingsFile);
        setDefaultCliArguments(defaultCliArguments);
        setMavenHome(mavenHome);
    }

    public void setLocalRepo(String localRepo) {
        this.localRepo = localRepo;
    }

    /**
     * @deprecated will be removed without replacement
     */
    @Deprecated
    public void resetStreams() {}

    /**
     * @deprecated will be removed without replacement
     */
    @Deprecated
    public void displayStreamBuffers() {}

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void verify(boolean chokeOnErrorOutput) throws VerificationException {
        List<String> lines = loadFile(getBasedir(), "expected-results.txt", false);

        for (String line : lines) {
            verifyExpectedResult(line);
        }

        if (chokeOnErrorOutput) {
            verifyErrorFreeLog();
        }
    }

    public void verifyErrorFreeLog() throws VerificationException {
        List<String> lines = loadFile(getLogFile(), false);

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
        List<String> lines = loadFile(getLogFile(), false);

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
        if (classifier != null && classifier.length() == 0) {
            classifier = null;
        }
        if ("maven-plugin".equals(ext)) {
            ext = "jar";
        }
        if ("coreit-artifact".equals(ext)) {
            ext = "jar";
            classifier = "it";
        }
        if ("test-jar".equals(ext)) {
            ext = "jar";
            classifier = "tests";
        }

        String repositoryPath;
        if ("legacy".equals(localRepoLayout)) {
            repositoryPath = gid + "/" + ext + "s/" + aid + "-" + version + "." + ext;
        } else if ("default".equals(localRepoLayout)) {
            repositoryPath = gid.replace('.', '/');
            repositoryPath = repositoryPath + "/" + aid + "/" + version;
            repositoryPath = repositoryPath + "/" + aid + "-" + version;
            if (classifier != null) {
                repositoryPath = repositoryPath + "-" + classifier;
            }
            repositoryPath = repositoryPath + "." + ext;
        } else {
            throw new IllegalStateException("Unknown layout: " + localRepoLayout);
        }

        return localRepo + "/" + repositoryPath;
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
        StringBuilder buffer = new StringBuilder(256);

        buffer.append(localRepo);
        buffer.append('/');

        if ("default".equals(localRepoLayout)) {
            buffer.append(gid.replace('.', '/'));
            buffer.append('/');

            if (aid != null) {
                buffer.append(aid);
                buffer.append('/');

                if (version != null) {
                    buffer.append(version);
                    buffer.append('/');
                }
            }

            buffer.append(filename);
        } else {
            throw new IllegalStateException("Unsupported repository layout: " + localRepoLayout);
        }

        return buffer.toString();
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

    static String retrieveLocalRepo(String settingsXmlPath) throws SettingsBuildingException {
        DefaultSettingsBuilderFactory settingsBuilderFactory = new DefaultSettingsBuilderFactory();
        DefaultSettingsBuilder settingsBuilder = settingsBuilderFactory.newInstance();

        File userSettingsFile;
        if (settingsXmlPath != null) {
            userSettingsFile = new File(settingsXmlPath);
        } else {
            userSettingsFile = DEFAULT_USER_SETTINGS_FILE;
        }

        SettingsBuildingRequest settingsBuildingRequest = new DefaultSettingsBuildingRequest();
        settingsBuildingRequest.setGlobalSettingsFile(DEFAULT_GLOBAL_SETTINGS_FILE);
        settingsBuildingRequest.setUserSettingsFile(userSettingsFile);
        settingsBuildingRequest.setSystemProperties(System.getProperties());

        // takes care of interpolation and merging
        SettingsBuildingResult result = settingsBuilder.build(settingsBuildingRequest);
        return result.getEffectiveSettings().getLocalRepository();
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
        String path;
        if ("default".equals(localRepoLayout)) {
            path = gid.replace('.', '/');
        } else if ("legacy".equals(localRepoLayout)) {
            path = gid;
        } else {
            throw new IllegalStateException("Unsupported repository layout: " + localRepoLayout);
        }

        FileUtils.deleteDirectory(new File(localRepo, path));
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
        String path;
        if ("default".equals(localRepoLayout)) {
            path = gid.replace('.', '/') + '/' + aid + '/' + version;
        } else {
            throw new IllegalStateException("Unsupported repository layout: " + localRepoLayout);
        }

        FileUtils.deleteDirectory(new File(localRepo, path));
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
     * Writes a text file with the specified contents. The contents will be encoded using UTF-8.
     *
     * @param path     The path to the file, relative to the base directory, must not be <code>null</code>.
     * @param contents The contents to write, must not be <code>null</code>.
     * @throws IOException If the file could not be written.
     * @since 1.2
     */
    public void writeFile(String path, String contents) throws IOException {
        FileUtils.fileWrite(new File(getBasedir(), path).getAbsolutePath(), "UTF-8", contents);
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
     * There are 226 references to this method in Maven core ITs. In most (all?) cases it is used together with
     * {@link #newDefaultFilterProperties()}. Need to remove both methods and update all clients eventually/
     *
     * @param srcPath          The path to the input file, relative to the base directory, must not be
     *                         <code>null</code>.
     * @param dstPath          The path to the output file, relative to the base directory and possibly equal to the
     *                         input file, must not be <code>null</code>.
     * @param fileEncoding     The file encoding to use, may be <code>null</code> or empty to use the platform's default
     *                         encoding.
     * @param filterProperties The mapping from tokens to replacement values, must not be <code>null</code>.
     * @return The path to the filtered output file, never <code>null</code>.
     * @throws IOException If the file could not be filtered.
     * @deprecated use {@link #filterFile(String, String, String, Map)}
     */
    @Deprecated
    @SuppressWarnings({"rawtypes", "unchecked"})
    public File filterFile(String srcPath, String dstPath, String fileEncoding, Properties filterProperties)
            throws IOException {
        return filterFile(srcPath, dstPath, fileEncoding, (Map) filterProperties);
    }

    /**
     * Gets a new copy of the default filter properties. These default filter properties map the tokens "@basedir@" and
     * "@baseurl@" to the test's base directory and its base <code>file:</code> URL, respectively.
     *
     * @return The (modifiable) map with the default filter properties, never <code>null</code>.
     * @since 1.2
     * @deprecated use {@link #newDefaultFilterMap()}
     */
    @Deprecated
    public Properties newDefaultFilterProperties() {
        Properties filterProperties = new Properties();
        filterProperties.putAll(newDefaultFilterMap());
        return filterProperties;
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
     * Verifies the given file's content matches an regular expression.
     * Note this method also checks that the file exists and is readable.
     *
     * @param file the path of the file to check
     * @param regex a regular expression
     * @throws VerificationException in case the file was not found or its content does not match the given pattern
     * @see Pattern
     */
    public void verifyFileContentMatches(String file, String regex) throws VerificationException {
        verifyFilePresent(file);
        try {
            String content = FileUtils.fileRead(file);
            if (!Pattern.matches(regex, content)) {
                throw new VerificationException("Content of " + file + " does not match " + regex);
            }
        } catch (IOException e) {
            throw new VerificationException("Could not read from " + file, e);
        }
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

    private void verifyExpectedResult(String line) throws VerificationException {
        boolean wanted = true;
        if (line.startsWith("!")) {
            line = line.substring(1);
            wanted = false;
        }

        verifyFilePresence(line, wanted);
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

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Execute Maven.
     *
     * @deprecated will be removed.
     * <p>
     * For replacement please use:
     * <pre>
     *   verifier.addCliArgument( "goal" );
     *   verifier.execute();
     * </pre>
     */
    @Deprecated
    public void executeGoal(String goal) throws VerificationException {
        executeGoal(goal, environmentVariables);
    }

    /**
     * Execute Maven.
     *
     * @deprecated will be removed.
     * <p>
     * For replacement please use:
     * <pre>
     *   verifier.addCliArgument( "goal" );
     *   verifier.setEnvironmentVariable( "key1", "value1" );
     *   verifier.setEnvironmentVariable( "key2", "value2" );
     *   verifier.execute();
     * </pre>
     */
    @Deprecated
    public void executeGoal(String goal, Map<String, String> envVars) throws VerificationException {
        executeGoals(Collections.singletonList(goal), envVars);
    }

    /**
     * Execute Maven.
     *
     * @deprecated will be removed
     * <p>
     * For replacement please use:
     * <pre>
     *   verifier.addCliArguments( "goal1", "goal2" );
     *   verifier.execute();
     * </pre>
     */
    @Deprecated
    public void executeGoals(List<String> goals) throws VerificationException {
        executeGoals(goals, environmentVariables);
    }

    public String getExecutable() {
        // Use a strategy for finding the maven executable, John has a simple method like this
        // but a little strategy + chain of command would be nicer.

        if (mavenHome != null) {
            return mavenHome + "/bin/mvn";
        } else {
            return "mvn";
        }
    }

    /**
     * Execute Maven.
     *
     * @deprecated will be removed
     * <p>
     * For replacement please use:
     * <pre>
     *   verifier.addCliArguments( "goal1", "goal2" );
     *   verifier.setEnvironmentVariable( "key1", "value1" );
     *   verifier.setEnvironmentVariable( "key2", "value2" );
     *   verifier.execute();
     * </pre>
     */
    @Deprecated
    public void executeGoals(List<String> goals, Map<String, String> envVars) throws VerificationException {
        cliArguments.addAll(goals);
        environmentVariables.putAll(envVars);
        execute();
    }

    /**
     * Execute Maven.
     */
    public void execute() throws VerificationException {

        List<String> args = new ArrayList<>();

        Collections.addAll(args, defaultCliArguments);

        if (this.mavenDebug) {
            args.add("-X");
        }

        /*
         * NOTE: Unless explicitly requested by the caller, the forked builds should use the current local
         * repository. Otherwise, the forked builds would in principle leave the sandbox environment which has been
         * setup for the current build. In particular, using "maven.repo.local" will make sure the forked builds use
         * the same local repo as the parent build even if a custom user settings is provided.
         */
        boolean useMavenRepoLocal = Boolean.valueOf(verifierProperties.getProperty("use.mavenRepoLocal", "true"));

        if (useMavenRepoLocal) {
            args.add("-Dmaven.repo.local=" + localRepo);
        }

        if (autoclean) {
            args.add(CLEAN_CLI_ARGUMENT);
        }

        for (String cliArgument : cliArguments) {
            args.add(cliArgument.replace("${basedir}", getBasedir()));
        }

        int ret;
        File logFile = getLogFile();

        try {
            MavenLauncher launcher = getMavenLauncher(environmentVariables);

            String[] cliArgs = args.toArray(new String[0]);
            ret = launcher.run(cliArgs, systemProperties, getBasedir(), logFile);
        } catch (LauncherException e) {
            throw new VerificationException("Failed to execute Maven", e);
        } catch (IOException e) {
            throw new VerificationException(e);
        }

        if (ret > 0) {
            throw new VerificationException("Exit code was non-zero: " + ret + "; command line and log = \n"
                    + new File(mavenHome, "bin/mvn") + " "
                    + StringUtils.join(args.iterator(), " ") + "\n" + getLogContents(logFile));
        }
    }

    protected MavenLauncher getMavenLauncher(Map<String, String> envVars) throws LauncherException {
        boolean fork;
        if (useWrapper) {
            fork = true;
        } else if (forkJvm != null) {
            fork = forkJvm;
        } else if ((envVars.isEmpty() && "auto".equalsIgnoreCase(forkMode)) || "embedded".equalsIgnoreCase(forkMode)) {
            fork = false;

            try {
                initEmbeddedLauncher();
            } catch (Exception e) {
                fork = true;
            }
        } else {
            fork = true;
        }

        if (!fork) {
            if (!envVars.isEmpty()) {
                throw new LauncherException("Environment variables are not supported in embedded runtime");
            }

            initEmbeddedLauncher();

            return embeddedLauncher;
        } else {
            return new ForkedLauncher(mavenHome, envVars, debugJvm, useWrapper);
        }
    }

    private void initEmbeddedLauncher() throws LauncherException {
        if (embeddedLauncher == null) {
            if (mavenHome == null || mavenHome.isEmpty()) {
                embeddedLauncher = Embedded3xLauncher.createFromClasspath();
            } else {
                String defaultClasspath = System.getProperty("maven.bootclasspath");
                String defaultClassworldConf = System.getProperty("classworlds.conf");
                embeddedLauncher = Embedded3xLauncher.createFromMavenHome(
                        mavenHome, defaultClassworldConf, parseClasspath(defaultClasspath));
            }
        }
    }

    private static List<URL> parseClasspath(String classpath) throws LauncherException {
        if (classpath == null) {
            return null;
        }
        ArrayList<URL> classpathUrls = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
        while (st.hasMoreTokens()) {
            try {
                classpathUrls.add(new File(st.nextToken()).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new LauncherException("Invalid launcher classpath " + classpath, e);
            }
        }
        return classpathUrls;
    }

    public String getMavenVersion() throws VerificationException {
        try {
            return getMavenLauncher(Collections.emptyMap()).getMavenVersion();
        } catch (LauncherException | IOException e) {
            throw new VerificationException(e);
        }
    }

    private static String getLogContents(File logFile) {
        try {
            return Files.readString(logFile.toPath());
        } catch (IOException e) {
            // ignore
            return "(Error reading log contents: " + e.getMessage() + ")";
        }
    }

    private void findLocalRepo(String settingsFile) throws VerificationException {
        if (localRepo == null) {
            localRepo = System.getProperty("maven.repo.local");
        }

        if (localRepo == null) {
            try {
                localRepo = retrieveLocalRepo(settingsFile);
            } catch (SettingsBuildingException e) {
                throw new VerificationException("Cannot read settings.xml to determine local repository location", e);
            }
        }

        if (localRepo == null) {
            localRepo = USER_HOME + "/.m2/repository";
        }

        File repoDir = new File(localRepo);

        if (!repoDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            repoDir.mkdirs();
        }

        // normalize path
        localRepo = repoDir.getAbsolutePath();

        localRepoLayout = System.getProperty("maven.repo.local.layout", "default");
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

    static class UserModelReader extends DefaultHandler {
        private String localRepository;

        private StringBuilder currentBody = new StringBuilder();

        public void parse(File file) throws VerificationException {
            try {
                SAXParserFactory saxFactory = SAXParserFactory.newInstance();

                SAXParser parser = saxFactory.newSAXParser();

                InputSource is = new InputSource(new FileInputStream(file));

                parser.parse(is, this);
            } catch (FileNotFoundException e) {
                throw new VerificationException("file not found path : " + file.getAbsolutePath(), e);
            } catch (IOException e) {
                throw new VerificationException(" IOException path : " + file.getAbsolutePath(), e);
            } catch (ParserConfigurationException e) {
                throw new VerificationException(e);
            } catch (SAXException e) {
                throw new VerificationException("Parsing exception for file " + file.getAbsolutePath(), e);
            }
        }

        public void warning(SAXParseException spe) {
            // ignore warnings
        }

        public void error(SAXParseException spe) throws SAXException {
            throw new SAXException(spe);
        }

        public void fatalError(SAXParseException spe) throws SAXException {
            throw new SAXException(spe);
        }

        public String getLocalRepository() {
            return localRepository;
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            currentBody.append(ch, start, length);
        }

        public void endElement(String uri, String localName, String rawName) throws SAXException {
            if ("localRepository".equals(rawName)) {
                if (notEmpty(currentBody.toString())) {
                    localRepository = currentBody.toString().trim();
                } else {
                    throw new SAXException(
                            "Invalid mavenProfile entry. Missing one or more " + "fields: {localRepository}.");
                }
            }

            currentBody = new StringBuilder();
        }

        private boolean notEmpty(String test) {
            return test != null && test.trim().length() > 0;
        }

        public void reset() {
            currentBody = null;
            localRepository = null;
        }
    }

    /**
     * @deprecated will be removed without replacement,
     * for arguments adding please use {@link #addCliArgument(String)}, {@link #addCliArguments(String...)}
     */
    @Deprecated
    public List<String> getCliOptions() {
        return cliArguments;
    }

    /**
     * @deprecated will be removed
     */
    @Deprecated
    public void setCliOptions(List<String> cliOptions) {
        this.cliArguments = cliOptions;
    }

    /**
     * Add a command line argument, each argument must be set separately one by one.
     * <p>
     * <code>${basedir}</code> in argument will be replaced by value of {@link #getBasedir()} during execution.
     * @param option an argument to add
     * @deprecated please use {@link #addCliArgument(String)}
     */
    @Deprecated
    public void addCliOption(String option) {
        addCliArgument(option);
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

    public void setSystemProperties(Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public void setSystemProperty(String key, String value) {
        if (value != null) {
            systemProperties.setProperty(key, value);
        } else {
            systemProperties.remove(key);
        }
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public void setEnvironmentVariable(String key, String value) {
        if (value != null) {
            environmentVariables.put(key, value);
        } else {
            environmentVariables.remove(key);
        }
    }

    public Properties getVerifierProperties() {
        return verifierProperties;
    }

    public void setVerifierProperties(Properties verifierProperties) {
        this.verifierProperties = verifierProperties;
    }

    public boolean isAutoclean() {
        return autoclean;
    }

    /**
     * Clean project before execution by adding {@link #CLEAN_CLI_ARGUMENT} to command line.
     * <p>
     * By default, options is enabled.
     *
     * @param autoclean indicate if option is enabled
     */
    public void setAutoclean(boolean autoclean) {
        this.autoclean = autoclean;
    }

    public String getBasedir() {
        return basedir;
    }

    /**
     * Gets the name of the file used to log build output.
     *
     * @return The name of the log file, relative to the base directory, never <code>null</code>.
     * @since 1.2
     */
    public String getLogFileName() {
        return this.logFileName;
    }

    /**
     * Sets the name of the file used to log build output.
     *
     * @param logFileName The name of the log file, relative to the base directory, must not be empty or
     *                    <code>null</code>.
     * @since 1.2
     */
    public void setLogFileName(String logFileName) {
        if (logFileName == null || logFileName.isEmpty()) {
            throw new IllegalArgumentException("log file name unspecified");
        }
        this.logFileName = logFileName;
    }

    /**
     *
     * @param logFile configure the log file used to log build output
     * @since 2.0 ish (not really sure if this will be this)
     */
    public void setLogFile(File logFile) {
        this.logFile = Objects.requireNonNull(logFile, "log file cannot be null");
    }

    /**
     *
     * @return The log file used to log build output
     * @since 2.0 ish (not really sure if this will be this)
     */
    public File getLogFile() {
        return logFile == null ? new File(getBasedir(), logFileName) : logFile;
    }

    /**
     * @deprecated will be removed without replacement
     */
    @Deprecated
    public void setDebug(boolean debug) {}

    /**
     * @deprecated will be removed without replacement
     */
    @Deprecated
    public boolean isMavenDebug() {
        return mavenDebug;
    }

    /**
     * For replacement please use:
     * <pre>
     *     verifier.addCliArgument( "-X" );
     * </pre>
     *
     * @deprecated will be removed without replacement.
     */
    @Deprecated
    public void setMavenDebug(boolean mavenDebug) {
        this.mavenDebug = mavenDebug;
    }

    public void setForkJvm(boolean forkJvm) {
        this.forkJvm = forkJvm;
    }

    public boolean isDebugJvm() {
        return debugJvm;
    }

    public void setDebugJvm(boolean debugJvm) {
        this.debugJvm = debugJvm;
    }

    public String getLocalRepoLayout() {
        return localRepoLayout;
    }

    public void setLocalRepoLayout(String localRepoLayout) {
        this.localRepoLayout = localRepoLayout;
    }

    public String getLocalRepository() {
        return localRepo;
    }

    public void setMavenHome(String mavenHome) {
        this.mavenHome = mavenHome;
        setUseWrapper(false);
        setForkMode();
    }

    public void setForkMode(String forkMode) {
        this.forkMode = forkMode;
        setForkMode();
    }

    public void setUseWrapper(boolean useWrapper) {
        this.useWrapper = useWrapper;
    }

    public void setSettingsFile(String settingsFile) throws VerificationException {
        this.settingsFile = settingsFile;
        findLocalRepo(settingsFile);
    }

    public void setDefaultCliArguments(String[] defaultCliArguments) {
        this.defaultCliArguments = defaultCliArguments == null ? new String[0] : defaultCliArguments.clone();
    }

    private void setForkMode() {
        if ((mavenHome == null || mavenHome.isEmpty()) && (forkMode == null || forkMode.isEmpty())) {
            forkMode = "auto";
        }
    }
}
