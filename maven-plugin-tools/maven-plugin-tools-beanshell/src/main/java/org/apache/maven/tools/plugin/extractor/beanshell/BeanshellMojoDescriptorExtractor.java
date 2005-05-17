package org.apache.maven.tools.plugin.extractor.beanshell;

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

import bsh.EvalError;
import bsh.Interpreter;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @todo share constants
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 * get validation directives to help users in IDEs.
 */
public class BeanshellMojoDescriptorExtractor
    extends AbstractLogEnabled
    implements MojoDescriptorExtractor
{
    private MojoDescriptor createMojoDescriptor( File basedir, String resource, PluginDescriptor pluginDescriptor )
        throws InvalidPluginDescriptorException
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor( pluginDescriptor );

        mojoDescriptor.setLanguage( "bsh" );
        mojoDescriptor.setComponentConfigurator( "bsh" );

        mojoDescriptor.setImplementation( resource );

        Interpreter interpreter = new Interpreter();

        try
        {
            interpreter.set( "file", new File( basedir, resource ) );

            interpreter.set( "mojoDescriptor", mojoDescriptor );

            interpreter.eval( new InputStreamReader( getClass().getResourceAsStream( "/extractor.bsh" ) ) );
        }
        catch ( EvalError evalError )
        {
            throw new InvalidPluginDescriptorException( "Error scanning beanshell script", evalError );
        }

        return mojoDescriptor;
    }

    public List execute( MavenProject project, PluginDescriptor pluginDescriptor )
        throws InvalidPluginDescriptorException
    {
        List descriptors = new ArrayList();

        for ( Iterator i = project.getScriptSourceRoots().iterator(); i.hasNext(); )
        {
            try
            {
                File basedir = new File( (String) i.next() );

                if ( basedir.exists() )
                {
                    List files = FileUtils.getFiles( basedir, "**/*.bsh", null, false );

                    for ( Iterator j = files.iterator(); j.hasNext(); )
                    {
                        File resource = (File) j.next();
                        String resourcePath = "/" + resource.getPath().replace( '\\', '/' );
                        MojoDescriptor mojoDescriptor = createMojoDescriptor( basedir, resourcePath, pluginDescriptor );
                        descriptors.add( mojoDescriptor );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new InvalidPluginDescriptorException( "Unable to locate files to process", e );
            }
        }

        return descriptors;
    }

}