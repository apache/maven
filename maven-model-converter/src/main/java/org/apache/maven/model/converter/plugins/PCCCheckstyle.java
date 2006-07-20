package org.apache.maven.model.converter.plugins;

/*
 * Copyright 2006 The Apache Software Foundation.
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
 * A <code>PluginConfigurationConverter</code> for the maven-checkstyle-plugin.
 *
 * @plexus.component role="org.apache.maven.model.converter.plugins.PluginConfigurationConverter" role-hint="checkstyle"
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PCCCheckstyle
    extends AbstractPluginConfigurationConverter
{
    /**
     * @see AbstractPluginConfigurationConverter#getArtifactId()
     */
    public String getArtifactId()
    {
        return "maven-checkstyle-plugin";
    }

    public String getType()
    {
        return TYPE_REPORT_PLUGIN;
    }

    protected void buildConfiguration( Xpp3Dom configuration, org.apache.maven.model.v3_0_0.Model v3Model,
                                       Properties projectProperties )
        throws ProjectConverterException
    {
        addConfigurationChild( configuration, projectProperties, "maven.checkstyle.cache.file", "cacheFile" );

        String format = projectProperties.getProperty( "maven.checkstyle.format" );
        if ( format != null )
        {
            String mavenTwoformat = null;
            if ( format.equals( "avalon" ) )
            {
                mavenTwoformat = "config/avalon_checks.xml";
            }
            else if ( format.equals( "turbine" ) )
            {
                mavenTwoformat = "config/turbine_checks.xml";
            }
            else if ( format.equals( "sun" ) )
            {
                mavenTwoformat = "config/sun_checks.xml";
            }
            if ( mavenTwoformat != null )
            {
                addConfigurationChild( configuration, "configLocation", mavenTwoformat );
            }
        }
        else
        {
            String propertiesURL = projectProperties.getProperty( "maven.checkstyle.propertiesURL" );
            if ( propertiesURL != null )
            {
                addConfigurationChild( configuration, "configLocation", propertiesURL );
            }
            else
            {
                addConfigurationChild( configuration, projectProperties, "maven.checkstyle.properties",
                                       "configLocation" );
            }
        }

        addConfigurationChild( configuration, projectProperties, "maven.checkstyle.excludes", "excludes" );

        addConfigurationChild( configuration, projectProperties, "maven.checkstyle.fail.on.violation", "failsOnError" );

        addConfigurationChild( configuration, projectProperties, "maven.checkstyle.header.file", "headerLocation" );

        addConfigurationChild( configuration, projectProperties, "maven.checkstyle.includes", "includes" );

        String outputText = projectProperties.getProperty( "maven.checkstyle.output.txt" );
        if ( outputText != null )
        {
            addConfigurationChild( configuration, "outputFile", outputText );
            addConfigurationChild( configuration, "outputFileFormat", "plain" );
        }
        else
        {
            String outputXml = projectProperties.getProperty( "maven.checkstyle.output.xml" );
            if ( outputXml != null )
            {
                addConfigurationChild( configuration, "outputFile", outputXml );
                addConfigurationChild( configuration, "outputFileFormat", "xml" );
            }
        }

        addConfigurationChild( configuration, projectProperties, "maven.checkstyle.suppressions.file",
                               "suppressionsLocation" );

        addConfigurationChild( configuration, projectProperties, "maven.checkstyle.usefile", "useFile" );
    }
}
