package org.apache.maven.plugins.release;

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

import org.apache.maven.Maven;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.release.helpers.ReleaseProgressTracker;
import org.apache.maven.plugins.release.helpers.ScmHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Perform a release from SCM
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DoxiaMojo.java 169372 2005-05-09 22:47:34Z evenisse $
 * @aggregator
 * @goal perform
 */
public class PerformReleaseMojo
    extends AbstractReleaseMojo
{
    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * @parameter expression="${goals}"
     */
    private String goals = "deploy";

    /**
     * @parameter expression="${project.build.directory}/checkout"
     * @required
     */
    protected String workingDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter expression="${settings.interactiveMode}"
     * @required
     * @readonly
     */
    private boolean interactive;
    
    /**
     * @parameter expression="${releasePom}"
     */
    private String releasePom;

    private ReleaseProgressTracker releaseProgress;

    public void execute()
        throws MojoExecutionException
    {
        checkout();

        runGoals();
    }

    private void checkout()
        throws MojoExecutionException
    {
        getLog().info( "Checking out the project to perform the release ..." );

        try
        {
            ScmHelper scm = getScm( workingDirectory );

            scm.checkout();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the checkout process.", e );
        }
    }

    private void runGoals()
        throws MojoExecutionException
    {
        // TODO: we need to get a reference to the maven component and use that so this
        // will work purely in an embedded mode. Not sure how to pass the release setting to the plugin in that
        // instance though - still via -D, or is there a better way?

        Commandline cl = new Commandline();

        cl.setExecutable( "mvn" );

        cl.setWorkingDirectory( workingDirectory );

        cl.createArgument().setLine( goals );

        cl.createArgument().setLine( "-DperformRelease=true" );

        cl.createArgument().setLine( "--no-plugin-updates" );

        if ( !interactive )
        {
            cl.createArgument().setLine( "--batch-mode" );
        }

        if ( StringUtils.isEmpty( releasePom ) )
        {
            File pomFile = project.getFile();

            releasePom = pomFile.getName();
        }
        
        if ( releasePom.equals( Maven.RELEASE_POMv4 ) && interactive )
        {
            StringBuffer warning = new StringBuffer();
            warning.append( "\n*******************************************************************************\n" );
            warning.append( "\nYou have chosen to use the fully resolved release-POM to deploy this project." );
            warning.append( "\n" );
            warning.append( "\nNOTE: Deploying artifacts using the fully resolved release-POM " );
            warning.append( "\nwill result in loss of any version ranges specified for your");
            warning.append( "\nproject's dependencies." );
            warning.append( "\n" );
            warning.append( "\nAre you sure you want to do this?" );
            warning.append( "\n" );
            warning.append( "\n*******************************************************************************\n" );
            
            getLog().warn( warning );
            
            getLog().info( "Enter the POM filename to use for deployment: [" + releasePom + "] " );

            try
            {
                String input = getInputHandler().readLine();

                if ( !StringUtils.isEmpty( input ) )
                {
                    releasePom = input;
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "An error has occurred while reading the pom file location.", e );
            }
        }
        
        getLog().info( "Releasing project based on POM: " + releasePom + " in working directory: " + workingDirectory );
        
        cl.createArgument().setLine( "-f " + releasePom );
        
        List profiles = project.getActiveProfiles();

        if ( profiles != null && !profiles.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append( "-P " );

            for ( Iterator it = profiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();

                buffer.append( profile.getId() ).append( "," );
            }

            buffer.setLength( buffer.length() - 1 );

            cl.createArgument().setLine( buffer.toString() );
        }

        StreamConsumer consumer = new DefaultConsumer();

        try
        {
            int result = CommandLineUtils.executeCommandLine( cl, consumer, consumer );

            if ( result != 0 )
            {
                throw new MojoExecutionException( "Result of mvn execution is: \'" + result + "\'. Release failed." );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Can't run goal " + goals, e );
        }
    }

    protected ReleaseProgressTracker getReleaseProgress()
        throws MojoExecutionException
    {
        if ( releaseProgress == null )
        {
            try
            {
                releaseProgress = ReleaseProgressTracker.load( basedir.getAbsolutePath() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to load release information from file: " +
                    ReleaseProgressTracker.getReleaseProgressFilename(), e );
            }
        }

        return releaseProgress;
    }
}
