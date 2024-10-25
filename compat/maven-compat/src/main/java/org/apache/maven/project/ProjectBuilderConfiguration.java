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
package org.apache.maven.project;

import java.util.Date;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;

/**
 * @deprecated use {@link ProjectBuildingRequest} instead
 */
@Deprecated
public interface ProjectBuilderConfiguration {

    ArtifactRepository getLocalRepository();

    ProfileManager getGlobalProfileManager();

    Properties getUserProperties();

    Properties getExecutionProperties();

    ProjectBuilderConfiguration setGlobalProfileManager(ProfileManager globalProfileManager);

    ProjectBuilderConfiguration setLocalRepository(ArtifactRepository localRepository);

    ProjectBuilderConfiguration setUserProperties(Properties userProperties);

    ProjectBuilderConfiguration setExecutionProperties(Properties executionProperties);

    Date getBuildStartTime();

    ProjectBuilderConfiguration setBuildStartTime(Date buildStartTime);
}
