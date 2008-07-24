package org.apache.maven.embedder;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsConfigurationException;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class MavenEmbedderTest
    extends TestCase
{
    protected String basedir;

    protected MavenEmbedder maven;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "." ).getCanonicalPath();
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Configuration configuration = new DefaultConfiguration()
            .setClassLoader( classLoader )
            .setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );
        configuration.setUserSettingsFile( MavenEmbedder.DEFAULT_USER_SETTINGS_FILE );

        maven = new MavenEmbedder( configuration );
    }

    protected void tearDown()
        throws Exception
    {
        maven.stop();
    }

    protected void assertNoExceptions( MavenExecutionResult result )
    {
        List exceptions = result.getExceptions();
        if ( ( exceptions == null ) || exceptions.isEmpty() )
        {
            // everything is a-ok.
            return;
        }

        System.err.println( "Encountered " + exceptions.size() + " exception(s)." );
        Iterator it = exceptions.iterator();
        while ( it.hasNext() )
        {
            Exception exception = (Exception) it.next();
            exception.printStackTrace( System.err );
        }

        fail( "Encountered Exceptions in MavenExecutionResult during " + getName() );
    }

    // ----------------------------------------------------------------------
    // Goal/Phase execution tests
    // ----------------------------------------------------------------------

    public void testExecutionUsingABaseDirectory()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project0" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( targetDirectory )
            .setShowErrors( true ).setGoals( Arrays.asList( new String[]{"package"} ) );

        MavenExecutionResult result = maven.execute( request );

        assertNoExceptions( result );

        MavenProject project = result.getProject();

        assertEquals( "embedder-test-project", project.getArtifactId() );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
    }

    public void testExecutionUsingAPomFile()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project1" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setPom( new File( targetDirectory, "pom.xml" ) ).setShowErrors( true )
            .setGoals( Arrays.asList( new String[] { "package" } ) );

        MavenExecutionResult result = maven.execute( request );

        assertNoExceptions( result );

        MavenProject project = result.getProject();

        assertEquals( "embedder-test-project", project.getArtifactId() );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
    }

    public void testExecutionUsingAProfileWhichSetsAProperty()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project2" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        // Check with profile not active

        MavenExecutionRequest requestWithoutProfile = new DefaultMavenExecutionRequest()
            .setPom( new File( targetDirectory, "pom.xml" ) ).setShowErrors( true )
            .setGoals( Arrays.asList( new String[] { "validate" } ) );

        MavenExecutionResult r0 = maven.execute( requestWithoutProfile );

        assertNoExceptions( r0 );

        MavenProject p0 = r0.getProject();

        assertNull( p0.getProperties().getProperty( "embedderProfile" ) );

        assertNull( p0.getProperties().getProperty( "name" ) );

        assertNull( p0.getProperties().getProperty( "occupation" ) );

        // Check with profile activated

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setPom( new File( targetDirectory, "pom.xml" ) )
            .setShowErrors( true )
            .setGoals( Arrays.asList( new String[] { "validate" } ) )
            .addActiveProfile( "embedderProfile" );

        MavenExecutionResult r1 = maven.execute( request );

        MavenProject p1 = r1.getProject();

        assertEquals( "true", p1.getProperties().getProperty( "embedderProfile" ) );

        assertEquals( "jason", p1.getProperties().getProperty( "name" ) );

        assertEquals( "somnambulance", p1.getProperties().getProperty( "occupation" ) );
    }

    /**
     * Test that two executions of the embedder don't share data that has changed, see MNG-3013
     *
     * @throws Exception
     */
    public void testTwoExecutionsDoNotCacheChangedData()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project-caching" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        File pom = new File( targetDirectory, "pom.xml" );

        /* Add the surefire plugin 2.2 to the pom */
        Model model = maven.readModel( pom );

        Plugin plugin = new Plugin();
        plugin.setArtifactId( "maven-surefire-plugin" );
        plugin.setVersion( "2.2" );
        model.setBuild( new Build() );
        model.getBuild().addPlugin( plugin );

        Writer writer = WriterFactory.newXmlWriter( pom );
        maven.writeModel( writer, model );
        writer.close();

        /* execute maven */
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setPom( pom ).setShowErrors( true )
            .setGoals( Arrays.asList( new String[] { "package" } ) );

        MavenExecutionResult result = maven.execute( request );

        assertNoExceptions( result );

        MavenProject project = result.getProject();

        Artifact p = (Artifact) project.getPluginArtifactMap().get( plugin.getKey() );
        assertEquals( "2.2", p.getVersion() );

        /* Add the surefire plugin 2.3 to the pom */
        plugin.setVersion( "2.3" );
        writer = WriterFactory.newXmlWriter( pom );
        maven.writeModel( writer, model );
        writer.close();

        /* execute Maven */
        request = new DefaultMavenExecutionRequest().setPom( pom ).setShowErrors( true )
            .setGoals( Arrays.asList( new String[] { "package" } ) );
        result = maven.execute( request );

        assertNoExceptions( result );

        project = result.getProject();

        p = (Artifact) project.getPluginArtifactMap().get( plugin.getKey() );
        assertEquals( "2.3", p.getVersion() );
    }

    // ----------------------------------------------------------------------
    // Lifecycle phases
    // ----------------------------------------------------------------------

    public void testRetrievingLifecyclePhases()
        throws Exception
    {
        List phases = maven.getLifecyclePhases();

        assertEquals( "validate", (String) phases.get( 0 ) );

        assertEquals( "initialize", (String) phases.get( 1 ) );

        assertEquals( "generate-sources", (String) phases.get( 2 ) );
    }

    // ----------------------------------------------------------------------
    // Repository
    // ----------------------------------------------------------------------

    public void testLocalRepositoryRetrieval()
        throws Exception
    {
        assertNotNull( maven.getLocalRepository().getBasedir() );
    }

    // ----------------------------------------------------------------------
    // Model Reading
    // ----------------------------------------------------------------------

    public void testModelReading()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Test model reading
        // ----------------------------------------------------------------------

        Model model = maven.readModel( getPomFile() );

        assertEquals( "org.apache.maven", model.getGroupId() );
    }

    public void testProjectReading()
        throws Exception
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setShowErrors( true ).setPom( getPomFile() );

        MavenExecutionResult result = maven.readProjectWithDependencies( request );

        assertNoExceptions( result );

        assertEquals( "org.apache.maven", result.getProject().getGroupId() );

        Set artifacts = result.getProject().getArtifacts();

        assertEquals( 1, artifacts.size() );

        artifacts.iterator().next();
    }

    public void testProjectReading_FromChildLevel_ScmInheritanceCalculations()
        throws Exception
    {
        File pomFile = new File( basedir, "src/test/projects/readProject-withScmInheritance/modules/child1/pom.xml" );

        MavenProject project = maven.readProject( pomFile );

        assertEquals( "http://host/viewer?path=/trunk/parent/child1", project.getScm().getUrl() );
        assertEquals( "scm:svn:http://host/trunk/parent/child1", project.getScm().getConnection() );
        assertEquals( "scm:svn:https://host/trunk/parent/child1", project.getScm().getDeveloperConnection() );
    }

    public void testProjectReading_SkipMissingModuleSilently()
        throws Exception
    {
        File pomFile = new File( basedir,
                                 "src/test/projects/readProject-missingModuleIgnored/pom.xml" );

        maven.readProject( pomFile );
    }

    /*
    public void testProjectReadingWithDistributionStatus()
        throws Exception
    {
        File pom = new File( basedir, "src/test/resources/pom-with-distribution-status.xml" );
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setShowErrors( true )
            .setPomFile( pom.getAbsolutePath() );

        MavenProject project = maven.readProject( pom );

        assertEquals( "deployed", project.getDistributionManagement().getStatus() );

        MavenExecutionResult result = maven.readProjectWithDependencies( request );

        assertNoExceptions( result );

        assertEquals( "org.apache.maven", result.getProject().getGroupId() );

        assertEquals( "deployed", result.getProject().getDistributionManagement().getStatus() );
    }
    */

    // ----------------------------------------------------------------------------
    // Model Writing
    // ----------------------------------------------------------------------------

    public void testModelWriting()
        throws Exception
    {
        Model model = maven.readModel( getPomFile() );

        model.setGroupId( "org.apache.maven.new" );

        File file = new File( basedir, "target/model.xml" );

        Writer writer = WriterFactory.newXmlWriter( file );

        maven.writeModel( writer, model );

        writer.close();

        model = maven.readModel( file );

        assertEquals( "org.apache.maven.new", model.getGroupId() );
    }

    // ----------------------------------------------------------------------
    // Settings-File Handling
    // ----------------------------------------------------------------------

    public void testReadSettings()
        throws IOException, SettingsConfigurationException, MavenEmbedderException
    {
        Settings s = new Settings();
        s.setOffline( true );

        String localRepoPath = "/path/to/local/repo";

        s.setLocalRepository( localRepoPath );

        File settingsFile = File.createTempFile( "embedder-test.settings.", "" );
        settingsFile.deleteOnExit();

        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( settingsFile );
            new SettingsXpp3Writer().write( writer, s );
        }
        finally
        {
            IOUtil.close( writer );
        }

        Settings result = MavenEmbedder.readSettings( settingsFile );

        assertEquals( localRepoPath, result.getLocalRepository() );
        assertTrue( result.isOffline() );
    }

    public void testReadSettings_shouldFailToValidate()
        throws IOException, SettingsConfigurationException, MavenEmbedderException
    {
        Settings s = new Settings();

        Profile p = new Profile();

        Repository r = new Repository();
        r.setUrl( "http://example.com" );

        p.addRepository( r );
        s.addProfile( p );

        File settingsFile = File.createTempFile( "embedder-test.settings.", "" );
        settingsFile.deleteOnExit();

        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( settingsFile );
            new SettingsXpp3Writer().write( writer, s );
        }
        finally
        {
            IOUtil.close( writer );
        }

        try
        {
            MavenEmbedder.readSettings( settingsFile );

            fail( "Settings should not pass validation when being read." );
        }
        catch ( IOException e )
        {
            String message = e.getMessage();
            assertTrue( message.indexOf( "Failed to validate" ) > -1 );
        }
    }

    public void testWriteSettings()
        throws IOException, SettingsConfigurationException, MavenEmbedderException, XmlPullParserException
    {
        Settings s = new Settings();

        s.setOffline( true );

        String localRepoPath = "/path/to/local/repo";

        s.setLocalRepository( localRepoPath );

        File settingsFile = new File( System.getProperty( "basedir" ), "target/test-settings.xml" );

        settingsFile.getParentFile().mkdirs();

        settingsFile.deleteOnExit();

        MavenEmbedder.writeSettings( settingsFile, s );

        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( settingsFile );

            Settings result = new SettingsXpp3Reader().read( reader );

            assertEquals( localRepoPath, result.getLocalRepository() );

            assertTrue( result.isOffline() );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    public void testWriteSettings_shouldFailToValidate()
        throws IOException, SettingsConfigurationException, MavenEmbedderException
    {
        Settings s = new Settings();

        Profile p = new Profile();

        Repository r = new Repository();
        r.setUrl( "http://example.com" );

        p.addRepository( r );
        s.addProfile( p );

        File settingsFile = File.createTempFile( "embedder-test.settings.", "" );
        settingsFile.deleteOnExit();

        try
        {
            MavenEmbedder.writeSettings( settingsFile, s );

            fail( "Validation of settings should fail before settings are written." );
        }
        catch ( IOException e )
        {
            String message = e.getMessage();
            assertTrue( message.indexOf( "Failed to validate" ) > -1 );
        }
    }

    // ----------------------------------------------------------------------
    // Internal Utilities
    // ----------------------------------------------------------------------

    protected File getPomFile()
    {
        return new File( basedir, "src/test/resources/pom.xml" );
    }
}
