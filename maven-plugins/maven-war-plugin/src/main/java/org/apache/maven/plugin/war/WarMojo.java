package org.apache.maven.plugin.war;

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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal war
 * @phase package
 * @description build a war/webapp
 * @parameter name="warName"
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
 * @parameter name="warSourceDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="#maven.war.src"
 * default="#basedir/src/main/webapp"
 * description=""
 * @parameter name="warSourceIncludes"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.war.src.includes"
 * default="**"
 * description=""
 * @parameter name="warSourceExcludes"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.war.src.excludes"
 * description=""
 * @parameter name="webXml"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.war.webxml"
 * description=""
 * @parameter name="webappDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="#maven.war.webapp.dir"
 * default="#project.build.directory/#project.build.finalName"
 * description=""
 * @parameter name="mode"
 * type="String"
 * required="true"
 * validator=""
 * expression="#maven.war.mode"
 * default="war"
 * description=""
 * @parameter name="classesDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.outputDirectory"
 * description=""
 * @parameter name="outputDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="#maven.war.build.dir"
 * default="#project.build.directory"
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
public class WarMojo
    extends AbstractPlugin
{
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/WEB-INF/web.xml"};

    public static final String WEB_INF = "WEB-INF";

    private String mode;

    private MavenProject project;

    private String basedir;

    /**
     * @todo File
     */
    private String classesDirectory;

    private String outputDirectory;

    /**
     * @todo File
     */
    private String webappDirectory;

    /**
     * @todo File
     */
    private String warSourceDirectory;

    private String warSourceIncludes;

    private String warSourceExcludes;

    private String webXml;

    private String warName;

    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    public void copyResources( File sourceDirectory, File webappDirectory, String includes, String excludes,
                               String webXml )
        throws IOException
    {
        if ( sourceDirectory != webappDirectory )
        {
            getLog().info( "Copy webapp resources to " + webappDirectory.getAbsolutePath() );

            if ( new File( warSourceDirectory ).exists() )
            {
                //TODO : Use includes and excludes
                FileUtils.copyDirectoryStructure( sourceDirectory, webappDirectory );
            }

            if ( webXml != null && !"".equals( webXml ) )
            {
                FileUtils.copyFileToDirectory( new File( webXml ), new File( webappDirectory, WEB_INF ) );
            }
        }
    }

    /**
     *
     */
    public void buildWebapp( MavenProject project )
        throws IOException
    {
        getLog().info( "Assembling webapp " + project.getArtifactId() + " in " + webappDirectory );

        File libDirectory = new File( webappDirectory, WEB_INF + "/lib" );

        File tldDirectory = new File( webappDirectory, WEB_INF + "/tld" );

        File webappClassesDirectory = new File( webappDirectory, WEB_INF + "/classes" );

        File classesDirectory = new File( this.classesDirectory );
        if ( classesDirectory.exists() )
        {
            FileUtils.copyDirectoryStructure( classesDirectory, webappClassesDirectory );
        }

        Set artifacts = project.getArtifacts();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();

            if ( "jar".equals( artifact.getType() ) && Artifact.SCOPE_RUNTIME.equals( artifact.getScope() ) )
            {
                FileUtils.copyFileToDirectory( artifact.getFile(), libDirectory );
            }
            if ( "tld".equals( artifact.getType() ) )
            {
                FileUtils.copyFileToDirectory( artifact.getFile(), tldDirectory );
            }
        }
    }

    public void generateExplodedWebapp()
        throws IOException
    {
        File webappDirectory = new File( this.webappDirectory );
        webappDirectory.mkdirs();

        File webinfDir = new File( webappDirectory, WEB_INF );

        webinfDir.mkdirs();

        copyResources( new File( warSourceDirectory ), webappDirectory, warSourceIncludes, warSourceExcludes, webXml );

        buildWebapp( project );
    }

    public void generateInPlaceWebapp()
        throws IOException
    {
        webappDirectory = warSourceDirectory;

        generateExplodedWebapp();
    }

    public void execute()
        throws PluginExecutionException
    {
        File warFile = new File( outputDirectory, warName + ".war" );

        try
        {
            performPackaging( warFile );
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new PluginExecutionException( "Error assembling WAR", e );
        }
    }

    private void performPackaging( File warFile )
        throws IOException, ArchiverException, ManifestException
    {
        if ( "inplace".equals( mode ) )
        {
            generateInPlaceWebapp();
        }
        else
        {
            generateExplodedWebapp();

            if ( !"exploded".equals( mode ) )
            {
                //generate war file
                getLog().info( "Generating war " + warFile.getAbsolutePath() );

                MavenArchiver archiver = new MavenArchiver();

                WarArchiver warArchiver = new WarArchiver();

                archiver.setArchiver( warArchiver );

                archiver.setOutputFile( warFile );

                warArchiver.addDirectory( new File( webappDirectory ), DEFAULT_INCLUDES, DEFAULT_EXCLUDES );

                warArchiver.setWebxml( new File( webappDirectory, "WEB-INF/web.xml" ) );

                // create archive
                archiver.createArchive( project, archive );
            }
        }
    }

}
