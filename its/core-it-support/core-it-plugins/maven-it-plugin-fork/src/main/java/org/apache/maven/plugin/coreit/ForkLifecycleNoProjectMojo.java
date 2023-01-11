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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 */
@Mojo( name = "fork-lifecycle-no-project", requiresProject = false )
@Execute( phase = LifecyclePhase.GENERATE_RESOURCES, lifecycle = "foo" )
public class ForkLifecycleNoProjectMojo
    extends AbstractMojo
{

    @Parameter( defaultValue = "${project.build.finalName}" )
    private String finalName;

    @Parameter( defaultValue = "target" )
    private File touchDirectory;

    public void execute()
        throws MojoExecutionException
    {
        TouchMojo.touch( touchDirectory, "fork-lifecycle-no-project.txt" );

        if ( TouchMojo.FINAL_NAME.equals( finalName ) )
        {
            throw new MojoExecutionException( "forked project was polluted. (should NOT be \'" + TouchMojo.FINAL_NAME
                + "\')." );
        }
    }

}
