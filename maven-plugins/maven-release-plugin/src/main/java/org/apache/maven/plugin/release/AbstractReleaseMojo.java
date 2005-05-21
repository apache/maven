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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.scm.ScmBean;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DoxiaMojo.java 169372 2005-05-09 22:47:34Z evenisse $
 */
public abstract class AbstractReleaseMojo
    extends AbstractMojo
    implements Contextualizable
{
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
     * @parameter expression="${maven.username}"
     * @required
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
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private PlexusContainer container;

    private ScmManager scmManager;

    public MavenProject getProject()
    {
        return project;
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    protected ScmManager getScmManager()
    {
        return scmManager;
    }

    public String getTag()
    {
        return tag;
    }

    protected ScmBean getScm()
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

    public PlexusContainer getContainer()
    {
        return container;
    }

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
            executeTask();
        }
        finally
        {
            releaseScmManager();
        }
    }

    protected abstract void executeTask()
        throws MojoExecutionException;

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
