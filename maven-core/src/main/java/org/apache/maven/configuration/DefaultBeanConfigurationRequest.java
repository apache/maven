package org.apache.maven.configuration;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.utils.Precondition;
import org.codehaus.plexus.util.StringUtils;

/**
 * A basic bean configuration request.
 *
 * @author Benjamin Bentmann
 */
public class DefaultBeanConfigurationRequest
    implements BeanConfigurationRequest
{

    private Object bean;

    private Object configuration;

    private String configurationElement;

    private ClassLoader classLoader;

    private BeanConfigurationValuePreprocessor valuePreprocessor;

    private BeanConfigurationPathTranslator pathTranslator;

    public Object getBean()
    {
        return bean;
    }

    public DefaultBeanConfigurationRequest setBean( Object bean )
    {
        this.bean = bean;
        return this;
    }

    public Object getConfiguration()
    {
        return configuration;
    }

    public String getConfigurationElement()
    {
        return configurationElement;
    }

    public DefaultBeanConfigurationRequest setConfiguration( Object configuration )
    {
        return setConfiguration( configuration, null );
    }

    public DefaultBeanConfigurationRequest setConfiguration( Object configuration, String element )
    {
        this.configuration = configuration;
        this.configurationElement = element;
        return this;
    }

    /**
     * Sets the configuration to the configuration taken from the specified build plugin in the POM. First, the build
     * plugins will be searched for the specified plugin, if that fails, the plugin management section will be searched.
     *
     * @param model The POM to extract the plugin configuration from, may be {@code null}.
     * @param pluginGroupId The group id of the plugin whose configuration should be used, must not be {@code null} or
     *            empty.
     * @param pluginArtifactId The artifact id of the plugin whose configuration should be used, must not be
     *            {@code null} or empty.
     * @param pluginExecutionId The id of a plugin execution whose configuration should be used, may be {@code null} or
     *            empty to use the general plugin configuration.
     * @return This request for chaining, never {@code null}.
     */
    public DefaultBeanConfigurationRequest setConfiguration( Model model, String pluginGroupId,
                                                             String pluginArtifactId, String pluginExecutionId )
    {
        Plugin plugin = findPlugin( model, pluginGroupId, pluginArtifactId );
        if ( plugin != null )
        {
            if ( StringUtils.isNotEmpty( pluginExecutionId ) )
            {
                for ( PluginExecution execution : plugin.getExecutions() )
                {
                    if ( pluginExecutionId.equals( execution.getId() ) )
                    {
                        setConfiguration( execution.getConfiguration() );
                        break;
                    }
                }
            }
            else
            {
                setConfiguration( plugin.getConfiguration() );
            }
        }
        return this;
    }

    private Plugin findPlugin( Model model, String groupId, String artifactId )
    {
        Precondition.notBlank( groupId, "groupId can neither be null, empty nor blank" );
        Precondition.notBlank( artifactId, "artifactId can neither be null, empty nor blank" );

        if ( model != null )
        {
            Build build = model.getBuild();
            if ( build != null )
            {
                for ( Plugin plugin : build.getPlugins() )
                {
                    if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                    {
                        return plugin;
                    }
                }

                PluginManagement mgmt = build.getPluginManagement();
                if ( mgmt != null )
                {
                    for ( Plugin plugin : mgmt.getPlugins() )
                    {
                        if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                        {
                            return plugin;
                        }
                    }
                }
            }
        }

        return null;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public DefaultBeanConfigurationRequest setClassLoader( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
        return this;
    }

    public BeanConfigurationValuePreprocessor getValuePreprocessor()
    {
        return valuePreprocessor;
    }

    public DefaultBeanConfigurationRequest setValuePreprocessor( BeanConfigurationValuePreprocessor valuePreprocessor )
    {
        this.valuePreprocessor = valuePreprocessor;
        return this;
    }

    public BeanConfigurationPathTranslator getPathTranslator()
    {
        return pathTranslator;
    }

    public DefaultBeanConfigurationRequest setPathTranslator( BeanConfigurationPathTranslator pathTranslator )
    {
        this.pathTranslator = pathTranslator;
        return this;
    }

}
