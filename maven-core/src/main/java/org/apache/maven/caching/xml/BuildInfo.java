package org.apache.maven.caching.xml;

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

import com.google.common.collect.Iterables;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.caching.ProjectUtils;
import org.apache.maven.caching.checksum.MavenProjectInput;
import org.apache.maven.caching.hash.HashAlgorithm;
import org.apache.maven.caching.xml.domain.ArtifactType;
import org.apache.maven.caching.xml.domain.BuildInfoType;
import org.apache.maven.caching.xml.domain.CompletedExecutionType;
import org.apache.maven.caching.xml.domain.DigestItemType;
import org.apache.maven.caching.xml.domain.ProjectsInputInfoType;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.apache.maven.caching.ProjectUtils.isLaterPhase;
import static org.apache.maven.caching.ProjectUtils.mojoExecutionKey;

/**
 * BuildInfo
 */
public class BuildInfo
{

    final BuildInfoType dto;
    CacheSource source;

    public BuildInfo( List<String> goals,
                      ArtifactType artifact,
                      List<ArtifactType> attachedArtifacts,
                      ProjectsInputInfoType projectsInputInfo,
                      List<CompletedExecutionType> completedExecutions,
                      String hashAlgorithm )
    {
        this.dto = new BuildInfoType();
        this.dto.setCacheImplementationVersion( MavenProjectInput.CACHE_IMPLMENTATION_VERSION );
        this.dto.setBuildTime( new Date() );
        try
        {
            this.dto.setBuildServer( InetAddress.getLocalHost().getCanonicalHostName() );
        }
        catch ( UnknownHostException ignore )
        {
            this.dto.setBuildServer( "unknown" );
        }
        this.dto.setHashFunction( hashAlgorithm );
        this.dto.setArtifact( artifact );
        this.dto.setGoals( goals );
        this.dto.setAttachedArtifacts( attachedArtifacts );
        this.dto.setExecutions( completedExecutions );
        this.dto.setProjectsInputInfo( projectsInputInfo );
        this.source = CacheSource.BUILD;
    }

    public CacheSource getSource()
    {
        return source;
    }

    public BuildInfo( BuildInfoType buildInfo, CacheSource source )
    {
        this.dto = buildInfo;
        this.source = source;
    }

    public static List<ArtifactType> createAttachedArtifacts( List<Artifact> artifacts,
                                                              HashAlgorithm algorithm ) throws IOException
    {
        List<ArtifactType> attachedArtifacts = new ArrayList<>();
        for ( Artifact artifact : artifacts )
        {
            final ArtifactType dto = DtoUtils.createDto( artifact );
            if ( artifact.getFile() != null )
            {
                dto.setFileHash( algorithm.hash( artifact.getFile().toPath() ) );
            }
            attachedArtifacts.add( dto );
        }
        return attachedArtifacts;
    }

    public boolean isAllExecutionsPresent( List<MojoExecution> mojos, Logger logger )
    {
        for ( MojoExecution mojo : mojos )
        {
            // TODO for strict check we might want exact match
            if ( !hasCompletedExecution( mojoExecutionKey( mojo ) ) )
            {
                logger.error( "Build mojo is not cached: " + mojo );
                return false;
            }
        }
        return true;
    }

    private boolean hasCompletedExecution( String mojoExecutionKey )
    {
        final List<CompletedExecutionType> completedExecutions = dto.getExecutions();
        if ( dto.getExecutions() != null )
        {
            for ( CompletedExecutionType completedExecution : completedExecutions )
            {
                if ( StringUtils.equals( completedExecution.getExecutionKey(), mojoExecutionKey ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "BuildInfo{" + "dto=" + dto + '}';
    }

    public CompletedExecutionType findMojoExecutionInfo( MojoExecution mojoExecution )
    {

        if ( dto.getExecutions() == null )
        {
            return null;
        }

        final List<CompletedExecutionType> executions = dto.getExecutions();
        for ( CompletedExecutionType execution : executions )
        {
            if ( StringUtils.equals( execution.getExecutionKey(), mojoExecutionKey( mojoExecution ) ) )
            {
                return execution;
            }
        }
        return null;
    }

    public String getCacheImplementationVersion()
    {
        return dto.getCacheImplementationVersion();
    }

    public ArtifactType getArtifact()
    {
        return dto.getArtifact();
    }

    public List<ArtifactType> getAttachedArtifacts()
    {
        if ( dto.getAttachedArtifacts() != null )
        {
            return dto.getAttachedArtifacts();
        }
        return Collections.emptyList();
    }

    public BuildInfoType getDto()
    {
        return dto;
    }

    public String getHighestCompletedGoal()
    {
        return Iterables.getLast( dto.getGoals() );
    }

    public List<MojoExecution> getCachedSegment( List<MojoExecution> mojoExecutions )
    {
        List<MojoExecution> list = new ArrayList<>();
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            if ( !isLaterPhase( mojoExecution.getLifecyclePhase(), "post-clean" ) )
            {
                continue;
            }
            if ( isLaterPhase( mojoExecution.getLifecyclePhase(), getHighestCompletedGoal() ) )
            {
                break;
            }
            list.add( mojoExecution );
        }
        return list;

    }

    public List<MojoExecution> getPostCachedSegment( List<MojoExecution> mojoExecutions )
    {
        List<MojoExecution> list = new ArrayList<>();
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            if ( isLaterPhase( mojoExecution.getLifecyclePhase(), getHighestCompletedGoal() ) )
            {
                list.add( mojoExecution );
            }
        }
        return list;
    }

    public DigestItemType findArtifact( Dependency dependency )
    {

        if ( ProjectUtils.isPom( dependency ) )
        {
            throw new IllegalArgumentException( "Pom dependencies should not be treated as artifacts: " + dependency );
        }
        List<ArtifactType> artifacts = new ArrayList<>( getAttachedArtifacts() );
        artifacts.add( getArtifact() );
        for ( ArtifactType artifact : artifacts )
        {
            if ( isEquals( dependency, artifact ) )
            {
                return DtoUtils.createdDigestedByProjectChecksum( artifact, dto.getProjectsInputInfo().getChecksum() );
            }
        }
        return null;
    }

    private boolean isEquals( Dependency dependency, ArtifactType artifact )
    {
        return Objects.equals( dependency.getGroupId(), artifact.getArtifactId() ) && Objects.equals(
                dependency.getArtifactId(), artifact.getArtifactId() ) && Objects.equals( dependency.getType(),
                artifact.getType() ) && Objects.equals( dependency.getClassifier(), artifact.getClassifier() );
    }
}
