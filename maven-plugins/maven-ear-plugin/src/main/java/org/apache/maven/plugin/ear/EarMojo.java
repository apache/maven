package org.apache.maven.plugin.ear;

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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Builds J2EE Enteprise Archive (EAR) files.
 *
 * @author <a href="stephane.nicoll@gmail.com">Stephane Nicoll</a>
 * @version $Id$
 * @goal ear
 * @phase package
 * @requiresDependencyResolution test
 * @description builds an ear
 */
public class EarMojo
    extends AbstractEarMojo
{
    /**
     * Single directory for extra files to include in the EAR.
     *
     * @parameter expression="${basedir}/src/application"
     * @required
     */
    private String earSourceDirectory;

    /**
     * The location of the manifest file to be used within the ear file.
     *
     * @parameter expression="${basedir}/src/application/META-INF/MANIFEST.MF"
     * @TODO handle this field
     */
    private String manifestLocation;

    /**
     * The location of the application.xml file to be used within the ear file.
     *
     * @parameter expression="${basedir}/src/application/META-INF/application.xml"
     */
    private String applicationXmlLocation;

    /**
     * The directory for the generated EAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the EAR file to generate.
     *
     * @parameter alias="earName" expression="${project.build.finalName}"
     * @required
     * @readonly
     */
    private String finalName;

    /**
     * The list of excluded dependencies with format groupId:artifactId[:type].
     *
     * @parameter
     * @TODO handle this field
     */
    private List excludedDependencies = new ArrayList();

    /**
     * The maven archiver to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();


    public void execute()
        throws MojoExecutionException
    {
        // Initializes ear modules
        super.execute();

        getLog().debug( " ======= EarMojo settings =======" );
        getLog().debug( "earSourceDirectory[" + earSourceDirectory + "]" );
        getLog().debug( "manifestLocation[" + manifestLocation + "]" );
        getLog().debug( "applicationXmlLocation[" + applicationXmlLocation + "]" );
        getLog().debug( "workDirectory[" + getWorkDirectory() + "]" );
        getLog().debug( "outputDirectory[" + outputDirectory + "]" );
        getLog().debug( "finalName[" + finalName + "]" );
        getLog().debug( "excludedDependencies[" + excludedDependencies + "]" );

        // Copy modules
        try
        {
            for ( Iterator iter = getModules().iterator(); iter.hasNext(); )
            {
                EarModule module = (EarModule) iter.next();
                getLog().info( "Copying artifact[" + module + "] to[" + module.getUri() + "]" );
                File destinationFile = buildDestinationFile( getBuildDir(), module.getUri() );
                FileUtils.copyFile( module.getArtifact().getFile(), destinationFile );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EAR modules", e );
        }

        // Copy source files
        try
        {
            File earSourceDir = new File( earSourceDirectory );
            if ( earSourceDir.exists() )
            {
                getLog().info( "Copy ear resources to " + getBuildDir().getAbsolutePath() );
                FileUtils.copyDirectoryStructure( earSourceDir, getBuildDir() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EAR resources", e );
        }

        // Check if deployment descriptor is there
        File ddFile = new File( getBuildDir(), APPLICATION_XML_URI );
        if ( !ddFile.exists() )
        {
            throw new MojoExecutionException(
                "Deployment descriptor: " + ddFile.getAbsolutePath() + " does not exist." );
        }

        try
        {
            File earFile = new File( outputDirectory, finalName + ".ear" );
            MavenArchiver archiver = new MavenArchiver();
            archiver.setOutputFile( earFile );

            archiver.getArchiver().addDirectory( getBuildDir() );
            archiver.createArchive( getProject(), archive );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling EAR", e );
        }
    }

    private static File buildDestinationFile( File buildDir, String uri )
    {
        return new File( buildDir, uri );
    }
}