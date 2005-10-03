package org.apache.maven.plugin.jar;

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
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;

/**
 * Base class for creating a jar from project classes.
 * 
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public abstract class AbstractJarMojo
    extends AbstractMojo
{

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/package.html"};

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    /**
	 * Directory containing the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     *
     * @todo Change type to File
     */
    private String basedir;

    /**
	 * Name of the generated JAR.
	 *
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
	 * The Jar archiver.
	 *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;

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
     * Return the specific output directory to serve as the root for the archive.
     */
    protected abstract File getOutputDirectory();
    
    protected final MavenProject getProject()
    {
        return project;
    }
    
    /**
     * Overload this to produce a test-jar, for example.
     */
    protected String getClassifier()
    {
        return "";
    }

    /**
	 * Generates the JAR.
     *
     * @todo Add license files in META-INF directory.
     */
    public File createArchive()
        throws MojoExecutionException
    {
        String classifier = getClassifier();
        
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }
        
        File jarFile = new File( basedir, finalName + classifier + ".jar" );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        try
        {
            File contentDirectory = getOutputDirectory();
            if ( !contentDirectory.exists() )
            {
                getLog().warn( "JAR will be empty - no content was marked for inclusion!" );
            }
            else
            {
                archiver.getArchiver().addDirectory( contentDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES );
            }

            archiver.createArchive( project, archive );
            
            return jarFile;
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error assembling JAR", e );
        }
    }
}
