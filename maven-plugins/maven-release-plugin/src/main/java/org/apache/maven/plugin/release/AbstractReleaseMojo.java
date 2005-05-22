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

import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DoxiaMojo.java 169372 2005-05-09 22:47:34Z evenisse $
 */
public abstract class AbstractReleaseMojo
    extends AbstractMojo
    implements Contextualizable
{
    public static final String RELEASE_PROPS = "release.properties";

    public static final String USERNAME = "maven.username";

    public static final String TAG = "tag";

    public static final String SCM_URL = "scm.url";

    /**
     * @parameter expression="${project.build.directory}/checkout"
     * @required
     */
    protected String workingDirectory;

    /**
     * @parameter expression="${project.scm.developerConnection}"
     * @required
     */
    protected String urlScm;

    /**
     * @parameter expression="${maven.username}"
     */
    protected String username;

    /**
     * @parameter expression="${password}"
     */
    protected String password;

    /**
     * @parameter expression="${tagBase}"
     */
    protected String tagBase = "../tags";

    /**
     * @parameter expression="${tag}"
     */
    protected String tag;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    private PlexusContainer container;

    protected ScmManager scmManager;

    private Properties releaseProperties;

    protected ScmBean getScm()
    {
        ScmBean scm = new ScmBean();

        scm.setScmManager( scmManager );

        if ( releaseProperties != null )
        {
            urlScm = releaseProperties.getProperty( SCM_URL );
        }

        scm.setUrl( urlScm );

        if ( releaseProperties != null )
        {
            tag = releaseProperties.getProperty( TAG );
        }

        scm.setTag( tag );

        scm.setTagBase( tagBase );

        if ( releaseProperties != null )
        {
            username = releaseProperties.getProperty( USERNAME );
        }

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

            // ----------------------------------------------------------------------
            // The release properties file has been created by the prepare phase and
            // wants to be shared with the perform.
            // ----------------------------------------------------------------------

            File releasePropertiesFile = new File( project.getFile().getParentFile(), RELEASE_PROPS );

            if ( releasePropertiesFile.exists() )
            {

                releaseProperties = new Properties();

                InputStream is = new FileInputStream( releasePropertiesFile );

                releaseProperties.load( is );
            }
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
