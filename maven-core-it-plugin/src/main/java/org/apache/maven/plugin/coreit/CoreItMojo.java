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
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;
import java.io.FileWriter;

/**
 * @goal touch
 * 
 * @phase process-sources
 *
 * @description Goal which cleans the build
 *
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.directory"
 *  description=""
 * 
 * @parameter
 *  name="basedirAlignmentDirectory"
 *  type="java.io.File"
 *  required="true"
 *  validator=""
 *  expression="target/test-basedir-alignment"
 *  description=""
 *
 * @parameter name="pluginItem" type="String" required="false" validator="" description="" expression="" defaultValue="foo"
 * @parameter name="goalItem" type="String" required="false" validator="" description="" expression="bar"
 */
public class CoreItMojo
    extends AbstractPlugin
{
    private String outputDirectory;

    private File basedirAlignmentDirectory;

    private String pluginItem;

    private String goalItem;

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        touch( new File( outputDirectory ), "touch.txt" );

        // This parameter should be aligned to the basedir as the parameter type is specified
        // as java.io.File
        
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
        throws Exception
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
}
