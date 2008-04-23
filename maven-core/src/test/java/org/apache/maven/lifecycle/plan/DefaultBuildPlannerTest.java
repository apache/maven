package org.apache.maven.lifecycle.plan;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.plan.testutils.TestPluginLoader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.realm.DefaultMavenRealmManager;
import org.apache.maven.realm.MavenRealmManager;
import org.apache.maven.shared.tools.easymock.MockManager;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class DefaultBuildPlannerTest
    extends PlexusTestCase
{

    private DefaultBuildPlanner buildPlanner;

    private TestPluginLoader pluginLoader;

    private MockManager mockManager = new MockManager();

    protected void setUp()
        throws Exception
    {
        super.setUp();
        buildPlanner = (DefaultBuildPlanner) lookup( BuildPlanner.class.getName(), "default" );
        pluginLoader = (TestPluginLoader) lookup( PluginLoader.class.getName(), "default" );
    }

    public void test_constructBuildPlan_ForkedPhaseFromMojoBoundInThatPhase()
        throws Exception
    {
        Model model = new Model();

        Build build = new Build();
        model.setBuild( build );

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-assembly-plugin" );
        plugin.setVersion( "1" );

        build.addPlugin( plugin );

        PluginExecution exec = new PluginExecution();
        exec.setId( "assembly" );
        exec.setPhase( "package" );
        exec.addGoal( "assembly" );

        plugin.addExecution( exec );

        PluginDescriptor pd = TestPluginLoader.createPluginDescriptor( plugin.getArtifactId(),
                                                                       "assembly",
                                                                       plugin.getGroupId(),
                                                                       plugin.getVersion() );
        MojoDescriptor md = TestPluginLoader.createMojoDescriptor( pd, "assembly" );
        md.setExecutePhase( "package" );

        pluginLoader.addPluginDescriptor( pd );

        MavenProject project = new MavenProject( model );

        MavenRealmManager realmManager = new DefaultMavenRealmManager( getContainer(), new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setRealmManager( realmManager );

        MavenSession session = new MavenSession( getContainer(), request, null, null );

        BuildPlan plan = buildPlanner.constructBuildPlan( Collections.singletonList( "package" ),
                                                          project,
                                                          session,
                                                          false );

        List rendered = plan.renderExecutionPlan( new Stack() );

        List checkIds = new ArrayList();

        checkIds.add( "org.apache.maven.plugins:maven-resources-plugin:resources" );
        checkIds.add( "org.apache.maven.plugins:maven-compiler-plugin:compile" );
        checkIds.add( "org.apache.maven.plugins:maven-resources-plugin:testResources" );
        checkIds.add( "org.apache.maven.plugins:maven-compiler-plugin:testCompile" );
        checkIds.add( "org.apache.maven.plugins:maven-surefire-plugin:test" );
        checkIds.add( "org.apache.maven.plugins:maven-jar-plugin:jar" );
        checkIds.add( "org.apache.maven.plugins.internal:maven-state-management:2.1:start-fork" );
        checkIds.add( "org.apache.maven.plugins:maven-resources-plugin:resources" );
        checkIds.add( "org.apache.maven.plugins:maven-compiler-plugin:compile" );
        checkIds.add( "org.apache.maven.plugins:maven-resources-plugin:testResources" );
        checkIds.add( "org.apache.maven.plugins:maven-compiler-plugin:testCompile" );
        checkIds.add( "org.apache.maven.plugins:maven-surefire-plugin:test" );
        checkIds.add( "org.apache.maven.plugins:maven-jar-plugin:jar" );
        checkIds.add( "org.apache.maven.plugins.internal:maven-state-management:2.1:end-fork" );
        checkIds.add( "org.apache.maven.plugins:maven-assembly-plugin:1:assembly" );
        checkIds.add( "org.apache.maven.plugins.internal:maven-state-management:2.1:clear-fork-context" );

        assertBindingIds( rendered, checkIds );
    }

    public void test_constructBuildPlan_CustomLifecycleIsUsedFromRealmManager()
        throws Exception
    {
        Model model = new Model();

        model.setPackaging( "test" );

        MavenProject project = new MavenProject( model );

        MavenRealmManager realmManager = new DefaultMavenRealmManager( getContainer(), new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

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

        BuildPlan plan = buildPlanner.constructBuildPlan( Collections.singletonList( "deploy" ),
                                                          project,
                                                          session,
                                                          false );

        List rendered = plan.renderExecutionPlan( new Stack() );

        List checkIds = new ArrayList();

        checkIds.add( "org.apache.maven.plugins:maven-deploy-plugin:deploy" );
        checkIds.add( "org.apache.maven.plugins:maven-install-plugin:install" );

        assertBindingIds( rendered, checkIds );

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

    private void assertBindingIds( List bindings,
                                   List checkIds )
    {
        assertEquals( checkIds.size(), bindings.size() );

        for ( int i = 0; i < bindings.size(); i++ )
        {
            MojoBinding binding = (MojoBinding) bindings.get( i );
            String checkId = (String) checkIds.get( i );

            assertEquals( checkId, MojoBindingUtils.toString( binding ) );
        }
    }

}
