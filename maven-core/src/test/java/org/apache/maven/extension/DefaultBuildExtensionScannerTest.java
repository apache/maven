package org.apache.maven.extension;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.model.DefaultModelLineage;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.reactor.MissingModuleException;
import org.apache.maven.shared.tools.easymock.MockManager;
import org.apache.maven.shared.tools.easymock.TestFileManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;

public class DefaultBuildExtensionScannerTest
    extends TestCase
{

    private MockManager mockManager;

    private TestFileManager fileManager;

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
        fileManager = new TestFileManager( "DefaultBuildExtensionScannerTest", "" );

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

    public void tearDown()
        throws Exception
    {
        fileManager.cleanUp();

        super.tearDown();
    }

    public void testIncludePluginWhenExtensionsFlagDirectlySet()
        throws ExtensionScanningException, ProjectBuildingException, ModelInterpolationException,
        ExtensionManagerException, MissingModuleException
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

        modelLineageBuilder.buildModelLineage( pomFile, new DefaultProjectBuilderConfiguration(), null, false, true );
        modelLineageBuilderCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        modelLineageBuilderCtl.setReturnValue( ml, MockControl.ZERO_OR_MORE );

        modelInterpolator.interpolate( model, null, null, false );
        modelInterpolatorCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        modelInterpolatorCtl.setReturnValue( model, MockControl.ZERO_OR_MORE );

        extensionManager.addPluginAsExtension( plugin, model, Collections.EMPTY_LIST, request );
        extensionManagerCtl.setVoidCallable( 1 );

        MavenProject superProject = new MavenProject( new Model() );
        superProject.setRemoteArtifactRepositories( Collections.EMPTY_LIST );

        projectBuilder.buildStandaloneSuperProject( new DefaultProjectBuilderConfiguration() );
        projectBuilderCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        projectBuilderCtl.setReturnValue( superProject, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        new DefaultBuildExtensionScanner( extensionManager, projectBuilder, modelLineageBuilder,
                                          modelInterpolator, new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                "test" ) ).scanForBuildExtensions( pomFile,
                                                                                                                   request,
                                                                                                                   false );

        mockManager.verifyAll();
    }

    public void testIncludePluginWhenExtensionsFlagSetInPluginManagement()
        throws ExtensionScanningException, ProjectBuildingException, ModelInterpolationException,
        ExtensionManagerException, MissingModuleException
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

        modelLineageBuilder.buildModelLineage( pomFile, new DefaultProjectBuilderConfiguration(), null, false, true );
        modelLineageBuilderCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        modelLineageBuilderCtl.setReturnValue( ml, MockControl.ZERO_OR_MORE );

        modelInterpolator.interpolate( model, null, null, false );
        modelInterpolatorCtl.setMatcher( new FirstArgFileMatcher() );
        modelInterpolatorCtl.setReturnValue( model, MockControl.ZERO_OR_MORE);

        modelInterpolator.interpolate( parentModel, null, null, false );
        modelInterpolatorCtl.setReturnValue( parentModel, MockControl.ZERO_OR_MORE );

        extensionManager.addPluginAsExtension( plugin, model, Collections.EMPTY_LIST, request );
        extensionManagerCtl.setVoidCallable( 1 );

        MavenProject superProject = new MavenProject( new Model() );
        superProject.setRemoteArtifactRepositories( Collections.EMPTY_LIST );

        projectBuilder.buildStandaloneSuperProject( new DefaultProjectBuilderConfiguration() );
        projectBuilderCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        projectBuilderCtl.setReturnValue( superProject, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        new DefaultBuildExtensionScanner( extensionManager, projectBuilder, modelLineageBuilder,
                                          modelInterpolator, new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                "test" ) ).scanForBuildExtensions( pomFile,
                                                                                                                   request,
                                                                                                                   false );

        mockManager.verifyAll();
    }

    public void testIncludePluginWithExtensionsFlagDeclaredInParentPluginManagementReferencedFromModule()
        throws ModelInterpolationException, ProjectBuildingException, ExtensionManagerException,
        ExtensionScanningException, IOException, MissingModuleException
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        File projectDir = fileManager.createTempDir();
        File pomFile = fileManager.createFile( projectDir, "pom.xml", "Placeholder file." ).getCanonicalFile();
        File modulePomFile = fileManager.createFile( projectDir,
                                                     "module/pom.xml",
                                                     "Placeholder file." ).getCanonicalFile();

        Model model = new Model();

        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        Parent parent = new Parent();
        parent.setGroupId( "group" );
        parent.setArtifactId( "parent" );
        parent.setVersion( "1" );
        model.setParent( parent );

        Model parentModel = new Model();
        parentModel.setGroupId( "group" );
        parentModel.setArtifactId( "parent" );
        parentModel.setVersion( "1" );

        Build parentBuild = new Build();
        parentModel.setBuild( parentBuild );

        PluginManagement pMgmt = new PluginManagement();
        parentBuild.setPluginManagement( pMgmt );

        Plugin parentPlugin = new Plugin();

        pMgmt.addPlugin( parentPlugin );

        parentPlugin.setGroupId( "test" );
        parentPlugin.setArtifactId( "artifact" );
        parentPlugin.setExtensions( true );

        Model module = new Model();
        module.setGroupId( "group" );
        module.setArtifactId( "module" );
        module.setVersion( "1" );

        Parent moduleParent = new Parent();
        moduleParent.setGroupId( model.getGroupId() );
        moduleParent.setArtifactId( model.getArtifactId() );
        moduleParent.setVersion( model.getVersion() );

        module.setParent( moduleParent );

        Build build = new Build();
        module.setBuild( build );

        Plugin plugin = new Plugin();

        build.addPlugin( plugin );

        plugin.setGroupId( "test" );
        plugin.setArtifactId( "artifact" );

        model.addModule( "module" );

        ModelLineage ml = new DefaultModelLineage();
        ml.setOrigin( model, pomFile, Collections.EMPTY_LIST, true );
        ml.addParent( parentModel, pomFile, Collections.EMPTY_LIST, false );

        ModelLineage moduleMl = new DefaultModelLineage();
        moduleMl.setOrigin( module, modulePomFile, Collections.EMPTY_LIST, true );
        moduleMl.addParent( model, pomFile, Collections.EMPTY_LIST, true );
        moduleMl.addParent( parentModel, pomFile, Collections.EMPTY_LIST, false );

        modelLineageBuilder.buildModelLineage( pomFile, new DefaultProjectBuilderConfiguration(), null, false, true );
        modelLineageBuilderCtl.setMatcher( new FirstArgFileMatcher() );
        modelLineageBuilderCtl.setReturnValue( ml, MockControl.ZERO_OR_MORE );

        modelLineageBuilder.buildModelLineage( modulePomFile, new DefaultProjectBuilderConfiguration(), null, false, true );
        modelLineageBuilderCtl.setReturnValue( moduleMl, MockControl.ZERO_OR_MORE );

        modelInterpolator.interpolate( model, null, null, false );
        modelInterpolatorCtl.setMatcher( new FirstArgModelIdMatcher() );
        modelInterpolatorCtl.setReturnValue( model, MockControl.ZERO_OR_MORE );

        modelInterpolator.interpolate( parentModel, null, null, false );
        modelInterpolatorCtl.setReturnValue( parentModel, MockControl.ZERO_OR_MORE );

        modelInterpolator.interpolate( module, null, null, false );
        modelInterpolatorCtl.setReturnValue( module, MockControl.ZERO_OR_MORE );

        extensionManager.addPluginAsExtension( plugin, module, Collections.EMPTY_LIST, request );
        extensionManagerCtl.setVoidCallable( 1 );

        MavenProject superProject = new MavenProject( new Model() );
        superProject.setRemoteArtifactRepositories( Collections.EMPTY_LIST );

        projectBuilder.buildStandaloneSuperProject( new DefaultProjectBuilderConfiguration() );
        projectBuilderCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        projectBuilderCtl.setReturnValue( superProject, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        new DefaultBuildExtensionScanner( extensionManager, projectBuilder, modelLineageBuilder,
                                          modelInterpolator, new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                "test" ) ).scanForBuildExtensions( pomFile,
                                                                                                                   request,
                                                                                                                   false );

        mockManager.verifyAll();
    }

    private static final class FirstArgFileMatcher
        implements ArgumentsMatcher
    {
        public boolean matches( Object[] expected,
                                Object[] actual )
        {
            return expected[0].equals( actual[0] );
        }

        public String toString( Object[] arguments )
        {
            return "matcher for file: " + arguments[0];
        }
    }

    private static final class FirstArgModelIdMatcher
        implements ArgumentsMatcher
    {
        public boolean matches( Object[] expected,
                                Object[] actual )
        {
            return ((Model)expected[0]).getId().equals( ((Model)actual[0]).getId() );
        }

        public String toString( Object[] arguments )
        {
            return "matcher for: " + ( (Model) arguments[0] ).getId();
        }
    }
}
