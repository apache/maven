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

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.converter.ModelUtils;
import org.apache.maven.model.converter.ProjectConverterException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Properties;

/**
 * @author Fabrizio Giustina
 * @author Dennis Lundberg
 * @version $Id$
 */
public abstract class AbstractPluginConfigurationConverter implements PluginConfigurationConverter
{
    public static final String TYPE_BUILD_PLUGIN = "build plugin";
    public static final String TYPE_REPORT_PLUGIN = "report plugin";

    public abstract String getArtifactId();

    public String getGroupId()
    {
        return "org.apache.maven.plugins";
    }

    public abstract String getType();

    /**
     * Add a child element to the configuration.
     *
     * @param configuration     The configuration to add the element to
     * @param projectProperties The M1 properties
     * @param mavenOneProperty  The name of the Maven 1 property to convert
     * @param mavenTwoElement   The name of the Maven 2 configuration element
     */
    protected void addConfigurationChild( Xpp3Dom configuration, Properties projectProperties, String mavenOneProperty,
                                          String mavenTwoElement )
    {
        String value = projectProperties.getProperty( mavenOneProperty );
        addConfigurationChild( configuration, mavenTwoElement, value );
    }

    /**
     * Add a child element to the configuration.
     *
     * @param configuration   The configuration to add the element to
     * @param mavenTwoElement The name of the Maven 2 configuration element
     * @param value           Set the value of the element to this
     */
    protected void addConfigurationChild( Xpp3Dom configuration, String mavenTwoElement, String value )
    {
        if ( value != null )
        {
            Xpp3Dom child = new Xpp3Dom( mavenTwoElement );
            child.setValue( value );
            configuration.addChild( child );
        }
    }

    public void convertConfiguration( Model v4Model, org.apache.maven.model.v3_0_0.Model v3Model,
                                      Properties projectProperties )
        throws ProjectConverterException
    {
        boolean addPlugin = false;

        Xpp3Dom configuration = new Xpp3Dom( "configuration" );

        buildConfiguration( configuration, v3Model, projectProperties );

        if ( configuration.getChildCount() > 0 )
        {
            if ( TYPE_BUILD_PLUGIN.equals( getType() ) )
            {
                Plugin plugin = ModelUtils.findBuildPlugin( v4Model, getGroupId(), getArtifactId() );
                if ( plugin == null )
                {
                    addPlugin = true;
                    plugin = new Plugin();
                    plugin.setGroupId( getGroupId() );
                    plugin.setArtifactId( getArtifactId() );
                }

                plugin.setConfiguration( configuration );

                if ( addPlugin )
                {
                    if ( v4Model.getBuild() == null )
                    {
                        v4Model.setBuild( new Build() );
                    }
                    v4Model.getBuild().addPlugin( plugin );
                }
            }
            else if ( TYPE_REPORT_PLUGIN.equals( getType() ) )
            {
                ReportPlugin plugin = ModelUtils.findReportPlugin( v4Model, getGroupId(), getArtifactId() );
                if ( plugin == null )
                {
                    addPlugin = true;
                    plugin = new ReportPlugin();
                    plugin.setGroupId( getGroupId() );
                    plugin.setArtifactId( getArtifactId() );
                }

                plugin.setConfiguration( configuration );

                if ( addPlugin )
                {
                    if ( v4Model.getReporting() == null )
                    {
                        v4Model.setReporting( new Reporting() );
                    }
                    v4Model.getReporting().addPlugin( plugin );
                }
            }
        }
    }

    protected abstract void buildConfiguration( Xpp3Dom configuration, org.apache.maven.model.v3_0_0.Model v3Model,
                                                Properties projectProperties )
        throws ProjectConverterException;
}
