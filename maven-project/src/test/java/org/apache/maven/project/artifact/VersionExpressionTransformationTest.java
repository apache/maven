package org.apache.maven.project.artifact;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.transform.ArtifactTransformation;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

public class VersionExpressionTransformationTest
    extends PlexusTestCase
{

    private static final String VERSION = "blah";

    private VersionExpressionTransformation transformation;

    public void setUp()
        throws Exception
    {
        super.setUp();

        transformation =
            (VersionExpressionTransformation) lookup( ArtifactTransformation.class.getName(), "version-expression" );
    }

    public void testTransformForResolve_DoNothing()
        throws IOException, XmlPullParserException, ArtifactResolutionException, ArtifactNotFoundException
    {
        Model model = buildTestModel();

        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();
        try
        {
            File pomFile = new File( pomDir, "pom.xml" );
            pomFile.deleteOnExit();

            FileWriter writer = null;
            try
            {
                writer = new FileWriter( pomFile );
                new MavenXpp3Writer().write( writer, model );
            }
            finally
            {
                IOUtil.close( writer );
            }

            Artifact a =
                new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1" ), null, "jar", null,
                                     new DefaultArtifactHandler( "jar" ), false );
            ProjectArtifactMetadata pam = new ProjectArtifactMetadata( a, pomFile );

            a.addMetadata( pam );

            transformation.transformForResolve( a, Collections.EMPTY_LIST, null );

            assertFalse( pam.isVersionExpressionsResolved() );
            assertEquals( pomFile, pam.getFile() );

            assertFalse( new File( pomDir, "target/pom-transformed.xml" ).exists() );

            FileReader reader = null;
            try
            {
                reader = new FileReader( pomFile );
                model = new MavenXpp3Reader().read( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }

            assertEquals( "${testVersion}", model.getVersion() );
        }
        finally
        {
            FileUtils.forceDelete( pomDir );
        }
    }

    public void testTransformForInstall_TransformBasedOnModelProperties()
        throws IOException, ArtifactInstallationException, XmlPullParserException
    {
        Model model = buildTestModel();

        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();
        try
        {
            File pomFile = new File( pomDir, "pom.xml" );
            pomFile.deleteOnExit();

            FileWriter writer = null;
            try
            {
                writer = new FileWriter( pomFile );
                new MavenXpp3Writer().write( writer, model );
            }
            finally
            {
                IOUtil.close( writer );
            }

            Artifact a =
                new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1" ), null, "jar", null,
                                     new DefaultArtifactHandler( "jar" ), false );

            ProjectArtifactMetadata pam = new ProjectArtifactMetadata( a, pomFile );

            a.addMetadata( pam );

            transformation.transformForInstall( a, null );

            File transformedFile = new File( pomDir, "target/pom-transformed.xml" );

            assertTrue( transformedFile.exists() );
            assertEquals( transformedFile, pam.getFile() );

            FileReader reader = null;
            try
            {
                reader = new FileReader( pam.getFile() );
                model = new MavenXpp3Reader().read( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }

            assertTransformedVersions( model );
        }
        finally
        {
            FileUtils.forceDelete( pomDir );
        }
    }

    public void testTransformForDeploy_TransformBasedOnModelProperties()
        throws IOException, XmlPullParserException, ArtifactDeploymentException
    {
        Model model = buildTestModel();

        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();
        try
        {
            File pomFile = new File( pomDir, "pom.xml" );
            pomFile.deleteOnExit();

            FileWriter writer = null;
            try
            {
                writer = new FileWriter( pomFile );
                new MavenXpp3Writer().write( writer, model );
            }
            finally
            {
                IOUtil.close( writer );
            }

            Artifact a =
                new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1" ), null, "jar", null,
                                     new DefaultArtifactHandler( "jar" ), false );

            ProjectArtifactMetadata pam = new ProjectArtifactMetadata( a, pomFile );

            a.addMetadata( pam );

            transformation.transformForDeployment( a, null, null );

            File transformedFile = new File( pomDir, "target/pom-transformed.xml" );

            assertTrue( transformedFile.exists() );
            assertEquals( transformedFile, pam.getFile() );

            FileReader reader = null;
            try
            {
                reader = new FileReader( pam.getFile() );
                model = new MavenXpp3Reader().read( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }

            assertTransformedVersions( model );
        }
        finally
        {
            FileUtils.forceDelete( pomDir );
        }
    }

    // FIXME: We can't be this smart (yet) since the deployment step transforms from the 
    // original POM once again and re-installs over the top of the install step.
//    public void testTransformForInstall_SkipIfProjectArtifactMetadataResolvedFlagIsSet()
//        throws IOException, ArtifactInstallationException, XmlPullParserException
//    {
//        Model model = buildTestModel();
//
//        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
//        pomDir.delete();
//        pomDir.mkdirs();
//        try
//        {
//            File pomFile = new File( pomDir, "pom.xml" );
//            pomFile.deleteOnExit();
//
//            FileWriter writer = null;
//            try
//            {
//                writer = new FileWriter( pomFile );
//                new MavenXpp3Writer().write( writer, model );
//            }
//            finally
//            {
//                IOUtil.close( writer );
//            }
//
//            Artifact a =
//                new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1" ), null, "jar", null,
//                                     new DefaultArtifactHandler( "jar" ), false );
//            ProjectArtifactMetadata pam = new ProjectArtifactMetadata( a, pomFile );
//            pam.setVersionExpressionsResolved( true );
//
//            a.addMetadata( pam );
//
//            transformation.transformForInstall( a, null );
//
//            assertEquals( pomFile, pam.getFile() );
//
//            assertFalse( new File( pomDir, "target/pom-transformed.xml" ).exists() );
//
//            FileReader reader = null;
//            try
//            {
//                reader = new FileReader( pomFile );
//                model = new MavenXpp3Reader().read( reader );
//            }
//            finally
//            {
//                IOUtil.close( reader );
//            }
//
//            assertEquals( "${testVersion}", model.getVersion() );
//        }
//        finally
//        {
//            FileUtils.forceDelete( pomDir );
//        }
//    }

    // FIXME: We can't be this smart (yet) since the deployment step transforms from the 
    // original POM once again and re-installs over the top of the install step.
//    public void testTransformForDeploy_SkipIfProjectArtifactMetadataResolvedFlagIsSet()
//        throws IOException, XmlPullParserException, ArtifactDeploymentException
//    {
//        Model model = buildTestModel();
//
//        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
//        pomDir.delete();
//        pomDir.mkdirs();
//        try
//        {
//            File pomFile = new File( pomDir, "pom.xml" );
//            pomFile.deleteOnExit();
//
//            FileWriter writer = null;
//            try
//            {
//                writer = new FileWriter( pomFile );
//                new MavenXpp3Writer().write( writer, model );
//            }
//            finally
//            {
//                IOUtil.close( writer );
//            }
//
//            Artifact a =
//                new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1" ), null, "jar", null,
//                                     new DefaultArtifactHandler( "jar" ), false );
//            ProjectArtifactMetadata pam = new ProjectArtifactMetadata( a, pomFile );
//            pam.setVersionExpressionsResolved( true );
//
//            a.addMetadata( pam );
//
//            transformation.transformForDeployment( a, null, null );
//
//            assertEquals( pomFile, pam.getFile() );
//
//            assertFalse( new File( pomDir, "target/pom-transformed.xml" ).exists() );
//
//            FileReader reader = null;
//            try
//            {
//                reader = new FileReader( pomFile );
//                model = new MavenXpp3Reader().read( reader );
//            }
//            finally
//            {
//                IOUtil.close( reader );
//            }
//
//            assertEquals( "${testVersion}", model.getVersion() );
//        }
//        finally
//        {
//            FileUtils.forceDelete( pomDir );
//        }
//    }

    public void testTransformVersion_ShouldInterpolate_VanillaArtifact_ModelProperties()
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        Model model = buildTestModel();

        model = runTransformVersion_VanillaArtifact( model );

        assertTransformedVersions( model );
    }

    public void testTransformVersion_ShouldInterpolate_ArtifactWithProject_ModelProperties()
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        Model model = buildTestModel();

        model = runTransformVersion_ArtifactWithProject( model, new DefaultProjectBuilderConfiguration() );

        assertTransformedVersions( model );
    }

    public void testTransformVersion_ShouldInterpolate_ArtifactWithProject_CLIProperties()
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        Model model = buildTestModel();

        Properties props = model.getProperties();
        model.setProperties( new Properties() );

        model =
            runTransformVersion_ArtifactWithProject(
                                                     model,
                                                     new DefaultProjectBuilderConfiguration().setExecutionProperties( props ) );

        assertTransformedVersions( model );
    }

    private Model runTransformVersion_VanillaArtifact( Model model )
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        File projectDir = File.createTempFile( "VersionExpressionTransformationTest.project.", ".tmp.dir" );
        projectDir.delete();
        projectDir.mkdirs();

        File repoDir = File.createTempFile( "VersionExpressionTransformationTest.repo.", ".tmp.dir" );
        repoDir.delete();
        repoDir.mkdirs();

        try
        {
            File pomFile = new File( projectDir, "pom.xml" );
            FileWriter writer = null;
            try
            {
                writer = new FileWriter( pomFile );
                new MavenXpp3Writer().write( writer, model );
            }
            finally
            {
                IOUtil.close( writer );
            }

            model.getBuild().setOutputDirectory( new File( projectDir, "target" ).getAbsolutePath() );

            Artifact a =
                new DefaultArtifact( model.getGroupId(), model.getArtifactId(), VersionRange.createFromVersion( "1" ),
                                     null, "jar", null, new DefaultArtifactHandler( "jar" ) );

            ArtifactRepository localRepository =
                new DefaultArtifactRepository( "local", repoDir.getAbsolutePath(), new DefaultRepositoryLayout() );

            transformation.transformVersions( pomFile, a, localRepository );

            FileReader reader = null;
            try
            {
                reader = new FileReader( new File( projectDir, "target/pom-transformed.xml" ) );

                model = new MavenXpp3Reader().read( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }
        finally
        {
            FileUtils.forceDelete( projectDir );
            FileUtils.forceDelete( repoDir );
        }

        return model;
    }

    private Model runTransformVersion_ArtifactWithProject( Model model, ProjectBuilderConfiguration pbConfig )
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        File projectDir = File.createTempFile( "VersionExpressionTransformationTest.project.", ".tmp.dir" );
        projectDir.delete();
        projectDir.mkdirs();

        File repoDir = File.createTempFile( "VersionExpressionTransformationTest.repo.", ".tmp.dir" );
        repoDir.delete();
        repoDir.mkdirs();

        try
        {
            File pomFile = new File( projectDir, "pom.xml" );
            FileWriter writer = null;
            try
            {
                writer = new FileWriter( pomFile );
                new MavenXpp3Writer().write( writer, model );
            }
            finally
            {
                IOUtil.close( writer );
            }

            model.getBuild().setDirectory( new File( projectDir, "target" ).getAbsolutePath() );

            MavenProject project = new MavenProject( model );
            project.setFile( pomFile );
            project.setBasedir( projectDir );
            project.setProjectBuilderConfiguration( pbConfig );

            ArtifactWithProject a =
                new ArtifactWithProject( project, "jar", null, new DefaultArtifactHandler( "jar" ), false );

            ArtifactRepository localRepository =
                new DefaultArtifactRepository( "local", repoDir.getAbsolutePath(), new DefaultRepositoryLayout() );

            transformation.transformVersions( pomFile, a, localRepository );

            FileReader reader = null;
            try
            {
                reader = new FileReader( new File( project.getBuild().getDirectory(), "pom-transformed.xml" ) );

                model = new MavenXpp3Reader().read( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }
        finally
        {
            FileUtils.forceDelete( projectDir );
            FileUtils.forceDelete( repoDir );
        }

        return model;
    }

    public void testInterpolate_ShouldNotInterpolateNonVersionFields()
        throws ModelInterpolationException
    {
        Model model = buildTestModel();

        Scm scm = new Scm();
        scm.setUrl( "http://${testVersion}" );

        model.setScm( scm );

        File projectDir = new File( "." ).getAbsoluteFile();

        transformation.interpolateVersions( model, projectDir, new DefaultProjectBuilderConfiguration() );

        // /project/scm/url
        assertFalse( model.getScm().getUrl().indexOf( VERSION ) > -1 );
    }

    public void testInterpolate_ShouldInterpolateAllVersionsUsingPOMProperties()
        throws ModelInterpolationException
    {
        Model model = buildTestModel();
        File projectDir = new File( "." ).getAbsoluteFile();

        transformation.interpolateVersions( model, projectDir, new DefaultProjectBuilderConfiguration() );

        assertTransformedVersions( model );
    }

    private void assertTransformedVersions( Model model )
    {
        // /project/version
        assertEquals( VERSION, model.getVersion() );

        // /project/dependenices/dependency/version
        Dependency dep = (Dependency) model.getDependencies().get( 0 );
        assertEquals( VERSION, dep.getVersion() );

        // /project/dependencyManagement/dependenices/dependency/version
        dep = (Dependency) model.getDependencyManagement().getDependencies().get( 0 );
        assertEquals( VERSION, dep.getVersion() );

        // /project/build/plugins/plugin/version
        Plugin plugin = (Plugin) model.getBuild().getPlugins().get( 0 );
        assertEquals( VERSION, plugin.getVersion() );

        // /project/build/plugins/plugin/dependencies/dependency/version
        dep = (Dependency) plugin.getDependencies().get( 0 );
        assertEquals( VERSION, dep.getVersion() );

        // /project/build/pluginManagement/plugins/plugin/version
        plugin = (Plugin) model.getBuild().getPluginManagement().getPlugins().get( 0 );
        assertEquals( VERSION, plugin.getVersion() );

        // /project/build/pluginManagement/plugins/plugin/dependencies/dependency/version
        dep = (Dependency) plugin.getDependencies().get( 0 );
        assertEquals( VERSION, dep.getVersion() );

        // /project/reporting/plugins/plugin/version
        ReportPlugin rplugin = (ReportPlugin) model.getReporting().getPlugins().get( 0 );
        assertEquals( VERSION, rplugin.getVersion() );
    }

    public void testInterpolate_ShouldInterpolateAllVersionsUsingCLIProperties()
        throws ModelInterpolationException
    {
        Model model = buildTestModel();
        File projectDir = new File( "." ).getAbsoluteFile();

        Properties props = model.getProperties();
        model.setProperties( new Properties() );

        transformation.interpolateVersions( model, projectDir,
                                            new DefaultProjectBuilderConfiguration().setExecutionProperties( props ) );

        assertTransformedVersions( model );
    }

    public Model buildTestModel()
    {
        Model model = new Model();

        model.setGroupId( "group.id" );
        model.setArtifactId( "artifact-id" );
        model.setPackaging( "jar" );

        String expression = "${testVersion}";

        Properties props = new Properties();
        props.setProperty( "testVersion", VERSION );

        model.setProperties( props );

        model.setVersion( expression );

        Dependency dep = new Dependency();
        dep.setGroupId( "group.id" );
        dep.setArtifactId( "artifact-id" );
        dep.setVersion( expression );

        model.addDependency( dep );

        dep = new Dependency();
        dep.setGroupId( "managed.group.id" );
        dep.setArtifactId( "managed-artifact-id" );
        dep.setVersion( expression );

        DependencyManagement dmgmt = new DependencyManagement();
        dmgmt.addDependency( dep );

        model.setDependencyManagement( dmgmt );

        Build build = new Build();
        model.setBuild( build );

        Plugin plugin = new Plugin();
        plugin.setGroupId( "plugin.group" );
        plugin.setArtifactId( "plugin-artifact" );
        plugin.setVersion( expression );

        dep = new Dependency();
        dep.setGroupId( "plugin.dep.group" );
        dep.setArtifactId( "plugin-dep-artifact" );
        dep.setVersion( expression );
        plugin.addDependency( dep );

        build.addPlugin( plugin );

        plugin = new Plugin();
        plugin.setGroupId( "plugin.other.group" );
        plugin.setArtifactId( "plugin-other-artifact" );
        plugin.setVersion( expression );

        dep = new Dependency();
        dep.setGroupId( "plugin.dep.other.group" );
        dep.setArtifactId( "plugin-dep-other-artifact" );
        dep.setVersion( expression );
        plugin.addDependency( dep );

        PluginManagement pmgmt = new PluginManagement();
        pmgmt.addPlugin( plugin );

        build.setPluginManagement( pmgmt );

        ReportPlugin rplugin = new ReportPlugin();
        rplugin.setGroupId( "report.group" );
        rplugin.setArtifactId( "report-artifact" );
        rplugin.setVersion( expression );

        Reporting reporting = new Reporting();
        reporting.addPlugin( rplugin );

        model.setReporting( reporting );

        return model;
    }

}
