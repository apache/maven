package org.apache.maven.dependency;

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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Benjamin Bentmann
 */
@Component( role = DependencyTreeBuilder.class )
public class DefaultDependencyTreeBuilder
    implements DependencyTreeBuilder
{

    public DependencyTreeResult buildDirtyTree( DependencyTreeRequest request )
    {
        RepositoryReader resolver = request.getRepositoryReader();
        if ( resolver == null )
        {
            throw new IllegalArgumentException( "repository reader is missing" );
        }

        DependencyTraverser traverser = request.getDependencyTraverser();

        DependencyManager manager = request.getDependencyManager();

        DependencyFilter filter = request.getDependencyFilter();

        DefaultDependencyProblemCollector problems = new DefaultDependencyProblemCollector( null );

        Dependency resolvedDependency = request.getDependency().clone();

        List<Model> relocations = new ArrayList<Model>( 1 );

        MetadataResult metadataResult =
            getMetadata( null, request.getDependency(), relocations, null, resolver, null, problems );

        DefaultDependencyNode root =
            new DefaultDependencyNode( resolvedDependency, metadataResult.getMetadata(),
                                       metadataResult.getRepositoryId(), relocations, null );

        recurse( root, traverser, manager, filter, resolver, problems );

        return new DefaultDependencyTreeResult( root, problems.getProblems() );
    }

    private void recurse( DefaultDependencyNode node, DependencyTraverser traverser, DependencyManager manager,
                          DependencyFilter filter, RepositoryReader resolver, DependencyProblemCollector problems )
    {
        if ( node.getMetadata() == null )
        {
            return;
        }

        if ( traverser != null && !traverser.accept( node ) )
        {
            return;
        }

        if ( manager != null )
        {
            manager = manager.deriveChildManager( node );
        }

        if ( filter != null )
        {
            filter = filter.deriveChildFilter( node );
        }

        resolver = resolver.addRepositories( node.getMetadata().getRepositories(), problems );

        List<Dependency> dependencies = node.getMetadata().getDependencies();

        DefaultVersionRequest versionRequest = new DefaultVersionRequest( problems );

        for ( Dependency declaredDependency : dependencies )
        {
            Dependency managedDependency = declaredDependency.clone();

            if ( manager != null )
            {
                manager.manageDependency( node, managedDependency );
            }

            if ( filter != null && !filter.accept( node, managedDependency ) )
            {
                continue;
            }

            versionRequest.set( managedDependency );
            VersionResult versionResult = resolver.getVersions( versionRequest );

            List<String> versions = versionResult.getVersions();

            if ( versions.isEmpty() )
            {
                problems.addError( "Found no versions for dependency " + managedDependency.getManagementKey()
                    + " that match " + managedDependency.getVersion() );

                continue;
            }

            for ( String version : versions )
            {
                Dependency resolvedDependency = managedDependency.clone();
                resolvedDependency.setVersion( version );

                List<Model> relocations = new ArrayList<Model>( 1 );

                MetadataResult metadataResult =
                    getMetadata( node, resolvedDependency, relocations,
                                 versionResult.getRepositoryIds().get( version ), resolver, filter, problems );

                if ( metadataResult == null )
                {
                    // relocated dependency was excluded by filter
                    continue;
                }

                DefaultDependencyNode child =
                    node.addChild( resolvedDependency, metadataResult.getMetadata(), metadataResult.getRepositoryId(),
                                   relocations );

                DependencyTraverser childTraverser =
                    ( traverser != null ) ? traverser.deriveChildTraverser( child ) : null;

                recurse( child, childTraverser, manager, filter, resolver, problems );
            }
        }
    }

    private MetadataResult getMetadata( DependencyNode node, Dependency dependency, List<Model> relocations,
                                        String repositoryId, RepositoryReader resolver,
                                        DependencyFilter dependencyFilter, DependencyProblemCollector problems )
    {
        MetadataResult metadataResult;

        Set<String> relocationKeys = new LinkedHashSet<String>();
        relocationKeys.add( getRelocationKey( dependency ) );

        MetadataRequest metadataRequest = new DefaultMetadataRequest( dependency, repositoryId, problems );

        while ( true )
        {
            metadataResult = resolver.getMetadata( metadataRequest );

            if ( relocateDependency( dependency, metadataResult.getMetadata() ) )
            {
                String relocationKey = getRelocationKey( dependency );

                if ( !relocationKeys.add( relocationKey ) )
                {
                    problems.addError( "Cyclic relocation: " + StringUtils.join( relocationKeys.iterator(), " -> " )
                        + " -> " + relocationKey );
                    break;
                }

                if ( dependencyFilter != null && !dependencyFilter.accept( node, dependency ) )
                {
                    return null;
                }

                relocations.add( metadataResult.getMetadata() );
            }
            else
            {
                break;
            }
        }

        return metadataResult;
    }

    private String getRelocationKey( Dependency dependency )
    {
        return dependency.getGroupId() + ':' + dependency.getArtifactId() + ':' + dependency.getVersion();
    }

    private boolean relocateDependency( Dependency dependency, Model model )
    {
        boolean relocated = false;

        if ( model.getDistributionManagement() != null )
        {
            Relocation relocation = model.getDistributionManagement().getRelocation();

            if ( relocation != null )
            {
                if ( StringUtils.isNotEmpty( relocation.getGroupId() ) )
                {
                    dependency.setGroupId( relocation.getGroupId() );
                    relocated = true;
                }

                if ( StringUtils.isNotEmpty( relocation.getArtifactId() ) )
                {
                    dependency.setArtifactId( relocation.getArtifactId() );
                    relocated = true;
                }

                if ( StringUtils.isNotEmpty( relocation.getVersion() ) )
                {
                    dependency.setVersion( relocation.getVersion() );
                    relocated = true;
                }
            }
        }

        return relocated;
    }

}
