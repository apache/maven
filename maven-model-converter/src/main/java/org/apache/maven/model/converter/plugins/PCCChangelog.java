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
 * A <code>PluginConfigurationConverter</code> for the maven-changelog-plugin.
 *
 * @plexus.component role="org.apache.maven.model.converter.plugins.PluginConfigurationConverter" role-hint="changelog"
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PCCChangelog
    extends AbstractPluginConfigurationConverter
{
    /**
     * @see AbstractPluginConfigurationConverter#getArtifactId()
     */
    public String getArtifactId()
    {
        return "maven-changelog-plugin";
    }

    public String getType()
    {
        return TYPE_REPORT_PLUGIN;
    }

    protected void buildConfiguration( Xpp3Dom configuration, org.apache.maven.model.v3_0_0.Model v3Model,
                                       Properties projectProperties )
        throws ProjectConverterException
    {
        addConfigurationChild( configuration, projectProperties, "maven.changelog.commentFormat", "commentFormat" );

        addConfigurationChild( configuration, projectProperties, "maven.changelog.dateformat", "dateFormat" );

        addConfigurationChild( configuration, projectProperties, "maven.changelog.svn.baseurl", "tagBase" );

        addConfigurationChild( configuration, projectProperties, "maven.changelog.type", "type" );

        String type = projectProperties.getProperty( "maven.changelog.type" );
        if ( type != null )
        {
            if ( "date".equals( type ) )
            {
                Xpp3Dom dates = new Xpp3Dom( "dates" );
                addConfigurationChild( dates, projectProperties, "maven.changelog.date", "date" );
                if ( dates.getChildCount() > 0 )
                {
                    configuration.addChild( dates );
                }
            }
            else if ( "range".equals( type ) )
            {
                addConfigurationChild( configuration, projectProperties, "maven.changelog.range", "range" );
            }
            else if ( "tag".equals( type ) )
            {
                Xpp3Dom tags = new Xpp3Dom( "tags" );
                addConfigurationChild( tags, projectProperties, "maven.changelog.tag", "tag" );
                if ( tags.getChildCount() > 0 )
                {
                    configuration.addChild( tags );
                }
            }
        }

        // Only add this if we have any other configuration for the changelog-plugin
        if ( configuration.getChildCount() > 0 )
        {
            // The Maven 1 plugin uses the same outputencoding as the generated documentation.
            addConfigurationChild( configuration, projectProperties, "maven.docs.outputencoding", "outputEncoding" );
        }
    }
}
