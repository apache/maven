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
 */
public class CoreItMojo
    extends AbstractPlugin
{
    private static final int DELETE_RETRY_SLEEP_MILLIS = 10;

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        File f = new File( outputDirectory );
        
        if ( !f.exists() )
        {
            f.mkdirs();
        }
        
        File touch = new File( f, "touch.txt" );
        
        FileWriter w = new FileWriter( touch );
        
        w.write( "touch.txt" );
        
        w.close();
        
        // This parameter should be aligned to the basedir as the parameter type is specified
        // as java.io.File
        
        String basedirAlignmentDirectory = (String) request.getParameter( "basedirAlignmentDirectory" );

        f = new File( basedirAlignmentDirectory );
        
        if ( !f.exists() )
        {
            f.mkdirs();
        }         
        
        touch = new File( f, "touch.txt" );
        
        w = new FileWriter( touch );
        
        w.write( "touch.txt" );
        
        w.close();        
    }
}
