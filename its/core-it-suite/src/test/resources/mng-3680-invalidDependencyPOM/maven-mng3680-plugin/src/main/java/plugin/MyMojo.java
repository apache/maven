package plugin;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Goal which touches a timestamp file.
 * 
 * @goal check
 * @requiresDependencyResolution compile
 * @phase validate
 */
public class MyMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project.compileArtifacts}"
     * @required
     */
    private List artifacts;

    public void execute()
        throws MojoExecutionException
    {
        boolean foundL1 = false;
        boolean foundL2 = false;

        if ( artifacts != null )
        {
            for ( Iterator it = artifacts.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                if ( !foundL1 && artifact.getArtifactId().equals( "dep-L1" ) )
                {
                    foundL1 = true;
                }
                else if ( !foundL2 && artifact.getArtifactId().equals( "dep-L2" ) )
                {
                    foundL2 = true;
                }

                if ( foundL1 && foundL2 )
                {
                    break;
                }
            }
        }

        if ( !foundL1 || !foundL2 )
        {
            throw new MojoExecutionException( "Didn't find the following artifacts: " + ( foundL1 ? "" : "\n- dep-L1" )
                + ( foundL2 ? "" : "\n- dep-L2" ) );
        }
    }
}
