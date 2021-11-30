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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.SessionScoped;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;

@SessionScoped
@Named
public class DefaultNormalizedModelProvider implements NormalizedModelProvider
{

    private static final String NORMALIZED_VERSION = "cache-extension-version";

    private final CacheConfig cacheConfig;
    private final MultiModuleSupport multiModuleSupport;
    private final ConcurrentMap<String, Model> modelCache = new ConcurrentHashMap<>();

    @Inject
    public DefaultNormalizedModelProvider( MultiModuleSupport multiModuleSupport, CacheConfig cacheConfig )
    {
        this.multiModuleSupport = multiModuleSupport;
        this.cacheConfig = cacheConfig;
    }

    @Override
    public Model normalizedModel( MavenProject project )
    {
        MavenProject validatedProject = Objects.requireNonNull( project, "project" );
        return modelCache.computeIfAbsent( BuilderCommon.getKey( validatedProject ),
                k -> normalizedModelInner( validatedProject ) );
    }

    private Model normalizedModelInner( MavenProject project )
    {
        //prefer project from multimodule than reactor because effective pom of reactor project
        //could be built with maven local/remote dependencies but not with artifacts from cache
        MavenProject projectToNormalize = multiModuleSupport.tryToResolveProject(
                project.getGroupId(),
                project.getArtifactId(),
                project.getVersion() )
                .orElse( project );
        Model prototype = projectToNormalize.getModel();

        //TODO validate status of the model - it should be in resolved state
        Model resultModel = new Model();

        resultModel.setGroupId( prototype.getGroupId() );
        resultModel.setArtifactId( prototype.getArtifactId() );
        //does not make sense to add project version to calculate hash
        resultModel.setVersion( NORMALIZED_VERSION );
        resultModel.setModules( prototype.getModules() );

        resultModel.setDependencies( normalizeDependencies( prototype.getDependencies() ) );

        org.apache.maven.model.Build protoBuild = prototype.getBuild();
        if ( protoBuild == null )
        {
            return resultModel;
        }

        Build build = new Build();
        List<Plugin> plugins = prototype.getBuild().getPlugins();
        build.setPlugins( normalizePlugins( plugins ) );
        resultModel.setBuild( build );
        return resultModel;
    }

    private List<Dependency> normalizeDependencies( Collection<Dependency> source )
    {
        return source.stream().map( it ->
        {
            if ( !multiModuleSupport.isPartOfMultiModule( it.getGroupId(), it.getArtifactId(), it.getVersion() ) )
            {
                return it;
            }
            Dependency cloned = it.clone();
            cloned.setVersion( NORMALIZED_VERSION );
            return cloned;
        } ).sorted( DefaultNormalizedModelProvider::compareDependencies ).collect( Collectors.toList() );
    }

    private List<Plugin> normalizePlugins( List<Plugin> plugins )
    {
        if ( plugins.isEmpty() )
        {
            return plugins;
        }

        return plugins.stream().map(
                plugin ->
                {
                    Plugin copy = plugin.clone();
                    List<String> excludeProperties = cacheConfig.getEffectivePomExcludeProperties( copy );
                    removeBlacklistedAttributes( copy.getConfiguration(), excludeProperties );
                    for ( PluginExecution execution : copy.getExecutions() )
                    {
                        removeBlacklistedAttributes( execution.getConfiguration(), excludeProperties );
                    }

                    copy.setDependencies(
                            normalizeDependencies(
                                    copy.getDependencies()
                                            .stream()
                                            .sorted( DefaultNormalizedModelProvider::compareDependencies )
                                            .collect( Collectors.toList() ) ) );
                    if ( multiModuleSupport.isPartOfMultiModule(
                            copy.getGroupId(),
                            copy.getArtifactId(),
                            copy.getVersion() ) )
                    {
                        copy.setVersion( NORMALIZED_VERSION );
                    }
                    return copy;
                } ).collect( Collectors.toList() );
    }

    private void removeBlacklistedAttributes( Object node, List<String> excludeProperties )
    {
        if ( node == null )
        {
            return;
        }

        Object[] children = Xpp3DomUtils.getChildren( node );
        int indexToRemove = 0;
        for ( Object child : children )
        {
            if ( excludeProperties.contains( Xpp3DomUtils.getName( child ) ) )
            {
                Xpp3DomUtils.removeChild( node, indexToRemove );
                continue;
            }
            indexToRemove++;
            removeBlacklistedAttributes( child, excludeProperties );
        }
    }

    private static int compareDependencies( Dependency d1, Dependency d2 )
    {
        return d1.getArtifactId().compareTo( d2.getArtifactId() );
    }
}
