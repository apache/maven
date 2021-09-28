package org.apache.maven.execution;

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

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.PluginsMetadataInfoProvider;
import org.eclipse.aether.artifact.Artifact;

/**
 * Default implementation of {@link PluginsMetadataInfoProvider}.
 */
@Named
@Singleton
public class MavenSessionPluginsMetadataInfoProvider
    extends AbstractMavenLifecycleParticipant
    implements PluginsMetadataInfoProvider
{
    private final AtomicReference<MavenSession> mavenSessionRef;

    public MavenSessionPluginsMetadataInfoProvider()
    {
        this.mavenSessionRef = new AtomicReference<>( null );
    }

    @Override
    public void afterSessionStart( final MavenSession session )
    {
        this.mavenSessionRef.set( session );
    }

    @Override
    public void afterSessionEnd( final MavenSession session )
    {
        this.mavenSessionRef.set( null );
    }

    @Override
    public PluginInfo getPluginInfo( final Artifact artifact )
    {
        MavenSession mavenSession = mavenSessionRef.get();
        if ( mavenSession != null )
        {
            MavenProject currentProject = mavenSession.getCurrentProject();
            if ( "maven-plugin".equals( currentProject.getPackaging() ) )
            {
                String pluginPrefix = "blah";

                return new PluginInfo()
                {
                    @Override
                    public String getPluginGroupId()
                    {
                        return artifact.getGroupId();
                    }

                    @Override
                    public String getPluginArtifactId()
                    {
                        return artifact.getArtifactId();
                    }

                    @Override
                    public String getPluginPrefix()
                    {
                        return pluginPrefix;
                    }

                    @Override
                    public String getPluginName()
                    {
                        return currentProject.getName();
                    }
                };
            }
        }

        return null;
    }
}
