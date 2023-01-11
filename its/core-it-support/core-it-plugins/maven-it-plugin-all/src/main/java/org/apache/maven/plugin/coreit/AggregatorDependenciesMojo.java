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
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 */
@Mojo( name = "aggregator-dependencies", defaultPhase = LifecyclePhase.VALIDATE,
       requiresDependencyResolution = ResolutionScope.TEST, aggregator = true )
public class AggregatorDependenciesMojo
    extends AbstractMojo
{

    /**
     * The path to the touch file, relative to the project's base directory.
     */
    @Parameter( property = "aggregator.touchFile", defaultValue = "${project.build.directory}/touch.txt" )
    private File touchFile;

    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Touching file: " + touchFile );

        if ( touchFile != null )
        {
            try
            {
                touchFile.getParentFile().mkdirs();
                touchFile.createNewFile();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to create touch file " + touchFile, e );
            }
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Touched file: " + touchFile );
    }

}
