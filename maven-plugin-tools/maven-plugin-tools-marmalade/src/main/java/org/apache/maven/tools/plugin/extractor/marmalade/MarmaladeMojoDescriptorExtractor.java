package org.apache.maven.tools.plugin.extractor.marmalade;

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

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.script.marmalade.MarmaladeMojoExecutionDirectives;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.marmalade.parsing.ScriptParser;
import org.codehaus.marmalade.util.LazyMansAccess;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author jdcasey
 */
public class MarmaladeMojoDescriptorExtractor
    implements MojoDescriptorExtractor
{

    private ScriptParser scriptParser = new ScriptParser();

    public Set execute( String sourceDir, MavenProject project ) throws Exception
    {
        String[] files = PluginUtils.findSources( sourceDir, "**/*.mmld" );

        Set descriptors = new HashSet();

        File dir = new File( sourceDir );
        for ( int i = 0; i < files.length; i++ )
        {
            String file = files[i];

            Map context = new TreeMap();
            context.put( MarmaladeMojoExecutionDirectives.SCRIPT_BASEPATH_INVAR, sourceDir );

            File scriptFile = new File( dir, file );

            context = LazyMansAccess.executeFromFile( scriptFile, context );

            MojoDescriptor descriptor = (MojoDescriptor) context.get( MarmaladeMojoExecutionDirectives.METADATA_OUTVAR );

            descriptor.setImplementation( file );

            descriptors.add( descriptor );
        }

        return descriptors;
    }

}