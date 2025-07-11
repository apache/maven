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
package org.apache.maven.cling.invoker.mvn;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Local UT.
 */
@Order(200)
public class MavenInvokerTest extends MavenInvokerTestSupport {
    @Override
    protected Invoker createInvoker(ClassWorld classWorld) {
        return new MavenInvoker(
                ProtoLookup.builder().addMapping(ClassWorld.class, classWorld).build(), null);
    }

    @Override
    protected Parser createParser() {
        return new MavenParser();
    }

    @Test
    void defaultFs(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path cwd,
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path userHome)
            throws Exception {
        invoke(cwd, userHome, List.of("verify"), List.of());
    }

    /**
     * Same source (user or project extensions.xml) must not contain same GA with different V.
     */
    @Test
    void conflictingExtensionsFromSameSource(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path cwd,
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path userHome)
            throws Exception {
        String projectExtensionsXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <extensions>
                    <extension>
                        <groupId>io.takari.maven</groupId>
                        <artifactId>takari-smart-builder</artifactId>
                        <version>1.0.2</version>
                    </extension>
                    <extension>
                        <groupId>io.takari.maven</groupId>
                        <artifactId>takari-smart-builder</artifactId>
                        <version>1.0.1</version>
                    </extension>
                </extensions>
                """;
        Path dotMvn = cwd.resolve(".mvn");
        Files.createDirectories(dotMvn);
        Path projectExtensions = dotMvn.resolve("extensions.xml");
        Files.writeString(projectExtensions, projectExtensionsXml);

        String userExtensionsXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <extensions>
                    <extension>
                        <groupId>io.takari.maven</groupId>
                        <artifactId>takari-smart-builder</artifactId>
                        <version>1.0.2</version>
                    </extension>
                </extensions>
                """;
        Path userConf = userHome.resolve(".m2");
        Files.createDirectories(userConf);
        Path userExtensions = userConf.resolve("extensions.xml");
        Files.writeString(userExtensions, userExtensionsXml);

        assertThrows(InvokerException.class, () -> invoke(cwd, userHome, List.of("validate"), List.of()));
    }

    /**
     * In case of conflict spanning different sources, precedence is applied: project > user > installation.
     */
    @Test
    void conflictingExtensionsFromDifferentSource(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path cwd,
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path userHome)
            throws Exception {
        String extensionsXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <extensions>
                    <extension>
                        <groupId>io.takari.maven</groupId>
                        <artifactId>takari-smart-builder</artifactId>
                        <version>1.0.2</version>
                    </extension>
                </extensions>
                """;
        Path dotMvn = cwd.resolve(".mvn");
        Files.createDirectories(dotMvn);
        Path projectExtensions = dotMvn.resolve("extensions.xml");
        Files.writeString(projectExtensions, extensionsXml);

        Path userConf = userHome.resolve(".m2");
        Files.createDirectories(userConf);
        Path userExtensions = userConf.resolve("extensions.xml");
        Files.writeString(userExtensions, extensionsXml);

        // this should not throw
        assertDoesNotThrow(() -> invoke(cwd, userHome, List.of("validate"), List.of()));
        // but warn

        // [main] WARNING org.apache.maven.cling.invoker.PlexusContainerCapsuleFactory - Found 1 extension conflict(s):
        // [main] WARNING org.apache.maven.cling.invoker.PlexusContainerCapsuleFactory - * Conflicting extension
        // eu.maveniverse.maven.mimir:extension3: /tmp/junit-191051426131307150/.mvn/extensions.xml:3 vs
        // /tmp/junit-16591192886395443631/.m2/extensions.xml:3
        // [main] WARNING org.apache.maven.cling.invoker.PlexusContainerCapsuleFactory -
        // [main] WARNING org.apache.maven.cling.invoker.PlexusContainerCapsuleFactory - Order of core extensions
        // precedence is project > user > installation. Selected extensions are:
        // [main] WARNING org.apache.maven.cling.invoker.PlexusContainerCapsuleFactory - *
        // eu.maveniverse.maven.mimir:extension3:0.3.4 configured in /tmp/junit-191051426131307150/.mvn/extensions.xml:3
    }

    @Test
    void conflictingSettings(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path cwd,
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path userHome)
            throws Exception {
        String settingsXml =
                """
<?xml version="1.0"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <interactiveMode>false</interactiveMode>
  <profiles>
    <profile>
      <id>oss-development</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2/</url>
          <releases>
            <enabled>true</enabled>
            <updatePolicy>never</updatePolicy>
          </releases>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2/</url>
          <releases>
            <enabled>true</enabled>
            <updatePolicy>never</updatePolicy>
          </releases>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>oss-development</activeProfile>
  </activeProfiles>
</settings>""";
        Path dotMvn = cwd.resolve(".mvn");
        Files.createDirectories(dotMvn);
        Path projectExtensions = dotMvn.resolve("settings.xml");
        Files.writeString(projectExtensions, settingsXml);

        Path userConf = userHome.resolve(".m2");
        Files.createDirectories(userConf);
        Path userExtensions = userConf.resolve("settings.xml");
        Files.writeString(userExtensions, settingsXml);

        // we just execute a Mojo for downloading it only and to assert from which URL it came
        Map<String, String> logs = invoke(
                cwd,
                userHome,
                List.of("eu.maveniverse.maven.plugins:toolbox:0.7.4:help"),
                List.of("--force-interactive"));

        String log = logs.get("eu.maveniverse.maven.plugins:toolbox:0.7.4:help");
        assertTrue(log.contains("https://repo1.maven.org/maven2"), log);
        assertFalse(log.contains("https://repo.maven.apache.org/maven2"), log);
    }

    @Disabled("Until we move off fully from File")
    @Test
    void jimFs() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            invoke(fs.getPath("/cwd"), fs.getPath("/home"), List.of("verify"), List.of());
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void msysRepoPathIsNormalised(@TempDir Path tmp) throws Exception {

        final Path cwd = tmp.resolve("project");
        final Path userHome = tmp.resolve("userHome");
        Files.createDirectories(cwd);
        Files.createDirectories(userHome.resolve(".m2"));

        /* ---------- PATCH: write minimal, well-formed toolchains.xml ---------- */
        Files.writeString(
                userHome.resolve(".m2/toolchains.xml"),
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 https://maven.apache.org/xsd/toolchains-1.1.0.xsd">
            </toolchains>
            """);

        /* minimal POM so Maven has something to parse */
        Files.writeString(
                cwd.resolve("pom.xml"),
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.apache.maven.samples</groupId>
                <artifactId>sample</artifactId>
                <version>1.0-SNAPSHOT</version>
            </project>
            """);

        /* pretend we are on Windows so the normaliser runs */
        final String origOs = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");
        try {
            final List<String> args = List.of("-Dmaven.repo.local=/c/projects/mmm/conf/.m2/repository");
            final List<String> goals = List.of("validate");

            final Map<String, String> logs = invoke(cwd, userHome, goals, args);
            final String log = String.join("", logs.values());

            /* no doubled-drive repository attempt */
            assertFalse(
                    log.contains("\\c\\projects") || log.contains("\\d\\projects"),
                    "Maven still tried to use a doubled-drive path:\n" + log);

        } finally {
            if (origOs == null) {
                System.clearProperty("os.name");
            } else {
                System.setProperty("os.name", origOs);
            }
        }
    }
}
