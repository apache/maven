package org.apache.maven.lifecycle.binding;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.BuildBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.realm.DefaultMavenRealmManager;
import org.apache.maven.realm.MavenRealmManager;
import org.apache.maven.shared.tools.easymock.MockManager;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.easymock.MockControl;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class DefaultLifecycleBindingManagerTest
    extends PlexusTestCase
{

    private LifecycleBindingManager mgr;

    public void setUp()
        throws Exception
    {
        super.setUp();

        mgr = (LifecycleBindingManager) lookup( LifecycleBindingManager.ROLE, "default" );
    }

    public void testLookup()
    {
        assertNotNull( mgr );
    }

    public void testGetBindingsForPackaging_TestMergePluginConfigToBinding()
        throws Exception
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        Build build = new Build();
        model.setBuild( build );

        Plugin plugin = new Plugin();
        build.addPlugin( plugin );

        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-compiler-plugin" );

        Properties pluginConfig = new Properties();

        pluginConfig.setProperty( "test", "value" );
        pluginConfig.setProperty( "test2", "other-value" );

        plugin.setConfiguration( createConfiguration( pluginConfig ) );

        MavenProject project = new MavenProject( model );

        MavenRealmManager realmManager = new DefaultMavenRealmManager(
                                                                       getContainer(),
                                                                       new ConsoleLogger(
                                                                                          Logger.LEVEL_DEBUG,
                                                                                          "test" ) );
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setRealmManager( realmManager );

        LifecycleBindings lifecycleBindings = mgr.getBindingsForPackaging( project,
                                                                           new MavenSession(
                                                                                             getContainer(),
                                                                                             request,
                                                                                             null,
                                                                                             null ) );

        List bindings = lifecycleBindings.getBuildBinding().getCompile().getBindings();

        assertNotNull( bindings );
        assertEquals( 1, bindings.size() );

        MojoBinding binding = (MojoBinding) bindings.get( 0 );

        Xpp3Dom config = (Xpp3Dom) binding.getConfiguration();

        assertNotNull( config );

        assertEquals( "value", config.getChild( "test" ).getValue() );
        assertEquals( "other-value", config.getChild( "test2" ).getValue() );
    }

    public void testGetBindingsForPackaging_TestMergePluginManagementConfigToBinding()
        throws Exception
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        Build build = new Build();
        model.setBuild( build );

        PluginManagement plugMgmt = new PluginManagement();
        build.setPluginManagement( plugMgmt );

        Plugin plugin = new Plugin();
        plugMgmt.addPlugin( plugin );

        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-compiler-plugin" );

        Properties pluginConfig = new Properties();

        pluginConfig.setProperty( "test", "value" );
        pluginConfig.setProperty( "test2", "other-value" );

        plugin.setConfiguration( createConfiguration( pluginConfig ) );

        MavenProject project = new MavenProject( model );

        MavenRealmManager realmManager = new DefaultMavenRealmManager(
                                                                       getContainer(),
                                                                       new ConsoleLogger(
                                                                                          Logger.LEVEL_DEBUG,
                                                                                          "test" ) );
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setRealmManager( realmManager );

        LifecycleBindings lifecycleBindings = mgr.getBindingsForPackaging( project,
                                                                           new MavenSession(
                                                                                             getContainer(),
                                                                                             request,
                                                                                             null,
                                                                                             null ) );

        List bindings = lifecycleBindings.getBuildBinding().getCompile().getBindings();

        assertNotNull( bindings );
        assertEquals( 1, bindings.size() );

        MojoBinding binding = (MojoBinding) bindings.get( 0 );

        Xpp3Dom config = (Xpp3Dom) binding.getConfiguration();

        assertNotNull( config );

        assertEquals( "value", config.getChild( "test" ).getValue() );
        assertEquals( "other-value", config.getChild( "test2" ).getValue() );
    }

    public void testGetProjectCustomBindings_ExecutionConfigShouldOverridePluginConfig()
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        Build build = new Build();
        model.setBuild( build );

        Plugin plugin = new Plugin();
        build.addPlugin( plugin );

        plugin.setGroupId( "plugin.group" );
        plugin.setArtifactId( "plugin-artifact" );
        plugin.setVersion( "1" );

        Properties pluginConfig = new Properties();

        pluginConfig.setProperty( "test", "value" );
        pluginConfig.setProperty( "test2", "other-value" );

        plugin.setConfiguration( createConfiguration( pluginConfig ) );

        PluginExecution exec = new PluginExecution();
        plugin.addExecution( exec );

        exec.setId( "test-execution" );
        exec.setPhase( "validate" );
        exec.setGoals( Collections.singletonList( "goal" ) );

        Properties execConfig = new Properties();
        execConfig.setProperty( "test", "value2" );

        exec.setConfiguration( createConfiguration( execConfig ) );

        MavenProject project = new MavenProject( model );

        LifecycleBindings lifecycleBindings = mgr.getProjectCustomBindings( project, null, new HashSet() );

        List bindings = lifecycleBindings.getBuildBinding().getValidate().getBindings();

        assertNotNull( bindings );
        assertEquals( 1, bindings.size() );

        MojoBinding binding = (MojoBinding) bindings.get( 0 );

        Xpp3Dom config = (Xpp3Dom) binding.getConfiguration();

        assertEquals( "value2", config.getChild( "test" ).getValue() );
        assertEquals( "other-value", config.getChild( "test2" ).getValue() );
    }

    public void test_GetBindingsForPackaging_CustomLifecycleIsUsedFromRealmManager()
        throws Exception
    {
        Model model = new Model();

        model.setPackaging( "test" );

        MavenProject project = new MavenProject( model );

        MavenRealmManager realmManager = new DefaultMavenRealmManager(
                                                                       getContainer(),
                                                                       new ConsoleLogger(
                                                                                          Logger.LEVEL_DEBUG,
                                                                                          "test" ) );

        MockManager mockManager = new MockManager();

        MockControl extArtifactCtl = MockControl.createControl( Artifact.class );
        mockManager.add( extArtifactCtl );

        Artifact extensionArtifact = (Artifact) extArtifactCtl.getMock();

        extensionArtifact.getGroupId();
        extArtifactCtl.setReturnValue( "group", MockControl.ZERO_OR_MORE );

        extensionArtifact.getArtifactId();
        extArtifactCtl.setReturnValue( "artifact", MockControl.ZERO_OR_MORE );

        extensionArtifact.getVersion();
        extArtifactCtl.setReturnValue( "1", MockControl.ZERO_OR_MORE );

        extensionArtifact.getFile();
        extArtifactCtl.setReturnValue( getResourceFile( "org/apache/maven/lifecycle/plan/test-custom-lifecycle-buildPlan-1.jar" ),
                                       MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        realmManager.createExtensionRealm( extensionArtifact, Collections.EMPTY_LIST );
        realmManager.importExtensionsIntoProjectRealm( "group", "project", "1", extensionArtifact );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setRealmManager( realmManager );

        MavenSession session = new MavenSession( getContainer(), request, null, null );

        LifecycleBindings lifecycleBindings = mgr.getBindingsForPackaging( project, session );

        BuildBinding buildBinding = lifecycleBindings.getBuildBinding();
        assertNotNull( buildBinding );

        Phase installPhase = buildBinding.getInstall();
        Phase deployPhase = buildBinding.getDeploy();
        Phase packagePhase = buildBinding.getCreatePackage();

        assertTrue( ( packagePhase.getBindings() == null ) || packagePhase.getBindings().isEmpty() );
        assertNotNull( installPhase.getBindings() );
        assertEquals( 1, installPhase.getBindings().size() );
        assertEquals( "maven-deploy-plugin",
                      ( (MojoBinding) installPhase.getBindings().get( 0 ) ).getArtifactId() );

        assertNotNull( deployPhase.getBindings() );
        assertEquals( 1, deployPhase.getBindings().size() );
        assertEquals( "maven-install-plugin",
                      ( (MojoBinding) deployPhase.getBindings().get( 0 ) ).getArtifactId() );

        mockManager.verifyAll();
    }

    private File getResourceFile( String path )
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        URL resource = cloader.getResource( path );

        if ( resource == null )
        {
            fail( "Cannot find test resource: " + path );
        }

        return new File( resource.getPath() );
    }

    private Object createConfiguration( final Properties configProperties )
    {
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        for ( Iterator it = configProperties.keySet().iterator(); it.hasNext(); )
        {
            String key = (String) it.next();
            String value = configProperties.getProperty( key );

            Xpp3Dom child = new Xpp3Dom( key );
            child.setValue( value );

            config.addChild( child );
        }

        return config;
    }

}
