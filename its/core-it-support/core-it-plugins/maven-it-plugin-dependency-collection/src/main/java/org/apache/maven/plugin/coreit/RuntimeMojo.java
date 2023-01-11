package org.apache.maven.plugin.coreit;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Creates text files that list the dependencies with scope runtime in the order returned from the Maven core.
 *
 * @author Benjamin Bentmann
 *
 */
@Mojo( name = "runtime", requiresDependencyCollection = ResolutionScope.RUNTIME )
public class RuntimeMojo
    extends AbstractDependencyMojo
{

    /**
     * The path to the output file for the project artifacts, relative to the project base directory. Each line of this
     * UTF-8 encoded file specifies an artifact identifier. If not specified, the artifact list will not be written to
     * disk. Unlike the runtime artifacts, the collection of project artifacts additionally contains those artifacts
     * that do not contribute to the class path.
     */
    @Parameter( property = "depres.projectArtifacts" )
    private String projectArtifacts;

    /**
     * The path to the output file for the runtime artifacts, relative to the project base directory. Each line of this
     * UTF-8 encoded file specifies an artifact identifier. If not specified, the artifact list will not be written to
     * disk.
     */
    @Parameter( property = "depres.runtimeArtifacts" )
    private String runtimeArtifacts;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created or any dependency could not be resolved.
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            writeArtifacts( projectArtifacts, project.getArtifacts() );
            writeArtifacts( runtimeArtifacts, project.getRuntimeArtifacts() );

            // NOTE: We can't make any assumptions about the class path but as a minimum it must not cause an exception
            project.getRuntimeClasspathElements();
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Failed to resolve dependencies", e );
        }
    }

}
