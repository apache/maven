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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

import sun.security.action.GetLongAction;
import sun.tools.jar.CommandLine;

/**
 * @goal perform
 * @description Perform a release from SCM
 * @requiresDependencyResolution test
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DoxiaMojo.java 169372 2005-05-09 22:47:34Z evenisse $
 */
public class PerformReleaseMojo
    extends AbstractReleaseMojo
{
    /**
     * @parameter expression="${goals}"
     * @required
     */
    private String goals = "deploy site:site site:deploy";

    protected void executeTask()
        throws MojoExecutionException
    {
        checkout();

        runGoals();
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

    private void runGoals()
        throws MojoExecutionException
    {
        Commandline cl = new Commandline();
        cl.setExecutable( "m2" );
        cl.setWorkingDirectory( getWorkingDirectory() );
        cl.createArgument().setLine( goals );
        StreamConsumer consumer = new DefaultConsumer();
        try
        {
            CommandLineUtils.executeCommandLine( cl, consumer, consumer );
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Can't run goal " + goals, e );
        }
    }
}
