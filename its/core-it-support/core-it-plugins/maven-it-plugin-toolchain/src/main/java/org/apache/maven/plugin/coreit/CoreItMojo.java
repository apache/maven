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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;

/**
 * @goal toolchain
 * @phase validate
 */
public class CoreItMojo
    extends AbstractMojo
{
    /**
     * @component
     */
    private ToolchainManagerPrivate toolchainManager;

    /**
     * The path to the output file for the properties.
     * 
     * @parameter expression="${toolchain.outputFile}" default-value="${project.build.directory}/toolchains.properties"
     */
    private File outputFile;

    /**
     * @parameter default-value="jdk"
     */
    private String type;

    /**
     * @parameter default-value="javac"
     */
    private String tool;

    public void execute()
        throws MojoExecutionException
    {
        ToolchainPrivate[] tcs;
        try
        {
            tcs = toolchainManager.getToolchainsForType( type );
        }
        catch ( MisconfiguredToolchainException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Toolchains in plugin: " + Arrays.asList( tcs ) );

        Properties properties = new Properties();

        int count = 1;
        for ( Iterator i = Arrays.asList( tcs ).iterator(); i.hasNext(); count++ )
        {
            ToolchainPrivate toolchain = (ToolchainPrivate) i.next();

            String foundTool = toolchain.findTool( tool );
            if ( foundTool != null )
            {
                properties.setProperty( "tool." + count, foundTool );
            }
        }

        OutputStream out = null;
        try
        {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream( outputFile );
            properties.store( out, "MAVEN-CORE-IT-LOG" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            if ( out != null )
            {
                try
                {
                    out.close();
                }
                catch ( IOException e )
                {
                }
            }
        }
    }
}
