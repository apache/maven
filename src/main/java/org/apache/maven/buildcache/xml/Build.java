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
package org.apache.maven.buildcache.xml;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.maven.buildcache.CacheUtils;
import org.apache.maven.buildcache.checksum.MavenProjectInput;
import org.apache.maven.buildcache.hash.HashAlgorithm;
import org.apache.maven.buildcache.xml.build.Artifact;
import org.apache.maven.buildcache.xml.build.CompletedExecution;
import org.apache.maven.buildcache.xml.build.DigestItem;
import org.apache.maven.buildcache.xml.build.ProjectsInputInfo;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;

import static org.apache.maven.buildcache.CacheUtils.mojoExecutionKey;

/**
 * Build
 */
public class Build
{

    final org.apache.maven.buildcache.xml.build.Build dto;
    CacheSource source;
    volatile Map<String, CompletedExecution> execMap;

    public Build( List<String> goals,
            Artifact artifact,
            List<Artifact> attachedArtifacts,
            ProjectsInputInfo projectsInputInfo,
            List<CompletedExecution> completedExecutions,
            String hashAlgorithm )
    {
        this.dto = new org.apache.maven.buildcache.xml.build.Build();
        this.dto.setCacheImplementationVersion( MavenProjectInput.CACHE_IMPLEMENTATION_VERSION );
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

    public Build( org.apache.maven.buildcache.xml.build.Build dto, CacheSource source )
    {
        this.dto = dto;
        this.source = source;
    }

    public static List<Artifact> createAttachedArtifacts( List<org.apache.maven.artifact.Artifact> artifacts,
            HashAlgorithm algorithm ) throws IOException
    {
        List<Artifact> attachedArtifacts = new ArrayList<>();
        for ( org.apache.maven.artifact.Artifact artifact : artifacts )
        {
            final Artifact dto = DtoUtils.createDto( artifact );
            if ( artifact.getFile() != null )
            {
                dto.setFileHash( algorithm.hash( artifact.getFile().toPath() ) );
            }
            attachedArtifacts.add( dto );
        }
        return attachedArtifacts;
    }

    public List<MojoExecution> getMissingExecutions( List<MojoExecution> mojos )
    {
        return mojos.stream()
                .filter( mojo -> !hasCompletedExecution( mojoExecutionKey( mojo ) ) )
                .collect( Collectors.toList() );
    }

    private boolean hasCompletedExecution( String mojoExecutionKey )
    {
        return getExecutionMap().containsKey( mojoExecutionKey );
    }

    @Override
    public String toString()
    {
        return "Build{" + "dto=" + dto + '}';
    }

    public CompletedExecution findMojoExecutionInfo( MojoExecution mojoExecution )
    {
        return getExecutionMap().get( mojoExecutionKey( mojoExecution ) );
    }

    public String getCacheImplementationVersion()
    {
        return dto.getCacheImplementationVersion();
    }

    public Artifact getArtifact()
    {
        return dto.getArtifact();
    }

    public List<Artifact> getAttachedArtifacts()
    {
        if ( dto.getAttachedArtifacts() != null )
        {
            return dto.getAttachedArtifacts();
        }
        return Collections.emptyList();
    }

    public org.apache.maven.buildcache.xml.build.Build getDto()
    {
        return dto;
    }

    public String getHighestCompletedGoal()
    {
        return CacheUtils.getLast( dto.getGoals() );
    }

    public DigestItem findArtifact( Dependency dependency )
    {
        if ( CacheUtils.isPom( dependency ) )
        {
            throw new IllegalArgumentException( "Pom dependencies should not be treated as artifacts: " + dependency );
        }
        List<Artifact> artifacts = new ArrayList<>( getAttachedArtifacts() );
        artifacts.add( getArtifact() );
        for ( Artifact artifact : artifacts )
        {
            if ( isEquals( dependency, artifact ) )
            {
                return DtoUtils.createdDigestedByProjectChecksum( artifact, dto.getProjectsInputInfo().getChecksum() );
            }
        }
        return null;
    }

    private boolean isEquals( Dependency dependency, Artifact artifact )
    {
        return Objects.equals( dependency.getGroupId(), artifact.getGroupId() )
                && Objects.equals( dependency.getArtifactId(), artifact.getArtifactId() )
                && Objects.equals( dependency.getType(), artifact.getType() )
                && Objects.equals( dependency.getClassifier(), artifact.getClassifier() );
    }

    private Map<String, CompletedExecution> getExecutionMap()
    {
        if ( execMap != null )
        {
            return execMap;
        }
        List<CompletedExecution> executionsList = dto.getExecutions();
        if ( executionsList == null || executionsList.isEmpty() )
        {
            execMap = Collections.emptyMap();
            return execMap;
        }
        execMap = executionsList.stream()
                .collect( Collectors.toMap( CompletedExecution::getExecutionKey, v -> v ) );
        return execMap;
    }
}
