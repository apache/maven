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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class AbstractWarMojo
    extends AbstractMojo
{
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File classesDirectory;

    /**
     * The directory where the webapp is built.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File webappDirectory;

    /**
     * Single directory for extra files to include in the WAR.
     *
     * @parameter expression="${basedir}/src/main/webapp"
     * @required
     */
    private File warSourceDirectory;

    /**
     * The path to the web.xml file to use.
     *
     * @parameter expression="${maven.war.webxml}"
     */
    private String webXml;

    public static final String WEB_INF = "WEB-INF";

    /**
     * The comma separated list of tokens to include in the WAR.
     * Default is '**'.
     *
     * @parameter alias="includes"
     */
    private String warSourceIncludes = "**";

    /**
     * The comma separated list of tokens to exclude from the WAR.
     *
     * @parameter alias="excludes"
     */
    private String warSourceExcludes;

    private static final String[] EMPTY_STRING_ARRAY = {};

    public abstract void execute()
        throws MojoExecutionException;

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public File getClassesDirectory()
    {
        return classesDirectory;
    }

    public void setClassesDirectory( File classesDirectory )
    {
        this.classesDirectory = classesDirectory;
    }

    public File getWebappDirectory()
    {
        return webappDirectory;
    }

    public void setWebappDirectory( File webappDirectory )
    {
        this.webappDirectory = webappDirectory;
    }

    public File getWarSourceDirectory()
    {
        return warSourceDirectory;
    }

    public void setWarSourceDirectory( File warSourceDirectory )
    {
        this.warSourceDirectory = warSourceDirectory;
    }

    public String getWebXml()
    {
        return webXml;
    }

    public void setWebXml( String webXml )
    {
        this.webXml = webXml;
    }

    /**
     * Returns a string array of the excludes to be used
     * when assembling/copying the war.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getExcludes()
    {
        List excludeList = FileUtils.getDefaultExcludesAsList();
        if ( warSourceExcludes != null && !"".equals( warSourceExcludes ) )
        {
            excludeList.add( warSourceExcludes );
        }

        // if webXML is specified, omit the one in the source directory
        if ( getWebXml() != null && !"".equals( getWebXml() ) )
        {
            excludeList.add( "**/" + WEB_INF + "/web.xml" );
        }

        return (String[]) excludeList.toArray( EMPTY_STRING_ARRAY );
    }

    /**
     * Returns a string array of the includes to be used
     * when assembling/copying the war.
     *
     * @return an array of tokens to include
     */
    protected String[] getIncludes()
    {
        return new String[]{warSourceIncludes};
    }

    public void buildExplodedWebapp( File webappDirectory )
        throws MojoExecutionException
    {
        getLog().info( "Exploding webapp..." );

        webappDirectory.mkdirs();

        File webinfDir = new File( webappDirectory, WEB_INF );

        webinfDir.mkdirs();

        try
        {
            copyResources( getWarSourceDirectory(), webappDirectory, getWebXml() );

            buildWebapp( getProject(), webappDirectory );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not explode webapp...", e );
        }
    }

    /**
     * Copies webapp resources from the specified directory.
     * <p/>
     * Note that the <tt>webXml</tt> parameter could be null and may
     * specify a file which is not named <tt>web.xml<tt>. If the file
     * exists, it will be copied to the <tt>META-INF</tt> directory and
     * renamed accordingly.
     *
     * @param sourceDirectory the source directory
     * @param webappDirectory the target directory
     * @param webXml the path to a custom web.xml
     * @throws java.io.IOException if an error occured while copying resources
     */
    public void copyResources( File sourceDirectory, File webappDirectory, String webXml )
        throws IOException
    {
        if ( !sourceDirectory.equals( webappDirectory ) )
        {
            getLog().info( "Copy webapp resources to " + webappDirectory.getAbsolutePath() );
            if ( getWarSourceDirectory().exists() )
            {
                String[] fileNames = getWarFiles( sourceDirectory );
                for ( int i = 0; i < fileNames.length; i++ )
                {
                    FileUtils.copyFile( new File( sourceDirectory, fileNames[i] ),
                                        new File( webappDirectory, fileNames[i] ) );
                }
            }

            if ( webXml != null && !"".equals( webXml ) )
            {
                //rename to web.xml
                File webinfDir = new File( webappDirectory, WEB_INF );
                FileUtils.copyFile( new File( webXml ), new File( webinfDir, "/web.xml" ) );
            }
        }
    }

    /**
     * Builds the webapp for the specified project.
     * <p/>
     * Classes, libraries and tld files are copied to
     * the <tt>webappDirectory</tt> during this phase.
     *
     * @param project the maven project
     * @param webappDirectory
     * @throws java.io.IOException if an error occured while building the webapp
     */
    public void buildWebapp( MavenProject project, File webappDirectory )
        throws IOException
    {
        getLog().info( "Assembling webapp " + project.getArtifactId() + " in " + webappDirectory );

        File libDirectory = new File( webappDirectory, WEB_INF + "/lib" );

        File tldDirectory = new File( webappDirectory, WEB_INF + "/tld" );

        File webappClassesDirectory = new File( webappDirectory, WEB_INF + "/classes" );

        if ( getClassesDirectory().exists() )
        {
            FileUtils.copyDirectoryStructure( getClassesDirectory(), webappClassesDirectory );
        }

        Set artifacts = project.getArtifacts();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();

            // TODO: utilise appropriate methods from project builder
            // TODO: scope handler
            // Include runtime and compile time libraries
            if ( !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) &&
                !Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
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

    /**
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param sourceDir the directory to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getWarFiles( File sourceDir )
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
