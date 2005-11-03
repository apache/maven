package org.apache.maven.plugin.coreit;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import org.apache.maven.project.MavenProject;

/**
 * @goal fork
 * 
 * @execute phase="generate-sources" lifecycle="foo"
 */
public class CoreItForkerMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    public void execute()
        throws MojoExecutionException
    {
        if ( !executedProject.getBuild().getFinalName().equals( "coreitified" ) )
        {
            throw new MojoExecutionException( "Unexpected result, final name of executed project is " + executedProject.getBuild().getFinalName() );
        }

        if ( project.getBuild().getFinalName().equals( "coreitified" ) )
        {
            throw new MojoExecutionException( "forked project was polluted" );
        }
    }
}
