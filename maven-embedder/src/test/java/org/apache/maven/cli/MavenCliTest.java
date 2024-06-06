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
import java.nio.file.Paths;

import org.apache.commons.cli.ParseException;
import org.apache.maven.Maven;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.codehaus.plexus.PlexusContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.InOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class MavenCliTest {
    MavenCli cli;

    private String origBasedir;

    @Before
    public void setUp() {
        cli = new MavenCli();
        origBasedir = System.getProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
    }

    @After
    public void tearDown() throws Exception {
        if (origBasedir != null) {
            System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, origBasedir);
        } else {
            System.getProperties().remove(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
        }
    }

    @Test
    public void testCalculateDegreeOfConcurrency() {
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("0"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("-1"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("0x4"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("1.0"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("1."));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("AA"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("C"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("C2.2C"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("C2.2"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("2C2"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("CXXX"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("XXXC"));

        int cpus = Runtime.getRuntime().availableProcessors();
        assertEquals((int) (cpus * 2.2), cli.calculateDegreeOfConcurrency("2.2C"));
        assertEquals(1, cli.calculateDegreeOfConcurrency("0.0001C"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("-2.2C"));
        assertThrows(IllegalArgumentException.class, new ConcurrencyCalculator("0C"));
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

    @Test
    public void testStyleColors() throws Exception {
        assumeTrue("ANSI not supported", MessageUtils.isColorEnabled());
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
        request.rootDirectory = Paths.get("myRootDirectory");
        request.topDirectory = Paths.get("myTopDirectory");

        // Act
        cli.cli(request);
        cli.properties(request);

        // Assert
        assertThat(request.getUserProperties().getProperty("valFound"), is("sbari"));
        assertThat(request.getUserProperties().getProperty("valNotFound"), is("s${foz}i"));
        assertThat(request.getUserProperties().getProperty("valRootDirectory"), is("myRootDirectory/.mvn/foo"));
        assertThat(request.getUserProperties().getProperty("valTopDirectory"), is("myTopDirectory/pom.xml"));
        assertThat(request.getCommandLine().getOptionValue('f'), is("myRootDirectory/my-child"));
        assertThat(request.getCommandLine().getArgs(), equalTo(new String[] {"prefix:3.0.0:bar", "validate"}));
    }

    class ConcurrencyCalculator implements ThrowingRunnable {

        private final String value;

        public ConcurrencyCalculator(String value) {
            this.value = value;
        }

        @Override
        public void run() throws Throwable {
            cli.calculateDegreeOfConcurrency(value);
        }
    }
}
