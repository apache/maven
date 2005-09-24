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
import java.util.Iterator;

/**
 * Builds J2EE Enteprise Archive (EAR) files.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
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
     * @parameter expression="${basedir}/src/main/application"
     * @required
     */
    private String earSourceDirectory;

    /**
     * The location of the manifest file to be used within the ear file.
     *
     * @parameter expression="${basedir}/src/main/application/META-INF/MANIFEST.MF"
     */
    private String manifestFile;

    /**
     * The location of the application.xml file to be used within the ear file.
     *
     * @parameter expression="${basedir}/src/main/application/META-INF/application.xml"
     */
    private String applicationXmlFile;

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
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/ear"
     */
    private File resourcesDir;
    
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
        getLog().debug( "manifestLocation[" + manifestFile + "]" );
        getLog().debug( "applicationXmlLocation[" + applicationXmlFile + "]" );
        getLog().debug( "workDirectory[" + getWorkDirectory() + "]" );
        getLog().debug( "outputDirectory[" + outputDirectory + "]" );
        getLog().debug( "finalName[" + finalName + "]" );

        // Copy modules
        try
        {
            for ( Iterator iter = getModules().iterator(); iter.hasNext(); )
            {
                EarModule module = (EarModule) iter.next();
                getLog().info( "Copying artifact[" + module + "] to[" + module.getUri() + "]" );
                File destinationFile = buildDestinationFile( getBuildDir(), module.getUri() );

                File sourceFile = module.getArtifact().getFile();

                if ( !sourceFile.isFile() )
                {
                    throw new MojoExecutionException( "Cannot copy a directory: " + sourceFile.getAbsolutePath() +
                        "; Did you package/install " + module.getArtifact().getId() + "?" );
                }

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
                getLog().info( "Copy ear sources to " + getBuildDir().getAbsolutePath() );
                FileUtils.copyDirectoryStructure( earSourceDir, getBuildDir() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EAR sources", e );
        }

        // Copy resources files
        try
        {
            if ( resourcesDir.exists() )
            {
                getLog().info( "Copy ear resources to " + getBuildDir().getAbsolutePath() );
                FileUtils.copyDirectoryStructure( resourcesDir, getBuildDir() );
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

            // Include custom manifest if necessary
            includeCustomManifestFile();

            archiver.getArchiver().addDirectory( getBuildDir() );
            archiver.createArchive( getProject(), archive );

            project.getArtifact().setFile( earFile );
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

    private void includeCustomManifestFile()
    {
        File customManifestFile = new File( manifestFile );
        if ( !customManifestFile.exists() )
        {
            getLog().info( "Could not find manifest file: " + manifestFile );
        }
        else
        {
            getLog().info( "Including custom manifest file[" + customManifestFile + "]" );
            archive.setManifestFile( customManifestFile );
        }
    }
}
