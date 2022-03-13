package org.apache.maven.execution.infoproviders;

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

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.PluginsMetadataInfoProvider;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadata;
import org.eclipse.aether.artifact.Artifact;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation of {@link PluginsMetadataInfoProvider}.
 */
@Named
@Singleton
public class DefaultPluginsMetadataInfoProvider
    implements PluginsMetadataInfoProvider
{
    private final Provider<MavenSession> mavenSessionProvider;

    @Inject
    public DefaultPluginsMetadataInfoProvider( final Provider<MavenSession> mavenSessionProvider )
    {
        this.mavenSessionProvider = requireNonNull( mavenSessionProvider );
    }

    @Override
    public PluginInfo getPluginInfo( final Artifact artifact )
    {
        MavenSession mavenSession = mavenSessionProvider.get();
        if ( mavenSession != null )
        {
            MavenProject mavenProject = searchForProject( mavenSession, artifact );
            if ( mavenProject != null && "maven-plugin".equals( mavenProject.getPackaging() ) )
            {
                Plugin plugin = searchForPluginGroupLevelRepositoryMetadata( mavenProject );

                if ( plugin != null )
                {
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
                            return plugin.getPrefix();
                        }

                        @Override
                        public String getPluginName()
                        {
                            return plugin.getName();
                        }
                    };
                }
            }
        }

        return null;
    }

    private MavenProject searchForProject( MavenSession mavenSession, Artifact artifact )
    {
        for ( MavenProject mavenProject : mavenSession.getProjects() )
        {
            if ( mavenProject.getArtifact() != null
                && Objects.equals( mavenProject.getGroupId(), artifact.getGroupId() )
                && Objects.equals( mavenProject.getArtifactId(), artifact.getArtifactId() ) )
            {
                return mavenProject;
            }
        }
        return null;
    }

    private Plugin searchForPluginGroupLevelRepositoryMetadata( MavenProject mavenProject )
    {
        org.apache.maven.artifact.Artifact projectArtifact = mavenProject.getArtifact();
        for ( ArtifactMetadata artifactMetadata : projectArtifact.getMetadataList() )
        {
            if ( artifactMetadata instanceof RepositoryMetadata )
            {
                RepositoryMetadata repositoryMetadata = (RepositoryMetadata) artifactMetadata;
                Metadata metadata = repositoryMetadata.getMetadata();

                for ( Plugin plugin : metadata.getPlugins() )
                {
                    if ( Objects.equals( plugin.getArtifactId(), mavenProject.getArtifactId() ) )
                    {
                        return plugin;
                    }
                }
            }
        }
        return null;
    }
}
