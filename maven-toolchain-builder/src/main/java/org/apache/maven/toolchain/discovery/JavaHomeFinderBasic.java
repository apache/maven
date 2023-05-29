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
package org.apache.maven.toolchain.discovery;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaHomeFinderBasic {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final List<Supplier<? extends Set<String>>> myFinders = new ArrayList<>();

    private boolean myCheckEmbeddedJava = false;

    private String[] mySpecifiedPaths = new String[0];

    public JavaHomeFinderBasic() {
        myFinders.add(this::findInPATH);
        myFinders.add(this::findInJavaHome);
        myFinders.add(this::findInSpecifiedPaths);
        myFinders.add(this::findJavaInstalledBySdkMan);
        myFinders.add(this::findJavaInstalledByAsdfJava);
        myFinders.add(this::findJavaInstalledByGradle);

        myFinders.add(() -> myCheckEmbeddedJava ? scanAll(getJavaHome(), false) : Collections.emptySet());
    }

    static String execCommand(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        try (InputStream is = process.getInputStream();
                ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[128];
            for (int length = is.read(buffer); length > 0; length = is.read(buffer)) {
                os.write(buffer, 0, length);
            }
            return new String(os.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            process.waitFor();
        }
    }

    public JavaHomeFinderBasic checkEmbeddedJava(boolean value) {
        myCheckEmbeddedJava = value;
        return this;
    }

    public JavaHomeFinderBasic checkSpecifiedPaths(String... paths) {
        mySpecifiedPaths = paths;
        return this;
    }

    private Set<String> findInSpecifiedPaths() {
        List<Path> paths = Stream.of(mySpecifiedPaths).map(Paths::get).collect(Collectors.toList());
        return scanAll(paths, true);
    }

    protected void registerFinder(Supplier<? extends Set<String>> finder) {
        myFinders.add(finder);
    }

    public final Set<String> findExistingJdks() {
        return myFinders.parallelStream()
                .flatMap(finder -> {
                    try {
                        return finder.get().stream();
                    } catch (Exception e) {
                        log.warn("Failed to find Java Home. " + e.getMessage(), e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toSet());
    }

    private Set<String> findInJavaHome() {
        String javaHome = getEnvironmentVariable("JAVA_HOME");
        return javaHome != null ? scanAll(Paths.get(javaHome), false) : Collections.emptySet();
    }

    private Set<String> findInPATH() {
        try {
            String pathVarString = getEnvironmentVariable("PATH");
            if (pathVarString == null || pathVarString.isEmpty()) {
                return Collections.emptySet();
            }

            Set<Path> dirsToCheck = new HashSet<>();
            for (String p : pathVarString.split(File.pathSeparator)) {
                Path dir = Paths.get(p);
                String fileName = dir.getFileName().toString();
                if (!JavaHomeFinder.IS_WINDOWS && !JavaHomeFinder.IS_MAC) {
                    fileName = fileName.toLowerCase(Locale.ROOT);
                }
                if (!"bin".equals(fileName)) {
                    continue;
                }

                Path parentFile = dir.getParent();
                if (parentFile == null) {
                    continue;
                }

                dirsToCheck.addAll(listPossibleJdkInstallRootsFromHomes(parentFile));
            }

            return scanAll(dirsToCheck, false);
        } catch (Exception e) {
            log.warn("Failed to scan PATH for JDKs. " + e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    protected Set<String> scanAll(Path file, boolean includeNestDirs) {
        if (file == null) {
            return Collections.emptySet();
        }
        return scanAll(Collections.singleton(file), includeNestDirs);
    }

    protected Set<String> scanAll(Collection<? extends Path> files, boolean includeNestDirs) {
        Set<String> result = new HashSet<>();
        for (Path root : new HashSet<>(files)) {
            scanFolder(root, includeNestDirs, result);
        }
        return result;
    }

    protected void scanFolder(Path folder, boolean includeNestDirs, Collection<? super String> result) {
        if (checkForJdk(folder)) {
            result.add(folder.toAbsolutePath().toString());
            return;
        }

        if (!includeNestDirs) {
            return;
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.forEach(candidate -> {
                for (Path adjusted : listPossibleJdkHomesFromInstallRoot(candidate)) {
                    scanFolder(adjusted, false, result);
                }
            });
        } catch (IOException ignore) {
        }
    }

    protected List<Path> listPossibleJdkHomesFromInstallRoot(Path path) {
        return Collections.singletonList(path);
    }

    protected List<Path> listPossibleJdkInstallRootsFromHomes(Path file) {
        return Collections.singletonList(file);
    }

    private static Path getJavaHome() {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        return Files.isDirectory(javaHome) ? javaHome : null;
    }

    /**
     * Finds Java home directories installed by <a href="https://github.com/sdkman">SDKMAN</a>
     */
    private Set<String> findJavaInstalledBySdkMan() {
        try {
            Path candidatesDir = findSdkManCandidatesDir();
            if (candidatesDir == null) {
                return Collections.emptySet();
            }
            Path javasDir = candidatesDir.resolve("java");
            if (!Files.isDirectory(javasDir)) {
                return Collections.emptySet();
            }
            return listJavaHomeDirsInstalledBySdkMan(javasDir);
        } catch (Exception e) {
            log.warn(
                    "Unexpected exception while looking for Sdkman directory: "
                            + e.getClass().getSimpleName() + ": " + e.getMessage(),
                    e);
            return Collections.emptySet();
        }
    }

    private Set<String> findJavaInstalledByGradle() {
        Path jdks = getPathInUserHome(".gradle/jdks");
        return jdks != null && Files.isDirectory(jdks) ? scanAll(jdks, true) : Collections.emptySet();
    }

    private Path findSdkManCandidatesDir() {
        // first, try the special environment variable
        String candidatesPath = getEnvironmentVariable("SDKMAN_CANDIDATES_DIR");
        if (candidatesPath != null) {
            Path candidatesDir = Paths.get(candidatesPath);
            if (Files.isDirectory(candidatesDir)) {
                return candidatesDir;
            }
        }

        // then, try to use its 'primary' variable
        String primaryPath = getEnvironmentVariable("SDKMAN_DIR");
        if (primaryPath != null) {
            Path candidatesDir = Paths.get(primaryPath, "candidates");
            if (Files.isDirectory(candidatesDir)) {
                return candidatesDir;
            }
        }

        // finally, try the usual location in UNIX
        if (!(this instanceof JavaHomeFinderWindows)) {
            Path candidates = getPathInUserHome(".sdkman/candidates");
            if (candidates != null && Files.isDirectory(candidates)) {
                return candidates;
            }
        }

        // no chances
        return null;
    }

    protected String getEnvironmentVariable(String name) {
        // TODO:
        // https://github.com/JetBrains/intellij-community/blob/cc98eb4ac9cdc9bff55384f1c880d0d82e8b05fd/platform/util/src/com/intellij/util/EnvironmentUtil.java#L72C4-L81
        return System.getenv(name);
    }

    protected Path getPathInUserHome(String relativePath) {
        Path userHome = Paths.get(System.getProperty("user.home"));
        return userHome.resolve(relativePath);
    }

    private Set<String> listJavaHomeDirsInstalledBySdkMan(Path javasDir) {
        boolean mac = this instanceof JavaHomeFinderMac;
        HashSet<String> result = new HashSet<>();

        try (Stream<Path> stream = Files.list(javasDir)) {
            List<Path> innerDirectories = stream.filter(Files::isDirectory).collect(Collectors.toList());
            for (Path innerDir : innerDirectories) {
                Path home = innerDir;
                Path releaseFile = home.resolve("release");
                if (!safeExists(releaseFile)) {
                    continue;
                }

                if (mac) {
                    // Zulu JDK on macOS has a rogue layout, with which Gradle failed to operate (see the bugreport
                    // IDEA-253051),
                    // and in order to get Gradle working with Zulu JDK we should use it's second home (when symbolic
                    // links are resolved).
                    try {
                        if (Files.isSymbolicLink(releaseFile)) {
                            Path realReleaseFile = releaseFile.toRealPath();
                            if (!safeExists(realReleaseFile)) {
                                log.warn("Failed to resolve the target file (it doesn't exist) for: " + releaseFile);
                                continue;
                            }
                            Path realHome = realReleaseFile.getParent();
                            if (realHome == null) {
                                log.warn(
                                        "Failed to resolve the target file (it has no parent dir) for: " + releaseFile);
                                continue;
                            }
                            home = realHome;
                        }
                    } catch (IOException ioe) {
                        log.warn("Failed to resolve the target file for: " + releaseFile + ": " + ioe.getMessage());
                        continue;
                    } catch (Exception e) {
                        log.warn("Failed to resolve the target file for: " + releaseFile + ": Unexpected exception "
                                + e.getClass().getSimpleName() + ": " + e.getMessage());
                        continue;
                    }
                }

                result.add(home.toString());
            }
        } catch (IOException ioe) {
            log.warn("I/O exception while listing Java home directories installed by Sdkman: " + ioe.getMessage(), ioe);
            return Collections.emptySet();
        } catch (Exception e) {
            log.warn(
                    "Unexpected exception while listing Java home directories installed by Sdkman: "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage(),
                    e);
            return Collections.emptySet();
        }

        return result;
    }

    /**
     * Finds Java home directories installed by <a href="https://github.com/halcyon/asdf-java">asdf-java</a>
     */
    private Set<String> findJavaInstalledByAsdfJava() {
        Path installsDir = findAsdfInstallsDir();
        if (installsDir == null) {
            return Collections.emptySet();
        }
        Path javasDir = installsDir.resolve("java");
        return safeIsDirectory(javasDir) ? scanAll(javasDir, true) : Collections.emptySet();
    }

    private Path findAsdfInstallsDir() {
        // try to use environment variable for custom data directory
        // https://asdf-vm.com/#/core-configuration?id=environment-variables
        String dataDir = getEnvironmentVariable("ASDF_DATA_DIR");
        if (dataDir != null) {
            Path primaryDir = Paths.get(dataDir);
            if (safeIsDirectory(primaryDir)) {
                Path installsDir = primaryDir.resolve("installs");
                if (safeIsDirectory(installsDir)) {
                    return installsDir;
                }
            }
        }

        // finally, try the usual location in Unix or macOS
        if (!(this instanceof JavaHomeFinderWindows)) {
            Path installsDir = getPathInUserHome(".asdf/installs");
            if (installsDir != null && safeIsDirectory(installsDir)) {
                return installsDir;
            }
        }

        // no chances
        return null;
    }

    private boolean safeIsDirectory(Path dir) {
        try {
            return Files.isDirectory(dir);
        } catch (SecurityException se) {
            return false; // when a directory is not accessible we should ignore it
        } catch (Exception e) {
            log.debug(
                    "Failed to check directory existence: unexpected exception "
                            + e.getClass().getSimpleName() + ": " + e.getMessage(),
                    e);
            return false;
        }
    }

    private boolean safeExists(Path path) {
        try {
            return Files.exists(path);
        } catch (Exception e) {
            log.debug(
                    "Failed to check file existence: unexpected exception "
                            + e.getClass().getSimpleName() + ": " + e.getMessage(),
                    e);
            return false;
        }
    }

    public static boolean checkForJdk(Path homePath) {
        return (Files.exists(homePath.resolve("bin/javac")) || Files.exists(homePath.resolve("bin/javac.exe")))
                && (isModularRuntime(homePath)
                        || // Jigsaw JDK/JRE
                        Files.exists(homePath.resolve("jre/lib/rt.jar"))
                        || // pre-modular JDK
                        Files.isDirectory(homePath.resolve("classes"))
                        || // custom build
                        Files.exists(homePath.resolve("jre/lib/vm.jar"))
                        || // IBM JDK
                        Files.exists(homePath.resolve("../Classes/classes.jar"))); // Apple JDK
    }

    public static boolean isModularRuntime(Path homePath) {
        return Files.isRegularFile(homePath.resolve("lib/jrt-fs.jar")) || isExplodedModularRuntime(homePath);
    }

    public static boolean isExplodedModularRuntime(Path homePath) {
        return Files.isDirectory(homePath.resolve("modules/java.base"));
    }
}
