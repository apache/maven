package org.apache.maven.project;

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

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Profile;

@Deprecated
public interface ProjectBuilderConfiguration
    extends ProjectBuildingRequest
{
    ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository );
    
    ArtifactRepository getLocalRepository();

    ProjectBuilderConfiguration setRemoteRepositories( List<ArtifactRepository> remoteRepositories );

    List<ArtifactRepository> getRemoteRepositories();

    ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties );

    Properties getSystemProperties();

    void setProject(MavenProject mavenProject);

    MavenProject getProject();
        
    ProjectBuilderConfiguration setProcessPlugins( boolean processPlugins );
    
    boolean isProcessPlugins();

    // Profiles
    
    /**
     * Set any active profiles that the {@link ProjectBuilder} should consider while constructing
     * a {@link MavenProject}.
     */
    void setActiveProfileIds( List<String> activeProfileIds );
        
    List<String> getActiveProfileIds();

    void setInactiveProfileIds( List<String> inactiveProfileIds );

    List<String> getInactiveProfileIds();
    
    /**
     * Add a {@link org.apache.maven.model.Profile} that has come from an external source. This may be from a custom configuration
     * like the MavenCLI settings.xml file, or from a custom dialog in an IDE integration like M2Eclipse.
     * @param profile
     */
    void addProfile( Profile profile );
    
    void setProfiles( List<Profile> profiles );
    
    List<Profile> getProfiles();
}
