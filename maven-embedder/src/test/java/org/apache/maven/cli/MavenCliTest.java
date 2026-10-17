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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.commons.cli.ParseException;
import org.apache.maven.Maven;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class MavenCliTest {
    MavenCli cli;

    private String origBasedir;

    @BeforeEach
    public void setUp() {
        cli = new MavenCli();
        origBasedir = System.getProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (origBasedir != null) {
            System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, origBasedir);
        } else {
            System.getProperties().remove(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
        }
    }

    @Test
    public void testCalculateDegreeOfConcurrency() {
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
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("1c"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency(" 1c"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("1c "));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency(" 1c "));

        int cpus = Runtime.getRuntime().availableProcessors();
        assertEquals((int) (cpus * 2.2), cli.calculateDegreeOfConcurrency("2.2C"));
        assertEquals(1, cli.calculateDegreeOfConcurrency("0.0001C"));
        assertEquals(1 * cpus, cli.calculateDegreeOfConcurrency(" 1C"));
        assertEquals(1 * cpus, cli.calculateDegreeOfConcurrency(" 1C "));
        assertEquals(1, cli.calculateDegreeOfConcurrency(" 1"));
        assertEquals(1, cli.calculateDegreeOfConcurrency(" 1 "));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("-2.2C"));
        assertThrows(IllegalArgumentException.class, () -> cli.calculateDegreeOfConcurrency("0C"));
    }

    @Test
    public void testMavenConfig() throws Exception {
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
    public void testMavenConfigInvalid() throws Exception {
        System.setProperty(
                MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                new File("src/test/projects/config-illegal").getCanonicalPath());
        CliRequest request = new CliRequest(new String[0], null);

        cli.initialize(request);
        try {
            cli.cli(request);
            fail();
        } catch (ParseException expected) {

        }
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
    public void testMVNConfigurationThreadCanBeOverwrittenViaCommandLine() throws Exception {
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
    public void testMVNConfigurationDefinedPropertiesCanBeOverwrittenViaCommandLine() throws Exception {
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
    public void testMVNConfigurationCLIRepeatedPropertiesLastWins() throws Exception {
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
    public void testMVNConfigurationFunkyArguments() throws Exception {
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
        assertEquals("Apache Maven", request.getSystemProperties().getProperty("label"));

        assertEquals("-Dpom.xml", request.getCommandLine().getOptionValue(CLIManager.ALTERNATE_POM_FILE));
    }

    @Nested
    class StyleColors {

        @BeforeEach
        public void setUp() {
            MessageUtils.systemInstall();
            MessageUtils.getTerminalWidth(); // wait for fast terminal initialize
        }

        @AfterEach
        public void tearDown() {
            MessageUtils.systemUninstall();
        }

        @Test
        public void testStyleColors() throws Exception {
            CliRequest request;

            MessageUtils.setColorEnabled(true);
            request = new CliRequest(new String[] {"-B"}, null);
            cli.cli(request);
            cli.properties(request);
            cli.logging(request);
            assertFalse(MessageUtils.isColorEnabled());

            MessageUtils.setColorEnabled(true);
            request = new CliRequest(new String[] {"-l", "target/temp/mvn.log"}, null);
            request.workingDirectory = "target/temp";
            cli.cli(request);
            cli.properties(request);
            cli.logging(request);
            assertFalse(MessageUtils.isColorEnabled());

            MessageUtils.setColorEnabled(false);
            request = new CliRequest(new String[] {"-Dstyle.color=always"}, null);
            cli.cli(request);
            cli.properties(request);
            cli.logging(request);
            assertTrue(MessageUtils.isColorEnabled());

            MessageUtils.setColorEnabled(true);
            request = new CliRequest(new String[] {"-Dstyle.color=never"}, null);
            cli.cli(request);
            cli.properties(request);
            cli.logging(request);
            assertFalse(MessageUtils.isColorEnabled());

            MessageUtils.setColorEnabled(false);
            request = new CliRequest(new String[] {"-Dstyle.color=always", "-B", "-l", "target/temp/mvn.log"}, null);
            request.workingDirectory = "target/temp";
            cli.cli(request);
            cli.properties(request);
            cli.logging(request);
            assertTrue(MessageUtils.isColorEnabled());

            try {
                MessageUtils.setColorEnabled(false);
                request = new CliRequest(new String[] {"-Dstyle.color=maybe", "-B", "-l", "target/temp/mvn.log"}, null);
                request.workingDirectory = "target/temp";
                cli.cli(request);
                cli.properties(request);
                cli.logging(request);
                fail("maybe is not a valid option");
            } catch (IllegalArgumentException e) {
                // noop
            }
        }

        @Test
        void testDumpTerminal() throws Exception {
            // test provide own sys out which is not a tty
            // similar will be with redirected output
            // check that colors are disabled

            MessageUtils.setColorEnabled(true);
            CliRequest request = new CliRequest(new String[] {}, null);
            cli.cli(request);
            cli.properties(request);
            cli.logging(request);
            assertFalse(MessageUtils.isColorEnabled());
        }
    }

    /**
     * Verifies MNG-6558
     */
    @Test
    public void testToolchainsBuildingEvents() throws Exception {
        final EventSpyDispatcher eventSpyDispatcherMock = mock(EventSpyDispatcher.class);
        MavenCli customizedMavenCli = new MavenCli() {
            @Override
            protected void customizeContainer(PlexusContainer container) {
                super.customizeContainer(container);
                container.addComponent(eventSpyDispatcherMock, "org.apache.maven.eventspy.internal.EventSpyDispatcher");
                container.addComponent(mock(Maven.class), "org.apache.maven.Maven");
            }
        };

        CliRequest cliRequest = new CliRequest(new String[] {}, null);

        customizedMavenCli.cli(cliRequest);
        customizedMavenCli.logging(cliRequest);
        customizedMavenCli.container(cliRequest);
        customizedMavenCli.toolchains(cliRequest);

        InOrder orderdEventSpyDispatcherMock = inOrder(eventSpyDispatcherMock);
        orderdEventSpyDispatcherMock
                .verify(eventSpyDispatcherMock, times(1))
                .onEvent(any(ToolchainsBuildingRequest.class));
        orderdEventSpyDispatcherMock
                .verify(eventSpyDispatcherMock, times(1))
                .onEvent(any(ToolchainsBuildingResult.class));
    }

    /**
     * The embedded entry point never goes through {@code main}, so no terminal is installed (#13357).
     */
    @Nested
    class EmbeddedColor {

        @BeforeEach
        void setUp() {
            // restored by the outer @AfterEach
            System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, ".");
            MessageUtils.setColorEnabled(true);
        }

        @AfterEach
        void tearDown() {
            MessageUtils.systemInstall((Terminal) null);
            MessageUtils.setColorEnabled(true);
        }

        private String embeddedVersionOutput(String... extraArgs) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            String[] args = new String[extraArgs.length + 1];
            args[0] = "--version";
            System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
            assertEquals(0, new MavenCli().doMain(args, ".", new PrintStream(out, true), new PrintStream(err, true)));
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }

        @Test
        void colorYesEmitsAnsiAndLeavesNoTerminalBehind() {
            assertNull(MessageUtils.getTerminal());

            String output = embeddedVersionOutput("--color=yes");

            assertTrue(output.contains("Apache Maven"));
            assertNotEquals(MessageUtils.stripAnsiCodes(output), output, "ANSI escapes expected: " + output);
            assertNull(MessageUtils.getTerminal());
        }

        @Test
        void colorNoAndAutoEmitNoAnsi() {
            String no = embeddedVersionOutput("--color=no");
            assertTrue(no.contains("Apache Maven"));
            assertEquals(MessageUtils.stripAnsiCodes(no), no);

            MessageUtils.setColorEnabled(true);
            String auto = embeddedVersionOutput();
            assertTrue(auto.contains("Apache Maven"));
            assertEquals(MessageUtils.stripAnsiCodes(auto), auto);
            assertNull(MessageUtils.getTerminal());
        }

        @Test
        void installedTerminalIsLeftAlone() throws Exception {
            Terminal mine = new DumbTerminal(
                    "test",
                    Terminal.TYPE_DUMB,
                    new ByteArrayInputStream(new byte[0]),
                    new ByteArrayOutputStream(),
                    null);
            MessageUtils.systemInstall(mine);
            try {
                embeddedVersionOutput("--color=yes");
                assertSame(mine, MessageUtils.getTerminal());
            } finally {
                MessageUtils.systemInstall((Terminal) null);
                mine.close();
            }
        }
    }

    /**
     * MNG-7032: Disable colours for {@code --version} if {@code --batch-mode} is also given.
     * @throws Exception cli invocation.
     */
    @Test
    public void testVersionStringWithoutAnsi() throws Exception {
        // given
        // - request with version and batch mode
        CliRequest cliRequest = new CliRequest(new String[] {"--version", "--batch-mode"}, null);
        ByteArrayOutputStream systemOut = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(systemOut));

        System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, ".");

        // when
        try {
            assertEquals(0, cli.doMain(cliRequest));
        } finally {
            // restore sysout
            System.setOut(oldOut);
            System.clearProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
        }
        String versionOut = new String(systemOut.toByteArray(), StandardCharsets.UTF_8);

        assertTrue(versionOut.contains("Apache Maven"));

        // then
        assertEquals(MessageUtils.stripAnsiCodes(versionOut), versionOut);
    }

    @Test
    public void testPropertiesInterpolation() throws Exception {
        // Arrange
        CliRequest request = new CliRequest(
                new String[] {
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
        request.request.setRootDirectory(Paths.get("myRootDirectory"));
        request.request.setTopDirectory(Paths.get("myTopDirectory"));

        // Act
        cli.cli(request);
        cli.properties(request);

        // Assert
        assertEquals("sbari", request.getUserProperties().getProperty("valFound"));
        assertEquals("s${foz}i", request.getUserProperties().getProperty("valNotFound"));
        assertEquals("myRootDirectory/.mvn/foo", request.getUserProperties().getProperty("valRootDirectory"));
        assertEquals("myTopDirectory/pom.xml", request.getUserProperties().getProperty("valTopDirectory"));
        assertEquals("myRootDirectory/my-child", request.getCommandLine().getOptionValue('f'));
        assertArrayEquals(
                new String[] {"prefix:3.0.0:bar", "validate"},
                request.getCommandLine().getArgs());
    }

    @Test
    public void testDistinguishedEnvVariableTransformation() throws Exception {
        try {
            // Arrange
            String url = "https://testDistinguishedEnvVariableTransformation/something";
            System.setProperty("env.MAVEN_REPO_CENTRAL", url);
            CliRequest request = new CliRequest(new String[] {}, null);
            request.request.setRootDirectory(Paths.get("myRootDirectory"));
            request.request.setTopDirectory(Paths.get("myTopDirectory"));

            // Act
            cli.cli(request);
            cli.properties(request);

            // Assert
            assertEquals(url, request.getSystemProperties().getProperty("maven.repo.central"));
        } finally {
            System.clearProperty("env.MAVEN_REPO_CENTRAL");
        }
    }

    /**
     * Verifies that {@code commands()} prints usage and exits with 0 when no goals are specified
     * and no POM exists in the working directory — the main behavioural change in MNG-8346 / PR #13269.
     * Also verifies that the check is skipped when {@code -f}/{@code --file} is specified.
     */
    @Nested
    class CommandsHelpOnEmpty {

        private MavenCli.ExitException invokeCommands(CliRequest request) throws Exception {
            Method m = MavenCli.class.getDeclaredMethod("commands", CliRequest.class);
            m.setAccessible(true);
            try {
                m.invoke(cli, request);
                return null;
            } catch (java.lang.reflect.InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof MavenCli.ExitException) {
                    return (MavenCli.ExitException) cause;
                }
                throw new RuntimeException(cause);
            }
        }

        private void injectModelProcessor(ModelProcessor processor) throws Exception {
            Field f = MavenCli.class.getDeclaredField("modelProcessor");
            f.setAccessible(true);
            f.set(cli, processor);
        }

        @Test
        void displaysHelpAndExits0WhenNoGoalsAndNoPom() throws Exception {
            System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, ".");

            // A ModelProcessor that always returns a non-existent file (simulates no POM)
            ModelProcessor mockProcessor = mock(ModelProcessor.class);
            when(mockProcessor.locatePom(any(File.class))).thenReturn(new File("nonexistent-pom.xml"));
            injectModelProcessor(mockProcessor);

            CliRequest request = new CliRequest(new String[0], null);
            request.workingDirectory = System.getProperty("user.dir");
            cli.cli(request);

            MavenCli.ExitException ex = invokeCommands(request);

            assertNotNull(ex, "Expected ExitException when no goals and no POM");
            assertEquals(0, ex.exitCode, "Expected exit code 0 (graceful help display)");
        }

        @Test
        void skipsCheckWhenAlternatePomFileSpecified() throws Exception {
            System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, ".");

            // ModelProcessor is injected but should NOT be consulted when -f is set
            ModelProcessor mockProcessor = mock(ModelProcessor.class);
            when(mockProcessor.locatePom(any(File.class))).thenReturn(new File("nonexistent-pom.xml"));
            injectModelProcessor(mockProcessor);

            CliRequest request = new CliRequest(new String[] {"-f", "/some/other/project"}, null);
            request.workingDirectory = System.getProperty("user.dir");
            cli.cli(request);

            // Should NOT throw — the -f flag skips the POM-existence check
            MavenCli.ExitException ex = invokeCommands(request);
            assertNull(ex, "Expected no ExitException when -f/--file is specified");
        }
    }

    @Test
    public void testCoreExtensionComponentVisibility() throws Exception {
        Field f = MavenCli.class.getDeclaredField("slf4jLogger");
        f.setAccessible(true);
        f.set(cli, LoggerFactory.getLogger(MavenCli.class));

        ClassWorld classWorld = new ClassWorld();
        ClassRealm coreRealm =
                classWorld.newRealm("plexus.core", Thread.currentThread().getContextClassLoader());
        ClassRealm extRealm1 =
                classWorld.newRealm("coreExtension>ext1", Thread.currentThread().getContextClassLoader());

        CoreExtensionEntry entry = new CoreExtensionEntry(extRealm1, Collections.emptyList(), Collections.emptyList());
        ClassRealm containerRealm = cli.setupContainerRealm(
                classWorld, coreRealm, Collections.emptyList(), Collections.singletonList(entry));

        // Structural validation: verify the bidirectional realm import
        assertTrue(
                extRealm1.getImportRealms().contains(containerRealm),
                "Extension realm must import container realm for Sisu component visibility");

        // Behavioral validation: verify that Plexus/Sisu component lookup from the container
        // succeeds even when TCCL is set to the extension realm
        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(classWorld)
                .setRealm(containerRealm)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setJSR250Lifecycle(true)
                .setName("maven");

        DefaultPlexusContainer container = new DefaultPlexusContainer(cc);
        try {
            container.setLookupRealm(null);

            ComponentDescriptor<Runnable> descriptor = new ComponentDescriptor<>();
            descriptor.setRole(Runnable.class.getName());
            descriptor.setRoleHint("test");
            descriptor.setImplementationClass(MyLazyComponent.class);
            container.addComponentDescriptor(descriptor);

            ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
            try {
                // Simulate DefaultMaven setting TCCL to the core extension realm
                Thread.currentThread().setContextClassLoader(extRealm1);

                Runnable lookedUp = container.lookup(Runnable.class, "test");
                assertNotNull(lookedUp);
                assertTrue(lookedUp instanceof MyLazyComponent);
            } finally {
                Thread.currentThread().setContextClassLoader(originalTccl);
            }
        } finally {
            container.dispose();
        }
    }

    public static class MyLazyComponent implements Runnable {
        @Override
        public void run() {}
    }
}
