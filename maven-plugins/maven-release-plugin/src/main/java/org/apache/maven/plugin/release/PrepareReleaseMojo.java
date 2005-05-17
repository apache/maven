package org.apache.maven.plugin.release;

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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.scm.ScmBean;
import org.apache.maven.plugin.transformer.PomTransformer;
import org.apache.maven.plugin.transformer.VersionTransformer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Prepare for a release in SCM
 *
 * @goal prepare
 * @requiresDependencyResolution test
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DoxiaMojo.java 169372 2005-05-09 22:47:34Z evenisse $
 */
public class PrepareReleaseMojo
    extends AbstractReleaseMojo
{
    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private String basedir;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private static final String SNAPSHOT = "-SNAPSHOT";

    private String projectVersion;

    protected void executeTask()
        throws MojoExecutionException
    {
        //checkStatus();

        //checkDependencies();

        transformPom();

        //checkin();

        //tag();
    }

    private boolean isSnapshot( String version )
    {
        return version.endsWith( SNAPSHOT );
    }

    private void checkStatus()
        throws MojoExecutionException
    {
        getLog().info( "Verifying no modifications are present." );

        List changedFiles;
        try
        {
            ScmBean scm = getScm();
            scm.setWorkingDirectory( basedir );
            changedFiles = scm.getStatus();
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "An error is occurred in the status process.", e );
        }

        if ( !changedFiles.isEmpty() )
        {
            StringBuffer message = new StringBuffer();
            for ( Iterator i = changedFiles.iterator(); i.hasNext(); )
            {
                ScmFile file = (ScmFile) i.next();
                message.append( file.toString() );
                message.append( "\n" );
            }
            throw new MojoExecutionException( "Cannot prepare a release - You have some uncommitted files : \n" + message.toString() );
        }
    }

    private void checkDependencies()
        throws MojoExecutionException
    {
        MavenProject currentProject = project;
        while ( currentProject.hasParent() )
        {
            Artifact parentArtifact = currentProject.getParentArtifact();

            if ( isSnapshot( parentArtifact.getVersion() ) )
            {
                throw new MojoExecutionException( "Can't release project due to non released parent." );
            }

            currentProject = project.getParent();
        }

        List snapshotDependencies = new ArrayList();

        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( isSnapshot( artifact.getVersion() ) )
            {
                snapshotDependencies.add( artifact );
            }
        }
        for ( Iterator i = project.getPluginArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( isSnapshot( artifact.getVersion() ) )
            {
                snapshotDependencies.add( artifact );
            }
        }
        if ( !snapshotDependencies.isEmpty() )
        {
            Collections.sort( snapshotDependencies );
            StringBuffer message = new StringBuffer();
            for ( Iterator i = snapshotDependencies.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                message.append( "    " );
                message.append( artifact.getGroupId() );
                message.append( ":" );
                message.append( artifact.getArtifactId() );
                message.append( ":" );
                message.append( artifact.getVersion() );
                message.append( "\n" );
            }
            throw new MojoExecutionException( "Can't release project due to non released dependencies :\n"
                + message.toString() );
        }
    }

    private void transformPom()
        throws MojoExecutionException
    {
        Model model = project.getModel();
        if ( !isSnapshot( model.getVersion() ) )
        {
            throw new MojoExecutionException( "This project isn't a snapshot (" + model.getVersion() + ")." );
        }

        //Rewrite project version
        projectVersion = model.getVersion().substring( 0, model.getVersion().length() - SNAPSHOT.length() );
        try
        {
            getLog().info( "What is the new version? [" + projectVersion + "]" );
            BufferedReader input = new BufferedReader( new InputStreamReader( System.in ) );
            String inputVersion = input.readLine();
            if ( !StringUtils.isEmpty( inputVersion ) )
            {
                projectVersion = inputVersion;
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Can't read user input.", e );
        }
        model.setVersion( projectVersion );

        //Rewrite parent version
        if ( project.hasParent() )
        {
            if ( isSnapshot( project.getParentArtifact().getBaseVersion() ) )
            {
                model.getParent().setVersion( project.getParentArtifact().getVersion() );
            }
        }

        //Rewrite dependencies version
        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( isSnapshot( artifact.getBaseVersion() ) )
            {
                for ( Iterator j = model.getDependencies().iterator(); j.hasNext(); )
                {
                    Dependency dependency = (Dependency) j.next();
                    if ( artifact.getGroupId().equals( dependency.getGroupId() )
                        && artifact.getArtifactId().equals( dependency.getArtifactId() )
                        && artifact.getBaseVersion().equals( dependency.getVersion() )
                        && artifact.getType().equals( dependency.getType() ) )
                    {
                        dependency.setVersion( artifact.getVersion() );
                    }
                }
            }
        }

        //Rewrite plugins version
        for ( Iterator i = project.getPluginArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( isSnapshot( artifact.getBaseVersion() ) )
            {
                for ( Iterator j = model.getBuild().getPlugins().iterator(); j.hasNext(); )
                {
                    Plugin plugin = (Plugin) j.next();
                    if ( artifact.getGroupId().equals( plugin.getGroupId() )
                        && artifact.getArtifactId().equals( plugin.getArtifactId() ) )
                    {
                        plugin.setGroupId( artifact.getGroupId() );
                        plugin.setVersion( artifact.getVersion() );
                    }
                }
            }
        }

        //Write pom
        MavenXpp3Writer modelWriter = new MavenXpp3Writer();
        try
        {
            PomTransformer transformer = new VersionTransformer();
            transformer.setOutputFile( project.getFile() );
            transformer.setProject( project.getFile() );
            transformer.setUpdatedModel ( model );
            transformer.transformNodes();
            transformer.write();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Can't update pom.", e );
        }
    }

    private void checkin()
        throws MojoExecutionException
    {
        try
        {
            getScm().checkin( "[maven-release-plugin] prepare release " + projectVersion, "pom.xml", null );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the checkin process.", e );
        }
    }

    private void tag()
        throws MojoExecutionException
    {
        try
        {
            if ( getScm().getTag() == null )
            {
                getLog().info( "What is the new tag name?" );
                BufferedReader input = new BufferedReader( new InputStreamReader( System.in ) );
                getScm().setTag( input.readLine() );
            }
            getScm().tag();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the tag process.", e );
        }
    }
}
