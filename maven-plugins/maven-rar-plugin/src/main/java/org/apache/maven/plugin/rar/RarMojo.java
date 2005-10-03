package org.apache.maven.plugin.rar;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Builds J2EE Resource Adapter Archive (RAR) files.
 *
 * @author <a href="stephane.nicoll@gmail.com">Stephane Nicoll</a>
 * @version $Id$
 * @goal rar
 * @phase package
 * @requiresDependencyResolution test
 * @description builds a rar
 */
public class RarMojo
    extends AbstractMojo
{
    public static final String RA_XML_URI = "META-INF/ra.xml";


    /**
     * Single directory for extra files to include in the RAR.
     *
     * @parameter expression="${basedir}/src/main/rar"
     * @required
     */
    private String rarSourceDirectory;

    /**
     * The location of the ra.xml file to be used within the rar file.
     *
     * @parameter expression="${basedir}/src/main/rar/META-INF/ra.xml"
     */
    private String raXmlFile;

    /**
     * Specify if the generated jar file of this project should be
     * included in the rar file ; default is true.
     *
     * @parameter
     */
    private Boolean includeJar = Boolean.TRUE;

    /**
     * The location of the manifest file to be used within the rar file.
     *
     * @parameter expression="${basedir}/src/main/rar/META-INF/MANIFEST.MF"
     */
    private String manifestFile;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private String workDirectory;

    /**
     * The directory for the generated RAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the RAR file to generate.
     *
     * @parameter alias="rarName" expression="${project.build.finalName}"
     * @required
     * @readonly
     */
    private String finalName;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     * @description "the maven project to use"
     */
    private MavenProject project;

    /**
     * The Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * The maven archiver to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();


    private File buildDir;


    public void execute()
        throws MojoExecutionException
    {
        getLog().debug( " ======= RarMojo settings =======" );
        getLog().debug( "rarSourceDirectory[" + rarSourceDirectory + "]" );
        getLog().debug( "manifestFile[" + manifestFile + "]" );
        getLog().debug( "raXmlFile[" + raXmlFile + "]" );
        getLog().debug( "workDirectory[" + workDirectory + "]" );
        getLog().debug( "outputDirectory[" + outputDirectory + "]" );
        getLog().debug( "finalName[" + finalName + "]" );

        // Check if jar file is there and if requested, copy it
        try
        {
            if (includeJar.booleanValue()) {
                File generatedJarFile = new File( outputDirectory, finalName + ".jar" );
                if (generatedJarFile.exists()) {
                    getLog().info( "Including generated jar file["+generatedJarFile.getName()+"]");
                    FileUtils.copyFileToDirectory( generatedJarFile, getBuildDir());
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying generated Jar file", e );
        }

        // Copy dependencies
        try
        {
            Set artifacts = project.getArtifacts();
            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( !Artifact.SCOPE_TEST.equals( artifact.getScope() ) &&
                    !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) )
                {
                    getLog().info("Copying artifact[" + artifact.getGroupId() + ", " + artifact.getId() + ", " +
                        artifact.getScope() + "]");
                    FileUtils.copyFileToDirectory( artifact.getFile(), getBuildDir() );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying RAR dependencies", e );
        }

        // Copy source files
        try
        {
            File rarSourceDir = new File( rarSourceDirectory );
            if ( rarSourceDir.exists() )
            {
                getLog().info( "Copy rar resources to " + getBuildDir().getAbsolutePath() );
                FileUtils.copyDirectoryStructure( rarSourceDir, getBuildDir() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying RAR resources", e );
        }

        // Include custom manifest if necessary
        try
        {
            includeCustomRaXmlFile();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying ra.xml file", e );
        }

        // Check if connector deployment descriptor is there
        File ddFile = new File( getBuildDir(), RA_XML_URI );
        if ( !ddFile.exists() )
        {
            getLog().warn(
                "Connector deployment descriptor: " + ddFile.getAbsolutePath() + " does not exist." );
        }

        try
        {
            File rarFile = new File( outputDirectory, finalName + ".rar" );
            MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver( jarArchiver );
            archiver.setOutputFile( rarFile );

            // Include custom manifest if necessary
            includeCustomManifestFile();

            archiver.getArchiver().addDirectory( getBuildDir() );
            archiver.createArchive( project, archive );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling RAR", e );
        }
    }

    protected File getBuildDir()
    {
        if ( buildDir == null )
        {
            buildDir = new File( workDirectory );
        }
        return buildDir;
    }

    private void includeCustomManifestFile()
    {
        File customManifestFile = new File( manifestFile );
        if ( !customManifestFile.exists() )
        {
            getLog().info( "Could not find manifest file: " + manifestFile +" - Generating one");
        }
        else
        {
            getLog().info( "Including custom manifest file[" + customManifestFile + "]" );
            archive.setManifestFile( customManifestFile );
        }
    }

    private void includeCustomRaXmlFile()
        throws IOException
    {
        if (raXmlFile == null || raXmlFile.trim().length() == 0) {

        }
        File raXml = new File(raXmlFile );
        if (raXml.exists()) {
            getLog().info( "Using ra.xml "+ raXmlFile);
            File metaInfDir = new File(getBuildDir(), "META-INF");
            FileUtils.copyFileToDirectory( raXml, metaInfDir);
        }
    }
}
