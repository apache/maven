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

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.project.MavenProject;
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
 * @parameter name="manifest"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.jar.manifest"
 * description=""
 * @parameter name="addExtensions"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.jar.addExtensions"
 * default="false"
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
    public static final String WEB_INF = "WEB-INF";

    private PluginExecutionRequest request;

    private String mode;

    private MavenProject project;

    private File classesDirectory;

    private String outputDirectory;

    private File webappDirectory;

    private File warSourceDirectory;

    private String warSourceIncludes;

    private String warSourceExcludes;

    private String webXml;

    private File warFile;

    public void copyResources( File sourceDirectory, File webappDirectory, String includes, String excludes,
                               String webXml )
        throws IOException
    {
        if ( sourceDirectory != webappDirectory )
        {
            request.getLog().info( "Copy webapp resources to " + webappDirectory.getAbsolutePath() );

            if ( warSourceDirectory.exists() )
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
     * @todo properties 'war.target.path'
     */
    public void buildWebapp( MavenProject project )
        throws IOException
    {
        request.getLog().info(
            "Assembling webapp " + project.getArtifactId() + " in " + webappDirectory.getAbsolutePath() );

        File libDirectory = new File( webappDirectory, WEB_INF + "/lib" );

        File tldDirectory = new File( webappDirectory, WEB_INF + "/tld" );

        File webappClassesDirectory = new File( webappDirectory, WEB_INF + "/classes" );

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
        webappDirectory.mkdirs();

        File webinfDir = new File( webappDirectory, WEB_INF );

        webinfDir.mkdirs();

        copyResources( warSourceDirectory, webappDirectory, warSourceIncludes, warSourceExcludes, webXml );

        buildWebapp( project );
    }

    public void generateInPlaceWebapp()
        throws IOException
    {
        webappDirectory = warSourceDirectory;

        generateExplodedWebapp();
    }

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        parseRequest( request );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

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
                request.getLog().info( "Generating war " + warFile.getAbsolutePath() );

                MavenArchiver archiver = new MavenArchiver();

                WarArchiver warArchiver = new WarArchiver();

                archiver.setArchiver( warArchiver );

                archiver.setOutputFile( warFile );

                warArchiver.addDirectory( webappDirectory, new String[]{"**/**"}, new String[]{"**/WEB-INF/web.xml"} );

                warArchiver.setWebxml( new File( webappDirectory, "WEB-INF/web.xml" ) );

                // create archive
                archiver.createArchive( request );
            }
        }
    }

    public void parseRequest( PluginExecutionRequest request )
    {
        this.request = request;

        project = (MavenProject) request.getParameter( "project" );

        classesDirectory = new File( (String) request.getParameter( "classesDirectory" ) );

        outputDirectory = (String) request.getParameter( "outputDirectory" );

        webappDirectory = new File( (String) request.getParameter( "webappDirectory" ) );

        warSourceDirectory = new File( (String) request.getParameter( "warSourceDirectory" ) );

        warSourceIncludes = (String) request.getParameter( "warSourceIncludes" );

        warSourceExcludes = (String) request.getParameter( "warSourceExcludes" );

        webXml = (String) request.getParameter( "webXml" );

        mode = (String) request.getParameter( "mode" );

        warFile = new File( outputDirectory, (String) request.getParameter( "warName" ) + ".war" );
    }
}
