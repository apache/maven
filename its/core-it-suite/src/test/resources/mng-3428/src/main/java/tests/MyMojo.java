package tests;

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
import org.apache.maven.plugin.MojoFailureException;

import java.util.Iterator;
import java.util.List;

/**
 * @goal test
 * @requiresProject false
 */
public class MyMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="${plugin.artifacts}"
     */
    private List pluginArtifacts;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        System.out.println( "\n\n\n\n\n\n\n" );

        boolean foundCommonsCli = false;
        for ( Iterator it = pluginArtifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            if ( "commons-cli".equals( artifact.getArtifactId() ) )
            {
                foundCommonsCli = true;
            }

            System.out.println( artifact.getArtifactId() );
        }

        System.out.println( "\n\n\n\n\n\n\n" );

        if ( !foundCommonsCli )
        {
            throw new MojoExecutionException( "Commons-cli dependency not found." );
        }
    }

}
