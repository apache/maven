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
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 *       get validation directives to help users in IDEs.
 */
public class PluginXdocGenerator
    implements Generator
{
    public void execute( String destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();
            processPluginDescriptor( descriptor, destinationDirectory );
        }
    }

    protected void processPluginDescriptor( MojoDescriptor mojoDescriptor, String destinationDirectory )
        throws IOException
    {
        String id = mojoDescriptor.getGoal();

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( new File( destinationDirectory, id + "-plugin.xml" ) );

            writeBody( writer, id, mojoDescriptor );

            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private void writeBody( FileWriter writer, String id, MojoDescriptor mojoDescriptor )
    {
        XMLWriter w = new PrettyPrintXMLWriter( writer );

        w.startElement( "document" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "properties" );

        w.startElement( "title" );

        w.writeText( "Documentation for the " + id + " plugin." );

        w.endElement();

        w.startElement( "author" );

        w.addAttribute( "email", "dev@maven.apache.org" );

        w.writeText( "Maven development team." );

        w.endElement();

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "section" );

        w.addAttribute( "name", "Goals" );

        w.startElement( "p" );

        w.writeText( "The goals for the " + id + " are as follows:" );

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "subsection" );

        w.addAttribute( "name", mojoDescriptor.getGoal() );

        if ( mojoDescriptor.getDescription() != null )
        {
            w.startElement( "p" );

            w.writeText( mojoDescriptor.getDescription() );

            w.endElement();
        }

        w.startElement( "p" );

        w.writeText( "These parameters for this goal: " );

        w.endElement();

        writeGoalParameterTable( mojoDescriptor, w );

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.endElement();
    }

    private void writeGoalParameterTable( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        w.startElement( "p" );

        w.startElement( "table" );

        w.startElement( "tr" );

        w.startElement( "th" );

        w.writeText( "Parameter" );

        w.endElement();

        w.startElement( "th" );

        w.writeText( "Type" );

        w.endElement();

        w.startElement( "th" );

        w.writeText( "Expression" );

        w.endElement();

        w.startElement( "th" );

        w.writeText( "Description" );

        w.endElement();

        w.startElement( "th" );

        w.writeText( "Required?" );

        w.endElement();

        w.startElement( "th" );

        w.writeText( "Deprecated?" );

        w.endElement();

        w.endElement();

        List parameters = mojoDescriptor.getParameters();

        Map parameterMap = mojoDescriptor.getParameterMap();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

            w.startElement( "tr" );

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            String paramName = parameter.getAlias();

            if ( StringUtils.isEmpty( paramName ) )
            {
                paramName = parameter.getName();
            }

            w.writeText( paramName );

            w.endElement();

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            w.writeText( parameter.getType() );

            w.endElement();

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            w.writeText( parameter.getExpression() );

            w.endElement();

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            w.writeText( parameter.getDescription() );

            w.endElement();

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            w.writeText( Boolean.toString( parameter.isRequired() ) );

            w.endElement();

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            String deprecationWarning = parameter.getDeprecated();
            if ( StringUtils.isNotEmpty( deprecationWarning ) )
            {
                w.startElement( "td" );

                w.writeText( deprecationWarning );

                w.endElement();
            }

            w.endElement();
        }

        w.endElement();

        w.endElement();
    }
}