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

/**
 * @todo add example usage tag that can be shown in the doco
 */
public class PluginXdocGenerator
    implements Generator
{
    public void execute( File destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        writeOverview( destinationDirectory, pluginDescriptor );

        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();
            processMojoDescriptor( descriptor, destinationDirectory );
        }
    }

    protected void processMojoDescriptor( MojoDescriptor mojoDescriptor, File destinationDirectory )
        throws IOException
    {
        FileWriter writer = null;
        try
        {
            writer = new FileWriter( new File( destinationDirectory, getMojoFilename( mojoDescriptor, "xml" ) ) );

            writeBody( writer, mojoDescriptor );

            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private String getMojoFilename( MojoDescriptor mojo, String ext )
    {
        return mojo.getGoal() + "-mojo." + ext;
    }

    private void writeOverview( File destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        FileWriter writer = null;
        try
        {
            writer = new FileWriter( new File( destinationDirectory, "index.xml" ) );

            writeOverview( writer, pluginDescriptor );

            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private void writeOverview( FileWriter writer, PluginDescriptor pluginDescriptor )
    {
        XMLWriter w = new PrettyPrintXMLWriter( writer );

        w.startElement( "document" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "properties" );

        w.startElement( "title" );

        // TODO: need a friendly name for a plugin
        w.writeText( pluginDescriptor.getArtifactId() + " - Overview" );

        w.endElement();

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "body" );

        w.startElement( "section" );

        // TODO: need a friendly name for a plugin
        w.addAttribute( "name", pluginDescriptor.getArtifactId() );

        // TODO: description of plugin, examples?

        w.startElement( "p" );

        w.writeText( "Goals available: " );

        w.endElement();

        writeGoalTable( pluginDescriptor, w );

        w.endElement();

        w.endElement();
    }

    private void writeGoalTable( PluginDescriptor pluginDescriptor, XMLWriter w )
    {
        w.startElement( "table" );

        w.startElement( "tr" );

        w.startElement( "th" );

        w.writeText( "Goal" );

        w.endElement();

        w.startElement( "th" );

        w.writeText( "Description" );

        w.endElement();

        w.endElement();

        List mojos = pluginDescriptor.getMojos();

        for ( Iterator i = mojos.iterator(); i.hasNext(); )
        {
            MojoDescriptor mojo = (MojoDescriptor) i.next();

            w.startElement( "tr" );

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            String paramName = mojo.getFullGoalName();

            w.startElement( "a" );

            w.addAttribute( "href", getMojoFilename( mojo, "html" ) );

            w.startElement( "code" );

            w.writeText( paramName );

            w.endElement();

            w.endElement();

            w.endElement();

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            if ( StringUtils.isNotEmpty( mojo.getDescription() ) )
            {
                w.writeMarkup( mojo.getDescription() );
            }
            else
            {
                w.writeText( "No description." );
            }

            String deprecationWarning = mojo.getDeprecated();
            if ( deprecationWarning != null )
            {
                w.writeMarkup( "<br/><b>Deprecated:</b> " );
                w.writeMarkup( deprecationWarning );
                if ( deprecationWarning.length() == 0 )
                {
                    w.writeText( "No reason given." );
                }

                w.endElement();
            }

            w.endElement();

            w.endElement();
        }

        w.endElement();

        w.endElement();
    }

    private void writeBody( FileWriter writer, MojoDescriptor mojoDescriptor )
    {
        XMLWriter w = new PrettyPrintXMLWriter( writer );

        w.startElement( "document" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "properties" );

        w.startElement( "title" );

        // TODO: need a friendly name for a plugin
        w.writeText( mojoDescriptor.getPluginDescriptor().getArtifactId() + " - " + mojoDescriptor.getFullGoalName() );

        w.endElement(); // title

        w.endElement(); // properties

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "body" );

        w.startElement( "section" );

        w.addAttribute( "name", mojoDescriptor.getFullGoalName() );

        w.startElement( "p" );

        if ( mojoDescriptor.getDescription() != null )
        {
            w.writeMarkup( mojoDescriptor.getDescription() );
        }
        else
        {
            w.writeText( "No description." );
        }

        w.endElement(); // p

        w.startElement( "p" );

        w.writeText( "Parameters for the goal: " );

        w.endElement(); // p

        writeGoalParameterTable( mojoDescriptor, w );

        w.endElement(); // section

        w.endElement(); // body

        w.endElement(); // document
    }

    private void writeGoalParameterTable( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        w.startElement( "table" );

        w.startElement( "tr" );

        w.startElement( "th" );

        w.writeText( "Parameter" );

        w.endElement(); // th

        w.startElement( "th" );

        w.writeText( "Type" );

        w.endElement(); // th

        w.startElement( "th" );

        w.writeText( "Expression" );

        w.endElement(); // th

        w.startElement( "th" );

        w.writeText( "Default Value" );

        w.endElement(); // th

        w.startElement( "th" );

        w.writeText( "Description" );

        w.endElement(); // th

        w.endElement(); // tr

        List parameters = mojoDescriptor.getParameters();

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

            w.startElement( "code" );

            w.writeText( paramName );

            w.endElement(); // code

            if ( !parameter.isRequired() )
            {
                w.writeMarkup( " <i>(Optional)</i>" );
            }

            if ( parameter.getExpression() != null && parameter.getExpression().startsWith( "${component." ) )
            {
                w.writeMarkup( " <i>(Discovered)</i>" );
            }

            w.endElement(); // td

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            w.startElement( "code" );

            w.addAttribute( "title", parameter.getType() );

            int index = parameter.getType().lastIndexOf( "." );
            if ( index >= 0 )
            {
                w.writeText( parameter.getType().substring( index + 1 ) );
            }
            else
            {
                w.writeText( parameter.getType() );
            }

            w.endElement(); // code

            w.endElement(); // td

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            w.startElement( "code" );

            if ( StringUtils.isNotEmpty( parameter.getExpression() ) &&
                !parameter.getExpression().startsWith( "${component." ) )
            {
                w.writeText( parameter.getExpression() );
            }
            else
            {
                w.writeText( "-" );
            }

            w.endElement(); // code

            w.endElement(); // td

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            w.startElement( "code" );

            if ( StringUtils.isNotEmpty( parameter.getDefaultValue() ) )
            {
                w.writeText( parameter.getDefaultValue() );
            }
            else
            {
                w.writeText( "-" );
            }

            w.endElement(); // code

            w.endElement(); // td

            // ----------------------------------------------------------------------
            //
            // ----------------------------------------------------------------------

            w.startElement( "td" );

            if ( StringUtils.isNotEmpty( parameter.getDescription() ) )
            {
                w.writeMarkup( parameter.getDescription() );
            }
            else
            {
                w.writeText( "No description." );
            }

            String deprecationWarning = parameter.getDeprecated();
            if ( deprecationWarning != null )
            {
                w.writeMarkup( "<br/><b>Deprecated:</b> " );
                w.writeMarkup( deprecationWarning );
                if ( deprecationWarning.length() == 0 )
                {
                    w.writeText( "No reason given." );
                }
            }

            w.endElement(); // td

            w.endElement(); // tr
        }

        w.endElement(); // table
    }
}