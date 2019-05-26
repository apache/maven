package org.apache.maven.its.mng5753.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo( name = "test", configurator = "test" )
public class TestMojo
    extends AbstractMojo
{
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;
    
    @Parameter
    private String name;
    
    public void execute()
        throws MojoExecutionException
    {        
        try
        {
            File file = new File( project.getBasedir(), "configuration.txt" );
            file.getParentFile().mkdirs();
            Writer w = new OutputStreamWriter( new FileOutputStream( file, true ), "UTF-8" );
            try
            {
                w.write( name );
            }
            finally
            {
                w.close();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        
    }
}
