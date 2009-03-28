package org.apache.maven.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * @author Jason van Zyl
 */
public interface PluginManager
{
    // - find the plugin [extension point: any client may wish to do whatever they choose]
    // - load the plugin into a classloader [extension point: we want to take them from a repository, some may take from disk or whatever]
    // - configure the plugin [extension point]
    // - execute the plugin    
    
    Plugin findPluginForPrefix( String prefix, MavenProject project, MavenSession session );
    
    PluginDescriptor loadPlugin( Plugin plugin, MavenProject project, MavenSession session )
        throws PluginLoaderException;
    
    MojoDescriptor getMojoDescriptor( Plugin plugin, String goal, MavenSession session )
        throws PluginLoaderException;
    
    //!!jvz The current project is contained in the session
    void executeMojo( MavenProject project, MojoExecution execution, MavenSession session )
        throws MojoFailureException, PluginExecutionException, PluginConfigurationException;       
}