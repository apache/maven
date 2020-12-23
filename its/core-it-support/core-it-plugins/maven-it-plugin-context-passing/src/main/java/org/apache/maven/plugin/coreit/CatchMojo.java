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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * "Catch" a parameter "thrown" by the ThrowMojo through the plugin context, and
 * write a file based on it's value to the build output directory.
 *
 * @goal catch
 */
public class CatchMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File outDir;

    public File getOutDir()
    {
        return outDir;
    }

    public void setOutDir( File outDir )
    {
        this.outDir = outDir;
    }

    public void execute()
        throws MojoExecutionException
    {
        String value = (String) getPluginContext().get( ThrowMojo.THROWN_PARAMETER );

        if ( !outDir.exists() )
        {
            outDir.mkdirs();
        }

        File outfile = new File( outDir, value );

        Writer writer = null;
        try
        {
            writer = new FileWriter( outfile );

            writer.write( value );

            writer.flush();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot write output file: " + outfile, e );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }

}
