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
import org.apache.maven.tools.plugin.PluginToolsException;
import org.apache.maven.tools.plugin.extractor.AbstractScriptedMojoDescriptorExtractor;
import org.codehaus.marmalade.launch.MarmaladeLaunchException;
import org.codehaus.marmalade.launch.MarmaladeLauncher;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.model.MarmaladeTag;
import org.codehaus.plexus.component.factory.marmalade.PlexusIntegratedLog;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    protected List extractMojoDescriptors( Map sourceFilesKeyedByBasedir, PluginDescriptor pluginDescriptor )
        throws PluginToolsException
    {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( MarmaladeMojoDescriptorExtractor.class.getClassLoader() );

            List descriptors = new ArrayList();

            for ( Iterator mapIterator = sourceFilesKeyedByBasedir.entrySet().iterator(); mapIterator.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) mapIterator.next();

                String basedir = (String) entry.getKey();
                Set scriptFiles = (Set) entry.getValue();

                for ( Iterator it = scriptFiles.iterator(); it.hasNext(); )
                {
                    File scriptFile = (File) it.next();

                    try
                    {
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
                            launcher.withVariable( MarmaladeMojoExecutionDirectives.SCRIPT_BASEPATH_INVAR, basedir );
                            launcher
                                .withVariable( MarmaladeMojoExecutionDirectives.PLUGIN_DESCRIPTOR, pluginDescriptor );

                            Map contextMap = launcher.run();

                            MojoDescriptor descriptor = (MojoDescriptor) contextMap
                                .get( MarmaladeMojoExecutionDirectives.METADATA_OUTVAR );

                            descriptors.add( descriptor );
                        }
                        else
                        {
                            getLogger().debug(
                                               "Found non-mojo marmalade script at: " + scriptFile
                                                   + ".\nIts root tag is {element: "
                                                   + rootTag.getTagInfo().getElement() + ", class: "
                                                   + rootTag.getClass().getName() + "}" );
                        }
                    }
                    catch ( IOException e )
                    {
                        throw new PluginToolsException( "Error reading descriptor Marmalade mojo in: " + scriptFile, e );
                    }
                    catch ( MarmaladeLaunchException e )
                    {
                        throw new PluginToolsException( "Error extracting descriptor Marmalade mojo from: " + scriptFile, e );
                    }
                }
            }

            return descriptors;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldCl );
        }
    }

}