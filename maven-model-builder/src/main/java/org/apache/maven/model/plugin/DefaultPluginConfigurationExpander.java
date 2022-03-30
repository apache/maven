package org.apache.maven.model.plugin;

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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.api.xml.Dom;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Handles expansion of general build plugin configuration into individual executions.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultPluginConfigurationExpander
    implements PluginConfigurationExpander
{

    @Override
    public Model expandPluginConfiguration( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        Build build = model.getBuild();

        if ( build != null )
        {
            Build.Builder builder = Build.newBuilder( build );

            builder.plugins( expand( build.getPlugins() ) );

            PluginManagement pluginManagement = build.getPluginManagement();

            if ( pluginManagement != null )
            {
                builder.pluginManagement( pluginManagement.withPlugins( expand( pluginManagement.getPlugins() ) ) );
            }

            return model.withBuild( builder.build() );
        }

        return model;
    }

    private List<Plugin> expand( List<Plugin> plugins )
    {
        List<Plugin> newPlugins = new ArrayList<>( plugins.size() );

        for ( Plugin plugin : plugins )
        {
            Dom parentDom = plugin.getConfiguration();

            if ( parentDom != null )
            {
                List<PluginExecution> executions = new ArrayList<>( plugin.getExecutions().size() );
                for ( PluginExecution execution : plugin.getExecutions() )
                {
                    Dom childDom = execution.getConfiguration();
                    if ( childDom != null )
                    {
                        childDom.merge( parentDom );
                    }
                    else
                    {
                        childDom = parentDom.clone();
                    }
                    executions.add( execution.withConfiguration(
                            childDom != null ? childDom.merge( parentDom ) : parentDom ) );
                }
                plugin = plugin.withExecutions( executions );
            }

            newPlugins.add( plugin );
        }

        return newPlugins;
    }

}
