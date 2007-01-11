package org.apache.maven.plugin.version;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

public interface PluginVersionManager
{
    String ROLE = PluginVersionManager.class.getName();

    String resolvePluginVersion( String groupId, String artifactId, MavenProject project, Settings settings,
                                 ArtifactRepository localRepository )
        throws PluginVersionResolutionException, InvalidPluginException, PluginVersionNotFoundException;

    String resolveReportPluginVersion( String groupId, String artifactId, MavenProject project, Settings settings,
                                       ArtifactRepository localRepository )
        throws PluginVersionResolutionException, InvalidPluginException, PluginVersionNotFoundException;

}
