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
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 * get validation directives to help users in IDEs.
 */
public class PluginDescriptorGenerator
    implements Generator
{
    public void execute( File destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        File f = new File( destinationDirectory, "plugin.xml" );

        if ( !f.getParentFile().exists() )
        {
            f.getParentFile().mkdirs();
        }

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( f );

            XMLWriter w = new PrettyPrintXMLWriter( writer );

            w.startElement( "plugin" );

            element( w, "groupId", pluginDescriptor.getGroupId() );

            element( w, "artifactId", pluginDescriptor.getArtifactId() );

            element( w, "version", pluginDescriptor.getVersion() );

            element( w, "goalPrefix", pluginDescriptor.getGoalPrefix() );
            
            element( w, "isolatedRealm", "" + pluginDescriptor.isIsolatedRealm() );

            element( w, "inheritedByDefault", "" + pluginDescriptor.isInheritedByDefault() );
            
            w.startElement( "mojos" );

            if ( pluginDescriptor.getMojos() != null )
            {
                for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
                {
                    MojoDescriptor descriptor = (MojoDescriptor) it.next();
                    processMojoDescriptor( descriptor, w );
                }
            }

            w.endElement();

            PluginUtils.writeDependencies( w, pluginDescriptor );

            w.endElement();

            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    protected void processMojoDescriptor( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        w.startElement( "mojo" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "goal" );

        w.writeText( mojoDescriptor.getGoal() );

        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.isDependencyResolutionRequired() != null )
        {
            element( w, "requiresDependencyResolution", mojoDescriptor.isDependencyResolutionRequired() );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        element( w, "requiresProject", "" + mojoDescriptor.isProjectRequired() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        element( w, "requiresOnline", "" + mojoDescriptor.isOnlineRequired() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        element( w, "inheritedByDefault", "" + mojoDescriptor.isInheritedByDefault() );

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

        if ( mojoDescriptor.getExecutePhase() != null )
        {
            element( w, "executePhase", mojoDescriptor.getExecutePhase() );
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

        if ( mojoDescriptor.getComponentConfigurator() != null )
        {
            w.startElement( "configurator" );

            w.writeText( mojoDescriptor.getComponentConfigurator() );

            w.endElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.getComponentComposer() != null )
        {
            w.startElement( "composer" );

            w.writeText( mojoDescriptor.getComponentComposer() );

            w.endElement();
        }

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
        
        Map configuration = new HashMap();
        
        if( parameters != null )
        {
            for ( int j = 0; j < parameters.size(); j++ )
            {
                Parameter parameter = (Parameter) parameters.get( j );

                String expression = parameter.getExpression();

                if ( StringUtils.isNotEmpty( expression ) && expression.startsWith( "${component." ) )
                {
                    // treat it as a component...a requirement, in other words.

                    requirements.add( parameter );
                }
                else
                {
                    // treat it as a normal parameter.

                    w.startElement( "parameter" );

                    element( w, "name", parameter.getName() );

                    if ( parameter.getAlias() != null )
                    {
                        element( w, "alias", parameter.getAlias() );
                    }

                    element( w, "type", parameter.getType() );

                    if ( parameter.getDeprecated() != null )
                    {
                        element( w, "deprecated", parameter.getDeprecated() );
                    }

                    element( w, "required", Boolean.toString( parameter.isRequired() ) );

                    element( w, "editable", Boolean.toString( parameter.isEditable() ) );

                    element( w, "description", parameter.getDescription() );

                    if ( expression != null && expression.length() > 0 )
                    {
                        configuration.put( parameter, expression );
                    }

                    w.endElement();
                }

            }
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

                String type = parameter.getType();
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

                // remove "component." plus expression delimiters
                String expression = requirement.getExpression();
                String role = expression.substring( "${component.".length(), expression.length() - 1 );

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
