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
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    private File earSourceDirectory;

    /**
     * The comma separated list of tokens to include in the EAR.
     * Default is '**'.
     *
     * @parameter alias="includes"
     */
    private String earSourceIncludes = "**";

    /**
     * The comma separated list of tokens to exclude from the EAR.
     *
     * @parameter alias="excludes"
     */
    private String earSourceExcludes;

    private static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * The location of the manifest file to be used within the ear file.
     *
     * @parameter expression="${basedir}/src/main/application/META-INF/MANIFEST.MF"
     */
    private File manifestFile;

    /**
     * The location of a custom application.xml file to be used
     * within the ear file.
     *
     * @parameter
     */
    private String applicationXml;

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
     * The directory to get the resources from.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File resourcesDir;

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


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // Initializes ear modules
        super.execute();

        getLog().debug( " ======= EarMojo settings =======" );
        getLog().debug( "earSourceDirectory[" + earSourceDirectory + "]" );
        getLog().debug( "manifestLocation[" + manifestFile + "]" );
        getLog().debug( "applicationXml[" + getApplicationXml() + "]" );
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
                File destinationFile = buildDestinationFile( getWorkDirectory(), module.getUri() );

                File sourceFile = module.getArtifact().getFile();

                if ( !sourceFile.isFile() )
                {
                    throw new MojoExecutionException( "Cannot copy a directory: " + sourceFile.getAbsolutePath() +
                        "; Did you package/install " + module.getArtifact() + "?" );
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
            File earSourceDir = earSourceDirectory;
            if ( earSourceDir.exists() )
            {
                getLog().info( "Copy ear sources to " + getWorkDirectory().getAbsolutePath() );
                String[] fileNames = getEarFiles( earSourceDir );
                for ( int i = 0; i < fileNames.length; i++ )
                {
                    FileUtils.copyFile( new File( earSourceDir, fileNames[i] ),
                                        new File( getWorkDirectory(), fileNames[i] ) );
                }
            }

            if ( applicationXml != null && !"".equals( applicationXml ) )
            {
                //rename to application.xml
                getLog().info( "Including custom application.xml[" + applicationXml + "]" );
                File metaInfDir = new File( getWorkDirectory(), META_INF );
                FileUtils.copyFile( new File( applicationXml ), new File( metaInfDir, "/application.xml" ) );
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
                getLog().info( "Copy ear resources to " + getWorkDirectory().getAbsolutePath() );
                String[] fileNames = getEarFiles( resourcesDir );
                for ( int i = 0; i < fileNames.length; i++ )
                {
                    FileUtils.copyFile( new File( resourcesDir, fileNames[i] ),
                                        new File( getWorkDirectory(), fileNames[i] ) );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EAR resources", e );
        }

        // Check if deployment descriptor is there
        File ddFile = new File( getWorkDirectory(), APPLICATION_XML_URI );
        if ( !ddFile.exists() )
        {
            throw new MojoExecutionException(
                "Deployment descriptor: " + ddFile.getAbsolutePath() + " does not exist." );
        }

        try
        {
            File earFile = new File( outputDirectory, finalName + ".ear" );
            MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver( jarArchiver );
            archiver.setOutputFile( earFile );

            // Include custom manifest if necessary
            includeCustomManifestFile();

            archiver.getArchiver().addDirectory( getWorkDirectory() );
            archiver.createArchive( getProject(), archive );

            project.getArtifact().setFile( earFile );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling EAR", e );
        }
    }

    public String getApplicationXml()
    {
        return applicationXml;
    }

    public void setApplicationXml( String applicationXml )
    {
        this.applicationXml = applicationXml;
    }

    /**
     * Returns a string array of the excludes to be used
     * when assembling/copying the ear.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getExcludes()
    {
        List excludeList = new ArrayList( FileUtils.getDefaultExcludesAsList() );
        if ( earSourceExcludes != null && !"".equals( earSourceExcludes ) )
        {
            excludeList.add( earSourceExcludes );
        }

        // if applicationXml is specified, omit the one in the source directory
        if ( getApplicationXml() != null && !"".equals( getApplicationXml() ) )
        {
            excludeList.add( "**/" + META_INF + "/application.xml" );
        }

        return (String[]) excludeList.toArray( EMPTY_STRING_ARRAY );
    }

    /**
     * Returns a string array of the includes to be used
     * when assembling/copying the ear.
     *
     * @return an array of tokens to include
     */
    protected String[] getIncludes()
    {
        return new String[]{earSourceIncludes};
    }

    private static File buildDestinationFile( File buildDir, String uri )
    {
        return new File( buildDir, uri );
    }

    private void includeCustomManifestFile()
    {
        File customManifestFile = manifestFile;

        if ( !customManifestFile.exists() )
        {
            getLog().info( "Could not find manifest file: " + manifestFile + " - Generating one" );
        }
        else
        {
            getLog().info( "Including custom manifest file[" + customManifestFile + "]" );
            archive.setManifestFile( customManifestFile );
        }
    }

    /**
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param sourceDir the directory to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getEarFiles( File sourceDir )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( sourceDir );
        scanner.setExcludes( getExcludes() );
        scanner.addDefaultExcludes();

        scanner.setIncludes( getIncludes() );

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
