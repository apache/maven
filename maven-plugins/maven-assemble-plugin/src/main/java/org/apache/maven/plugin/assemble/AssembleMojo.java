package org.apache.maven.plugin.assemble;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import org.apache.maven.plugins.assemble.model.Assembly;
import org.apache.maven.plugins.assemble.model.FileSet;
import org.apache.maven.plugins.assemble.model.io.xpp3.AssemblyXpp3Reader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal assemble
 * @description assemble an application bundle or distribution
 * @parameter name="buildDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.directory/assembly"
 * description=""
 * @parameter name="outputDirectory" type="String" required="true" validator="" expression="#project.build.directory" description=""
 * @parameter name="descriptor" type="String" required="true" validator="" expression="#maven.assemble.descriptor" description=""
 * @parameter name="finalName" type="String" required="true" validator="" expression="#project.build.finalName" description=""
 */
public class AssembleMojo
    extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // TODO: align all to basedir
        String outputDirectory = (String) request.getParameter( "outputDirectory" );
        String buildDirectory = (String) request.getParameter( "buildDirectory" );
        String descriptor = (String) request.getParameter( "descriptor" );
        String finalName = (String) request.getParameter( "finalName" );

        AssemblyXpp3Reader reader = new AssemblyXpp3Reader();
        Assembly assembly = reader.read( new FileReader( new File( descriptor ) ) );

        // TODO: include in bootstrap
        // TODO: include dependencies marked for distribution under certain formats
        // TODO: have a default set of descriptors that can be used instead of the file

        String fullName = finalName + "-" + assembly.getId();
        File outputBase = new File( buildDirectory, fullName );
        outputBase.mkdirs();

        for ( Iterator i = assembly.getFilesets().iterator(); i.hasNext(); )
        {
            FileSet fileset = (FileSet) i.next();
            String directory = fileset.getDirectory();
            String output = fileset.getOutputDirectory();
            if ( output == null )
            {
                output = directory;
            }

            // TODO: includes/excludes

            FileUtils.copyDirectoryStructure( new File( directory ), new File( outputBase, output ));
        }

        // TODO: package it up
    }
}
