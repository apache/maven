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

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * @author Jason van Zyl
 */
public interface PluginManager
{
    // - find the plugin [extension point: any client may wish to do whatever they choose]
    // - load the plugin into a classloader [extension point: we want to take them from a repository, some may take from disk or whatever]
    // - configure the plugin [extension point] -- actually this should happen before the plugin manager gets anything i.e. do whatever you want to get the information
    // - execute the plugin    
        
    // Use more meaningful exceptions
    // plugin not found
    // plugin resolution exception
    // plugin configuration can't be parsed -- and this may be a result of client insertion of configuration
    // plugin component deps have a cycle -- this should be prevented for the most part but client code may inject an override component which itself has a cycle
    // igorf: Way too many declared exceptions!
    PluginDescriptor loadPlugin( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, InvalidPluginDescriptorException;

    // igorf: Way too many declared exceptions!
    MojoDescriptor getMojoDescriptor( String groupId, String artifactId, String version, String goal, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, InvalidPluginDescriptorException;

    // igorf: Way too many declared exceptions!
    MojoDescriptor getMojoDescriptor( Plugin plugin, String goal, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, InvalidPluginDescriptorException;

    // Why do we have a plugin execution exception as well?
    // igorf: Way too many declared exceptions!
    void executeMojo( MavenSession session, MojoExecution execution )
        throws MojoFailureException, MojoExecutionException, PluginConfigurationException, PluginExecutionException, PluginManagerException;

    ClassRealm getPluginRealm( MavenSession session, PluginDescriptor pluginDescriptor ) throws PluginManagerException;
}