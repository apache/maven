package org.apache.maven.project.injection;

import junit.framework.TestCase;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.List;

public class DefaultProfileInjectorTest
    extends TestCase
{

    public void testProfilePluginConfigurationShouldOverrideCollidingModelPluginConfiguration()
    {
        Plugin mPlugin = new Plugin();
        mPlugin.setGroupId( "test" );
        mPlugin.setArtifactId( "test-artifact" );
        mPlugin.setVersion( "1.0-SNAPSHOT" );

        Xpp3Dom mConfigChild = new Xpp3Dom( "test" );
        mConfigChild.setValue( "value" );

        Xpp3Dom mConfigChild2 = new Xpp3Dom( "test2" );
        mConfigChild2.setValue( "value2" );

        Xpp3Dom mConfig = new Xpp3Dom( "configuration" );
        mConfig.addChild( mConfigChild );
        mConfig.addChild( mConfigChild2 );

        mPlugin.setConfiguration( mConfig );

        Build mBuild = new Build();
        mBuild.addPlugin( mPlugin );

        Model model = new Model();
        model.setBuild( mBuild );

        Plugin pPlugin = new Plugin();
        pPlugin.setGroupId( "test" );
        pPlugin.setArtifactId( "test-artifact" );
        pPlugin.setVersion( "1.0-SNAPSHOT" );

        Xpp3Dom pConfigChild = new Xpp3Dom( "test" );
        pConfigChild.setValue( "replacedValue" );

        Xpp3Dom pConfig = new Xpp3Dom( "configuration" );
        pConfig.addChild( pConfigChild );

        pPlugin.setConfiguration( pConfig );

        BuildBase pBuild = new BuildBase();
        pBuild.addPlugin( pPlugin );

        Profile profile = new Profile();
        profile.setId( "testId" );

        profile.setBuild( pBuild );

        new DefaultProfileInjector().inject( profile, model );

        Build rBuild = model.getBuild();
        Plugin rPlugin = (Plugin) rBuild.getPlugins().get( 0 );
        Xpp3Dom rConfig = (Xpp3Dom) rPlugin.getConfiguration();

        Xpp3Dom rChild = rConfig.getChild( "test" );

        assertEquals( "replacedValue", rChild.getValue() );
        
        Xpp3Dom rChild2 = rConfig.getChild( "test2" );

        assertEquals( "value2", rChild2.getValue() );
    }

    public void testModelConfigShouldPersistWhenPluginHasExecConfigs()
    {
        Plugin mPlugin = new Plugin();
        mPlugin.setGroupId( "test" );
        mPlugin.setArtifactId( "test-artifact" );
        mPlugin.setVersion( "1.0-SNAPSHOT" );

        Xpp3Dom mConfigChild = new Xpp3Dom( "test" );
        mConfigChild.setValue( "value" );

        Xpp3Dom mConfigChild2 = new Xpp3Dom( "test2" );
        mConfigChild2.setValue( "value2" );

        Xpp3Dom mConfig = new Xpp3Dom( "configuration" );
        mConfig.addChild( mConfigChild );
        mConfig.addChild( mConfigChild2 );

        mPlugin.setConfiguration( mConfig );

        Build mBuild = new Build();
        mBuild.addPlugin( mPlugin );

        Model model = new Model();
        model.setBuild( mBuild );

        Plugin pPlugin = new Plugin();
        pPlugin.setGroupId( "test" );
        pPlugin.setArtifactId( "test-artifact" );
        pPlugin.setVersion( "1.0-SNAPSHOT" );

        PluginExecution pExec = new PluginExecution();
        pExec.setId("profile-injected");
        
        Xpp3Dom pConfigChild = new Xpp3Dom( "test" );
        pConfigChild.setValue( "replacedValue" );

        Xpp3Dom pConfig = new Xpp3Dom( "configuration" );
        pConfig.addChild( pConfigChild );

        pExec.setConfiguration( pConfig );

        pPlugin.addExecution( pExec );
        
        BuildBase pBuild = new BuildBase();
        pBuild.addPlugin( pPlugin );

        Profile profile = new Profile();
        profile.setId( "testId" );

        profile.setBuild( pBuild );

        new DefaultProfileInjector().inject( profile, model );

        Build rBuild = model.getBuild();
        Plugin rPlugin = (Plugin) rBuild.getPlugins().get( 0 );
        
        PluginExecution rExec = (PluginExecution) rPlugin.getExecutionsAsMap().get( "profile-injected" );
        
        assertNotNull( rExec );
        
        Xpp3Dom rExecConfig = (Xpp3Dom) rExec.getConfiguration();

        Xpp3Dom rChild = rExecConfig.getChild( "test" );

        assertEquals( "replacedValue", rChild.getValue() );
        
        Xpp3Dom rConfig = (Xpp3Dom) rPlugin.getConfiguration();
        
        assertNotNull( rConfig );
        
        Xpp3Dom rChild2 = rConfig.getChild( "test2" );

        assertEquals( "value2", rChild2.getValue() );
    }
    
    public void testProfileRepositoryShouldOverrideModelRepository()
    {
        Repository mRepository = new Repository();
        mRepository.setId( "testId" );
        mRepository.setName( "Test repository" );
        mRepository.setUrl( "http://www.google.com" );

        Model model = new Model();
        model.addRepository( mRepository );

        Repository pRepository = new Repository();
        pRepository.setId( "testId" );
        pRepository.setName( "Test repository" );
        pRepository.setUrl( "http://www.yahoo.com" );

        Profile profile = new Profile();
        profile.setId( "testId" );

        profile.addRepository( pRepository );

        new DefaultProfileInjector().inject( profile, model );

        Repository rRepository = (Repository) model.getRepositories().get( 0 );

        assertEquals( "http://www.yahoo.com", rRepository.getUrl() );
    }

    public void testShouldPreserveModelModulesWhenProfileHasNone()
    {
        Model model = new Model();

        model.addModule( "module1" );

        Profile profile = new Profile();
        profile.setId( "testId" );

        new DefaultProfileInjector().inject( profile, model );

        List rModules = model.getModules();

        assertEquals( 1, rModules.size() );
        assertEquals( "module1", rModules.get( 0 ) );
    }

}
