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
package org.apache.maven.caching;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.SessionScoped;
import org.apache.maven.caching.checksum.KeyUtils;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.config.Discovery;
import org.apache.maven.caching.xml.config.MultiModule;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SessionScoped
@Named
public class DefaultMultiModuleSupport implements MultiModuleSupport
{

    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultMultiModuleSupport.class );

    private final ProjectBuilder projectBuilder;
    private final CacheConfig cacheConfig;
    private final MavenSession session;

    private volatile boolean built;
    private volatile Map<String, MavenProject> projectMap;
    private volatile Map<String, MavenProject> sessionProjectMap;

    @Inject
    public DefaultMultiModuleSupport( ProjectBuilder projectBuilder,
            CacheConfig cacheConfig,
            MavenSession session )
    {
        this.projectBuilder = projectBuilder;
        this.cacheConfig = cacheConfig;
        this.session = session;
    }

    @Override
    public boolean isPartOfSession( String groupId, String artifactId, String version )
    {
        return getProjectMap( session )
                .containsKey( KeyUtils.getProjectKey( groupId, artifactId, version ) );
    }

    @Override
    public Optional<MavenProject> tryToResolveProject( String groupId, String artifactId, String version )
    {
        return Optional.ofNullable( getMultiModuleProjectsMap()
                .get( KeyUtils.getProjectKey( groupId, artifactId, version ) ) );
    }

    @Override
    public boolean isPartOfMultiModule( String groupId, String artifactId, String version )
    {
        String projectKey = KeyUtils.getProjectKey( groupId, artifactId, version );
        return getProjectMap( session ).containsKey( projectKey )
                || getMultiModuleProjectsMap().containsKey( projectKey );
    }

    private Map<String, MavenProject> getProjectMap( MavenSession session )
    {
        if ( sessionProjectMap != null )
        {
            return sessionProjectMap;
        }
        sessionProjectMap = session.getProjects().stream().collect( Collectors.toMap(
                KeyUtils::getProjectKey,
                Function.identity() ) );
        return sessionProjectMap;
    }

    private Map<String, MavenProject> getMultiModuleProjectsMap()
    {
        if ( projectMap != null )
        {
            return projectMap;
        }
        return getMultiModuleProjectsMapInner( session );
    }

    private synchronized Map<String, MavenProject> getMultiModuleProjectsMapInner( MavenSession session )
    {
        if ( projectMap != null )
        {
            return projectMap;
        }
        buildModel( session );
        return projectMap;
    }

    private synchronized void buildModel( MavenSession session )
    {
        if ( built )
        {
            return;
        }

        Optional<Discovery> multiModuleDiscovery = Optional.ofNullable( cacheConfig.getMultiModule() )
                .map( MultiModule::getDiscovery );

        //no discovery configuration, use only projects in session
        if ( !multiModuleDiscovery.isPresent() )
        {
            projectMap = buildProjectMap( session.getProjects() );
            return;
        }

        Set<String> scanProfiles = new TreeSet<>(
                multiModuleDiscovery
                        .map( Discovery::getScanProfiles )
                        .orElse( Collections.emptyList() ) );
        MavenProject currentProject = session.getCurrentProject();
        File multiModulePomFile = getMultiModulePomFile( session );

        ProjectBuildingRequest projectBuildingRequest = currentProject.getProjectBuildingRequest();
        boolean profilesMatched = projectBuildingRequest.getActiveProfileIds().containsAll( scanProfiles );

        //we are building from root with the same profiles, no need to re-scan the whole multi-module project
        if ( currentProject.getFile().getAbsolutePath().equals( multiModulePomFile.getAbsolutePath() )
                && profilesMatched )
        {
            projectMap = buildProjectMap( session.getProjects() );
            return;
        }

        long t0 = System.currentTimeMillis();

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest( projectBuildingRequest );
        //clear properties because after first build request some properties could be set to profiles
        //these properties could change effective pom when we try to rebuild whole multi module project again
        //for example the first model build process do not resolve ${os.detected.classifier}
        //but once build completed this property is set to profile
        //if we try to rebuild model for the whole project here string interpolator replaces this value
        //and effective pom could be different (depending on OS) if this property is used in pom.xml
        buildingRequest.setProfiles(
                buildingRequest.getProfiles().stream()
                        .peek( it -> it.setProperties( new Properties() ) )
                        .collect( Collectors.toList() ) );
        if ( !profilesMatched )
        {
            Set<String> profiles = new LinkedHashSet<>( buildingRequest.getActiveProfileIds() );
            //remove duplicates
            profiles.addAll( scanProfiles );
            buildingRequest.setActiveProfileIds( new ArrayList<>( profiles ) );
        }
        try
        {
            List<ProjectBuildingResult> buildingResults = projectBuilder.build(
                    Collections.singletonList( multiModulePomFile ),
                    true,
                    buildingRequest );
            LOGGER.info( "Multi module project model calculated [activeProfiles={}, time={} ms ",
                    buildingRequest.getActiveProfileIds(), System.currentTimeMillis() - t0 );

            List<MavenProject> projectList = buildingResults.stream().map( ProjectBuildingResult::getProject )
                    .collect( Collectors.toList() );
            projectMap = buildProjectMap( projectList );

        }
        catch ( ProjectBuildingException e )
        {
            LOGGER.error( "Unable to build model", e );
        }
        finally
        {
            built = true;
        }
    }

    private Map<String, MavenProject> buildProjectMap( List<MavenProject> projectList )
    {
        return projectList.stream().collect( Collectors.toMap(
                KeyUtils::getProjectKey,
                Function.identity() ) );
    }

    private static File getMultiModulePomFile( MavenSession session )
    {
        return CacheUtils.getMultimoduleRoot( session ).resolve( "pom.xml" ).toFile();
    }
}
