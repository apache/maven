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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.transform.ReleaseArtifactTransformation;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.scm.ScmBean;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @goal release
 * @description Release plugin
 * @requiresDependencyResolution test
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DoxiaMojo.java 169372 2005-05-09 22:47:34Z evenisse $
 */
public class ReleaseMojo
    extends AbstractMojo
    implements Contextualizable
{
    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private String basedir;

    /**
     * @parameter expression="${project.build.directory}/checkout"
     * @required
     */
    private String workingDirectory;

    /**
     * @parameter expression="${project.scm.developerConnection}"
     * @required
     */
    private String urlScm;

    /**
     * @parameter expression="${username}"
     */
    private String username;

    /**
     * @parameter expression="${password}"
     */
    private String password;

    /**
     * @parameter expression="${tagBase}"
     */
    private String tagBase = "../tags";

    /**
     * @parameter expression="${tag}"
     */
    private String tag;

    /**
     * @parameter expression="${project.artifacts}"
     * @readonly
     */
    private Set dependencies;

    /**
     * @parameter expression="${project.pluginArtifacts}"
     * @readonly
     */
    private Set plugins;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private PlexusContainer container;

    private ScmManager scmManager;

    private static final String SNAPSHOT = "-SNAPSHOT";

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            initScmManager();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Can't initialize ReleaseMojo.", e );
        }

        try
        {
            prepareRelease();

            performRelease();
        }
        finally
        {
            releaseScmManager();
        }
    }

    private void prepareRelease()
        throws MojoExecutionException
    {
        //checkStatus();

        checkDependencies();

        transformPom();

        //commit();

        //tag();
    }

    private void performRelease()
        throws MojoExecutionException
    {
        //checkout();
    }

    private boolean isSnapshot( String version )
    {
        return version.endsWith( SNAPSHOT );
    }

    private ScmBean getScm()
    {
        ScmBean scm = new ScmBean();
        scm.setScmManager( scmManager );
        scm.setUrl( urlScm );
        scm.setTag( tag );
        scm.setTagBase( tagBase );
        scm.setUsername( username );
        scm.setPassword( password );
        scm.setWorkingDirectory( workingDirectory );
        return scm;
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
//                throw new MojoExecutionException( "Can't release project due to non released parent." );
            }

            currentProject = project.getParent();
        }

        List snapshotDependencies = new ArrayList();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( isSnapshot( artifact.getVersion() ) )
            {
                snapshotDependencies.add( artifact );
            }
        }
        for ( Iterator i = plugins.iterator(); i.hasNext(); )
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
//            throw new MojoExecutionException( "Can't release project due to non released dependencies :\n" +
//                                              message.toString() );
        }
    }

    private void checkStatus()
        throws MojoExecutionException
    {
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
        finally
        {
            releaseScmManager();
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
            throw new MojoExecutionException( "You have some uncommitted files : \n" + message.toString() );
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
        model.setVersion( model.getVersion().substring( 0, model.getVersion().length() - SNAPSHOT.length() ) );

        //Rewrite parent version
        if ( project.hasParent() )
        {
            if ( isSnapshot( project.getParentArtifact().getBaseVersion() ) )
            {
                model.getParent().setVersion( project.getParentArtifact().getVersion() );
            }
        }

        //Rewrite dependencies version
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( isSnapshot( artifact.getBaseVersion() ) )
            {
                for ( Iterator j = model.getDependencies().iterator(); j.hasNext(); )
                {
                    Dependency dependency = (Dependency) j.next();
                    if ( artifact.getGroupId().equals( dependency.getGroupId() ) &&
                         artifact.getArtifactId().equals( dependency.getArtifactId() ) &&
                         artifact.getBaseVersion().equals( dependency.getVersion() ) &&
                         artifact.getType().equals( dependency.getType() ) )
                    {
                        dependency.setVersion( artifact.getVersion() );
                    }
                }
            }
        }

        //Rewrite plugins version
        //TODO Resolve version
        for ( Iterator i = plugins.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( isSnapshot( artifact.getBaseVersion() ) )
            {
                for ( Iterator j = model.getBuild().getPlugins().iterator(); j.hasNext(); )
                {
                    Plugin plugin = (Plugin) j.next();
                    if ( artifact.getGroupId().equals( plugin.getGroupId() ) &&
                         artifact.getArtifactId().equals( plugin.getArtifactId() ) )
                    {
						plugin.setGroupId( artifact.getGroupId() );
                        plugin.setVersion( artifact.getVersion() );
                    }
                }
            }
        }

        MavenXpp3Writer modelWriter = new MavenXpp3Writer();
        try
        {
			//TODO: Write in pom file
            java.io.StringWriter writer = new java.io.StringWriter();
            modelWriter.write( writer, model );
            getLog().info( writer.toString() );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Can't update pom.", e );
        }
    }

    private void tag()
        throws MojoExecutionException
    {
        try
        {
            getScm().tag();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the tag process.", e );
        }
    }

    private void checkout()
        throws MojoExecutionException
    {
        try
        {
            getScm().checkout();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the checkout process.", e );
        }
    }

    private void initScmManager()
        throws Exception
    {
        scmManager = (ScmManager) container.lookup( ScmManager.ROLE );
    }

    private void releaseScmManager()
    {
        try
        {
            container.release( scmManager );
        }
        catch ( Exception e )
        {
            getLog().warn( "Error releasing component - ignoring", e );
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
