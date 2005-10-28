package org.apache.maven.plugin.par;

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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Builds J2EE5 Persistence Archive (PAR) files.
 *
 * @author <a href="piotr@bzdyl.net">Piotr Bzdyl</a>
 * @version $Id$
 * @goal par
 * @phase package
 * @description build a par
 */
public class ParMojo
    extends AbstractMojo
{

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/package.html"};

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};
    

    /**
     * Directory containing the generated PAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * Name of the generated PAR.
     *
     * @parameter alias="parName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * Directory containing the classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * Single directory for extra files to include in the PAR.
     *
     * @parameter expression="${basedir}/src/main/par"
     * @required
     */
    private File parSourceDirectory;

    /**
     * The location of the persistence.xml file to be used within the par file.
     *
     * @parameter expression="${basedir}/src/main/par/META-INF/persistence.xml"
     */
    private File persistenceXmlLocation;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The maven archiver to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Generates the PAR.
     *
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws MojoExecutionException
    {
        // Copy source files
        try
        {
            if ( parSourceDirectory != null
                    && parSourceDirectory.exists() )
            {
                getLog().info( "Copy par resources to " + outputDirectory.getAbsolutePath() );
                FileUtils.copyDirectoryStructure( parSourceDirectory, outputDirectory );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying PAR resources", e );
        }

        // Check if persistence deployment descriptor is there
        if ( persistenceXmlLocation == null
                || !persistenceXmlLocation.exists() )
        {
            getLog().warn(
                "Persistence deployment descriptor: " + persistenceXmlLocation + " does not exist." );
        }


        
        File parFile = new File( basedir, finalName + ".par" );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setOutputFile( parFile );

        try
        {
            if ( outputDirectory == null || !outputDirectory.exists() )
            {
                getLog().warn( "PAR will be empty - no content was marked for inclusion!" );
            }
            else
            {
                archiver.getArchiver().addDirectory( outputDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES );
            }

            archiver.createArchive( project, archive );

            project.getArtifact().setFile( parFile );
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error assembling PAR", e );
        }
    }
}
