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

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

/**
 * @goal touch
 * 
 * @phase process-sources
 *
 * @description Goal which cleans the build
 */
public class CoreItMojo
    extends AbstractPlugin
{
    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="target/test-basedir-alignment"
     */
    private File basedirAlignmentDirectory;

    /**
     * @parameter
     */
    private String pluginItem = "foo";

    /**
     * @parameter
     */
    private String goalItem = "bar";

    public void execute()
        throws PluginExecutionException
    {
        touch( new File( outputDirectory ), "touch.txt" );

        // This parameter should be aligned to the basedir as the parameter type is specified
        // as java.io.File

        if ( basedirAlignmentDirectory.getPath().equals( "target/test-basedir-alignment" ) )
        {
            throw new PluginExecutionException( "basedirAlignmentDirectory not aligned" );
        }
        
        touch( basedirAlignmentDirectory, "touch.txt" );

        // Test parameter setting
        if ( pluginItem != null )
        {
            touch( new File( outputDirectory ), pluginItem );
        }

        if ( goalItem != null )
        {
            touch( new File( outputDirectory ), goalItem );
        }
    }

    private static void touch( File dir, String file )
        throws PluginExecutionException
    {
        try
        {
             if ( !dir.exists() )
             {
                 dir.mkdirs();
             }
             
             File touch = new File( dir, file );
     
             FileWriter w = new FileWriter( touch );
             
             w.write( file );
             
             w.close();
        }
        catch ( IOException e )
        {
            throw new PluginExecutionException( "Error touching file", e );
        }
    }
}
