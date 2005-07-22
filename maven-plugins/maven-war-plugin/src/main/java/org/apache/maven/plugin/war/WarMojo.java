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
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Build a war/webapp.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal war
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class WarMojo
    extends AbstractMojo
{
    public static final String WEB_INF = "WEB-INF";

    /**
     * @parameter
     */
    private String mode = "war";

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @todo Convert to File
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private String classesDirectory;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * @todo Convert to File
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private String webappDirectory;

    /**
     * @todo Convert to File
     * @parameter expression="${basedir}/src/main/webapp"
     * @required
     */
    private String warSourceDirectory;

    /**
     * @parameter alias="includes"
     */
    private String warSourceIncludes = "**";

    /**
     * @parameter alias="excludes"
     */
    private String warSourceExcludes;

    /**
     * @parameter expression="${maven.war.webxml}"
     */
    private String webXml;

    /**
     * @parameter expression="${project.build.finalName}"
     * @required
     * @deprecated "Please use the finalName element of build instead"
     */
    private String warName;

    /**
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    private static final String[] EMPTY_STRING_ARRAY = {};

    public void copyResources( File sourceDirectory, File webappDirectory, String includes, String excludes,
                               String webXml )
        throws IOException
    {
        if ( !sourceDirectory.equals( webappDirectory ) )
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

            // TODO: utilise appropriate methods from project builder
            // TODO: scope handler
            // Include runtime and compile time libraries
            if ( !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) &&
                !!Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
            {
                String type = artifact.getType();
                if ( "tld".equals( type ) )
                {
                    FileUtils.copyFileToDirectory( artifact.getFile(), tldDirectory );
                }
                else if ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type ) )
                {
                    FileUtils.copyFileToDirectory( artifact.getFile(), libDirectory );
                }
                else
                {
                    getLog().debug( "Skipping artifact of type " + type + " for WEB-INF/lib" );
                }
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
        throws MojoExecutionException
    {
        File warFile = new File( outputDirectory, warName + ".war" );

        try
        {
            performPackaging( warFile );
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error assembling WAR", e );
        }
    }

    private void performPackaging( File warFile )
        throws IOException, ArchiverException, ManifestException, DependencyResolutionRequiredException
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

                String[] excludes = (String[]) getDefaultExcludes().toArray( EMPTY_STRING_ARRAY );
                warArchiver.addDirectory( new File( webappDirectory ), null, excludes );

                warArchiver.setWebxml( new File( webappDirectory, "WEB-INF/web.xml" ) );

                // create archive
                archiver.createArchive( project, archive );
            }
        }
    }

    /**
     * @todo copied again. Next person to touch it puts it in the right place! :)
     */
    public List getDefaultExcludes()
    {
        List defaultExcludes = new ArrayList();
        defaultExcludes.add( "**/*~" );
        defaultExcludes.add( "**/#*#" );
        defaultExcludes.add( "**/.#*" );
        defaultExcludes.add( "**/%*%" );
        defaultExcludes.add( "**/._*" );

        // CVS
        defaultExcludes.add( "**/CVS" );
        defaultExcludes.add( "**/CVS/**" );
        defaultExcludes.add( "**/.cvsignore" );

        // SCCS
        defaultExcludes.add( "**/SCCS" );
        defaultExcludes.add( "**/SCCS/**" );

        // Visual SourceSafe
        defaultExcludes.add( "**/vssver.scc" );

        // Subversion
        defaultExcludes.add( "**/.svn" );
        defaultExcludes.add( "**/.svn/**" );

        // Mac
        defaultExcludes.add( "**/.DS_Store" );

        // Special one for WARs
        defaultExcludes.add( "**/" + WEB_INF + "/web.xml" );

        return defaultExcludes;
    }
}
