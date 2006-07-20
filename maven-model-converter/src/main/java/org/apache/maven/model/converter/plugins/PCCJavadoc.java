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
import java.util.StringTokenizer;

/**
 * A <code>PluginConfigurationConverter</code> for the maven-javadoc-plugin.
 *
 * @plexus.component role="org.apache.maven.model.converter.plugins.PluginConfigurationConverter" role-hint="javadoc"
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PCCJavadoc
    extends AbstractPluginConfigurationConverter
{
    /**
     * @see org.apache.maven.model.converter.plugins.AbstractPluginConfigurationConverter#getArtifactId()
     */
    public String getArtifactId()
    {
        return "maven-javadoc-plugin";
    }

    public String getType()
    {
        return TYPE_BUILD_PLUGIN;
    }

    protected void buildConfiguration( Xpp3Dom configuration, org.apache.maven.model.v3_0_0.Model v3Model,
                                       Properties projectProperties )
        throws ProjectConverterException
    {
        addConfigurationChild( configuration, projectProperties, "maven.javadoc.additionalparam", "additionalparam" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.author", "author" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.bottom", "bottom" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.destdir", "destDir" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.doclet", "doclet" );
        addConfigurationChild( configuration, projectProperties, "maven.javadoc.docletpath", "docletPath" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.windowtitle", "doctitle" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.excludepackagenames",
                               "excludePackageNames" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.footer", "footer" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.header", "header" );

        String online = projectProperties.getProperty( "maven.javadoc.mode.online" );
        if ( online != null )
        {
            addConfigurationChild( configuration, "isOffline", PropertyUtils.invertBoolean( online ) );
        }

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.links", "links" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.locale", "locale" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.maxmemory", "maxmemory" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.offlineLinks", "offlineLinks" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.overview", "overview" );

        String show = projectProperties.getProperty( "maven.javadoc.private" );
        if ( show != null && Boolean.valueOf( show ).booleanValue() )
        {
            addConfigurationChild( configuration, "show", "private" );
        }
        else
        {
            show = projectProperties.getProperty( "maven.javadoc.package" );
            if ( show != null && Boolean.valueOf( show ).booleanValue() )
            {
                addConfigurationChild( configuration, "show", "package" );
            }
            else
            {
                show = projectProperties.getProperty( "maven.javadoc.public" );
                if ( show != null && Boolean.valueOf( show ).booleanValue() )
                {
                    addConfigurationChild( configuration, "show", "public" );
                }
            }
        }

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.source", "source" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.stylesheet", "stylesheetfile" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.taglets", "taglet" );
        addConfigurationChild( configuration, projectProperties, "maven.javadoc.tagletpath", "tagletpath" );

        String customtags = projectProperties.getProperty( "maven.javadoc.customtags" );
        if ( customtags != null )
        {
            StringTokenizer tokenizer = new StringTokenizer( customtags );
            if ( tokenizer.hasMoreTokens() )
            {
                Xpp3Dom tagsConfiguration = new Xpp3Dom( "tags" );
                while ( tokenizer.hasMoreTokens() )
                {
                    String tag = tokenizer.nextToken();
                    Xpp3Dom tagConfiguration = new Xpp3Dom( "tag" );
                    addConfigurationChild( tagConfiguration, projectProperties, tag + ".description", "head" );
                    addConfigurationChild( tagConfiguration, projectProperties, tag + ".name", "name" );
                    String placement = "";
                    String enabled = projectProperties.getProperty( tag + ".enabled" );
                    if ( !Boolean.valueOf( enabled ).booleanValue() )
                    {
                        placement = "X";
                    }
                    String scope = projectProperties.getProperty( tag + ".scope" );
                    if ( "all".equals( scope ) )
                    {
                        placement += "a";
                    }
                    if ( placement.length() > 0 )
                    {
                        addConfigurationChild( tagConfiguration, "placement", placement );
                    }
                    tagsConfiguration.addChild( tagConfiguration );
                }
                configuration.addChild( tagsConfiguration );
            }
        }

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.use", "use" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.version", "version" );

        addConfigurationChild( configuration, projectProperties, "maven.javadoc.windowtitle", "windowtitle" );

        // Only add these if we have any other configuration for the javadoc-plugin
        if ( configuration.getChildCount() > 0 )
        {
            // The Maven 1 plugin uses the same outputencoding as the generated documentation.
            addConfigurationChild( configuration, projectProperties, "maven.docs.outputencoding", "docencoding" );

            // The Maven 1 plugin uses the same encoding as the compile plugin.
            addConfigurationChild( configuration, projectProperties, "maven.compile.encoding", "encoding" );

            // The Maven 1 plugin uses the same package as the pom.
            addConfigurationChild( configuration, projectProperties, "pom.package", "subpackages" );
        }
    }
}
