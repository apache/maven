package org.apache.maven;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public abstract class AbstractMavenToolsTest
    extends PlexusTestCase
{
    
    private MavenTools mavenTools;
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        
        mavenTools = (MavenTools) lookup( MavenTools.ROLE, getMavenToolsRoleHint() );
    }

    protected abstract String getMavenToolsRoleHint();

    public void testReadSettings()
        throws IOException, SettingsConfigurationException
    {
        Settings s = new Settings();
        s.setOffline( true );

        String localRepoPath = "/path/to/local/repo";

        s.setLocalRepository( localRepoPath );

        File settingsFile = File.createTempFile( "mavenTools-test.settings.", "" );
        settingsFile.deleteOnExit();

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( settingsFile );
            new SettingsXpp3Writer().write( writer, s );
        }
        finally
        {
            IOUtil.close( writer );
        }

        FileReader reader = null;
        try
        {
            reader = new FileReader( settingsFile );
            Settings result = mavenTools.readSettings( reader );

            assertEquals( localRepoPath, result.getLocalRepository() );
            assertTrue( result.isOffline() );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    public void testReadSettings_shouldFailToValidate()
        throws IOException, SettingsConfigurationException
    {
        Settings s = new Settings();

        Profile p = new Profile();

        Repository r = new Repository();
        r.setUrl( "http://example.com" );

        p.addRepository( r );
        s.addProfile( p );

        File settingsFile = File.createTempFile( "mavenTools-test.settings.", "" );
        settingsFile.deleteOnExit();

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( settingsFile );
            new SettingsXpp3Writer().write( writer, s );
        }
        finally
        {
            IOUtil.close( writer );
        }

        FileReader reader = null;
        try
        {
            reader = new FileReader( settingsFile );
            mavenTools.readSettings( reader );

            fail( "Settings should not pass validation when being read." );
        }
        catch ( IOException e )
        {
            String message = e.getMessage();
            assertTrue( message.indexOf( "Failed to validate" ) > -1 );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    public void testWriteSettings()
        throws IOException, SettingsConfigurationException, XmlPullParserException
    {
        Settings s = new Settings();
        s.setOffline( true );

        String localRepoPath = "/path/to/local/repo";

        s.setLocalRepository( localRepoPath );

        File settingsFile = File.createTempFile( "mavenTools-test.settings.", "" );
        settingsFile.deleteOnExit();

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( settingsFile );
            mavenTools.writeSettings( s, writer );
        }
        finally
        {
            IOUtil.close( writer );
        }

        FileReader reader = null;
        try
        {
            reader = new FileReader( settingsFile );
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
        throws IOException, SettingsConfigurationException
    {
        Settings s = new Settings();

        Profile p = new Profile();

        Repository r = new Repository();
        r.setUrl( "http://example.com" );

        p.addRepository( r );
        s.addProfile( p );

        File settingsFile = File.createTempFile( "mavenTools-test.settings.", "" );
        settingsFile.deleteOnExit();

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( settingsFile );
            mavenTools.writeSettings( s, writer );

            fail( "Validation of settings should fail before settings are written." );
        }
        catch ( IOException e )
        {
            String message = e.getMessage();
            assertTrue( message.indexOf( "Failed to validate" ) > -1 );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

}
