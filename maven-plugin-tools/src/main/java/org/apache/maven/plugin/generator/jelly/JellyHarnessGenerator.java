package org.apache.maven.plugin.generator.jelly;

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

import com.thoughtworks.xstream.xml.text.PrettyPrintXMLWriter;
import com.thoughtworks.xstream.xml.XMLWriter;
import com.thoughtworks.xstream.xml.xpp3.Xpp3Dom;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.generator.AbstractGenerator;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * @todo use the descriptions in the descriptor for the javadoc pushed into the source code.
 * @todo write plugin.properties (as a place holder, we don't technially need it)
 * @todo convert POM or just strip out the dependencies to create a project.xml that
 * will serve as the trigger to download dependencies.
 */
public class JellyHarnessGenerator
    extends AbstractGenerator
{
    protected String getClassName( MojoDescriptor pluginDescriptor )
    {
        return pluginDescriptor.getImplementation() + "Bean";
    }

    protected void processPluginDescriptors( MojoDescriptor[] mojoDescriptors, String destinationDirectory, Xpp3Dom pomDom )
        throws Exception
    {
        FileWriter writer = new FileWriter( new File( destinationDirectory, "plugin.jelly" ) );

        PrettyPrintXMLWriter w = new PrettyPrintXMLWriter( writer );

        String pluginId = pluginId( pomDom );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "project" );

        w.addAttribute( "xmlns:j", "jelly:core" );

        w.addAttribute( "xmlns:d", "jelly:define" );

        w.addAttribute( "xmlns:" + pluginId, pluginId );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "d:taglib" );

        w.addAttribute( "uri", pluginId );

        for ( int i = 0; i < mojoDescriptors.length; i++ )
        {
            processPluginDescriptor( mojoDescriptors[i], w, pomDom );
        }

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        for ( int i = 0; i < mojoDescriptors.length; i++ )
        {
            writeGoals( mojoDescriptors[i], w );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        writer.flush();

        writer.close();

        // ----------------------------------------------------------------------
        // project.xml
        // ----------------------------------------------------------------------

        writer = new FileWriter( new File( destinationDirectory, "project.xml" ) );

        w = new PrettyPrintXMLWriter( writer );

        w.startElement( "project" );

        w.startElement( "dependencies" );

        writeDependencies( w, pomDom );

        w.endElement();

        w.endElement();

        writer.flush();

        writer.close();

    }

    protected void processPluginDescriptor( MojoDescriptor mojoDescriptor, XMLWriter w, Xpp3Dom pomDom )
        throws Exception
    {
        String pluginId = pluginId( pomDom );

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

        w.startElement( pluginId + ":" + goalName + "Bean" );

        List parameters = mojoDescriptor.getParameters();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

            w.addAttribute( parameter.getName(), "${" + parameter.getName() + "}" );
        }

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.endElement();
    }

    private void writeGoals( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        String id = mojoDescriptor.getId();

        w.startElement( "goal" );

        w.addAttribute( "name", id + ":" + mojoDescriptor.getGoal() );

        List goalPrereqs = mojoDescriptor.getPrereqs();

        if ( goalPrereqs.size() > 0 )
        {
            StringBuffer prereqs = new StringBuffer();

            for ( int j = 0; j < goalPrereqs.size(); j++ )
            {
                String prereq = (String) goalPrereqs.get( j );

                prereqs.append( prereq );

                if ( j < goalPrereqs.size() - 1 )
                {
                    prereqs.append( "," );
                }
            }

            w.addAttribute( "prereqs", prereqs.toString() );
        }

        if ( mojoDescriptor.getDescription() != null )
        {
            w.addAttribute( "description", mojoDescriptor.getDescription() );
        }

        w.startElement( id + ":" + mojoDescriptor.getGoal() + "Bean" );

        List goalParameters = mojoDescriptor.getParameters();

        for ( int j = 0; j < goalParameters.size(); j++ )
        {
            Parameter p = (Parameter) goalParameters.get( j );

            String expression = p.getExpression();

            int projectIndex = expression.indexOf( "project" );

            if ( projectIndex > 0 )
            {
                expression = expression.substring( 0, projectIndex ) + "pom" + expression.substring( projectIndex + 7 );
            }

            if ( expression.startsWith( "#" ) )
            {
                expression = "${" + expression.substring( 1 ) + "}";
            }

            w.addAttribute( p.getName(), expression );
        }

        w.endElement();

        w.endElement();
    }

    protected void writeDependencies( XMLWriter w, Xpp3Dom pomDom )
    {
        writeDependency( w, "maven", "maven-plugin", "2.0-SNAPSHOT" );

        Xpp3Dom depElement = pomDom.getChild( "dependencies" );

        if ( depElement != null )
        {
            Xpp3Dom[] deps = depElement.getChildren( "dependency" );

            for ( int i = 0; i < deps.length; i++ )
            {
                Xpp3Dom dep = deps[i];

                String groupId = dep.getChild( "artifactId" ).getValue();

                String artifactId = dep.getChild( "groupId" ).getValue();

                String version = dep.getChild( "version" ).getValue();

                writeDependency( w, groupId, artifactId, version );
            }
        }
    }

    protected void writeDependency( XMLWriter w, String groupId, String artifactId, String version )
    {
        w.startElement( "dependency" );

        w.startElement( "groupId" );

        w.writeText( groupId );

        w.endElement();

        w.startElement( "artifactId" );

        w.writeText( artifactId );

        w.endElement();

        w.startElement( "version" );

        w.writeText( version );

        w.endElement();

        w.endElement();
    }

    protected String capitalise( String str )
    {
        if ( str == null || str.length() == 0 )
        {
            return str;
        }

        return new StringBuffer( str.length() )
            .append( Character.toTitleCase( str.charAt( 0 ) ) )
            .append( str.substring( 1 ) )
            .toString();
    }
}