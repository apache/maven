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
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.script.marmalade.MarmaladeMojoExecutionDirectives;
import org.apache.maven.script.marmalade.tags.MojoTag;
import org.apache.maven.tools.plugin.extractor.AbstractScriptedMojoDescriptorExtractor;
import org.apache.maven.tools.plugin.PluginToolsException;
import org.codehaus.marmalade.launch.MarmaladeLauncher;
import org.codehaus.marmalade.launch.MarmaladeLaunchException;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.model.MarmaladeTag;
import org.codehaus.marmalade.runtime.DefaultContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;
import org.codehaus.plexus.component.factory.marmalade.PlexusIntegratedLog;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author jdcasey
 */
public class MarmaladeMojoDescriptorExtractor
    extends AbstractScriptedMojoDescriptorExtractor
{

    protected String getScriptFileExtension()
    {
        return ".mmld";
    }

    protected Set extractMojoDescriptors( Map sourceFilesKeyedByBasedir, PluginDescriptor pluginDescriptor )
        throws PluginToolsException
    {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( MarmaladeMojoDescriptorExtractor.class.getClassLoader() );

            Set descriptors = new HashSet();

            for ( Iterator mapIterator = sourceFilesKeyedByBasedir.entrySet().iterator(); mapIterator.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) mapIterator.next();

                String basedir = (String) entry.getKey();
                Set scriptFiles = (Set) entry.getValue();

                for ( Iterator it = scriptFiles.iterator(); it.hasNext(); )
                {
                    File scriptFile = (File) it.next();

                    MarmaladeLauncher launcher = new MarmaladeLauncher().withInputFile( scriptFile );

                    Logger logger = getLogger();

                    if ( logger != null )
                    {
                        PlexusIntegratedLog log = new PlexusIntegratedLog();

                        log.enableLogging( logger );

                        launcher = launcher.withLog( log );
                    }

                    MarmaladeScript script = launcher.getMarmaladeScript();

                    MarmaladeTag rootTag = script.getRoot();
                    if ( rootTag instanceof MojoTag )
                    {
                        Map contextMap = new TreeMap();
                        contextMap.put( MarmaladeMojoExecutionDirectives.SCRIPT_BASEPATH_INVAR, basedir );
                        contextMap.put( MarmaladeMojoExecutionDirectives.PLUGIN_DESCRIPTOR, pluginDescriptor );

                        MarmaladeExecutionContext context = new DefaultContext( contextMap );

                        script.execute( context );

                        contextMap = context.getExternalizedVariables();

                        MojoDescriptor descriptor = (MojoDescriptor) contextMap
                            .get( MarmaladeMojoExecutionDirectives.METADATA_OUTVAR );

                        descriptors.add( descriptor );
                    }
                    else
                    {
                        System.out.println( "This script is not a mojo. Its root tag is {element: "
                            + rootTag.getTagInfo().getElement() + ", class: " + rootTag.getClass().getName() + "}" );
                    }
                }
            }

            return descriptors;
        }
        catch ( IOException e )
        {
            throw new PluginToolsException( "Error reading Marmalade for extracting mojo descriptor", e );
        }
        catch ( MarmaladeExecutionException e )
        {
            throw new PluginToolsException( "Error executing Marmalade for extracting mojo descriptor", e );
        }
        catch ( MarmaladeLaunchException e )
        {
            throw new PluginToolsException( "Error executing Marmalade for extracting mojo descriptor", e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldCl );
        }
    }

}