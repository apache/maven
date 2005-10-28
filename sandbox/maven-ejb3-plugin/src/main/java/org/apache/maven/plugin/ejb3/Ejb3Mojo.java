package org.apache.maven.plugin.ejb3;

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
 * Builds J2EE EJB3 archive.
 *
 * @author <a href="piotr@bzdyl.net">Piotr Bzdyl</a>
 * @version $Id$
 * @goal ejb3
 * @phase package
 * @description build an ejb3
 *
 * @todo Add deployment descriptor file handling
 */
public class Ejb3Mojo
    extends AbstractMojo
{

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/package.html"};

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};
    

    /**
     * Directory containing the generated EJB3.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * Name of the generated EJB3.
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
     * Single directory for extra files to include in the ejb3.
     *
     * @parameter expression="${basedir}/src/main/ejb3"
     * @required
     */
    private File ejb3SourceDirectory;

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
     * Generates the EJB3.
     *
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws MojoExecutionException
    {
        // Copy source files
        try
        {
            if ( ejb3SourceDirectory != null
                    && ejb3SourceDirectory.exists() )
            {
                getLog().info( "Copy ejb3 resources to " + outputDirectory.getAbsolutePath() );
                FileUtils.copyDirectoryStructure( ejb3SourceDirectory, outputDirectory );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EJB3 resources", e );
        }

        File ejb3File = new File( basedir, finalName + ".ejb3" );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setOutputFile( ejb3File );

        try
        {
            if ( outputDirectory == null || !outputDirectory.exists() )
            {
                getLog().warn( "EJB3 will be empty - no content was marked for inclusion!" );
            }
            else
            {
                archiver.getArchiver().addDirectory( outputDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES );
            }

            archiver.createArchive( project, archive );

            project.getArtifact().setFile( ejb3File );
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error assembling EJB3", e );
        }
    }
}
