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

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.Manifest;

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
 * @parameter name="compress"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.jar.compress"
 * default="true"
 * description=""
 * @parameter name="index"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.jar.index"
 * default="false"
 * description=""
 * @parameter name="packageName"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.jar.package"
 * description=""
 * @parameter name="manifest"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.jar.manifest"
 * description=""
 * @parameter name="mainClass"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.jar.mainClass"
 * description=""
 * @parameter name="addClasspath"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.jar.addClasspath"
 * default="false"
 * description=""
 * @parameter name="addExtensions"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.jar.addExtensions"
 * default="false"
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

    private String manifest;

    private String mainClass;

    private String packageName;

    /**
     * @todo boolean instead
     */
    private String addClasspath;

    /**
     * @todo boolean instead
     */
    private String addExtensions;

    /**
     * @todo boolean instead
     */
    private String index;

    /**
     * @todo boolean instead
     */
    private String compress;

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

            // create archive
            Manifest configuredManifest = archiver.getManifest( project, mainClass, packageName,
                                                                convertBoolean( addClasspath ),
                                                                convertBoolean( addExtensions ) );
            archiver.createArchive( project, manifest, convertBoolean( compress ), convertBoolean( index ),
                                    configuredManifest );
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new PluginExecutionException( "Error assembling JAR", e );
        }
    }

    private static boolean convertBoolean( String s )
    {
        return Boolean.valueOf( s ).booleanValue();
    }
}
