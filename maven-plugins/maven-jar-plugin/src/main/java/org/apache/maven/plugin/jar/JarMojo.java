package org.apache.maven.plugin.jar;

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

import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @goal jar
 *
 * @description build a jar
 *
 * @prereq surefire:test
 * @prereq resources:resources
 *
 * @parameter
 *  name="jarName"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#maven.final.name"
 *  description=""
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.output"
 *  description=""
 * @parameter
 *  name="basedir"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.directory"
 *  description=""
 *
 * @author <a href="michal@codehaus">Michal Maczka</a>
 * @version $Id$
 */
public class JarMojo
    extends AbstractJarMojo
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        File basedir = new File( (String) request.getParameter( "basedir" ) );

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        String jarName = (String) request.getParameter( "jarName" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------


        File jarFile = new File( basedir, jarName + ".jar" );

        Map includes = new LinkedHashMap();
        
        addDirectory(includes, "**/**", "**/package.html", "", new File( outputDirectory ) );
        
        createJar( jarFile, includes );
    }
}
