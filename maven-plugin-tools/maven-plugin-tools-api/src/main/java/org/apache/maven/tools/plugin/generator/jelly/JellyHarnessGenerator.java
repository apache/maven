package org.apache.maven.tools.plugin.generator.jelly;

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
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @todo use the descriptions in the descriptor for the javadoc pushed into the
 * source code.
 * @todo write plugin.properties (as a place holder, we don't technially need
 * it)
 * @todo convert POM or just strip out the dependencies to create a project.xml
 * that will serve as the trigger to download dependencies.
 */
public class JellyHarnessGenerator
    implements Generator
{
    protected String getClassName( MojoDescriptor pluginDescriptor )
    {
        return pluginDescriptor.getImplementation() + "Bean";
    }

    public void execute( String destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        FileWriter writer = null;
        PrettyPrintXMLWriter w;
        try
        {
            writer = new FileWriter( new File( destinationDirectory, "plugin.jelly" ) );

            w = new PrettyPrintXMLWriter( writer );

            writeProjectFile( w, pluginDescriptor );

            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }

        // ----------------------------------------------------------------------
        // project.xml
        // ----------------------------------------------------------------------

        writer = null;
        try
        {
            writer = new FileWriter( new File( destinationDirectory, "project.xml" ) );

            w = new PrettyPrintXMLWriter( writer );

            writeProjectFile( w, pluginDescriptor );

            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private void writePluginFile( PrettyPrintXMLWriter w, String goalPrefix, Set mojoDescriptors, MavenProject project )
    {
        w.startElement( "project" );

        w.addAttribute( "xmlns:j", "jelly:core" );

        w.addAttribute( "xmlns:d", "jelly:define" );

        w.addAttribute( "xmlns:" + goalPrefix, goalPrefix );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "d:taglib" );

        w.addAttribute( "uri", goalPrefix );

        for ( Iterator it = mojoDescriptors.iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();
            processPluginDescriptor( descriptor, w, project );
        }

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        for ( Iterator it = mojoDescriptors.iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();
            writeGoals( descriptor, w );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.endElement();
    }

    private void writeProjectFile( PrettyPrintXMLWriter w, PluginDescriptor pluginDescriptor )
    {
        w.startElement( "project" );

        w.startElement( "dependencies" );

        PluginUtils.writeDependencies( w, pluginDescriptor );

        w.endElement();

        w.endElement();
    }

    protected void processPluginDescriptor( MojoDescriptor mojoDescriptor, XMLWriter w, MavenProject project )
    {
        String goalName = mojoDescriptor.getGoal();

        // ----------------------------------------------------------------------
        // jellybean
        // ----------------------------------------------------------------------
        //
        //   <define:jellybean
        //     name="vdocletBean"
        //     className="org.apache.maven.vdoclet.VDocletBean"
        //     method="execute">
        //   </define:jellybean>
        //
        // ----------------------------------------------------------------------

        w.startElement( "d:jellybean" );

        w.addAttribute( "name", goalName + "Bean" );

        w.addAttribute( "className", getClassName( mojoDescriptor ) );

        w.addAttribute( "method", "execute" );

        w.endElement();

        // ----------------------------------------------------------------------
        // tag
        // ----------------------------------------------------------------------
        //
        // <define:tag name="vdoclet">
        //   <vdoclet:vdocletBean
        //     srcDir="${srcDir}"
        //     destDir="${destDir}"
        //     template="${template}"
        //     outputFile="${outputFile}"
        //     encoding="${encoding}"
        //     jellyContext="${context}"
        //   />
        // </define:tag>
        //
        // ----------------------------------------------------------------------

        w.startElement( "d:tag" );

        w.addAttribute( "name", goalName );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( mojoDescriptor.getFullGoalName() + "Bean" );

        List parameters = mojoDescriptor.getParameters();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

            String paramName = parameter.getAlias();

            if ( StringUtils.isEmpty( paramName ) )
            {
                paramName = parameter.getName();
            }

            w.addAttribute( paramName, "${" + paramName + "}" );
        }

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.endElement();
    }

    private void writeGoals( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        w.startElement( "goal" );

        w.addAttribute( "name", mojoDescriptor.getFullGoalName() );

        if ( mojoDescriptor.getDescription() != null )
        {
            w.addAttribute( "description", mojoDescriptor.getDescription() );
        }

        w.startElement( mojoDescriptor.getFullGoalName() + "Bean" );

        List goalParameters = mojoDescriptor.getParameters();

        for ( int j = 0; j < goalParameters.size(); j++ )
        {
            Parameter p = (Parameter) goalParameters.get( j );

            String expression = p.getExpression();

            int projectIndex = expression.indexOf( "project" );

            if ( projectIndex > 0 )
            {
                expression = expression.substring( 0, projectIndex ) + "pom" +
                    expression.substring( projectIndex + 7 );
            }

            w.addAttribute( p.getName(), expression );
        }

        w.endElement();

        w.endElement();
    }

    protected String capitalise( String str )
    {
        if ( str == null || str.length() == 0 )
        {
            return str;
        }

        return new StringBuffer( str.length() ).append( Character.toTitleCase( str.charAt( 0 ) ) ).append(
            str.substring( 1 ) ).toString();
    }
}