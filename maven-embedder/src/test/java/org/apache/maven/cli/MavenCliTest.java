package org.apache.maven.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static java.util.Arrays.asList;
import static org.apache.maven.cli.MavenCli.performProfileActivation;
import static org.apache.maven.cli.MavenCli.performProjectActivation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.maven.Maven;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.ProfileActivation;
import org.apache.maven.execution.ProjectActivation;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

public class MavenCliTest
{
    private MavenCli cli;

    private String origBasedir;

    @BeforeEach
    public void setUp()
    {
        cli = new MavenCli();
        origBasedir = System.getProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY );
    }

    @AfterEach
    public void tearDown()
        throws Exception
    {
        if ( origBasedir != null )
        {
            System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY, origBasedir );
        }
        else
        {
            System.getProperties().remove( MavenCli.MULTIMODULE_PROJECT_DIRECTORY );
        }
    }

    @Test
    public void testPerformProfileActivation() throws ParseException
    {
        final Parser parser = new GnuParser();

        final Options options = new Options();
        options.addOption( Option.builder( Character.toString( CLIManager.ACTIVATE_PROFILES ) ).hasArg().build() );

        ProfileActivation activation;

        activation = new ProfileActivation();
        performProfileActivation( parser.parse( options, new String[]{ "-P", "test1,+test2,?test3,+?test4" } ), activation );
        assertThat( activation.getRequiredActiveProfileIds(), containsInAnyOrder( "test1", "test2" ) );
        assertThat( activation.getOptionalActiveProfileIds(), containsInAnyOrder( "test3", "test4" ) );

        activation = new ProfileActivation();
        performProfileActivation( parser.parse( options, new String[]{ "-P", "!test1,-test2,-?test3,!?test4" } ), activation );
        assertThat( activation.getRequiredInactiveProfileIds(), containsInAnyOrder( "test1", "test2" ) );
        assertThat( activation.getOptionalInactiveProfileIds(), containsInAnyOrder( "test3", "test4" ) );

        activation = new ProfileActivation();
        performProfileActivation( parser.parse( options, new String[]{ "-P", "-test1,+test2" } ), activation );
        assertThat( activation.getRequiredActiveProfileIds(), containsInAnyOrder( "test2" ) );
        assertThat( activation.getRequiredInactiveProfileIds(), containsInAnyOrder( "test1" ) );
    }

    @Test
    public void testDetermineProjectActivation() throws ParseException
    {
        final Parser parser = new GnuParser();

        final Options options = new Options();
        options.addOption( Option.builder( CLIManager.PROJECT_LIST ).hasArg().build() );

        ProjectActivation activation;

        activation = new ProjectActivation();
        performProjectActivation( parser.parse( options, new String[]{ "-pl", "test1,+test2,?test3,+?test4" } ), activation );
        assertThat( activation.getRequiredActiveProjectSelectors(), containsInAnyOrder( "test1", "test2" ) );
        assertThat( activation.getOptionalActiveProjectSelectors(), containsInAnyOrder( "test3", "test4" ) );

        activation = new ProjectActivation();
        performProjectActivation( parser.parse( options, new String[]{ "-pl", "!test1,-test2,-?test3,!?test4" } ), activation );
        assertThat( activation.getRequiredInactiveProjectSelectors(), containsInAnyOrder( "test1", "test2" ) );
        assertThat( activation.getOptionalInactiveProjectSelectors(), containsInAnyOrder( "test3", "test4" ) );

        activation = new ProjectActivation();
        performProjectActivation( parser.parse( options, new String[]{ "-pl", "-test1,+test2" } ), activation );
        assertThat( activation.getRequiredActiveProjectSelectors(), containsInAnyOrder( "test2" ) );
        assertThat( activation.getRequiredInactiveProjectSelectors(), containsInAnyOrder( "test1" ) );
    }

    @Test
    public void testCalculateDegreeOfConcurrencyWithCoreMultiplier()
    {
        int cores = Runtime.getRuntime().availableProcessors();
        // -T2.2C
        assertEquals( (int) ( cores * 2.2 ), cli.calculateDegreeOfConcurrencyWithCoreMultiplier( "C2.2" ) );
        // -TC2.2
        assertEquals( (int) ( cores * 2.2 ), cli.calculateDegreeOfConcurrencyWithCoreMultiplier( "2.2C" ) );

        assertThrows(
                NumberFormatException.class,
                () -> cli.calculateDegreeOfConcurrencyWithCoreMultiplier( "CXXX" ),
                "Should have failed with a NumberFormatException" );
    }

    @Test
    public void testMavenConfig()
        throws Exception
    {
        System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                            new File( "src/test/projects/config" ).getCanonicalPath() );
        CliRequest request = new CliRequest( new String[0], null );

        // read .mvn/maven.config
        cli.initialize( request );
        cli.cli( request );
        assertEquals( "multithreaded", request.commandLine.getOptionValue( CLIManager.BUILDER ) );
        assertEquals( "8", request.commandLine.getOptionValue( CLIManager.THREADS ) );

        // override from command line
        request = new CliRequest( new String[]{ "--builder", "foobar" }, null );
        cli.cli( request );
        assertEquals( "foobar", request.commandLine.getOptionValue( "builder" ) );
    }

    @Test
    public void testMavenConfigInvalid()
        throws Exception
    {
        System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                            new File( "src/test/projects/config-illegal" ).getCanonicalPath() );
        CliRequest request = new CliRequest( new String[0], null );

        cli.initialize( request );
        assertThrows( ParseException.class, () -> cli.cli( request ) );
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
    public void testMVNConfigurationThreadCanBeOverwrittenViaCommandLine()
        throws Exception
    {
        System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                            new File( "src/test/projects/mavenConfigProperties" ).getCanonicalPath() );
        CliRequest request = new CliRequest( new String[]{ "-T", "5" }, null );

        cli.initialize( request );
        // read .mvn/maven.config
        cli.cli( request );

        assertEquals( "5", request.commandLine.getOptionValue( CLIManager.THREADS ) );
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
    public void testMVNConfigurationDefinedPropertiesCanBeOverwrittenViaCommandLine()
        throws Exception
    {
        System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                            new File( "src/test/projects/mavenConfigProperties" ).getCanonicalPath() );
        CliRequest request = new CliRequest( new String[]{ "-Drevision=8.1.0" }, null );

        cli.initialize( request );
        // read .mvn/maven.config
        cli.cli( request );
        cli.properties( request );

        String revision = System.getProperty( "revision" );
        assertEquals( "8.1.0", revision );
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
    public void testMVNConfigurationCLIRepeatedPropertiesLastWins()
        throws Exception
    {
        System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                            new File( "src/test/projects/mavenConfigProperties" ).getCanonicalPath() );
        CliRequest request = new CliRequest( new String[]{ "-Drevision=8.1.0", "-Drevision=8.2.0" }, null );

        cli.initialize( request );
        // read .mvn/maven.config
        cli.cli( request );
        cli.properties( request );

        String revision = System.getProperty( "revision" );
        assertEquals( "8.2.0", revision );
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
    public void testMVNConfigurationFunkyArguments()
        throws Exception
    {
        System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY,
                            new File( "src/test/projects/mavenConfigProperties" ).getCanonicalPath() );
        CliRequest request = new CliRequest(
            new String[]{ "-Drevision=8.1.0", "--file=-Dpom.xml", "\"-Dfoo=bar ", "\"-Dfoo2=bar two\"",
                "-Drevision=8.2.0" }, null );

        cli.initialize( request );
        // read .mvn/maven.config
        cli.cli( request );
        cli.properties( request );

        assertEquals( "3", request.commandLine.getOptionValue( CLIManager.THREADS ) );

        String revision = System.getProperty( "revision" );
        assertEquals( "8.2.0", revision );

        assertEquals( "bar ", request.getSystemProperties().getProperty( "foo" ) );
        assertEquals( "bar two", request.getSystemProperties().getProperty( "foo2" ) );
        assertEquals( "Apache Maven", request.getSystemProperties().getProperty( "label" ) );

        assertEquals( "-Dpom.xml", request.getCommandLine().getOptionValue( CLIManager.ALTERNATE_POM_FILE ) );
    }

    @Test
    public void testStyleColors()
        throws Exception
    {
        assumeTrue( MessageUtils.isColorEnabled(), "ANSI not supported" );
        CliRequest request;

        MessageUtils.setColorEnabled( true );
        request = new CliRequest( new String[] { "-B" }, null );
        cli.cli( request );
        cli.properties( request );
        cli.logging( request );
        assertFalse( MessageUtils.isColorEnabled() );

        MessageUtils.setColorEnabled( true );
        request = new CliRequest( new String[] { "-l", "target/temp/mvn.log" }, null );
        request.workingDirectory = "target/temp";
        cli.cli( request );
        cli.properties( request );
        cli.logging( request );
        assertFalse( MessageUtils.isColorEnabled() );

        MessageUtils.setColorEnabled( false );
        request = new CliRequest( new String[] { "-Dstyle.color=always" }, null );
        cli.cli( request );
        cli.properties( request );
        cli.logging( request );
        assertTrue( MessageUtils.isColorEnabled() );

        MessageUtils.setColorEnabled( true );
        request = new CliRequest( new String[] { "-Dstyle.color=never" }, null );
        cli.cli( request );
        cli.properties( request );
        cli.logging( request );
        assertFalse( MessageUtils.isColorEnabled() );

        MessageUtils.setColorEnabled( false );
        request = new CliRequest( new String[] { "-Dstyle.color=always", "-B", "-l", "target/temp/mvn.log" }, null );
        request.workingDirectory = "target/temp";
        cli.cli( request );
        cli.properties( request );
        cli.logging( request );
        assertTrue( MessageUtils.isColorEnabled() );

        MessageUtils.setColorEnabled( false );
        CliRequest maybeColorRequest = new CliRequest( new String[] { "-Dstyle.color=maybe", "-B", "-l", "target/temp/mvn.log" }, null );
        request.workingDirectory = "target/temp";
        cli.cli( maybeColorRequest );
        cli.properties( maybeColorRequest );
        assertThrows(
                IllegalArgumentException.class,
                () -> cli.logging( maybeColorRequest ),
                "maybe is not a valid option" );
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
                container.addComponent(mock(Maven.class), "org.apache.maven.Maven");

                ( (DefaultPlexusContainer) container ).addPlexusInjector( Collections.emptyList(),
                        binder -> binder.bind( EventSpyDispatcher.class ).toInstance( eventSpyDispatcherMock ) );
            }
        };

        CliRequest cliRequest = new CliRequest(new String[]{}, null);

        customizedMavenCli.cli(cliRequest);
        customizedMavenCli.logging(cliRequest);
        customizedMavenCli.container(cliRequest);
        customizedMavenCli.toolchains(cliRequest);

        InOrder orderdEventSpyDispatcherMock = inOrder(eventSpyDispatcherMock);
        orderdEventSpyDispatcherMock.verify(eventSpyDispatcherMock, times(1)).onEvent(any(ToolchainsBuildingRequest.class));
        orderdEventSpyDispatcherMock.verify(eventSpyDispatcherMock, times(1)).onEvent(any(ToolchainsBuildingResult.class));
    }

    @Test
    public void resumeFromSelectorIsSuggestedWithoutGroupId()
    {
        List<MavenProject> allProjects = asList(
                createMavenProject( "group", "module-a" ),
                createMavenProject( "group", "module-b" ) );
        MavenProject failedProject = allProjects.get( 0 );

        String selector = cli.getResumeFromSelector( allProjects, failedProject );

        assertThat( selector, is( ":module-a" ) );
    }

    @Test
    public void resumeFromSelectorContainsGroupIdWhenArtifactIdIsNotUnique()
    {
        List<MavenProject> allProjects = asList(
                createMavenProject( "group-a", "module" ),
                createMavenProject( "group-b", "module" ) );
        MavenProject failedProject = allProjects.get( 0 );

        String selector = cli.getResumeFromSelector( allProjects, failedProject );

        assertThat( selector, is( "group-a:module" ) );
    }

    @Test
    public void verifyLocalRepositoryPath()
    {
        MavenCli cli = new MavenCli();
        CliRequest request = new CliRequest( new String[] { }, null );
        request.commandLine = new CommandLine.Builder().build();
        MavenExecutionRequest executionRequest;

        // Use default
        executionRequest = cli.populateRequest( request );
        assertThat( executionRequest.getLocalRepositoryPath(),
                is( nullValue() ) );

        // System-properties override default
        request.getSystemProperties().setProperty( MavenCli.LOCAL_REPO_PROPERTY, "." + File.separatorChar + "custom1" );
        executionRequest = cli.populateRequest( request );
        assertThat( executionRequest.getLocalRepositoryPath(),
                is( notNullValue() ) );
        assertThat( executionRequest.getLocalRepositoryPath().toString(),
                is( "." + File.separatorChar + "custom1" ) );

        // User-properties override system properties
        request.getUserProperties().setProperty( MavenCli.LOCAL_REPO_PROPERTY, "." + File.separatorChar + "custom2" );
        executionRequest = cli.populateRequest( request );
        assertThat( executionRequest.getLocalRepositoryPath(),
                is( notNullValue() ) );
        assertThat( executionRequest.getLocalRepositoryPath().toString(),
                is( "." + File.separatorChar + "custom2" ) );
    }

    /**
     * MNG-7032: Disable colours for {@code --version} if {@code --batch-mode} is also given.
     * @throws Exception cli invocation.
     */
    @Test
    public void testVersionStringWithoutAnsi() throws Exception
    {
        // given
        // - request with version and batch mode
        CliRequest cliRequest = new CliRequest( new String[] {
                "--version",
                "--batch-mode"
        }, null );
        ByteArrayOutputStream systemOut = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut( new PrintStream( systemOut ) );

        // when
        try {
            cli.cli( cliRequest );
        } catch ( MavenCli.ExitException exitException ) {
            // expected
        } finally {
            // restore sysout
            System.setOut( oldOut );
        }
        String versionOut = new String( systemOut.toByteArray(), StandardCharsets.UTF_8 );

        // then
        assertEquals( MessageUtils.stripAnsiCodes( versionOut ), versionOut );
    }

    private MavenProject createMavenProject( String groupId, String artifactId )
    {
        MavenProject project = new MavenProject();
        project.setGroupId( groupId );
        project.setArtifactId( artifactId );
        return project;
    }
}
