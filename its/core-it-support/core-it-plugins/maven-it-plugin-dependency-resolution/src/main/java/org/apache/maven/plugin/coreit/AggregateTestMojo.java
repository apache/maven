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
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * Combines dependency resolution with aggregation. The path parameters of this mojo support the token
 * <code>&#64;artifactId&#64;</code> to dynamically adjust the output file for each project in the reactor whose
 * dependencies are dumped.
 *
 * @author Benjamin Bentmann
 */
@Mojo( name = "aggregate-test", requiresDependencyResolution = ResolutionScope.TEST, aggregator = true )
public class AggregateTestMojo
    extends AbstractDependencyMojo
{

    /**
     * The path to the output file for the project artifacts, relative to the project base directory. Each line of this
     * UTF-8 encoded file specifies an artifact identifier. If not specified, the artifact list will not be written to
     * disk. Unlike the test artifacts, the collection of project artifacts additionally contains those artifacts that
     * do not contribute to the class path.
     */
    @Parameter( property = "depres.projectArtifacts" )
    private String projectArtifacts;

    /**
     * The path to the output file for the test class path, relative to the project base directory. Each line of
     * this UTF-8 encoded file specifies the absolute path to a class path element. If not specified, the class path
     * will not be written to disk.
     */
    @Parameter( property = "depres.testClassPath" )
    private String testClassPath;

    /**
     * The path to the properties file for the checksums of the test class path elements, relative to the project base
     * directory. The (trimmed) path to a JAR is used as the property key, the property value is the SHA-1 hash of the
     * JAR. If not specified, the class path checksums will not be calculated.
     */
    @Parameter( property = "depres.testClassPathChecksums" )
    private String testClassPathChecksums;

    /**
     * The Maven projects in the reactor.
     */
    @Parameter( defaultValue = "${reactorProjects}", readonly = true )
    private List<MavenProject> reactorProjects;

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
            for ( MavenProject project : reactorProjects )
            {
                writeArtifacts( filter( projectArtifacts, project ), project.getArtifacts() );
                writeClassPath( filter( testClassPath, project ), project.getTestClasspathElements() );
                writeClassPathChecksums( filter( testClassPathChecksums, project ),
                                         project.getTestClasspathElements() );
            }
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Failed to resolve dependencies", e );
        }
    }

    private String filter( String filename, MavenProject project )
    {
        String result = filename;

        if ( filename != null )
        {
            result = result.replaceAll( "@artifactId@", project.getArtifactId() );
        }

        return result;
    }

}
