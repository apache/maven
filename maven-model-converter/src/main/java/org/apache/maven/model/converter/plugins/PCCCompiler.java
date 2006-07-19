package org.apache.maven.model.converter.plugins;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.model.converter.ProjectConverterException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Properties;

/**
 * @plexus.component role="org.apache.maven.model.converter.plugins.PluginConfigurationConverter" role-hint="compiler"
 *
 * @author Fabrizio Giustina
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PCCCompiler
    extends AbstractPluginConfigurationConverter
{
    /**
     * @see org.apache.maven.model.converter.plugins.AbstractPluginConfigurationConverter#getArtifactId()
     */
    public String getArtifactId()
    {
        return "maven-compiler-plugin";
    }

    public String getType()
    {
        return TYPE_BUILD_PLUGIN;
    }

    protected void addOnOffConfigurationChild( Xpp3Dom configuration, Properties projectProperties,
                                               String mavenOneProperty, String mavenTwoElement )
        throws ProjectConverterException
    {
        String value = projectProperties.getProperty( mavenOneProperty );
        if ( value != null )
        {
            addConfigurationChild( configuration, mavenTwoElement, PropertyUtils.convertOnOffToBoolean( value ) );
        }
    }

    protected void buildConfiguration( Xpp3Dom configuration, org.apache.maven.model.v3_0_0.Model v3Model,
                                       Properties projectProperties )
        throws ProjectConverterException
    {
        addOnOffConfigurationChild( configuration, projectProperties, "maven.compile.debug", "debug" );

        addConfigurationChild( configuration, projectProperties, "maven.compile.encoding", "encoding" );

        addConfigurationChild( configuration, projectProperties, "maven.compile.executable", "executable" );

        String fork = projectProperties.getProperty( "maven.compile.fork" );
        if ( fork != null )
        {
            addConfigurationChild( configuration, "fork", PropertyUtils.convertYesNoToBoolean( fork ) );
        }

        addConfigurationChild( configuration, projectProperties, "maven.compile.memoryMaximumSize", "maxmem" );

        addConfigurationChild( configuration, projectProperties, "maven.compile.memoryInitialSize", "meminitial" );

        addOnOffConfigurationChild( configuration, projectProperties, "maven.compile.optimize", "optimize" );

        addOnOffConfigurationChild( configuration, projectProperties, "maven.compile.deprecation", "showDeprecation" );

        String nowarn = projectProperties.getProperty( "maven.compile.nowarn" );
        if ( nowarn != null )
        {
            String convertedNowarn = PropertyUtils.convertOnOffToBoolean( nowarn );
            if ( convertedNowarn != null )
            {
                String showWarnings = PropertyUtils.invertBoolean( convertedNowarn );
                addConfigurationChild( configuration, "showWarnings", showWarnings );
            }
        }

        addConfigurationChild( configuration, projectProperties, "maven.compile.source", "source" );

        addConfigurationChild( configuration, projectProperties, "maven.compile.target", "target" );

        String value = projectProperties.getProperty( "maven.compile.verbose" );
        if ( value != null )
        {
            addConfigurationChild( configuration, "verbose", PropertyUtils.convertYesNoToBoolean( value ) );
        }
    }
}
