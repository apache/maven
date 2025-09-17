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
package org.apache.maven.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.Maven;
import org.apache.maven.api.Constants;
import org.apache.maven.cli.transfer.ConsoleMavenTransferListener;
import org.apache.maven.cli.transfer.QuietMavenTransferListener;
import org.apache.maven.cli.transfer.SimplexTransferListener;
import org.apache.maven.cli.transfer.Slf4jMavenTransferListener;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.ProfileActivation;
import org.apache.maven.execution.ProjectActivation;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.model.root.DefaultRootLocator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.transfer.TransferListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;

import static java.util.Arrays.asList;
import static org.apache.maven.cli.MavenCli.performProfileActivation;
import static org.apache.maven.cli.MavenCli.performProjectActivation;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@Deprecated
class MavenCliTest {
    private MavenCli cli;

    private String origBasedir;

    @BeforeEach
    void setUp() {
        cli = new MavenCli();
        origBasedir = System.getProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (origBasedir != null) {
            System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, origBasedir);
        } else {
            System.getProperties().remove(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
        }
    }

    // Helper method for containsExactlyInAnyOrder assertion
    private static <T> void assertContainsExactlyInAnyOrder(Collection<T> actual, T... expected) {
        assertEquals(expected.length, actual.size(), "Collection size mismatch");
        for (T item : expected) {
            assertTrue(actual.contains(item), "Collection should contain: " + item);
        }
    }

    @Test
    void testPerformProfileActivation() throws ParseException {
        final CommandLineParser parser = new DefaultParser();

        final Options options = new Options();
        options.addOption(Option.builder(Character.toString(CLIManager.ACTIVATE_PROFILES))
                .hasArg()
                .build());

        ProfileActivation activation;

        activation = new ProfileActivation();
        performProfileActivation(parser.parse(options, new String[] {"-P", "test1,+test2,?test3,+?test4"}), activation);
        assertContainsExactlyInAnyOrder(activation.getRequiredActiveProfileIds(), "test1", "test2");
        assertContainsExactlyInAnyOrder(activation.getOptionalActiveProfileIds(), "test3", "test4");

        activation = new ProfileActivation();
        performProfileActivation(
                parser.parse(options, new String[] {"-P", "!test1,-test2,-?test3,!?test4"}), activation);
        assertContainsExactlyInAnyOrder(activation.getRequiredInactiveProfileIds(), "test1", "test2");
        assertContainsExactlyInAnyOrder(activation.getOptionalInactiveProfileIds(), "test3", "test4");

        activation = new ProfileActivation();
        performProfileActivation(parser.parse(options, new String[] {"-P", "-test1,+test2"}), activation);
        assertContainsExactlyInAnyOrder(activation.getRequiredActiveProfileIds(), "test2");
        assertContainsExactlyInAnyOrder(activation.getRequiredInactiveProfileIds(), "test1");
    }

    @Test
    void testDetermineProjectActivation() throws ParseException {
        final CommandLineParser parser = new DefaultParser();

        final Options options = new Options();
        options.addOption(Option.builder(CLIManager.PROJECT_LIST).hasArg().build());

        ProjectActivation activation;

        activation = new ProjectActivation();
        performProjectActivation(
                parser.parse(options, new String[] {"-pl", "test1,+test2,?test3,+?test4"}), activation);
        assertContainsExactlyInAnyOrder(activation.getRequiredActiveProjectSelectors(), "test1", "test2");
        assertContainsExactlyInAnyOrder(activation.getOptionalActiveProjectSelectors(), "test3", "test4");

        activation = new ProjectActivation();
        performProjectActivation(
                parser.parse(options, new String[] {"-pl", "!test1,-test2,-?test3,!?test4"}), activation);
        assertContainsExactlyInAnyOrder(activation.getRequiredInactiveProjectSelectors(), "test1", "test2");
        assertContainsExactlyInAnyOrder(activation.getOptionalInactiveProjectSelectors(), "test3", "test4");

        activation = new ProjectActivation();
        performProjectActivation(parser.parse(options, new String[] {"-pl", "-test1,+test2"}), activation);
        assertContainsExactlyInAnyOrder(activation.getRequiredActiveProjectSelectors(), "test2");
        assertContainsExactlyInAnyOrder(activation.getRequiredInactiveProjectSelectors(), "test1");
    }

