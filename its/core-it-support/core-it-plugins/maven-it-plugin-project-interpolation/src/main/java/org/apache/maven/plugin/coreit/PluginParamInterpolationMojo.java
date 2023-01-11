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

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *
 */
@Mojo( name = "check-plugin", defaultPhase = LifecyclePhase.VALIDATE )
public class PluginParamInterpolationMojo
    extends AbstractMojo
{
    /**
     * myDirectory
     */
    @Parameter
    private String myDirectory;

    /**
     * The current Maven project.
     */
    @Parameter( defaultValue = "${project}" )
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        myDirectory = normalize( myDirectory );
        String value = normalize( new File( project.getBuild().getDirectory(), "foo" ).getAbsolutePath() );

        if ( !myDirectory.equals( value ) )
        {
            throw new MojoExecutionException( "Directory supplied: " + myDirectory
                + " is not the same as the project build directory: " + project.getBuild().getDirectory()
                + " + '/foo'" );
        }
    }

    private String normalize( String src )
    {
        return src.replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );
    }
}
