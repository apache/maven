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
import org.apache.maven.project.MavenProject;

/**
 * @goal check-property
 *
 * @phase validate
 */
public class PropertyInterpolationMojo
    extends AbstractMojo
{

    /** @parameter default-value="${project}" */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        String value = normalize( project.getProperties().getProperty( "myDirectory" ) );
        String targetValue = normalize( new File( project.getBuild().getDirectory(), "foo" ).getAbsolutePath() );

        if ( !value.equals( targetValue ) )
        {
            throw new MojoExecutionException( "Property value of 'myDirectory': " + value
                + " should equal the 'foo' subpath of the project build directory: " + targetValue );
        }
    }

    private String normalize( String src )
    {
        return src.replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );
    }
}
