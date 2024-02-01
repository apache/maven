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
package org.apache.maven.repository.internal.reader;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.*;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.sisu.Priority;

/**
 * The core source, that populates all elements Maven needs.
 *
 * @since TBD
 */
@Singleton
@Named(CorePluginArtifactDescriptorReaderSource.NAME)
@Priority(15)
public class CorePluginArtifactDescriptorReaderSource extends ArtifactDescriptorReaderSourceSupport {
    public static final String NAME = "corePlugin";

    @Override
    public void populateResult(RepositorySystemSession session, ArtifactDescriptorResult result, Model model) {
        String context = result.getRequest().getRequestContext();
        String type = result.getRequest().getArtifact().getProperty(ArtifactProperties.TYPE, "");
        // augment PluginDependenciesResolver with Maven Core constituents
        if (context != null && context.startsWith("plugin") && type.equals("maven-plugin")) {
            // TODO: inject current runtime Maven BOM into result
        }
    }
}
