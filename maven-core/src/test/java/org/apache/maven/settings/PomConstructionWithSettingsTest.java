package org.apache.maven.settings;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Profile;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileActivationContext;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.harness.PomTestWrapper;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomConstructionWithSettingsTest     
	extends PlexusTestCase
{
    private static String BASE_DIR = "src/test";

    private static String BASE_POM_DIR = BASE_DIR + "/resources-settings";

    private DefaultMavenProjectBuilder mavenProjectBuilder;

    private File testDirectory;

    protected void setUp()
        throws Exception
    {
        testDirectory = new File( getBasedir(), BASE_POM_DIR );
        mavenProjectBuilder = (DefaultMavenProjectBuilder) lookup( MavenProjectBuilder.class );
    }
    
    public void testSettingsNoPom() throws Exception
    {
    	PomTestWrapper pom = buildPom( "settings-no-pom" );
    	assertEquals( "local-profile-prop-value", pom.getValue( "properties/local-profile-prop" ) );
    }
    
    /**MNG-4107 */
    public void testPomAndSettingsInterpolation() throws Exception
    {
    	PomTestWrapper pom = buildPom( "test-pom-and-settings-interpolation" );
    	assertEquals("applied", pom.getValue( "properties/settingsProfile" ) );
    	assertEquals("applied", pom.getValue( "properties/pomProfile" ) );
    	assertEquals("settings", pom.getValue( "properties/pomVsSettings" ) );
    	assertEquals("settings", pom.getValue( "properties/pomVsSettingsInterpolated" ) );
    }    

    private PomTestWrapper buildPom( String pomPath )
    throws Exception
	{
	    File pomFile = new File( testDirectory + File.separator + pomPath , "pom.xml" );
	    File settingsFile = new File( testDirectory + File.separator + pomPath, "settings.xml" );
	    
	    Settings settings = readSettingsFile(settingsFile);
	    
	    ProfileActivationContext pCtx = new ProfileActivationContext(null, true);
	    ProfileManager profileManager = new DefaultProfileManager(pCtx);
	    
	    for ( org.apache.maven.settings.Profile rawProfile : settings.getProfiles() )
	    {
	        Profile profile = SettingsUtils.convertFromSettingsProfile( rawProfile );
	
	        profileManager.addProfile( profile );
	    }    

        List<String> settingsActiveProfileIds = settings.getActiveProfiles();

        if ( settingsActiveProfileIds != null )
        {
            for ( String profileId : settingsActiveProfileIds )
            {
                profileManager.getProfileActivationContext().setActive( profileId );
            }
        }	    
	    
	    ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();
	    config.setLocalRepository(new DefaultArtifactRepository("default", "", new DefaultRepositoryLayout()));
	
	    config.setGlobalProfileManager(profileManager);
	    return new PomTestWrapper( pomFile, mavenProjectBuilder.build( pomFile, config ) );
	}  
    
    private static Settings readSettingsFile(File settingsFile) 
    	throws IOException, XmlPullParserException
    {
        Settings settings = null;

        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( settingsFile );

            SettingsXpp3Reader modelReader = new SettingsXpp3Reader();

            settings = modelReader.read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return settings;    	
    }
}
