package org.apache.maven.project.interpolation;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public abstract class AbstractCoordinateInterpolationTest
    extends PlexusTestCase
{

    private static final String VERSION = "blah";

    private CoordinateInterpolator interpolator;
    
    private MavenProjectBuilder projectBuilder;

    private Set<File> toDelete = new HashSet<File>();

    protected abstract String getRoleHint();

    public void setUp()
        throws Exception
    {
        super.setUp();

        // getContainer().getLoggerManager().setThreshold( Logger.LEVEL_DEBUG );

        interpolator = (DefaultCoordinateInterpolator) lookup( CoordinateInterpolator.class.getName(), getRoleHint() );
        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.class.getName() );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();

        if ( toDelete != null && !toDelete.isEmpty() )
        {
            for ( File f : toDelete )
            {
                try
                {
                    FileUtils.forceDelete( f );
                }
                catch ( IOException e )
                {
                    System.out.println( "Failed to delete temp file: '" + f.getAbsolutePath() + "'." );
                    e.printStackTrace();
                }
            }
        }
    }

    private Model readModel( File pomFile )
        throws IOException, XmlPullParserException
    {
        FileReader reader = null;
        try
        {
            reader = new FileReader( pomFile );
            return new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    public void testTransform_MaintainEncoding()
        throws URISyntaxException, IOException, XmlPullParserException, ModelInterpolationException
    {
        String pomResource = "coord-expressions/alternative-encoding-pom.xml";
        File pomFile = getPom( pomResource );

        File projectDir;
        if ( pomFile != null )
        {
            projectDir = pomFile.getParentFile();
        }
        else
        {
            projectDir = File.createTempFile( "CoordinateInterpolationTest.project.", ".tmp.dir" );
            projectDir.delete();
            projectDir.mkdirs();

            toDelete.add( projectDir );

            File newPomFile = new File( projectDir, "pom.xml" );
            FileUtils.copyFile( pomFile, newPomFile );

            pomFile = newPomFile;
        }

        File repoDir = File.createTempFile( "CoordinateInterpolationTest.repo.", ".tmp.dir" );
        repoDir.delete();
        repoDir.mkdirs();

        toDelete.add( repoDir );

        ArtifactRepository localRepository =
            new DefaultArtifactRepository( "local", repoDir.getAbsolutePath(), new DefaultRepositoryLayout() );

        MavenProject project = new MavenProject( readModel( pomFile ) );
        project.setOriginalModel( project.getModel() );
        project.setFile( pomFile );

        ProjectBuilderConfiguration pbc = new DefaultProjectBuilderConfiguration();
        pbc.setLocalRepository( localRepository );

        project.setProjectBuilderConfiguration( pbc );

        interpolator.interpolateArtifactCoordinates( project );

        assertNotSame( pomFile, project.getFile() );
        String xml = FileUtils.fileRead( project.getFile() );

        assertTrue( xml.indexOf( "encoding=\"ISO-8859-1\"" ) > -1 );
    }

    public void testTransformForInstall_PreserveComments()
        throws URISyntaxException, IOException, XmlPullParserException, ModelInterpolationException
    {
        String pomResource = "coord-expressions/pom-with-comments.xml";
        File pomFile = getPom( pomResource );

        Model model;
        Reader reader = null;
        try
        {
            reader = new FileReader( pomFile );

            model = new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        File newPom = runTransformVersion_VanillaArtifact( model, pomFile );

        StringWriter writer = new StringWriter();
        reader = null;
        try
        {
            reader = new FileReader( newPom );
            IOUtil.copy( reader, writer );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTrue( "XML comment not found.", writer.toString().indexOf( "This is a comment." ) > -1 );

        reader = new StringReader( writer.toString() );
        try
        {
            model = new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertEquals( "1.0", model.getVersion() );

        assertNotNull( model.getProperties() );

        assertNotNull( model.getProperties().getProperty( "other.version" ) );
        assertEquals( "${testVersion}", model.getProperties().getProperty( "other.version" ) );

        assertNotNull( model.getScm() );

        assertNotNull( model.getScm().getConnection() );
        assertEquals( "${testVersion}", model.getScm().getConnection() );

        assertNotNull( model.getScm().getUrl() );
        assertEquals( "${testVersion}", model.getScm().getUrl() );
    }

    private File getPom( String pom )
        throws URISyntaxException, IOException
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        URL resource = cloader.getResource( pom );

        if ( resource == null )
        {
            fail( "POM classpath resource not found: '" + pom + "'." );
        }

        File tempDir = File.createTempFile( "CoordinateInterpolationTest.", ".dir.tmp" );
        tempDir.delete();
        tempDir.mkdirs();

        toDelete.add( tempDir );

        File pomFile = new File( tempDir, "pom.xml" );
        FileUtils.copyFile( new File( new URI( resource.toExternalForm() ).normalize() ), pomFile );

        return pomFile;
    }

    public void testTransformForInstall_TransformBasedOnModelProperties()
        throws IOException, ModelInterpolationException, XmlPullParserException
    {
        File pomDir = File.createTempFile( "CoordinateInterpolationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

        Model model = buildTestModel( pomDir );

        File pomFile = new File( pomDir, "pom.xml" );

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

        MavenProject project = new MavenProject( model );
        project.setOriginalModel( model );
        project.setFile( pomFile );

        ProjectBuilderConfiguration pbc = new DefaultProjectBuilderConfiguration();

        project.setProjectBuilderConfiguration( pbc );

        interpolator.interpolateArtifactCoordinates( project );

        assertNotSame( pomFile, project.getFile() );

        File transformedFile = new File( pomDir, CoordinateInterpolator.COORDINATE_INTERPOLATED_POMFILE );

        assertTrue( transformedFile.exists() );
        assertEquals( transformedFile, project.getFile() );

        FileReader reader = null;
        try
        {
            reader = new FileReader( project.getFile() );
            StringWriter swriter = new StringWriter();
            IOUtil.copy( reader, swriter );

            // System.out.println( "Transformed POM:\n\n\n" + swriter.toString() );
            // System.out.flush();

            model = new MavenXpp3Reader().read( new StringReader( swriter.toString() ) );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTransformedVersions( model );
    }

    public void testTransformForDeploy_TransformBasedOnModelProperties()
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        File pomDir = File.createTempFile( "CoordinateInterpolationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

        Model model = buildTestModel( pomDir );

        File pomFile = new File( pomDir, "pom.xml" );

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

        MavenProject project = new MavenProject( model );
        project.setOriginalModel( model );
        project.setFile( pomFile );

        ProjectBuilderConfiguration pbc = new DefaultProjectBuilderConfiguration();

        project.setProjectBuilderConfiguration( pbc );

        interpolator.interpolateArtifactCoordinates( project );

        assertNotSame( pomFile, project.getFile() );

        File transformedFile = new File( pomDir, CoordinateInterpolator.COORDINATE_INTERPOLATED_POMFILE );

        assertTrue( transformedFile.exists() );
        assertEquals( transformedFile, project.getFile() );

        FileReader reader = null;
        try
        {
            reader = new FileReader( project.getFile() );
            model = new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTransformedVersions( model );
    }

    public void testTransformVersion_ShouldInterpolate_VanillaArtifact_ModelProperties()
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        File pomDir = File.createTempFile( "CoordinateInterpolationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

        Model model = buildTestModel( pomDir );

        File newPom = runTransformVersion_VanillaArtifact( model, new File( pomDir, "pom.xml" ) );

        FileReader reader = null;
        try
        {
            reader = new FileReader( newPom );

            model = new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTransformedVersions( model );
    }

    public void testTransformVersion_ShouldInterpolate_ArtifactWithProject_ModelProperties()
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        File pomDir = File.createTempFile( "CoordinateInterpolationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

        Model model = buildTestModel( pomDir );

        File newPom =
            runTransformVersion_ArtifactWithProject( model, new DefaultProjectBuilderConfiguration(),
                                                     new File( pomDir, "pom.xml" ) );

        FileReader reader = null;
        try
        {
            reader = new FileReader( newPom );

            model = new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTransformedVersions( model );
    }

    public void testTransformVersion_ShouldInterpolate_ArtifactWithProject_CLIProperties()
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        File pomDir = File.createTempFile( "CoordinateInterpolationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

        Model model = buildTestModel( pomDir );

        Properties props = model.getProperties();
        model.setProperties( new Properties() );

        File newPom =
            runTransformVersion_ArtifactWithProject(
                                                     model,
                                                     new DefaultProjectBuilderConfiguration().setExecutionProperties( props ),
                                                     new File( pomDir, "pom.xml" ) );

        FileReader reader = null;
        try
        {
            reader = new FileReader( newPom );

            model = new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTransformedVersions( model );
    }

    private File runTransformVersion_VanillaArtifact( Model model, File pomFile )
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        File projectDir = pomFile.getParentFile();

        if ( !pomFile.exists() )
        {
            projectDir.mkdirs();
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
        }

        File repoDir = File.createTempFile( "CoordinateInterpolationTest.repo.", ".tmp.dir" );
        repoDir.delete();
        repoDir.mkdirs();

        toDelete.add( repoDir );

        File dir = new File( projectDir, "target" );
        dir.mkdirs();

        if ( model.getBuild() == null )
        {
            model.setBuild( new Build() );
        }

        model.getBuild().setDirectory( dir.getAbsolutePath() );

        ArtifactRepository localRepository =
            new DefaultArtifactRepository( "local", repoDir.getAbsolutePath(), new DefaultRepositoryLayout() );

        MavenProject project = new MavenProject( model );
        project.setOriginalModel( model );
        project.setFile( pomFile );

        ProjectBuilderConfiguration pbc = new DefaultProjectBuilderConfiguration();
        pbc.setLocalRepository( localRepository );

        project.setProjectBuilderConfiguration( pbc );

        interpolator.interpolateArtifactCoordinates( project );

        assertNotSame( pomFile, project.getFile() );

        return new File( projectDir, CoordinateInterpolator.COORDINATE_INTERPOLATED_POMFILE );
    }

    private File runTransformVersion_ArtifactWithProject( Model model, ProjectBuilderConfiguration pbConfig,
                                                          File pomFile )
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        File projectDir = pomFile.getParentFile();

        if ( !pomFile.exists() )
        {
            projectDir.mkdirs();
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
        }

        File repoDir = File.createTempFile( "CoordinateInterpolationTest.repo.", ".tmp.dir" );
        repoDir.delete();
        repoDir.mkdirs();

        toDelete.add( repoDir );

        File dir = new File( projectDir, "target" );
        dir.mkdirs();

        if ( model.getBuild() == null )
        {
            model.setBuild( new Build() );
        }

        model.getBuild().setDirectory( dir.getAbsolutePath() );

        MavenProject project = new MavenProject( model );
        project.setOriginalModel( model );
        project.setFile( pomFile );
        project.setBasedir( projectDir );
        project.setProjectBuilderConfiguration( pbConfig );

        ArtifactRepository localRepository =
            new DefaultArtifactRepository( "local", repoDir.getAbsolutePath(), new DefaultRepositoryLayout() );

        pbConfig.setLocalRepository( localRepository );

        interpolator.interpolateArtifactCoordinates( project );

        assertNotSame( pomFile, project.getFile() );

        return new File( projectDir, CoordinateInterpolator.COORDINATE_INTERPOLATED_POMFILE );
    }

    public void testInterpolate_ShouldNotInterpolateNonVersionFields()
        throws ModelInterpolationException, IOException, XmlPullParserException
    {
        File pomDir = File.createTempFile( "CoordinateInterpolationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

        Model model = buildTestModel( pomDir );

        Scm scm = new Scm();
        scm.setUrl( "http://${testVersion}" );

        model.setScm( scm );

        File pomFile = new File( pomDir, "pom.xml" );

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

        File output = new File( pomDir, CoordinateInterpolator.COORDINATE_INTERPOLATED_POMFILE );

        MavenProject project = new MavenProject( model );
        project.setOriginalModel( model );
        project.setFile( pomFile );
        project.setProjectBuilderConfiguration( new DefaultProjectBuilderConfiguration() );

        interpolator.interpolateArtifactCoordinates( project );

        assertTrue( output.exists() );

        model = readModel( output );

        // /project/scm/url
        assertFalse( model.getScm().getUrl().indexOf( VERSION ) > -1 );
    }

    public void testInterpolate_ShouldInterpolateAllVersionsUsingPOMProperties()
        throws ModelInterpolationException, IOException, XmlPullParserException
    {
        File pomDir = File.createTempFile( "CoordinateInterpolationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

        Model model = buildTestModel( pomDir );

        File pomFile = new File( pomDir, "pom.xml" );

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

        File output = new File( pomDir, CoordinateInterpolator.COORDINATE_INTERPOLATED_POMFILE );

        MavenProject project = new MavenProject( model );
        project.setOriginalModel( model );
        project.setFile( pomFile );
        project.setProjectBuilderConfiguration( new DefaultProjectBuilderConfiguration() );

        interpolator.interpolateArtifactCoordinates( project );

        assertTrue( output.exists() );

        FileReader reader = null;
        try
        {
            reader = new FileReader( output );
            model = new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

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

        // ---

        Profile profile = (Profile) model.getProfiles().get( 0 );

        // /project/profiles/profile/dependencies/dependency/version
        dep = (Dependency) profile.getDependencies().get( 0 );
        assertEquals( VERSION, dep.getVersion() );

        // /project/profiles/profile/dependencyManagement/dependencies/dependency/version
        dep = (Dependency) profile.getDependencyManagement().getDependencies().get( 0 );
        assertEquals( VERSION, dep.getVersion() );

        // /project/profiles/profile/build/plugins/plugin/version
        plugin = (Plugin) profile.getBuild().getPlugins().get( 0 );
        assertEquals( VERSION, plugin.getVersion() );

        // /project/profiles/profile/build/plugins/plugin/dependencies/dependency/version
        dep = (Dependency) profile.getDependencies().get( 0 );
        assertEquals( VERSION, dep.getVersion() );

        // /project/profiles/profile/build/pluginManagement/plugins/plugin/version
        plugin = (Plugin) profile.getBuild().getPluginManagement().getPlugins().get( 0 );
        assertEquals( VERSION, plugin.getVersion() );

        // /project/profiles/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/version
        dep = (Dependency) profile.getDependencies().get( 0 );
        assertEquals( VERSION, dep.getVersion() );

        // /project/profiles/profile/reporting/plugins/plugin/version
        rplugin = (ReportPlugin) profile.getReporting().getPlugins().get( 0 );
        assertEquals( VERSION, rplugin.getVersion() );
    }

    public void testInterpolate_ShouldInterpolateAllVersionsUsingCLIProperties()
        throws ModelInterpolationException, IOException, XmlPullParserException
    {
        File pomDir = File.createTempFile( "CoordinateInterpolationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

        Model model = buildTestModel( pomDir );

        File pomFile = new File( pomDir, "pom.xml" );

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

        Properties props = model.getProperties();
        model.setProperties( new Properties() );

        File output = new File( pomDir, CoordinateInterpolator.COORDINATE_INTERPOLATED_POMFILE );

        MavenProject project = new MavenProject( model );
        project.setOriginalModel( model );
        project.setFile( pomFile );
        project.setProjectBuilderConfiguration( new DefaultProjectBuilderConfiguration().setExecutionProperties( props ) );

        interpolator.interpolateArtifactCoordinates( project );

        assertTrue( output.exists() );

        FileReader reader = null;
        try
        {
            reader = new FileReader( output );
            model = new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTransformedVersions( model );
    }

    public Model buildTestModel( File pomDir )
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

        File dir = new File( pomDir, "target" );
        dir.mkdirs();

        build.setDirectory( dir.getAbsolutePath() );

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

        Profile profile = new Profile();
        profile.setId( "profile" );

        model.addProfile( profile );

        dep = new Dependency();
        dep.setGroupId( "profile.group.id" );
        dep.setArtifactId( "profile-artifact-id" );
        dep.setVersion( expression );

        profile.addDependency( dep );

        dep = new Dependency();
        dep.setGroupId( "profile.managed.group.id" );
        dep.setArtifactId( "profile-managed-artifact-id" );
        dep.setVersion( expression );

        dmgmt = new DependencyManagement();
        dmgmt.addDependency( dep );

        profile.setDependencyManagement( dmgmt );

        build = new Build();
        profile.setBuild( build );

        plugin = new Plugin();
        plugin.setGroupId( "profile.plugin.group" );
        plugin.setArtifactId( "profile-plugin-artifact" );
        plugin.setVersion( expression );

        dep = new Dependency();
        dep.setGroupId( "profile.plugin.dep.group" );
        dep.setArtifactId( "profile-plugin-dep-artifact" );
        dep.setVersion( expression );
        plugin.addDependency( dep );

        build.addPlugin( plugin );

        plugin = new Plugin();
        plugin.setGroupId( "profile.plugin.other.group" );
        plugin.setArtifactId( "profile-plugin-other-artifact" );
        plugin.setVersion( expression );

        dep = new Dependency();
        dep.setGroupId( "profile.plugin.dep.other.group" );
        dep.setArtifactId( "profile.plugin-dep-other-artifact" );
        dep.setVersion( expression );
        plugin.addDependency( dep );

        pmgmt = new PluginManagement();
        pmgmt.addPlugin( plugin );

        build.setPluginManagement( pmgmt );

        rplugin = new ReportPlugin();
        rplugin.setGroupId( "profile.report.group" );
        rplugin.setArtifactId( "profile-report-artifact" );
        rplugin.setVersion( expression );

        reporting = new Reporting();
        reporting.addPlugin( rplugin );

        profile.setReporting( reporting );

        return model;
    }

}
