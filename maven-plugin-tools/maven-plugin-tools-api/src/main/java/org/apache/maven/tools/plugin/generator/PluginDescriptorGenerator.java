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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 * get validation directives to help users in IDEs.
 */
public class PluginDescriptorGenerator
    implements Generator
{
    public void execute( String destinationDirectory, Set mavenMojoDescriptors, MavenProject project )
        throws Exception
    {
        File f = new File( destinationDirectory, "plugin.xml" );

        if ( !f.getParentFile().exists() )
        {
            f.getParentFile().mkdirs();
        }

        FileWriter writer = new FileWriter( f );

        XMLWriter w = new PrettyPrintXMLWriter( writer );

        w.startElement( "plugin" );

        element( w, "groupId", project.getGroupId() );

        element( w, "artifactId", project.getArtifactId() );

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

        if ( mojoDescriptor.getRequiresDependencyResolution() != null )
        {
            element( w, "requiresDependencyResolution", mojoDescriptor.getRequiresDependencyResolution() );
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

        Collection requirements = new ArrayList();
        Map configuration = new HashMap( parameters.size() );
        for ( int j = 0; j < parameters.size(); j++ )
        {
            Parameter parameter = (Parameter) parameters.get( j );

            w.startElement( "parameter" );

            element( w, "name", parameter.getName() );

            element( w, "type", parameter.getType() );

            if ( parameter.getDeprecated() != null )
            {
                element( w, "deprecated", parameter.getDeprecated() );
            }

            element( w, "validator", parameter.getValidator() );

            String value = null;
            if ( parameter.getExpression().startsWith( "#component." ) ||
                parameter.getExpression().startsWith( "${component." ) )
            {
                requirements.add( parameter );
            }
            else
            {
                element( w, "required", Boolean.toString( parameter.isRequired() ) );

                value = parameter.getExpression();
            }

            element( w, "description", parameter.getDescription() );

            if ( value == null || value.length() == 0 )
            {
                value = parameter.getDefaultValue();
            }

            if ( value != null && value.length() > 0 )
            {
                configuration.put( parameter, value );
            }

            w.endElement();
        }

        w.endElement();

        // ----------------------------------------------------------------------
        // Coinfiguration
        // ----------------------------------------------------------------------

        if ( !configuration.isEmpty() )
        {
            w.startElement( "configuration" );

            for ( Iterator i = configuration.keySet().iterator(); i.hasNext(); )
            {
                Parameter parameter = (Parameter) i.next();

                w.startElement( parameter.getName() );

                String type = convertType( parameter.getType() );
                if ( type != null )
                {
                    w.addAttribute( "implementation", type );
                }

                w.writeText( (String) configuration.get( parameter ) );

                w.endElement();
            }

            w.endElement();
        }

        // ----------------------------------------------------------------------
        // Requirements
        // ----------------------------------------------------------------------

        if ( !requirements.isEmpty() )
        {
            w.startElement( "requirements" );

            for ( Iterator i = requirements.iterator(); i.hasNext(); )
            {
                Parameter requirement = (Parameter) i.next();

                w.startElement( "requirement" );

                String role;
                // remove "component." plus expression delimiters
                String expression = requirement.getExpression();
                if ( expression.startsWith( "${" ) )
                {
                    role = expression.substring( "${component.".length(), expression.length() - 1 );
                }
                else
                {
                    role = expression.substring( "#component.".length() );
                }
                element( w, "role", role );

                element( w, "field-name", requirement.getName() );

                w.endElement();
            }

            w.endElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.endElement();
    }

    /**
     * @param type
     * @return
     * @deprecated - should force proper class specification
     */
    private static String convertType( String type )
    {
        if ( "String".equals( type ) )
        {
            return "java.lang.String";
        }
        else if ( "File".equals( type ) )
        {
            return "java.io.File";
        }
        else if ( "List".equals( type ) )
        {
            return "java.util.List";
        }
        else if ( "".equals( type ) )
        {
            return null;
        }
        else
        {
            return type;
        }
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