    @Test
    void testCalculateDegreeOfConcurrency() {
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("0"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("-1"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("0x4"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("1.0"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("1."));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("AA"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("C"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("C2.2C"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("C2.2"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("2C2"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("CXXX"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("XXXC"));

        int cpus = Runtime.getRuntime().availableProcessors();
        assertEquals((int) (cpus * 2.2), cli.calculateDegreeOfConcurrency("2.2C"));
        assertEquals(1, cli.calculateDegreeOfConcurrency("0.0001C"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("-2.2C"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("0C"));
    }

    @Test
    void testMavenConfig() throws Exception {
        System.setProperty(
                MavenCli.MULTIMODULE_PROJECT_DIRECTORY, new File("src/test/projects/config").getCanonicalPath());
        CliRequest request = new CliRequest(new String[0], null);

        // read .mvn/maven.config
        cli.initialize(request);
        cli.cli(request);
        assertEquals("multithreaded", request.commandLine.getOptionValue(CLIManager.BUILDER));
        assertEquals("8", request.commandLine.getOptionValue(CLIManager.THREADS));

        // override from command line
        request = new CliRequest(new String[] {"--builder", "foobar"}, null);
        cli.cli(request);
        assertEquals("foobar", request.commandLine.getOptionValue("builder"));
    }

    @Test
    void testMavenConfigInvalid() throws Exception {
        System.setProperty(
                MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                new File("src/test/projects/config-illegal").getCanonicalPath());
        CliRequest request = new CliRequest(new String[0], null);

        cli.initialize(request);
        assertThrows(ParseException.class, () -> cli.cli(request));
    }

    /**
     * Read .mvn/maven.config with the following definitions:
     * <pre>
     *   -T
     *   3
     *   -Drevision=1.3.0
     *   "-Dlabel=Apache Maven"
     * </pre>
     * and check if the {@code -T 3} option can be overwritten via command line
     * argument.
     *
     * @throws Exception in case of failure.
     */
    @Test
    void testMVNConfigurationThreadCanBeOverwrittenViaCommandLine() throws Exception {
        System.setProperty(
                MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                new File("src/test/projects/mavenConfigProperties").getCanonicalPath());
        CliRequest request = new CliRequest(new String[] {"-T", "5"}, null);

        cli.initialize(request);
        // read .mvn/maven.config
        cli.cli(request);

        assertEquals("5", request.commandLine.getOptionValue(CLIManager.THREADS));
    }

    /**
     * Read .mvn/maven.config with the following definitions:
     * <pre>
     *   -T
     *   3
     *   -Drevision=1.3.0
     *   "-Dlabel=Apache Maven"
     * </pre>
     * and check if the {@code -Drevision-1.3.0} option can be overwritten via command line
     * argument.
     *
     * @throws Exception
     */
    @Test
    void testMVNConfigurationDefinedPropertiesCanBeOverwrittenViaCommandLine() throws Exception {
        System.setProperty(
                MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                new File("src/test/projects/mavenConfigProperties").getCanonicalPath());
        CliRequest request = new CliRequest(new String[] {"-Drevision=8.1.0"}, null);

        cli.initialize(request);
        // read .mvn/maven.config
        cli.cli(request);
        cli.properties(request);

        String revision = request.getUserProperties().getProperty("revision");
        assertEquals("8.1.0", revision);
    }

    /**
     * Read .mvn/maven.config with the following definitions:
     * <pre>
     *   -T
     *   3
     *   -Drevision=1.3.0
     *   "-Dlabel=Apache Maven"
     * </pre>
     * and check if the {@code -Drevision-1.3.0} option can be overwritten via command line
     * argument.
     *
     * @throws Exception
     */
    @Test
    void testMVNConfigurationCLIRepeatedPropertiesLastWins() throws Exception {
        System.setProperty(
                MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                new File("src/test/projects/mavenConfigProperties").getCanonicalPath());
        CliRequest request = new CliRequest(new String[] {"-Drevision=8.1.0", "-Drevision=8.2.0"}, null);

        cli.initialize(request);
        // read .mvn/maven.config
        cli.cli(request);
        cli.properties(request);

        String revision = request.getUserProperties().getProperty("revision");
        assertEquals("8.2.0", revision);
    }

    /**
     * Read .mvn/maven.config with the following definitions:
     * <pre>
     *   -T
     *   3
     *   -Drevision=1.3.0
     *   "-Dlabel=Apache Maven"
     * </pre>
     * and check if the {@code -Drevision-1.3.0} option can be overwritten via command line argument when there are
     * funky arguments present.
     *
     * @throws Exception
     */
    @Test
    void testMVNConfigurationFunkyArguments() throws Exception {
        System.setProperty(
                MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                new File("src/test/projects/mavenConfigProperties").getCanonicalPath());
        CliRequest request = new CliRequest(
                new String[] {
                    "-Drevision=8.1.0", "--file=-Dpom.xml", "\"-Dfoo=bar ", "\"-Dfoo2=bar two\"", "-Drevision=8.2.0"
                },
                null);

        cli.initialize(request);
        // read .mvn/maven.config
        cli.cli(request);
        cli.properties(request);

        assertEquals("3", request.commandLine.getOptionValue(CLIManager.THREADS));

        String revision = request.getUserProperties().getProperty("revision");
        assertEquals("8.2.0", revision);

        assertEquals("bar ", request.getUserProperties().getProperty("foo"));
        assertEquals("bar two", request.getUserProperties().getProperty("foo2"));
        assertEquals("Apache Maven", request.getUserProperties().getProperty("label"));

        assertEquals("-Dpom.xml", request.getCommandLine().getOptionValue(CLIManager.ALTERNATE_POM_FILE));
    }

    @Test
    void testStyleColors() throws Exception {
        assumeTrue(MessageUtils.isColorEnabled(), "ANSI not supported");
        CliRequest request;

        MessageUtils.setColorEnabled(true);
        request = new CliRequest(new String[] {"-B"}, null);
        cli.cli(request);
        cli.properties(request);
        cli.logging(request);
        assertFalse(MessageUtils.isColorEnabled(), "Expected MessageUtils.isColorEnabled() to return false");

        MessageUtils.setColorEnabled(true);
        request = new CliRequest(new String[] {"--non-interactive"}, null);
        cli.cli(request);
        cli.properties(request);
        cli.logging(request);
        assertFalse(MessageUtils.isColorEnabled(), "Expected MessageUtils.isColorEnabled() to return false");

        MessageUtils.setColorEnabled(true);
        request = new CliRequest(new String[] {"--force-interactive", "--non-interactive"}, null);
        cli.cli(request);
        cli.properties(request);
        cli.logging(request);
        assertTrue(MessageUtils.isColorEnabled(), "Expected MessageUtils.isColorEnabled() to return true");

        MessageUtils.setColorEnabled(true);
        request = new CliRequest(new String[] {"-l", "target/temp/mvn.log"}, null);
        request.workingDirectory = "target/temp";
        cli.cli(request);
        cli.properties(request);
        cli.logging(request);
        assertFalse(MessageUtils.isColorEnabled(), "Expected MessageUtils.isColorEnabled() to return false");

        MessageUtils.setColorEnabled(false);
        request = new CliRequest(new String[] {"-Dstyle.color=always"}, null);
        cli.cli(request);
        cli.properties(request);
        cli.logging(request);
        assertTrue(MessageUtils.isColorEnabled(), "Expected MessageUtils.isColorEnabled() to return true");

        MessageUtils.setColorEnabled(true);
        request = new CliRequest(new String[] {"-Dstyle.color=never"}, null);
        cli.cli(request);
        cli.properties(request);
        cli.logging(request);
        assertFalse(MessageUtils.isColorEnabled(), "Expected MessageUtils.isColorEnabled() to return false");

        MessageUtils.setColorEnabled(false);
        request = new CliRequest(new String[] {"-Dstyle.color=always", "-B", "-l", "target/temp/mvn.log"}, null);
        request.workingDirectory = "target/temp";
        cli.cli(request);
        cli.properties(request);
        cli.logging(request);
        assertTrue(MessageUtils.isColorEnabled(), "Expected MessageUtils.isColorEnabled() to return true");

        MessageUtils.setColorEnabled(false);
        CliRequest maybeColorRequest =
                new CliRequest(new String[] {"-Dstyle.color=maybe", "-B", "-l", "target/temp/mvn.log"}, null);
        request.workingDirectory = "target/temp";
        cli.cli(maybeColorRequest);
        cli.properties(maybeColorRequest);
        assertThrows(
                IllegalArgumentException.class, () -> cli.logging(maybeColorRequest), "maybe is not a valid option");
    }

    /**
     * Verifies MNG-6558
     */
    @Test
    void testToolchainsBuildingEvents() throws Exception {
        final EventSpyDispatcher eventSpyDispatcherMock = mock(EventSpyDispatcher.class);
        MavenCli customizedMavenCli = new MavenCli() {
            @Override
            protected void customizeContainer(PlexusContainer container) {
                super.customizeContainer(container);
                container.addComponent(mock(Maven.class), "org.apache.maven.Maven");

                ((DefaultPlexusContainer) container)
                        .addPlexusInjector(Collections.emptyList(), binder -> binder.bind(EventSpyDispatcher.class)
                                .toInstance(eventSpyDispatcherMock));
            }
        };

        CliRequest cliRequest = new CliRequest(new String[] {}, null);

        customizedMavenCli.cli(cliRequest);
        customizedMavenCli.logging(cliRequest);
        customizedMavenCli.container(cliRequest);
        customizedMavenCli.toolchains(cliRequest);

        InOrder orderedEventSpyDispatcherMock = inOrder(eventSpyDispatcherMock);
        orderedEventSpyDispatcherMock
                .verify(eventSpyDispatcherMock, times(1))
                .onEvent(any(ToolchainsBuildingRequest.class));
        orderedEventSpyDispatcherMock
                .verify(eventSpyDispatcherMock, times(1))
                .onEvent(any(ToolchainsBuildingResult.class));
    }

    @Test
    void resumeFromSelectorIsSuggestedWithoutGroupId() {
        List<MavenProject> allProjects =
                asList(createMavenProject("group", "module-a"), createMavenProject("group", "module-b"));
        MavenProject failedProject = allProjects.get(0);

        String selector = cli.getResumeFromSelector(allProjects, failedProject);

        assertEquals(":module-a", selector);
    }

    @Test
    void resumeFromSelectorContainsGroupIdWhenArtifactIdIsNotUnique() {
        List<MavenProject> allProjects =
                asList(createMavenProject("group-a", "module"), createMavenProject("group-b", "module"));
        MavenProject failedProject = allProjects.get(0);

        String selector = cli.getResumeFromSelector(allProjects, failedProject);

        assertEquals("group-a:module", selector);
    }

    @Test
    void verifyLocalRepositoryPath() throws Exception {
        MavenCli cli = new MavenCli();
        CliRequest request = new CliRequest(new String[] {}, null);
        request.commandLine = new CommandLine.Builder().build();
        MavenExecutionRequest executionRequest;

        // Use default
        cli.cli(request);
        executionRequest = cli.populateRequest(request);
        assertNull(executionRequest.getLocalRepositoryPath());

        // System-properties override default
        request.getSystemProperties().setProperty(Constants.MAVEN_REPO_LOCAL, "." + File.separatorChar + "custom1");
        executionRequest = cli.populateRequest(request);
        assertNotNull(executionRequest.getLocalRepositoryPath());
        assertEquals(
                "." + File.separatorChar + "custom1",
                executionRequest.getLocalRepositoryPath().toString());

        // User-properties override system properties
        request.getUserProperties().setProperty(Constants.MAVEN_REPO_LOCAL, "." + File.separatorChar + "custom2");
        executionRequest = cli.populateRequest(request);
        assertNotNull(executionRequest.getLocalRepositoryPath());
        assertEquals(
                "." + File.separatorChar + "custom2",
                executionRequest.getLocalRepositoryPath().toString());
    }

    /**
     * MNG-7032: Disable colours for {@code --version} if {@code --batch-mode} is also given.
     * @throws Exception cli invocation.
     */
    @Test
    void testVersionStringWithoutAnsi() throws Exception {
        // given
        // - request with version and batch mode
        CliRequest cliRequest = new CliRequest(new String[] {"--version", "--batch-mode"}, null);
        ByteArrayOutputStream systemOut = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(systemOut));

        // when
        try {
            cli.cli(cliRequest);
        } catch (MavenCli.ExitException exitException) {
            // expected
        } finally {
            // restore sysout
            System.setOut(oldOut);
        }
        String versionOut = new String(systemOut.toByteArray(), StandardCharsets.UTF_8);

        // then
        assertEquals(stripAnsiCodes(versionOut), versionOut);
    }

    @Test
    void populatePropertiesCanContainEqualsSign() throws Exception {
        // Arrange
        CliRequest request = new CliRequest(new String[] {"-Dw=x=y", "validate"}, null);

        // Act
        cli.cli(request);
        cli.properties(request);

        // Assert
        assertEquals("x=y", request.getUserProperties().getProperty("w"));
    }

    @Test
    void populatePropertiesSpace() throws Exception {
        // Arrange
        CliRequest request = new CliRequest(new String[] {"-D", "z=2", "validate"}, null);

        // Act
        cli.cli(request);
        cli.properties(request);

        // Assert
        assertEquals("2", request.getUserProperties().getProperty("z"));
    }

    @Test
    void populatePropertiesShorthand() throws Exception {
        // Arrange
        CliRequest request = new CliRequest(new String[] {"-Dx", "validate"}, null);

        // Act
        cli.cli(request);
        cli.properties(request);

        // Assert
        assertEquals("true", request.getUserProperties().getProperty("x"));
    }

    @Test
    void populatePropertiesMultiple() throws Exception {
        // Arrange
        CliRequest request = new CliRequest(new String[] {"-Dx=1", "-Dy", "validate"}, null);

        // Act
        cli.cli(request);
        cli.properties(request);

        // Assert
        assertEquals("1", request.getUserProperties().getProperty("x"));
        assertEquals("true", request.getUserProperties().getProperty("y"));
    }

    @Test
    void populatePropertiesOverwrite() throws Exception {
        // Arrange
        CliRequest request = new CliRequest(new String[] {"-Dx", "-Dx=false", "validate"}, null);

        // Act
        cli.cli(request);
        cli.properties(request);

        // Assert
        assertEquals("false", request.getUserProperties().getProperty("x"));
    }

    @Test
    public void findRootProjectWithAttribute() {
        Path test = Paths.get("src/test/projects/root-attribute");
        assertEquals(test, new DefaultRootLocator().findRoot(test.resolve("child")));
    }

    @Test
    public void testPropertiesInterpolation() throws Exception {
        FileSystem fs = Jimfs.newFileSystem(Configuration.windows());

        Path mavenHome = fs.getPath("C:\\maven");
        Files.createDirectories(mavenHome);
        Path mavenConf = mavenHome.resolve("conf");
        Files.createDirectories(mavenConf);
        Path mavenUserProps = mavenConf.resolve("maven-user.properties");
        Files.writeString(mavenUserProps, "${includes} = ?${session.rootDirectory}/.mvn/maven-user.properties\n");
        Path rootDirectory = fs.getPath("C:\\myRootDirectory");
        Path topDirectory = rootDirectory.resolve("myTopDirectory");
        Path mvn = rootDirectory.resolve(".mvn");
        Files.createDirectories(mvn);
        Files.writeString(
                mvn.resolve("maven-user.properties"),
                "${includes} = env-${envName}.properties\nfro = ${bar}z\n" + "bar = chti${java.version}\n");
        Files.writeString(mvn.resolve("env-test.properties"), "\n");

        // Arrange
        CliRequest request = new CliRequest(
                new String[] {
                    "-DenvName=test",
                    "-Dfoo=bar",
                    "-DvalFound=s${foo}i",
                    "-DvalNotFound=s${foz}i",
                    "-DvalRootDirectory=${session.rootDirectory}/.mvn/foo",
                    "-DvalTopDirectory=${session.topDirectory}/pom.xml",
                    "-f",
                    "${session.rootDirectory}/my-child",
                    "prefix:3.0.0:${foo}",
                    "validate"
                },
                null);
        request.rootDirectory = rootDirectory;
        request.topDirectory = topDirectory;
        System.setProperty("maven.installation.conf", mavenConf.toString());

        // Act
        cli.setFileSystem(fs);
        cli.cli(request);
        cli.properties(request);

        // Assert
        assertTrue(request.getUserProperties().getProperty("fro").startsWith("chti"));
        assertEquals("sbari", request.getUserProperties().getProperty("valFound"));
        assertEquals("s${foz}i", request.getUserProperties().getProperty("valNotFound"));
        assertEquals("C:\\myRootDirectory/.mvn/foo", request.getUserProperties().getProperty("valRootDirectory"));
        assertEquals(
                "C:\\myRootDirectory\\myTopDirectory/pom.xml",
                request.getUserProperties().getProperty("valTopDirectory"));
        assertEquals("C:\\myRootDirectory/my-child", request.getCommandLine().getOptionValue('f'));
        assertArrayEquals(
                new String[] {"prefix:3.0.0:bar", "validate"},
                request.getCommandLine().getArgs());

        Path p = fs.getPath(request.getUserProperties().getProperty("valTopDirectory"));
        assertEquals("C:\\myRootDirectory\\myTopDirectory\\pom.xml", p.toString());
    }

    @Test
    public void testEmptyProfile() throws Exception {
        CliRequest request = new CliRequest(new String[] {"-P", ""}, null);
        cli.cli(request);
        cli.populateRequest(request);
    }

    @Test
    public void testEmptyProject() throws Exception {
        CliRequest request = new CliRequest(new String[] {"-pl", ""}, null);
        cli.cli(request);
        cli.populateRequest(request);
    }

    @ParameterizedTest
    @MethodSource("activateBatchModeArguments")
    public void activateBatchMode(boolean ciEnv, String[] cliArgs, boolean isBatchMode) throws Exception {
        CliRequest request = new CliRequest(cliArgs, null);
        if (ciEnv) {
            request.getSystemProperties().put("env.CI", "true");
        }
        cli.cli(request);

        boolean batchMode = !cli.populateRequest(request).isInteractiveMode();

        assertEquals(isBatchMode, batchMode);
    }

    public static Stream<Arguments> activateBatchModeArguments() {
        return Stream.of(
                Arguments.of(false, new String[] {}, false),
                Arguments.of(true, new String[] {}, true),
                Arguments.of(true, new String[] {"--force-interactive"}, false),
                Arguments.of(true, new String[] {"--force-interactive", "--non-interactive"}, false),
                Arguments.of(true, new String[] {"--force-interactive", "--batch-mode"}, false),
                Arguments.of(true, new String[] {"--force-interactive", "--non-interactive", "--batch-mode"}, false),
                Arguments.of(false, new String[] {"--non-interactive"}, true),
                Arguments.of(false, new String[] {"--batch-mode"}, true),
                Arguments.of(false, new String[] {"--non-interactive", "--batch-mode"}, true));
    }

    @ParameterizedTest
    @MethodSource("calculateTransferListenerArguments")
    public void calculateTransferListener(boolean ciEnv, String[] cliArgs, Class<TransferListener> expectedSubClass)
            throws Exception {
        CliRequest request = new CliRequest(cliArgs, null);
        if (ciEnv) {
            request.getSystemProperties().put("env.CI", "true");
        }
        cli.cli(request);
        cli.logging(request);

        TransferListener transferListener = cli.populateRequest(request).getTransferListener();
        if (transferListener instanceof SimplexTransferListener simplexTransferListener) {
            transferListener = simplexTransferListener.getDelegate();
        }

        assertEquals(expectedSubClass, transferListener.getClass());
    }

    public static Stream<Arguments> calculateTransferListenerArguments() {
        return Stream.of(
                Arguments.of(false, new String[] {}, ConsoleMavenTransferListener.class),
                Arguments.of(true, new String[] {}, QuietMavenTransferListener.class),
                Arguments.of(false, new String[] {"-ntp"}, QuietMavenTransferListener.class),
                Arguments.of(false, new String[] {"--quiet"}, QuietMavenTransferListener.class),
                Arguments.of(true, new String[] {"--force-interactive"}, ConsoleMavenTransferListener.class),
                Arguments.of(
                        true,
                        new String[] {"--force-interactive", "--non-interactive"},
                        ConsoleMavenTransferListener.class),
                Arguments.of(
                        true, new String[] {"--force-interactive", "--batch-mode"}, ConsoleMavenTransferListener.class),
                Arguments.of(
                        true,
                        new String[] {"--force-interactive", "--non-interactive", "--batch-mode"},
                        ConsoleMavenTransferListener.class),
                Arguments.of(false, new String[] {"--non-interactive"}, Slf4jMavenTransferListener.class),
                Arguments.of(false, new String[] {"--batch-mode"}, Slf4jMavenTransferListener.class),
                Arguments.of(
                        false, new String[] {"--non-interactive", "--batch-mode"}, Slf4jMavenTransferListener.class));
    }

    private MavenProject createMavenProject(String groupId, String artifactId) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        return project;
    }

    static String stripAnsiCodes(String msg) {
        return msg.replaceAll("\u001b\\[[;\\d]*[ -/]*[@-~]", "");
    }
}
