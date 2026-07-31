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
package org.apache.maven.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.impl.util.Os;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers JDK installations on the local filesystem by scanning well-known
 * directories, environment variables, and tool manager locations.
 * <p>
 * This is used by {@link DefaultToolchainManager} as a lazy fallback when auto-selection
 * needs a compatible JDK but none are configured in {@code toolchains.xml}.
 * Discovery only runs when the running JDK cannot compile the project's source level
 * and no configured toolchain matches — normal builds pay zero cost.
 * <p>
 * JDK version is read from the {@code release} file present in every JDK since Java 9
 * (and backported to JDK 8u updates), avoiding the need to execute {@code java} processes.
 * <p>
 * All environment and system property access goes through the {@code properties} map
 * supplied by the caller (typically {@link org.apache.maven.api.Session#getSystemProperties()}),
 * where environment variables are available as {@code env.VAR_NAME} entries and JVM system
 * properties as plain keys ({@code java.home}, {@code user.home}, etc.).
 */
@Named
@Singleton
public class JdkToolchainDiscoverer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdkToolchainDiscoverer.class);

    private volatile List<ToolchainModel> cachedToolchains;

    /**
     * Returns discovered JDK toolchain models. Results are cached after first invocation
     * (the properties map from the first call wins; subsequent calls with different
     * properties still return the cached result, since JDK locations don't change mid-build).
     *
     * @param properties system properties map (from {@code Session.getSystemProperties()}).
     *                   Environment variables are expected as {@code env.VAR_NAME} entries.
     */
    public List<ToolchainModel> discoverToolchains(Map<String, String> properties) {
        List<ToolchainModel> result = cachedToolchains;
        if (result == null) {
            synchronized (this) {
                result = cachedToolchains;
                if (result == null) {
                    result = doDiscover(properties);
                    cachedToolchains = result;
                }
            }
        }
        return result;
    }

    private List<ToolchainModel> doDiscover(Map<String, String> properties) {
        Set<Path> candidates = new LinkedHashSet<>();
        collectFromEnvironment(candidates, properties);
        collectFromToolManagers(candidates, properties);
        collectFromSystemDirectories(candidates, properties);

        List<ToolchainModel> toolchains = new ArrayList<>();
        for (Path candidate : candidates) {
            try {
                Path jdkHome = resolveJdkHome(candidate);
                if (jdkHome != null && isValidJdkHome(jdkHome)) {
                    Optional<ToolchainModel> model = buildToolchainModel(jdkHome);
                    model.ifPresent(toolchains::add);
                }
            } catch (Exception e) {
                LOGGER.debug("Skipping JDK candidate {}: {}", candidate, e.getMessage());
            }
        }

        LOGGER.debug("Discovered {} JDK installation(s) on the filesystem", toolchains.size());
        return List.copyOf(toolchains);
    }

    /**
     * Collects JDK candidates from environment variables matching {@code JAVA*_HOME}.
     * Environment variables are read from the properties map as {@code env.VAR_NAME} entries.
     */
    void collectFromEnvironment(Set<Path> candidates, Map<String, String> properties) {
        // Current JDK (from java.home system property)
        String javaHome = properties.get("java.home");
        if (javaHome != null) {
            addCandidate(candidates, Paths.get(javaHome));
        }

        // env.JAVA*_HOME env vars (e.g. env.JAVA11_HOME, env.JAVA17_HOME, env.JAVA_HOME)
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("env.JAVA") && name.endsWith("_HOME")) {
                addCandidate(candidates, Paths.get(entry.getValue()));
            }
        }
    }

    /**
     * Collects JDK candidates from common tool manager directories under the user's home.
     */
    void collectFromToolManagers(Set<Path> candidates, Map<String, String> properties) {
        String userHomeProp = properties.get("user.home");
        if (userHomeProp == null) {
            return;
        }
        Path userHome = Paths.get(userHomeProp);

        // IntelliJ IDEA / common
        scanSubdirectories(candidates, userHome.resolve(".jdks"));
        // Maven-managed JDKs
        scanSubdirectories(candidates, userHome.resolve(".m2").resolve("jdks"));
        // SDKMAN
        scanSubdirectories(
                candidates, userHome.resolve(".sdkman").resolve("candidates").resolve("java"));
        // Gradle
        scanSubdirectories(candidates, userHome.resolve(".gradle").resolve("jdks"));
        // jEnv
        scanSubdirectories(candidates, userHome.resolve(".jenv").resolve("versions"));
        // JBang
        scanSubdirectories(
                candidates, userHome.resolve(".jbang").resolve("cache").resolve("jdks"));
        // asdf
        scanSubdirectories(
                candidates, userHome.resolve(".asdf").resolve("installs").resolve("java"));
        // Jabba
        scanSubdirectories(candidates, userHome.resolve(".jabba").resolve("jdk"));
        // mise (formerly rtx)
        scanSubdirectories(
                candidates,
                userHome.resolve(".local")
                        .resolve("share")
                        .resolve("mise")
                        .resolve("installs")
                        .resolve("java"));
    }

    /**
     * Collects JDK candidates from OS-specific system directories.
     */
    void collectFromSystemDirectories(Set<Path> candidates, Map<String, String> properties) {
        if (Os.IS_WINDOWS) {
            collectWindowsDirectories(candidates, properties);
        } else if (Os.isFamily("mac")) {
            collectMacDirectories(candidates, properties);
        } else {
            collectLinuxDirectories(candidates);
        }
    }

    private void collectLinuxDirectories(Set<Path> candidates) {
        scanSubdirectories(candidates, Paths.get("/usr/lib/jvm"));
        scanSubdirectories(candidates, Paths.get("/usr/lib64/jvm"));
        scanSubdirectories(candidates, Paths.get("/usr/jdk"));
        scanSubdirectories(candidates, Paths.get("/usr/java"));
        scanSubdirectories(candidates, Paths.get("/usr/local/java"));
        scanSubdirectories(candidates, Paths.get("/opt/java"));
        scanSubdirectories(candidates, Paths.get("/opt/hostedtoolcache"));
    }

    private void collectMacDirectories(Set<Path> candidates, Map<String, String> properties) {
        scanSubdirectories(candidates, Paths.get("/Library/Java/JavaVirtualMachines"));
        String userHomeProp = properties.get("user.home");
        if (userHomeProp != null) {
            Path userHome = Paths.get(userHomeProp);
            scanSubdirectories(
                    candidates, userHome.resolve("Library").resolve("Java").resolve("JavaVirtualMachines"));
        }
    }

    private void collectWindowsDirectories(Set<Path> candidates, Map<String, String> properties) {
        Path progFiles = Paths.get("C:\\Program Files");
        scanSubdirectories(candidates, progFiles.resolve("Java"));
        scanSubdirectories(candidates, progFiles.resolve("Eclipse Adoptium"));
        scanSubdirectories(candidates, progFiles.resolve("Zulu"));
        scanSubdirectories(candidates, progFiles.resolve("Amazon Corretto"));
        scanSubdirectories(candidates, progFiles.resolve("BellSoft"));
        // Scoop
        String userHomeProp = properties.get("user.home");
        if (userHomeProp != null) {
            Path userHome = Paths.get(userHomeProp);
            scanSubdirectories(candidates, userHome.resolve("scoop").resolve("apps"));
        }
    }

    /**
     * Lists immediate subdirectories of the given directory and adds them as candidates.
     */
    private void scanSubdirectories(Set<Path> candidates, Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, Files::isDirectory)) {
            for (Path child : stream) {
                addCandidate(candidates, child);
            }
        } catch (IOException e) {
            LOGGER.debug("Cannot scan directory {}: {}", directory, e.getMessage());
        }
    }

    private void addCandidate(Set<Path> candidates, Path path) {
        try {
            candidates.add(path.toRealPath());
        } catch (IOException e) {
            // Broken symlink or inaccessible — add normalized path as fallback
            candidates.add(path.normalize().toAbsolutePath());
        }
    }

    /**
     * Resolves the actual JDK home from a candidate path.
     * On macOS, JDKs may be nested under {@code Contents/Home}.
     */
    Path resolveJdkHome(Path candidate) {
        if (isValidJdkHome(candidate)) {
            return candidate;
        }
        // macOS bundle layout: /path/to/jdk-17.jdk/Contents/Home
        Path contentsHome = candidate.resolve("Contents").resolve("Home");
        if (isValidJdkHome(contentsHome)) {
            return contentsHome;
        }
        return null;
    }

    /**
     * Checks if a directory is a valid JDK home by looking for {@code bin/javac}.
     */
    boolean isValidJdkHome(Path jdkHome) {
        Path bin = jdkHome.resolve("bin");
        return Files.exists(bin.resolve("javac")) || Files.exists(bin.resolve("javac.exe"));
    }

    /**
     * Builds a {@link ToolchainModel} from a validated JDK home by reading the {@code release} file.
     *
     * @return the model, or empty if the version cannot be determined
     */
    Optional<ToolchainModel> buildToolchainModel(Path jdkHome) {
        String version = readVersionFromRelease(jdkHome);
        if (version == null) {
            LOGGER.debug("Cannot determine version for JDK at {}, skipping", jdkHome);
            return Optional.empty();
        }

        int majorVersion = JdkSourceLevelSupport.normalizeSourceLevel(version);
        if (majorVersion <= 0) {
            LOGGER.debug("Cannot parse major version from '{}' for JDK at {}, skipping", version, jdkHome);
            return Optional.empty();
        }

        XmlNode jdkHomeNode = XmlNode.newInstance("jdkHome", jdkHome.toString());
        XmlNode configuration = XmlNode.newInstance("configuration", List.of(jdkHomeNode));

        ToolchainModel model = ToolchainModel.newBuilder()
                .type("jdk")
                .provides(Map.of("version", String.valueOf(majorVersion)))
                .configuration(configuration)
                .build();

        LOGGER.debug("Discovered JDK {} at {}", majorVersion, jdkHome);
        return Optional.of(model);
    }

    /**
     * Reads the {@code JAVA_VERSION} property from the JDK's {@code release} file.
     * The release file format uses shell-style assignments: {@code JAVA_VERSION="17.0.2"}.
     *
     * @return the version string (e.g. "17.0.2"), or null if not found
     */
    String readVersionFromRelease(Path jdkHome) {
        Path releaseFile = jdkHome.resolve("release");
        if (!Files.exists(releaseFile)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(releaseFile)) {
                if (line.startsWith("JAVA_VERSION=")) {
                    String value = line.substring("JAVA_VERSION=".length()).trim();
                    // Remove surrounding quotes
                    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value.isEmpty() ? null : value;
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Cannot read release file at {}: {}", releaseFile, e.getMessage());
        }
        return null;
    }
}
