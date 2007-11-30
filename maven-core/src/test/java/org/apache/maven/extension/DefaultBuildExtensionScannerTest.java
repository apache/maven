package org.apache.maven.extension;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.model.DefaultModelLineage;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.shared.tools.easymock.MockManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;

import java.io.File;
import java.util.Collections;

import junit.framework.TestCase;

public class DefaultBuildExtensionScannerTest
    extends TestCase
{

    private MockManager mockManager;

    private MockControl extensionManagerCtl;

    private ExtensionManager extensionManager;

    private MockControl projectBuilderCtl;

    private MavenProjectBuilder projectBuilder;

    private MockControl modelLineageBuilderCtl;

    private ModelLineageBuilder modelLineageBuilder;

    private MockControl modelInterpolatorCtl;

    private ModelInterpolator modelInterpolator;

    public void setUp()
        throws Exception
    {
        super.setUp();

        mockManager = new MockManager();

        extensionManagerCtl = MockControl.createControl( ExtensionManager.class );
        mockManager.add( extensionManagerCtl );
        extensionManager = (ExtensionManager) extensionManagerCtl.getMock();

        projectBuilderCtl = MockControl.createControl( MavenProjectBuilder.class );
        mockManager.add( projectBuilderCtl );
        projectBuilder = (MavenProjectBuilder) projectBuilderCtl.getMock();

        modelLineageBuilderCtl = MockControl.createControl( ModelLineageBuilder.class );
        mockManager.add( modelLineageBuilderCtl );
        modelLineageBuilder = (ModelLineageBuilder) modelLineageBuilderCtl.getMock();

        modelInterpolatorCtl = MockControl.createControl( ModelInterpolator.class );
        mockManager.add( modelInterpolatorCtl );
        modelInterpolator = (ModelInterpolator) modelInterpolatorCtl.getMock();
    }

    public void testIncludePluginWhenExtensionsFlagDirectlySet()
        throws ExtensionScanningException, ProjectBuildingException, ModelInterpolationException,
        ExtensionManagerException
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        File pomFile = new File( "pom" );

        Model model = new Model();

        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );

        Build build = new Build();
        model.setBuild( build );

        Plugin plugin = new Plugin();

        build.addPlugin( plugin );

        plugin.setGroupId( "test" );
        plugin.setArtifactId( "artifact" );
        plugin.setExtensions( true );

        ModelLineage ml = new DefaultModelLineage();
        ml.setOrigin( model, pomFile, Collections.EMPTY_LIST, true );

        modelLineageBuilder.buildModelLineage( pomFile, null, null, null, false, true );
        modelLineageBuilderCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        modelLineageBuilderCtl.setReturnValue( ml );

        modelInterpolator.interpolate( model, null, false );
        modelInterpolatorCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        modelInterpolatorCtl.setReturnValue( model );

        extensionManager.addPluginAsExtension( plugin, model, Collections.EMPTY_LIST, request );
        extensionManagerCtl.setVoidCallable();

        MavenProject superProject = new MavenProject( new Model() );
        superProject.setRemoteArtifactRepositories( Collections.EMPTY_LIST );

        projectBuilder.buildStandaloneSuperProject();
        projectBuilderCtl.setReturnValue( superProject );

        mockManager.replayAll();

        new DefaultBuildExtensionScanner( extensionManager, projectBuilder, modelLineageBuilder,
                                          modelInterpolator, new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                "test" ) ).scanForBuildExtensions( pomFile,
                                                                                                                   request );

        mockManager.verifyAll();
    }

    public void testIncludePluginWhenExtensionsFlagSetInPluginManagement()
        throws ExtensionScanningException, ProjectBuildingException, ModelInterpolationException,
        ExtensionManagerException
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        File pomFile = new File( "pom" );

        Model model = new Model();

        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );

        Build build = new Build();
        model.setBuild( build );

        Plugin plugin = new Plugin();

        build.addPlugin( plugin );

        plugin.setGroupId( "test" );
        plugin.setArtifactId( "artifact" );

        Parent parent = new Parent();
        parent.setGroupId( "group" );
        parent.setArtifactId( "parent" );
        model.setParent( parent );

        Model parentModel = new Model();
        parentModel.setGroupId( "group" );
        parentModel.setArtifactId( "parent" );

        Build parentBuild = new Build();
        parentModel.setBuild( parentBuild );

        PluginManagement pMgmt = new PluginManagement();
        parentBuild.setPluginManagement( pMgmt );

        Plugin parentPlugin = new Plugin();

        pMgmt.addPlugin( parentPlugin );

        parentPlugin.setGroupId( "test" );
        parentPlugin.setArtifactId( "artifact" );
        parentPlugin.setExtensions( true );

        ModelLineage ml = new DefaultModelLineage();
        ml.setOrigin( model, pomFile, Collections.EMPTY_LIST, true );
        ml.addParent( parentModel, pomFile, Collections.EMPTY_LIST, false );

        modelLineageBuilder.buildModelLineage( pomFile, null, null, null, false, true );
        modelLineageBuilderCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        modelLineageBuilderCtl.setReturnValue( ml );

        modelInterpolator.interpolate( model, null, false );
        modelInterpolatorCtl.setMatcher( new ArgumentsMatcher(){
            public boolean matches( Object[] expected,
                                    Object[] actual )
            {
                return expected[0].equals( actual[0] );
            }

            public String toString( Object[] arguments )
            {
                return "matcher for: " + ((Model) arguments[0]).getId();
            }
        } );
        modelInterpolatorCtl.setReturnValue( model, 1 );

        modelInterpolator.interpolate( parentModel, null, false );
        modelInterpolatorCtl.setReturnValue( parentModel, 1 );

        extensionManager.addPluginAsExtension( plugin, model, Collections.EMPTY_LIST, request );
        extensionManagerCtl.setVoidCallable();

        MavenProject superProject = new MavenProject( new Model() );
        superProject.setRemoteArtifactRepositories( Collections.EMPTY_LIST );

        projectBuilder.buildStandaloneSuperProject();
        projectBuilderCtl.setReturnValue( superProject );

        mockManager.replayAll();

        new DefaultBuildExtensionScanner( extensionManager, projectBuilder, modelLineageBuilder,
                                          modelInterpolator, new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                "test" ) ).scanForBuildExtensions( pomFile,
                                                                                                                   request );

        mockManager.verifyAll();
    }
}
