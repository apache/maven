package org.apache.maven.tools.plugin.scanner;

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

import org.apache.maven.plugin.descriptor.DuplicateMojoDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.PluginToolsException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author jdcasey
 */
public class DefaultMojoScanner
    extends AbstractLogEnabled
    implements MojoScanner
{

    private Map mojoDescriptorExtractors;

    public DefaultMojoScanner( Map extractors )
    {
        this.mojoDescriptorExtractors = extractors;

        this.enableLogging( new ConsoleLogger( Logger.LEVEL_INFO, "standalone-scanner-logger" ) );
    }

    public DefaultMojoScanner()
    {
    }

    public void populatePluginDescriptor( MavenProject project, PluginDescriptor pluginDescriptor )
        throws PluginToolsException
    {
        Logger logger = getLogger();

        logger.debug( "Using " + mojoDescriptorExtractors.size() + " extractors." );

        for ( Iterator it = mojoDescriptorExtractors.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            String language = (String) entry.getKey();
            MojoDescriptorExtractor extractor = (MojoDescriptorExtractor) entry.getValue();

            logger.debug( "Applying extractor for language: " + language );

            List extractorDescriptors = extractor.execute( project, pluginDescriptor );

            logger.debug( "Extractor for language: " + language + " found " + extractorDescriptors.size()
                + " mojo descriptors." );

            for ( Iterator descriptorIt = extractorDescriptors.iterator(); descriptorIt.hasNext(); )
            {
                MojoDescriptor descriptor = (MojoDescriptor) descriptorIt.next();

                logger.debug( "Adding mojo: " + descriptor + " to plugin descriptor." );

                descriptor.setPluginDescriptor( pluginDescriptor );

                try
                {
                    pluginDescriptor.addMojo( descriptor );
                }
                catch ( DuplicateMojoDescriptorException e )
                {
                    throw new PluginToolsException( "Duplicate goal specification detected.\nError was: "
                        + e.getLocalizedMessage(), e );
                }
            }
        }
    }

}