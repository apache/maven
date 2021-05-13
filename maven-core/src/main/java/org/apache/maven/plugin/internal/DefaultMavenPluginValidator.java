package org.apache.maven.plugin.internal;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * DefaultMavenPluginValidator
 */
@Named
@Singleton
class DefaultMavenPluginValidator
        implements MavenPluginValidator
{

    @Override
    public void validate( Artifact pluginArtifact, PluginDescriptor pluginDescriptor, List<String> errors )
    {
        if ( !pluginArtifact.getGroupId().equals( pluginDescriptor.getGroupId() ) )
        {
            errors.add( "Plugin's descriptor contains the wrong group ID: " + pluginDescriptor.getGroupId() );
        }

        if ( !pluginArtifact.getArtifactId().equals( pluginDescriptor.getArtifactId() ) )
        {
            errors.add( "Plugin's descriptor contains the wrong artifact ID: " + pluginDescriptor.getArtifactId() );
        }

        if ( !pluginArtifact.getBaseVersion().equals( pluginDescriptor.getVersion() ) )
        {
            errors.add( "Plugin's descriptor contains the wrong version: " + pluginDescriptor.getVersion() );
        }
    }
}
