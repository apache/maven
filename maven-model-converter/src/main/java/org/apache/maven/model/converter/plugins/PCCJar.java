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
 * A <code>PluginConfigurationConverter</code> for the maven-jar-plugin.
 *
 * @plexus.component role="org.apache.maven.model.converter.plugins.PluginConfigurationConverter" role-hint="jar"
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PCCJar
    extends AbstractPluginConfigurationConverter
{
    /**
     * @see org.apache.maven.model.converter.plugins.AbstractPluginConfigurationConverter#getArtifactId()
     */
    public String getArtifactId()
    {
        return "maven-jar-plugin";
    }

    public String getType()
    {
        return TYPE_BUILD_PLUGIN;
    }

    protected void buildConfiguration( Xpp3Dom configuration, org.apache.maven.model.v3_0_0.Model v3Model,
                                       Properties projectProperties )
        throws ProjectConverterException
    {
        Xpp3Dom archive = new Xpp3Dom( "archive" );
        addConfigurationChild( archive, projectProperties, "maven.jar.compress", "compress" );
        addConfigurationChild( archive, projectProperties, "maven.jar.index", "index" );

        Xpp3Dom manifest = new Xpp3Dom( "manifest" );
        addConfigurationChild( manifest, projectProperties, "maven.jar.manifest.classpath.add", "addClasspath" );
        addConfigurationChild( manifest, projectProperties, "maven.jar.manifest.extensions.add", "addExtensions" );
        if ( manifest.getChildCount() > 0 )
        {
            archive.addChild( manifest );
        }
        addConfigurationChild( manifest, projectProperties, "maven.jar.mainclass", "mainClass" );

        String manifestEntriesProperty = projectProperties.getProperty( "maven.jar.manifest.attributes.list" );
        if ( manifestEntriesProperty != null )
        {
            Xpp3Dom manifestEntries = new Xpp3Dom( "manifestEntries" );

            // Loop through property and add values to manifestEntries
            StringTokenizer tokenizer = new StringTokenizer( manifestEntriesProperty, "," );
            while ( tokenizer.hasMoreTokens() )
            {
                String attribute = tokenizer.nextToken();
                addConfigurationChild( manifestEntries, projectProperties, "maven.jar.manifest.attribute." + attribute,
                                       attribute );
            }

            if ( manifestEntries.getChildCount() > 0 )
            {
                archive.addChild( manifestEntries );
            }
        }

        addConfigurationChild( archive, projectProperties, "maven.jar.manifest", "manifestFile" );

        if ( archive.getChildCount() > 0 )
        {
            configuration.addChild( archive );
        }

        addConfigurationChild( configuration, projectProperties, "maven.jar.final.name", "finalName" );
    }
}
