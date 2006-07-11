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
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * @todo add example usage tag that can be shown in the doco
 */
public class PluginXdocGenerator
    implements Generator
{

    public void execute( File destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException
    {
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

        writeGoalAttributes( mojoDescriptor, w );

        writeGoalParameterTable( mojoDescriptor, w );

        w.endElement(); // section

        w.endElement(); // body

        w.endElement(); // document
    }

    private void writeGoalAttributes( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        w.startElement( "p" );
        w.writeMarkup( "<b>Mojo Attributes</b>:" );
        w.startElement( "ul" );

        String value = mojoDescriptor.getDeprecated();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( "This plugin goal has been deprecated: " + value + "" );
            w.endElement(); //li
        }

        if ( mojoDescriptor.isProjectRequired() )
        {
            w.startElement( "li" );
            w.writeMarkup( "Requires a Maven 2.0 project to execute." );
            w.endElement(); //li
        }

        if ( mojoDescriptor.isAggregator() )
        {
            w.startElement( "li" );
            w.writeMarkup( "Executes as an aggregator plugin." );
            w.endElement(); //li
        }

        if ( mojoDescriptor.isDirectInvocationOnly() )
        {
            w.startElement( "li" );
            w.writeMarkup( "Executes by direct invocation only." );
            w.endElement(); //li
        }

        value = mojoDescriptor.isDependencyResolutionRequired();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( "Requires dependency resolution of artifacts in scope: <code>" + value +
                "</code>" );
            w.endElement(); //li
        }

        value = mojoDescriptor.getPhase();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( "Automatically executes within the lifecycle phase: <code>" + value + "</code>" );
            w.endElement(); //li
        }

        value = mojoDescriptor.getExecutePhase();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( "Invokes the execution of the lifecycle phase <code>" + value +
                "</code> prior to executing itself." );
            w.endElement(); //li
        }

        value = mojoDescriptor.getExecuteGoal();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( "Invokes the execution of this plugin's goal <code>" + value +
                "</code> prior to executing itself." );
            w.endElement(); //li
        }

        value = mojoDescriptor.getExecuteLifecycle();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( "Executes in its own lifecycle: <code>" + value + "</code>" );
            w.endElement(); //li
        }

        if ( mojoDescriptor.isOnlineRequired() )
        {
            w.startElement( "li" );
            w.writeMarkup( "Requires that mvn runs in online mode." );
            w.endElement(); //li
        }

        if ( !mojoDescriptor.isInheritedByDefault() )
        {
            w.startElement( "li" );
            w.writeMarkup( "Is NOT inherited by default in multi-project builds." );
            w.endElement(); //li
        }

        w.endElement();//ul
        w.endElement();//p
    }

    private void writeGoalParameterTable( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        List parameterList = mojoDescriptor.getParameters();

        //remove components and read-only parameters
        List list = filterParameters( parameterList );

        if ( list != null && list.size() > 0 )
        {
            writeParameterSummary( list, w );

            writeParameterDetails( list, w );
        }
    }

    private List filterParameters( List parameterList )
    {
        List filtered = new ArrayList();

        for ( Iterator parameters = parameterList.iterator(); parameters.hasNext(); )
        {
            Parameter parameter = (Parameter) parameters.next();

            if ( parameter.isEditable() )
            {
                String expression = parameter.getExpression();

                if ( expression == null || !expression.startsWith( "${component." ) )
                {
                    filtered.add( parameter );
                }
            }
        }

        return filtered;
    }

    private void writeParameterDetails( List parameterList, XMLWriter w )
    {
        w.startElement( "subsection" );
        w.addAttribute( "name", "Parameter Details" );

        for( Iterator parameters = parameterList.iterator(); parameters.hasNext(); )
        {
            Parameter parameter = (Parameter) parameters.next();

            w.startElement( "p" );
            w.writeMarkup( "<b><a name=\"" + parameter.getName() + "\">" + parameter.getName() + "</a></b>" );
            w.endElement(); //p

            String description = parameter.getDescription();
            if ( StringUtils.isEmpty( description ) )
            {
                description = "No Description.";
            }
            w.startElement( "p" );
            w.writeMarkup( description );
            w.endElement(); //p

            w.startElement( "ul" );

            writeDetail( "Type", parameter.getType(), w );

            writeDetail( "Since", parameter.getSince(), w );

            if ( parameter.isRequired() )
            {
                writeDetail( "Required", "Yes", w );
            }
            else
            {
                writeDetail( "Required", "No", w );
            }

            writeDetail( "Expression", parameter.getExpression(), w );

            writeDetail( "Default", parameter.getDefaultValue(), w );

            w.endElement();//ul

            if ( parameters.hasNext() )
            {
                w.writeMarkup( "<hr/>" );
            }
        }

        w.endElement();
    }

    private void writeDetail( String param, String value, XMLWriter w )
    {
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( "<b>" + param + "</b>: <code>" );
            w.writeText( value );
            w.writeMarkup( "</code>" );
            w.endElement(); //li
        }
    }

    private void writeParameterSummary( List parameterList, XMLWriter w )
    {
        List requiredParams = getParametersByRequired( true, parameterList );
        if ( requiredParams.size() > 0 )
        {
            writeParameterList( "Required Parameters", requiredParams, w );
        }

        List optionalParams = getParametersByRequired( false, parameterList );
        if ( optionalParams.size() > 0 )
        {
            writeParameterList( "Optional Parameters", optionalParams, w );
        }
    }

    private void writeParameterList( String title, List parameterList, XMLWriter w )
    {
        w.startElement( "subsection" );
        w.addAttribute( "name", title );

        w.startElement( "table" );

        w.startElement( "tr" );
        w.startElement( "th" );
        w.writeText( "Name" );
        w.endElement();//th
        w.startElement( "th" );
        w.writeText( "Type" );
        w.endElement();//th
        w.startElement( "th" );
        w.writeText( "Description" );
        w.endElement();//th
        w.endElement();//tr

        for( Iterator parameters = parameterList.iterator(); parameters.hasNext(); )
        {
            Parameter parameter = (Parameter) parameters.next();

            w.startElement( "tr" );
            w.startElement( "td" );
            w.writeMarkup( "<b><a href=\"#" + parameter.getName() + "\">" + parameter.getName() + "</a></b>");
            w.endElement();//td
            w.startElement( "td" );
            int index = parameter.getType().lastIndexOf( "." );
            w.writeMarkup( "<code>" + parameter.getType().substring( index + 1 ) + "</code>" );
            w.endElement();//td
            w.startElement( "td" );
            String description = parameter.getDescription();
            if ( StringUtils.isEmpty( description ) )
            {
                description = "No description.";
            }
            w.writeMarkup( description );

            if ( StringUtils.isNotEmpty( parameter.getDefaultValue() ) )
            {
                w.writeMarkup( " Default value is <code>" );
                w.writeText( parameter.getDefaultValue() );
                w.writeMarkup( "</code>." );
            }
            w.endElement();//td
            w.endElement(); //tr
        }

        w.endElement();//table
        w.endElement();//section
    }

    private List getParametersByRequired( boolean required, List parameterList )
    {
        List list = new ArrayList();

        for ( Iterator parameters = parameterList.iterator(); parameters.hasNext(); )
        {
            Parameter parameter = (Parameter) parameters.next();

            if ( parameter.isRequired() == required )
            {
                list.add( parameter );
            }
        }

        return list;
    }
}

