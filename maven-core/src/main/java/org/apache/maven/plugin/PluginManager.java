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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.discovery.ComponentDiscoverer;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;

/**
 * @author Jason van Zyl
 */
public interface PluginManager
    extends ComponentDiscoverer, ComponentDiscoveryListener
{
    // - find the plugin [extension point: any client may wish to do whatever they choose]
    // - load the plugin into a classloader [extension point: we want to take them from a repository, some may take from disk or whatever]
    // - configure the plugin [extension point]
    // - execute the plugin    
    
    Plugin findPluginForPrefix( String prefix, MavenProject project );
    
    PluginDescriptor loadPlugin( Plugin plugin, MavenProject project, ArtifactRepository localRepository )
        throws PluginLoaderException;
    
    MojoDescriptor getMojoDescriptor( Plugin plugin, String goal, MavenProject project, ArtifactRepository localRepository )
        throws PluginLoaderException;
    
    void executeMojo( MavenSession session, MojoExecution execution )
        throws MojoFailureException, PluginExecutionException, PluginConfigurationException;       
}