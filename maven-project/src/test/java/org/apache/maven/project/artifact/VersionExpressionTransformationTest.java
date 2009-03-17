package org.apache.maven.project.artifact;

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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

public class VersionExpressionTransformationTest
    extends PlexusTestCase
{

    private static final String VERSION = "blah";

    private VersionExpressionTransformation transformation;

    private Set toDelete = new HashSet();

    public void setUp()
        throws Exception
    {
        super.setUp();

        transformation =
            (VersionExpressionTransformation) lookup( ArtifactTransformation.class.getName(), "version-expression" );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();

        if ( toDelete != null && !toDelete.isEmpty() )
        {
            for ( Iterator it = toDelete.iterator(); it.hasNext(); )
            {
                File f = (File) it.next();

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

    public void testTransformForInstall_AbortOnInvalidPOM()
        throws URISyntaxException, IOException, XmlPullParserException, ModelInterpolationException
    {
        String pomResource = "version-expressions/invalid-pom.xml";
        File pomFile = getPom( pomResource );
        
        File projectDir;
        if ( pomFile != null )
        {
            projectDir = pomFile.getParentFile();
        }
        else
        {
            projectDir = File.createTempFile( "VersionExpressionTransformationTest.project.", ".tmp.dir" );
            projectDir.delete();
            projectDir.mkdirs();

            toDelete.add( projectDir );

            File newPomFile = new File( projectDir, "pom.xml" );
            FileUtils.copyFile( pomFile, newPomFile );
            
            pomFile = newPomFile;
        }

        File repoDir = File.createTempFile( "VersionExpressionTransformationTest.repo.", ".tmp.dir" );
        repoDir.delete();
        repoDir.mkdirs();

        toDelete.add( repoDir );

        Artifact a =
            new DefaultArtifact( "groupId", "artifactId", VersionRange.createFromVersion( "1" ),
                                 null, "jar", null, new DefaultArtifactHandler( "jar" ) );

        ProjectArtifactMetadata pam = new ProjectArtifactMetadata( a, pomFile );
        a.addMetadata( pam );

        ArtifactRepository localRepository =
            new DefaultArtifactRepository( "local", repoDir.getAbsolutePath(), new DefaultRepositoryLayout() );

        transformation.transformVersions( pomFile, a, localRepository );
        
        assertEquals( pomFile, pam.getFile() );
    }

    public void testTransformForInstall_PreserveComments()
        throws URISyntaxException, IOException, XmlPullParserException, ModelInterpolationException
    {
        String pomResource = "version-expressions/pom-with-comments.xml";
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

        File tempDir = File.createTempFile( "VersionExpressionTransformationTest.", ".dir.tmp" );
        tempDir.delete();
        tempDir.mkdirs();

        toDelete.add( tempDir );

        File pomFile = new File( tempDir, "pom.xml" );
        FileUtils.copyFile( new File( new URI( resource.toExternalForm() ).normalize() ), pomFile );

        return pomFile;
    }

    public void testTransformForResolve_DoNothing()
        throws IOException, XmlPullParserException, ArtifactResolutionException, ArtifactNotFoundException
    {
        Model model = buildTestModel();

        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

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

    public void testTransformForInstall_TransformBasedOnModelProperties()
        throws IOException, ArtifactInstallationException, XmlPullParserException
    {
        Model model = buildTestModel();

        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

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
            StringWriter swriter = new StringWriter();
            IOUtil.copy( reader, swriter );

//            System.out.println( "Transformed POM:\n\n\n" + swriter.toString() );
//            System.out.flush();

            model = new MavenXpp3Reader().read( new StringReader( swriter.toString() ) );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTransformedVersions( model );
    }

    public void testTransformForDeploy_TransformBasedOnModelProperties()
        throws IOException, XmlPullParserException, ArtifactDeploymentException
    {
        Model model = buildTestModel();

        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

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

    // FIXME: We can't be this smart (yet) since the deployment step transforms from the
    // original POM once again and re-installs over the top of the install step.
    // public void testTransformForInstall_SkipIfProjectArtifactMetadataResolvedFlagIsSet()
    // throws IOException, ArtifactInstallationException, XmlPullParserException
    // {
    // Model model = buildTestModel();
    //
    // File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
    // pomDir.delete();
    // pomDir.mkdirs();
    // try
    // {
    // File pomFile = new File( pomDir, "pom.xml" );
    // pomFile.deleteOnExit();
    //
    // FileWriter writer = null;
    // try
    // {
    // writer = new FileWriter( pomFile );
    // new MavenXpp3Writer().write( writer, model );
    // }
    // finally
    // {
    // IOUtil.close( writer );
    // }
    //
    // Artifact a =
    // new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1" ), null, "jar", null,
    // new DefaultArtifactHandler( "jar" ), false );
    // ProjectArtifactMetadata pam = new ProjectArtifactMetadata( a, pomFile );
    // pam.setVersionExpressionsResolved( true );
    //
    // a.addMetadata( pam );
    //
    // transformation.transformForInstall( a, null );
    //
    // assertEquals( pomFile, pam.getFile() );
    //
    // assertFalse( new File( pomDir, "target/pom-transformed.xml" ).exists() );
    //
    // FileReader reader = null;
    // try
    // {
    // reader = new FileReader( pomFile );
    // model = new MavenXpp3Reader().read( reader );
    // }
    // finally
    // {
    // IOUtil.close( reader );
    // }
    //
    // assertEquals( "${testVersion}", model.getVersion() );
    // }
    // finally
    // {
    // FileUtils.forceDelete( pomDir );
    // }
    // }

    // FIXME: We can't be this smart (yet) since the deployment step transforms from the
    // original POM once again and re-installs over the top of the install step.
    // public void testTransformForDeploy_SkipIfProjectArtifactMetadataResolvedFlagIsSet()
    // throws IOException, XmlPullParserException, ArtifactDeploymentException
    // {
    // Model model = buildTestModel();
    //
    // File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
    // pomDir.delete();
    // pomDir.mkdirs();
    // try
    // {
    // File pomFile = new File( pomDir, "pom.xml" );
    // pomFile.deleteOnExit();
    //
    // FileWriter writer = null;
    // try
    // {
    // writer = new FileWriter( pomFile );
    // new MavenXpp3Writer().write( writer, model );
    // }
    // finally
    // {
    // IOUtil.close( writer );
    // }
    //
    // Artifact a =
    // new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1" ), null, "jar", null,
    // new DefaultArtifactHandler( "jar" ), false );
    // ProjectArtifactMetadata pam = new ProjectArtifactMetadata( a, pomFile );
    // pam.setVersionExpressionsResolved( true );
    //
    // a.addMetadata( pam );
    //
    // transformation.transformForDeployment( a, null, null );
    //
    // assertEquals( pomFile, pam.getFile() );
    //
    // assertFalse( new File( pomDir, "target/pom-transformed.xml" ).exists() );
    //
    // FileReader reader = null;
    // try
    // {
    // reader = new FileReader( pomFile );
    // model = new MavenXpp3Reader().read( reader );
    // }
    // finally
    // {
    // IOUtil.close( reader );
    // }
    //
    // assertEquals( "${testVersion}", model.getVersion() );
    // }
    // finally
    // {
    // FileUtils.forceDelete( pomDir );
    // }
    // }

    public void testTransformVersion_ShouldInterpolate_VanillaArtifact_ModelProperties()
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        Model model = buildTestModel();

        File newPom = runTransformVersion_VanillaArtifact( model, null );

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
        Model model = buildTestModel();

        File newPom = runTransformVersion_ArtifactWithProject( model, new DefaultProjectBuilderConfiguration(), null );

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
        Model model = buildTestModel();

        Properties props = model.getProperties();
        model.setProperties( new Properties() );

        File newPom =
            runTransformVersion_ArtifactWithProject(
                                                     model,
                                                     new DefaultProjectBuilderConfiguration().setExecutionProperties( props ),
                                                     null );

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
        File projectDir;
        if ( pomFile != null )
        {
            projectDir = pomFile.getParentFile();
        }
        else
        {
            projectDir = File.createTempFile( "VersionExpressionTransformationTest.project.", ".tmp.dir" );
            projectDir.delete();
            projectDir.mkdirs();

            toDelete.add( projectDir );

            pomFile = new File( projectDir, "pom.xml" );

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

        File repoDir = File.createTempFile( "VersionExpressionTransformationTest.repo.", ".tmp.dir" );
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

        Artifact a =
            new DefaultArtifact( model.getGroupId(), model.getArtifactId(), VersionRange.createFromVersion( "1" ),
                                 null, "jar", null, new DefaultArtifactHandler( "jar" ) );

        a.addMetadata( new ProjectArtifactMetadata( a, pomFile ) );

        ArtifactRepository localRepository =
            new DefaultArtifactRepository( "local", repoDir.getAbsolutePath(), new DefaultRepositoryLayout() );

        transformation.transformVersions( pomFile, a, localRepository );

        return new File( projectDir, "target/pom-transformed.xml" );
    }

    private File runTransformVersion_ArtifactWithProject( Model model, ProjectBuilderConfiguration pbConfig,
                                                          File pomFile )
        throws IOException, XmlPullParserException, ModelInterpolationException
    {
        File projectDir;
        if ( pomFile != null )
        {
            projectDir = pomFile.getParentFile();
        }
        else
        {
            projectDir = File.createTempFile( "VersionExpressionTransformationTest.project.", ".tmp.dir" );
            projectDir.delete();
            projectDir.mkdirs();

            toDelete.add( projectDir );

            pomFile = new File( projectDir, "pom.xml" );

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

        File repoDir = File.createTempFile( "VersionExpressionTransformationTest.repo.", ".tmp.dir" );
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
        project.setFile( pomFile );
        project.setBasedir( projectDir );
        project.setProjectBuilderConfiguration( pbConfig );

        ArtifactWithProject a =
            new ArtifactWithProject( project, "jar", null, new DefaultArtifactHandler( "jar" ), false );

        a.addMetadata( new ProjectArtifactMetadata( a, pomFile ) );

        ArtifactRepository localRepository =
            new DefaultArtifactRepository( "local", repoDir.getAbsolutePath(), new DefaultRepositoryLayout() );

        transformation.transformVersions( pomFile, a, localRepository );

        return new File( project.getBuild().getDirectory(), "pom-transformed.xml" );
    }

    public void testInterpolate_ShouldNotInterpolateNonVersionFields()
        throws ModelInterpolationException, IOException, XmlPullParserException
    {
        Model model = buildTestModel();

        Scm scm = new Scm();
        scm.setUrl( "http://${testVersion}" );

        model.setScm( scm );

        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );
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

        File output = new File( pomDir, "output.xml" );

        transformation.interpolateVersions( pomFile, output, model, pomDir, new DefaultProjectBuilderConfiguration() );

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

        // /project/scm/url
        assertFalse( model.getScm().getUrl().indexOf( VERSION ) > -1 );
    }

    public void testInterpolate_ShouldInterpolateAllVersionsUsingPOMProperties()
        throws ModelInterpolationException, IOException, XmlPullParserException
    {
        Model model = buildTestModel();
        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );
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

        File output = new File( pomDir, "output.xml" );

        transformation.interpolateVersions( pomFile, output, model, pomDir, new DefaultProjectBuilderConfiguration() );

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
    }

    public void testInterpolate_ShouldInterpolateAllVersionsUsingCLIProperties()
        throws ModelInterpolationException, IOException, XmlPullParserException
    {
        Model model = buildTestModel();
        File pomDir = File.createTempFile( "VersionExpressionTransformationTest.", ".tmp.dir" );
        pomDir.delete();
        pomDir.mkdirs();

        toDelete.add( pomDir );

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

        File output = new File( pomDir, "output.xml" );

        transformation.interpolateVersions( pomFile, output, model, pomDir,
                                            new DefaultProjectBuilderConfiguration().setExecutionProperties( props ) );

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
