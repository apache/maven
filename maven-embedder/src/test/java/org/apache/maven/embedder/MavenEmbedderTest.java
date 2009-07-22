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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
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

public class MavenEmbedderTest
    extends AbstractCoreMavenComponentTestCase
{
    protected String basedir;

    protected MavenEmbedder mavenEmbedder;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "." ).getCanonicalPath();
        }

        Configuration configuration = new SimpleConfiguration();

        mavenEmbedder = new MavenEmbedder( configuration );
    }

    protected void tearDown()
        throws Exception
    {
        mavenEmbedder.stop();
        mavenEmbedder = null;
    }

    protected void assertNoExceptions( MavenExecutionResult result )
    {
        List<Exception> exceptions = result.getExceptions();
        if ( ( exceptions == null ) || exceptions.isEmpty() )
        {
            // everything is a-ok.
            return;
        }

        System.err.println( "Encountered " + exceptions.size() + " exception(s)." );
        for (Exception exception : exceptions)
        {
            exception.printStackTrace( System.err );
        }

        fail( "Encountered Exceptions in MavenExecutionResult during " + getName() );
    }
        
    /*MNG-3919*/
    public void testWithInvalidGoal()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/projects/invalid-goal" );

        File targetDirectory = new File( basedir, "target/projects/invalid-goal" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = createMavenExecutionRequest( new File( targetDirectory, "pom.xml" ) );        
        request.setGoals( Arrays.asList( new String[]{"validate"} ) );

        MavenExecutionResult result = mavenEmbedder.execute( request );
        List<Exception> exceptions = result.getExceptions();
        assertEquals("Incorrect number of exceptions", 1, exceptions.size());

        if ( ( exceptions.get( 0 ) instanceof NullPointerException ) )
        {
            fail("Null Pointer on Exception");
        }
    }

    public void testExecutionUsingAPomFile()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project1" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = createMavenExecutionRequest( new File( targetDirectory, "pom.xml" ) );        

        MavenExecutionResult result = mavenEmbedder.execute( request );

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

        MavenExecutionRequest requestWithoutProfile = createMavenExecutionRequest( new File( targetDirectory, "pom.xml" ) );        

        MavenExecutionResult r0 = mavenEmbedder.execute( requestWithoutProfile );

        assertNoExceptions( r0 );

        MavenProject p0 = r0.getProject();

        assertNull( p0.getProperties().getProperty( "embedderProfile" ) );

        assertNull( p0.getProperties().getProperty( "name" ) );

        assertNull( p0.getProperties().getProperty( "occupation" ) );

        // Check with profile activated

        MavenExecutionRequest request = createMavenExecutionRequest( new File( targetDirectory, "pom.xml" ) );        
        request.addActiveProfile( "embedderProfile" );
        
        MavenExecutionResult r1 = mavenEmbedder.execute( request );

        MavenProject p1 = r1.getProject();

        assertEquals( "true", p1.getProperties().getProperty( "embedderProfile" ) );

        assertEquals( "jason", p1.getProperties().getProperty( "name" ) );

        assertEquals( "somnambulance", p1.getProperties().getProperty( "occupation" ) );
    }

    //TODO: This needs to be a separate test and we can't use production plugins for the test.
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

        Model model = mavenEmbedder.readModel( pom );

        Plugin plugin = new Plugin();
        plugin.setArtifactId( "maven-surefire-plugin" );
        plugin.setVersion( "2.4.2" );
        model.setBuild( new Build() );
        model.getBuild().addPlugin( plugin );

        Writer writer = WriterFactory.newXmlWriter( pom );
        mavenEmbedder.writeModel( writer, model );
        writer.close();

        MavenExecutionRequest request = createMavenExecutionRequest( pom );
        
        MavenExecutionResult result = mavenEmbedder.execute( request );

        assertNoExceptions( result );

        MavenProject project = result.getProject();

        Artifact p = project.getPluginArtifactMap().get( plugin.getKey() );
        assertEquals( "2.4.2", p.getVersion() );

        /* Add the surefire plugin 2.3 to the pom */
        plugin.setVersion( "2.4.3" );
        writer = WriterFactory.newXmlWriter( pom );
        mavenEmbedder.writeModel( writer, model );
        writer.close();

        request = createMavenExecutionRequest( pom );
                    
        result = mavenEmbedder.execute( request );

        assertNoExceptions( result );

        project = result.getProject();

        p = project.getPluginArtifactMap().get( plugin.getKey() );
        assertEquals( "2.4.3", p.getVersion() );
    }

    public void testModelReading()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Test model reading
        // ----------------------------------------------------------------------

        Model model = mavenEmbedder.readModel( getPomFile() );

        assertEquals( "org.apache.maven", model.getGroupId() );
    }

    // ----------------------------------------------------------------------------
    // Model Writing
    // ----------------------------------------------------------------------------

    public void testModelWriting()
        throws Exception
    {
        Model model = mavenEmbedder.readModel( getPomFile() );

        model.setGroupId( "org.apache.maven.new" );

        File file = new File( basedir, "target/model.xml" );

        Writer writer = WriterFactory.newXmlWriter( file );

        mavenEmbedder.writeModel( writer, model );

        writer.close();

        model = mavenEmbedder.readModel( file );

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
        return getPomFile( "pom.xml" );
    }

    protected File getPomFile( String name )
    {
        return new File( basedir, "src/test/resources/" + name );
    }

    @Override
    protected String getProjectsDirectory()
    {
        return null;
    }
}
