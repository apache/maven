package org.apache.maven.tools.plugin.generator;

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
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 *       get validation directives to help users in IDEs.
 */
public class PluginDescriptorGenerator
    implements Generator
{
    public void execute( String destinationDirectory, Set mavenMojoDescriptors, MavenProject project ) throws Exception
    {
        File f = new File( destinationDirectory, "plugin.xml" );

        if ( !f.getParentFile().exists() )
        {
            f.getParentFile().mkdirs();
        }

        FileWriter writer = new FileWriter( f );

        XMLWriter w = new PrettyPrintXMLWriter( writer );

        w.startElement( "plugin" );

        element( w, "id", PluginUtils.pluginId( project ) );

        element( w, "isolatedRealm", "true" );

        w.startElement( "mojos" );

        for ( Iterator it = mavenMojoDescriptors.iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();
            processPluginDescriptor( descriptor, w, project );
        }

        w.endElement();

        PluginUtils.writeDependencies( w, project );

        w.endElement();

        writer.flush();

        writer.close();
    }

    protected void processPluginDescriptor( MojoDescriptor mojoDescriptor, XMLWriter w, MavenProject project )
        throws Exception
    {
        w.startElement( "mojo" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "id" );

        w.writeText( mojoDescriptor.getId() + ":" + mojoDescriptor.getGoal() );

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.requiresDependencyResolution() )
        {
            element( w, "requiresDependencyResolution", "true" );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.getPhase() != null )
        {
            element( w, "phase", mojoDescriptor.getPhase() );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "implementation" );

        w.writeText( mojoDescriptor.getImplementation() );

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "language" );

        w.writeText( mojoDescriptor.getLanguage() );

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "instantiationStrategy" );

        w.writeText( mojoDescriptor.getInstantiationStrategy() );

        w.endElement();

        // ----------------------------------------------------------------------
        // Strategy for handling repeated reference to mojo in
        // the calculated (decorated, resolved) execution stack
        // ----------------------------------------------------------------------
        w.startElement( "executionStrategy" );

        w.writeText( mojoDescriptor.getExecutionStrategy() );

        w.endElement();

        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        List parameters = mojoDescriptor.getParameters();

        w.startElement( "parameters" );

        for ( int j = 0; j < parameters.size(); j++ )
        {
            Parameter parameter = (Parameter) parameters.get( j );

            w.startElement( "parameter" );

            element( w, "name", parameter.getName() );

            element( w, "type", parameter.getType() );

            element( w, "required", Boolean.toString( parameter.isRequired() ) );

            element( w, "validator", parameter.getValidator() );

            element( w, "expression", parameter.getExpression() );

            element( w, "description", parameter.getDescription() );

            element( w, "default", parameter.getDefaultValue() );

            w.endElement();
        }

        w.endElement();

        // ----------------------------------------------------------------------
        // Prereqs
        // ----------------------------------------------------------------------

        // TODO: remove

        List prereqs = mojoDescriptor.getPrereqs();

        if ( prereqs != null && prereqs.size() > 0 )
        {
            w.startElement( "prereqs" );

            for ( int j = 0; j < prereqs.size(); j++ )
            {
                element( w, "prereq", (String) prereqs.get( j ) );
            }

            w.endElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.endElement();
    }

    public void element( XMLWriter w, String name, String value )
    {
        w.startElement( name );

        if ( value == null )
        {
            value = "";
        }

        w.writeText( value );

        w.endElement();
    }
}