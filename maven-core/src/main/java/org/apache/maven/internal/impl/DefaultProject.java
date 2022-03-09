package org.apache.maven.internal.impl;

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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Project;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

public class DefaultProject implements Project
{

    private final DefaultSession session;
    private final MavenProject project;

    public DefaultProject( DefaultSession session, MavenProject project )
    {
        this.session = session;
        this.project = project;
    }

    public DefaultSession getSession()
    {
        return session;
    }

    public MavenProject getProject()
    {
        return project;
    }

    @Nonnull
    @Override
    public String getGroupId()
    {
        return project.getGroupId();
    }

    @Nonnull
    @Override
    public String getArtifactId()
    {
        return project.getArtifactId();
    }

    @Nonnull
    @Override
    public String getVersion()
    {
        return project.getVersion();
    }

    @Nonnull
    @Override
    public Artifact getArtifact()
    {
        return session.getArtifact( RepositoryUtils.toArtifact( project.getArtifact() ) );
    }

    @Nonnull
    @Override
    public String getPackaging()
    {
        return project.getPackaging();
    }

    @Nonnull
    @Override
    public Model getModel()
    {
        return project.getModel();
    }

    @Nonnull
    @Override
    public Path getPomPath()
    {
        File file = project.getFile();
        return file != null ? file.toPath() : null;
    }

    @Nonnull
    @Override
    public List<Dependency> getDependencies()
    {
        return new MappedList<>( project.getDependencies(), this::toDependency );
    }

    @Nonnull
    @Override
    public List<Dependency> getManagedDependencies()
    {
        DependencyManagement dependencyManagement = project.getModel().getDependencyManagement();
        if ( dependencyManagement != null )
        {
            return new MappedList<>( dependencyManagement.getDependencies(), this::toDependency );
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isExecutionRoot()
    {
        return project.isExecutionRoot();
    }

    private Dependency toDependency( org.apache.maven.model.Dependency dependency )
    {
        return new Dependency()
        {
            @Nonnull
            @Override
            public Artifact getArtifact()
            {
                return session.createArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                        dependency.getVersion(), dependency.getType() );
            }

            @Nonnull
            @Override
            public String getScope()
            {
                return dependency.getScope();
            }

            @Override
            public Boolean getOptional()
            {
                return dependency.isOptional();
            }

            @Nonnull
            @Override
            public Collection<Exclusion> getExclusions()
            {
                return new MappedCollection<>( dependency.getExclusions(), this::toExclusion );
            }

            private Exclusion toExclusion( org.apache.maven.model.Exclusion exclusion )
            {
                return new Exclusion()
                {
                    @Nullable
                    @Override
                    public String getGroupId()
                    {
                        return exclusion.getGroupId();
                    }

                    @Nullable
                    @Override
                    public String getArtifactId()
                    {
                        return exclusion.getArtifactId();
                    }
                };
            }
        };
    }
}
