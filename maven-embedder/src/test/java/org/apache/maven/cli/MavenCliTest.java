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
import static org.apache.maven.cli.MavenCli.determineProfileActivation;
import static org.apache.maven.cli.MavenCli.determineProjectActivation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
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
import org.apache.maven.Maven;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.sisu.plexus.PlexusBeanModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import com.google.inject.Binder;
import com.google.inject.Module;

public class MavenCliTest
{
    private MavenCli cli;

    private String origBasedir;

    @Before
    public void setUp()
    {
        cli = new MavenCli();
        origBasedir = System.getProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY );
    }

    @After
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
    public void testDetermineProfileActivation() throws ParseException
    {
        MavenCli.ProfileActivation result;
        Options options = new Options();
        options.addOption( Option.builder( Character.toString( CLIManager.ACTIVATE_PROFILES ) ).hasArg().build() );

        result = determineProfileActivation( new GnuParser().parse( options, new String[]{ "-P", "test1,+test2" } ) );
        assertThat( result.activeProfiles.size(), is( 2 ) );
        assertThat( result.activeProfiles, contains( "test1", "test2" ) );

        result = determineProfileActivation( new GnuParser().parse( options, new String[]{ "-P", "!test1,-test2" } ) );
        assertThat( result.inactiveProfiles.size(), is( 2 ) );
        assertThat( result.inactiveProfiles, contains( "test1", "test2" ) );

        result = determineProfileActivation( new GnuParser().parse( options, new String[]{ "-P", "-test1,+test2" } ) );
        assertThat( result.activeProfiles.size(), is( 1 ) );
        assertThat( result.activeProfiles, contains( "test2" ) );
        assertThat( result.inactiveProfiles.size(), is( 1 ) );
        assertThat( result.inactiveProfiles, contains( "test1" ) );
    }

    @Test
    public void testDetermineProjectActivation() throws ParseException
    {
        MavenCli.ProjectActivation result;
        Options options = new Options();
        options.addOption( Option.builder( CLIManager.PROJECT_LIST ).hasArg().build() );

        result = determineProjectActivation( new GnuParser().parse( options, new String[0] ) );
        assertThat( result.activeProjects, is( nullValue() ) );
        assertThat( result.inactiveProjects, is( nullValue() ) );

        result = determineProjectActivation( new GnuParser().parse( options, new String[]{ "-pl", "test1,+test2" } ) );
        assertThat( result.activeProjects.size(), is( 2 ) );
        assertThat( result.activeProjects, contains( "test1", "test2" ) );

        result = determineProjectActivation( new GnuParser().parse( options, new String[]{ "-pl", "!test1,-test2" } ) );
        assertThat( result.inactiveProjects.size(), is( 2 ) );
        assertThat( result.inactiveProjects, contains( "test1", "test2" ) );

        result = determineProjectActivation( new GnuParser().parse( options, new String[]{ "-pl" ,"-test1,+test2" } ) );
        assertThat( result.activeProjects.size(), is( 1 ) );
        assertThat( result.activeProjects, contains( "test2" ) );
        assertThat( result.inactiveProjects.size(), is( 1 ) );
        assertThat( result.inactiveProjects, contains( "test1" ) );
    }

    @Test
    public void testCalculateDegreeOfConcurrencyWithCoreMultiplier()
    {
        int cores = Runtime.getRuntime().availableProcessors();
        // -T2.2C
        assertEquals( (int) ( cores * 2.2 ), cli.calculateDegreeOfConcurrencyWithCoreMultiplier( "C2.2" ) );
        // -TC2.2
        assertEquals( (int) ( cores * 2.2 ), cli.calculateDegreeOfConcurrencyWithCoreMultiplier( "2.2C" ) );

        try
        {
            cli.calculateDegreeOfConcurrencyWithCoreMultiplier( "CXXX" );
            fail( "Should have failed with a NumberFormatException" );
        }
        catch ( NumberFormatException e )
        {
            // carry on
        }
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
        try
        {
            cli.cli( request );
            fail();
        }
        catch ( ParseException expected )
        {

        }
    }

    /**
     * Read .mvn/maven.config with the following definitions:
     * <pre>
     *   -T 3
     *   -Drevision=1.3.0
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
     *   -T 3
     *   -Drevision=1.3.0
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
     *   -T 3
     *   -Drevision=1.3.0
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
     *   -T 3
     *   -Drevision=1.3.0
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

        String revision = System.getProperty( "revision" );
        assertEquals( "8.2.0", revision );

        assertEquals( "bar ", request.getSystemProperties().getProperty( "foo" ) );
        assertEquals( "bar two", request.getSystemProperties().getProperty( "foo2" ) );

        assertEquals( "-Dpom.xml", request.getCommandLine().getOptionValue( CLIManager.ALTERNATE_POM_FILE ) );
    }

    @Test
    public void testStyleColors()
        throws Exception
    {
        assumeTrue( "ANSI not supported", MessageUtils.isColorEnabled() );
        CliRequest request;

        MessageUtils.setColorEnabled( true );
        request = new CliRequest( new String[] { "-B" }, null );
        cli.cli( request );
        cli.properties( request );
        cli.logging( request );
        assertFalse( MessageUtils.isColorEnabled() );

        MessageUtils.setColorEnabled( true );
        request = new CliRequest( new String[] { "-l", "target/temp/mvn.log" }, null );
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
        cli.cli( request );
        cli.properties( request );
        cli.logging( request );
        assertTrue( MessageUtils.isColorEnabled() );

        try
        {
            MessageUtils.setColorEnabled( false );
            request = new CliRequest( new String[] { "-Dstyle.color=maybe", "-B", "-l", "target/temp/mvn.log" }, null );
            cli.cli( request );
            cli.properties( request );
            cli.logging( request );
            fail( "maybe is not a valid option" );
        }
        catch ( IllegalArgumentException e )
        {
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
                container.addComponent(mock(Maven.class), "org.apache.maven.Maven");

                ((DefaultPlexusContainer)container).addPlexusInjector(Collections.<PlexusBeanModule>emptyList(),
                        new Module()
                        {
                            public void configure( final Binder binder )
                            {
                                binder.bind( EventSpyDispatcher.class ).toInstance( eventSpyDispatcherMock );
                            }
                        }
                    );
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
