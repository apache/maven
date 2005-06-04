package org.apache.maven.plugin.ejb;

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

import java.io.File;

/**
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal ejb
 * @phase package
 * @description build an ejb
 */
public class EjbMojo
    extends AbstractMojo
{
    // TODO: will null work instead?
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/*Bean.class", "**/*CMP.class",
                                                                  "**/*Session.class", "**/package.html"};

    /**
     * @todo File instead
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private String basedir;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String jarName;

    /**
     * @todo boolean instead
     * 
     * @parameter
     */
    private String generateClient = Boolean.FALSE.toString();

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "Building ejb " + jarName );

        File jarFile = new File( basedir, jarName + ".jar" );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setOutputFile( jarFile );

        String ejbJarXmlFile = "META-INF/ejb-jar.xml";

        try
        {
            archiver.getArchiver().addDirectory( new File( outputDirectory ), DEFAULT_INCLUDES,
                                                 new String[]{ejbJarXmlFile, "**/package.html"} );

            archiver.getArchiver().addFile( new File( outputDirectory, ejbJarXmlFile ), ejbJarXmlFile );

            // create archive
            archiver.createArchive( project, archive );

            if ( new Boolean( generateClient ).booleanValue() )
            {
                getLog().info( "Building ejb client " + jarName + "-client" );

                File clientJarFile = new File( basedir, jarName + "-client.jar" );

                MavenArchiver clientArchiver = new MavenArchiver();

                clientArchiver.setOutputFile( clientJarFile );

                clientArchiver.getArchiver().addDirectory( new File( outputDirectory ), DEFAULT_INCLUDES,
                                                           DEFAULT_EXCLUDES );

                // create archive
                clientArchiver.createArchive( project, archive );
            }
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error assembling EJB", e );
        }
    }

}