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
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal jar
 * @phase package
 * @description build a jar
 * @parameter name="jarName"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.finalName"
 * description=""
 * @parameter name="archive"
 * type=""
 * required="false"
 * expression=""
 * validator=""
 * description=""
 * @parameter name="outputDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.outputDirectory"
 * description=""
 * @parameter name="basedir"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.directory"
 * description=""
 * @parameter name="project"
 * type="org.apache.maven.project.MavenProject"
 * required="true"
 * validator=""
 * expression="#project"
 * description="current MavenProject instance"
 */
public class JarMojo
    extends AbstractPlugin
{
    /**
     * @todo File
     */
    private String basedir;

    private String jarName;

    private String outputDirectory;

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/package.html"};

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    private MavenProject project;

    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws PluginExecutionException
    {
        File jarFile = new File( basedir, jarName + ".jar" );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setOutputFile( jarFile );

        try
        {
            File contentDirectory = new File( outputDirectory );
            if ( !contentDirectory.exists() )
            {
                getLog().warn( "JAR will be empty - no content was marked for inclusion!" );
            }
            else
            {
                archiver.getArchiver().addDirectory( contentDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES );
            }

            archiver.createArchive( project, archive );
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new PluginExecutionException( "Error assembling JAR", e );
        }
    }

}
