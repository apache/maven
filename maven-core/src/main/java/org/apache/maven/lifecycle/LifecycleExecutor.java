package org.apache.maven.lifecycle;

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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.CycleDetectedInPluginGraphException;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;

/**
 * @author Jason van  Zyl
 */
public interface LifecycleExecutor
{    
    List<String> getLifecyclePhases();
       
    /**
     * Calculate the list of {@link org.apache.maven.plugin.descriptor.MojoDescriptor} objects to run for the selected lifecycle phase.
     * 
     * @param phase
     * @param session
     * @return
     * @throws InvalidPluginDescriptorException 
     * @throws LifecycleExecutionException
     */
    List<MojoExecution> calculateBuildPlan( MavenSession session, String... tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException;
        
    // For a given project packaging find all the plugins that are bound to any registered
    // lifecycles. The project builder needs to now what default plugin information needs to be
    // merged into POM being built. Once the POM builder has this plugin information, versions can be assigned
    // by the POM builder because they will have to be defined in plugin management. Once this is done then it
    // can be passed back so that the default configuraiton information can be populated.
    //
    // We need to know the specific version so that we can lookup the right version of the plugin descriptor
    // which tells us what the default configuration is.
    //
    Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging );

    // Given a set of {@link org.apache.maven.Plugin} objects where the GAV is set we can lookup the plugin
    // descriptor and populate the default configuration.
    //
    void populateDefaultConfigurationForPlugins( Collection<Plugin> plugins, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws LifecycleExecutionException;
    
    void execute( MavenSession session );
}
