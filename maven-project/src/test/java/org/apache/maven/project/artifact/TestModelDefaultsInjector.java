package org.apache.maven.project.artifact;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.ModelUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author jdcasey Created on Feb 1, 2005
 */
public class TestModelDefaultsInjector
{
    public void injectDefaults( Model model )
    {
        injectDependencyDefaults( model.getDependencies(), model.getDependencyManagement() );
        if ( model.getBuild() != null )
        {
            injectPluginDefaults( model.getBuild(), model.getBuild().getPluginManagement() );
        }
    }

    private static void injectPluginDefaults( Build build, PluginManagement pluginManagement )
    {
        if ( pluginManagement == null )
        {
            // nothing to inject.
            return ;
        }
        
        List buildPlugins = build.getPlugins();
        
        if ( buildPlugins != null && !buildPlugins.isEmpty() )
        {
            Map pmPlugins = pluginManagement.getPluginsAsMap();
            
            if ( pmPlugins != null && !pmPlugins.isEmpty() )
            {
                for ( Iterator it = buildPlugins.iterator(); it.hasNext(); )
                {
                    Plugin buildPlugin = (Plugin) it.next();
                    
                    Plugin pmPlugin = (Plugin) pmPlugins.get( buildPlugin.getKey() );
                    
                    if ( pmPlugin != null )
                    {
                        ModelUtils.mergePluginDefinitions( buildPlugin, pmPlugin, false );
                    }
                }
            }
        }
        
    }

    private static void injectDependencyDefaults( List dependencies, DependencyManagement dependencyManagement )
    {
        if ( dependencyManagement != null )
        {
            // a given project's dependencies should be smaller than the
            // group-defined defaults set...
            // in other words, the project's deps will probably be a subset of
            // those specified in defaults.
            Map depsMap = new TreeMap();
            for ( Iterator it = dependencies.iterator(); it.hasNext(); )
            {
                Dependency dep = (Dependency) it.next();
                depsMap.put( dep.getManagementKey(), dep );
            }

            List managedDependencies = dependencyManagement.getDependencies();

            for ( Iterator it = managedDependencies.iterator(); it.hasNext(); )
            {
                Dependency def = (Dependency) it.next();
                String key = def.getManagementKey();

                Dependency dep = (Dependency) depsMap.get( key );
                if ( dep != null )
                {
                    mergeDependencyWithDefaults( dep, def );
                }
            }
        }
    }

    private static void mergeDependencyWithDefaults( Dependency dep, Dependency def )
    {
        if ( dep.getScope() == null && def.getScope() != null )
        {
            dep.setScope( def.getScope() );
            dep.setSystemPath( def.getSystemPath() );
        }

        if ( dep.getVersion() == null && def.getVersion() != null )
        {
            dep.setVersion( def.getVersion() );
        }
        
        if ( dep.getClassifier() == null && def.getClassifier() != null )
        {
            dep.setClassifier( def.getClassifier() );
        }
        
        if ( dep.getType() == null && def.getType() != null )
        {
            dep.setType( def.getType() );
        }
        
        List exclusions = dep.getExclusions();
        if ( exclusions == null || exclusions.isEmpty() )
        {
            dep.setExclusions( def.getExclusions() );
        }
    }
}