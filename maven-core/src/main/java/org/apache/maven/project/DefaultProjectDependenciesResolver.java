package org.apache.maven.project;

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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;

/**
 * @author Benjamin Bentmann
 */
@Component( role = ProjectDependenciesResolver.class )
public class DefaultProjectDependenciesResolver
    implements ProjectDependenciesResolver
{

    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repoSystem;

    @Requirement
    private List<RepositorySessionDecorator> decorators;

    public DependencyResolutionResult resolve( DependencyResolutionRequest request )
        throws DependencyResolutionException
    {
        final RequestTrace trace = RequestTrace.newChild( null, request );

        final DefaultDependencyResolutionResult result = new DefaultDependencyResolutionResult();

        final MavenProject project = request.getMavenProject();
        final DependencyFilter filter = request.getResolutionFilter();
        RepositorySystemSession session = request.getRepositorySession();
        ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

        if ( logger.isDebugEnabled()
                 && session.getConfigProperties().get( DependencyManagerUtils.CONFIG_PROP_VERBOSE ) == null )
        {
            DefaultRepositorySystemSession verbose = new DefaultRepositorySystemSession( session );
            verbose.setConfigProperty( DependencyManagerUtils.CONFIG_PROP_VERBOSE, Boolean.TRUE );
            session = verbose;
        }

        for ( RepositorySessionDecorator decorator : decorators )
        {
            RepositorySystemSession decorated = decorator.decorate( project, session );
            if ( decorated != null )
            {
                session = decorated;
            }
        }

        CollectRequest collect = new CollectRequest();
        collect.setRootArtifact( RepositoryUtils.toArtifact( project.getArtifact() ) );
        collect.setRequestContext( "project" );
        collect.setRepositories( project.getRemoteProjectRepositories() );

        if ( project.getDependencyArtifacts() == null )
        {
            for ( Dependency dependency : project.getDependencies() )
            {
                if ( StringUtils.isEmpty( dependency.getGroupId() ) || StringUtils.isEmpty( dependency.getArtifactId() )
                         || StringUtils.isEmpty( dependency.getVersion() ) )
                {
                    // guard against case where best-effort resolution for invalid models is requested
                    continue;
                }
                collect.addDependency( RepositoryUtils.toDependency( dependency, stereotypes ).
                    setSourceHint( toSourceHint( project.getModel() ) ) );

            }
        }
        else
        {
            Map<String, Dependency> dependencies = new HashMap<>();
            for ( Dependency dependency : project.getDependencies() )
            {
                String classifier = dependency.getClassifier();
                if ( classifier == null )
                {
                    ArtifactType type = stereotypes.get( dependency.getType() );
                    if ( type != null )
                    {
                        classifier = type.getClassifier();
                    }
                }
                String key =
                    ArtifactIdUtils.toVersionlessId( dependency.getGroupId(), dependency.getArtifactId(),
                                                     dependency.getType(), classifier );
                dependencies.put( key, dependency );
            }
            for ( Artifact artifact : project.getDependencyArtifacts() )
            {
                String key = artifact.getDependencyConflictId();
                Dependency dependency = dependencies.get( key );
                Collection<Exclusion> exclusions = dependency != null ? dependency.getExclusions() : null;
                org.eclipse.aether.graph.Dependency dep = RepositoryUtils.toDependency( artifact, exclusions );
                if ( !JavaScopes.SYSTEM.equals( dep.getScope() ) && dep.getArtifact().getFile() != null )
                {
                    // enable re-resolution
                    org.eclipse.aether.artifact.Artifact art = dep.getArtifact();
                    art = art.setFile( null ).setVersion( art.getBaseVersion() );
                    dep = dep.setArtifact( art );
                }
                collect.addDependency( dep.setSourceHint( toSourceHint( project.getModel() ) ) );
            }
        }

        DependencyManagement depMgmt = project.getDependencyManagement();
        if ( depMgmt != null )
        {
            for ( Dependency dependency : depMgmt.getDependencies() )
            {
                collect.addManagedDependency( RepositoryUtils.toDependency( dependency, stereotypes ).
                    setSourceHint( toSourceHint( project.getModel() ) ) );

            }
        }

        DependencyRequest depRequest = new DependencyRequest( collect, filter );
        depRequest.setTrace( trace );

        DependencyNode node;
        try
        {
            collect.setTrace( RequestTrace.newChild( trace, depRequest ) );
            node = repoSystem.collectDependencies( session, collect ).getRoot();
            result.setDependencyGraph( node );
        }
        catch ( DependencyCollectionException e )
        {
            result.setDependencyGraph( e.getResult().getRoot() );
            result.setCollectionErrors( e.getResult().getExceptions() );

            throw new DependencyResolutionException( result, "Could not resolve dependencies for project "
                                                                 + project.getId() + ": " + e.getMessage(), e );
        }

        depRequest.setRoot( node );

        if ( logger.isWarnEnabled() )
        {
            for ( DependencyNode child : node.getChildren() )
            {
                if ( !child.getRelocations().isEmpty() )
                {
                    logger.warn( "The artifact " + child.getRelocations().get( 0 ) + " has been relocated to "
                                     + child.getDependency().getArtifact() );
                }
            }
        }

        if ( logger.isDebugEnabled() )
        {
            node.accept( new GraphLogger( project ) );
        }

        try
        {
            process( result, repoSystem.resolveDependencies( session, depRequest ).getArtifactResults() );
        }
        catch ( org.eclipse.aether.resolution.DependencyResolutionException e )
        {
            process( result, e.getResult().getArtifactResults() );

            throw new DependencyResolutionException( result, "Could not resolve dependencies for project "
                                                                 + project.getId() + ": " + e.getMessage(), e );
        }

        return result;
    }

    private void process( DefaultDependencyResolutionResult result, Collection<ArtifactResult> results )
    {
        for ( ArtifactResult ar : results )
        {
            DependencyNode node = ar.getRequest().getDependencyNode();
            if ( ar.isResolved() )
            {
                result.addResolvedDependency( node.getDependency() );
            }
            else
            {
                result.setResolutionErrors( node.getDependency(), ar.getExceptions() );
            }
        }
    }

    private static String toSourceHint( final Model model )
    {
        String sourceHint = null;

        if ( model != null )
        {
            final StringBuilder sourceHintBuilder = new StringBuilder( 128 );

            sourceHintBuilder.append( toId( model ) );

            File pomFile = model.getPomFile();
            if ( pomFile != null )
            {
                sourceHintBuilder.append( " @ " ).append( pomFile );
            }

            sourceHint = sourceHintBuilder.toString();
        }

        return sourceHint;
    }

    private static String toId( final Model model )
    {
        String id = null;

        if ( model != null )
        {
            String groupId = model.getGroupId();
            if ( groupId == null && model.getParent() != null )
            {
                groupId = model.getParent().getGroupId();
            }

            String artifactId = model.getArtifactId();

            String version = model.getVersion();
            if ( version == null && model.getParent() != null )
            {
                version = model.getParent().getVersion();
            }

            id = toId( groupId, artifactId, version );
        }

        return id;
    }

    private static String toId( final String groupId, final String artifactId, final String version )
    {
        final StringBuilder idBuilder = new StringBuilder( 128 );

        idBuilder.append( ( groupId != null && groupId.length() > 0 ) ? groupId : "[unknown-group-id]" );
        idBuilder.append( ':' );
        idBuilder.append( ( artifactId != null && artifactId.length() > 0 ) ? artifactId : "[unknown-artifact-id]" );
        idBuilder.append( ':' );
        idBuilder.append( ( version != null && version.length() > 0 ) ? version : "[unknown-version]" );

        return idBuilder.toString();
    }

    class GraphLogger
        implements DependencyVisitor
    {

        private final MavenProject project;

        private String indent = "";

        GraphLogger( MavenProject project )
        {
            this.project = project;
        }

        public boolean visitEnter( DependencyNode node )
        {
            StringBuilder buffer = new StringBuilder( 128 );
            buffer.append( indent );
            org.eclipse.aether.graph.Dependency dep = node.getDependency();
            if ( dep != null )
            {
                org.eclipse.aether.artifact.Artifact art = dep.getArtifact();

                buffer.append( art );
                buffer.append( ':' ).append( dep.getScope() ).append( ":optional(" ).
                    append( dep.getOptional() == null
                                ? "default"
                                : dep.isOptional()
                                      ? "true"
                                      : "false" ).append( ')' );

                if ( !dep.getExclusions().isEmpty() )
                {
                    buffer.append( ":exclusions(" ).append( dep.getExclusions() ).append( ')' );
                }

                if ( !dep.getArtifact().getProperties().isEmpty() )
                {
                    buffer.append( ":properties(" ).append( dep.getArtifact().getProperties() ).append( ')' );
                }

                if ( ( node.getManagedBits() & DependencyNode.MANAGED_SCOPE ) == DependencyNode.MANAGED_SCOPE )
                {
                    final String premanagedScope = DependencyManagerUtils.getPremanagedScope( node );
                    if ( premanagedScope != null && !premanagedScope.equals( dep.getScope() ) )
                    {
                        buffer.append( " (scope managed from " );
                        buffer.append( StringUtils.defaultString( premanagedScope, "default" ) );

                        final String sourceHint = DependencyManagerUtils.getScopeManagementSourceHint( node );
                        if ( sourceHint != null )
                        {
                            buffer.append( " by " ).append( sourceHint ).append( ' ' );
                        }

                        buffer.append( ')' );
                    }
                }

                if ( ( node.getManagedBits() & DependencyNode.MANAGED_VERSION ) == DependencyNode.MANAGED_VERSION )
                {
                    final String premanagedVersion = DependencyManagerUtils.getPremanagedVersion( node );
                    if ( premanagedVersion != null && !premanagedVersion.equals( dep.getArtifact().getVersion() ) )
                    {
                        buffer.append( " (version managed from " );
                        buffer.append( StringUtils.defaultString( premanagedVersion, "default" ) );

                        final String sourceHint = DependencyManagerUtils.getVersionManagementSourceHint( node );
                        if ( sourceHint != null )
                        {
                            buffer.append( " by " ).append( sourceHint ).append( ' ' );
                        }

                        buffer.append( ')' );
                    }
                }

                if ( ( node.getManagedBits() & DependencyNode.MANAGED_OPTIONAL ) == DependencyNode.MANAGED_OPTIONAL )
                {
                    final Boolean premanagedOptional = DependencyManagerUtils.getPremanagedOptional( node );
                    if ( premanagedOptional != null && !premanagedOptional.equals( dep.getOptional() ) )
                    {
                        buffer.append( " (optionality managed from " );
                        buffer.append( StringUtils.defaultString( premanagedOptional, "default" ) );

                        final String sourceHint = DependencyManagerUtils.getOptionalityManagementSourceHint( node );
                        if ( sourceHint != null )
                        {
                            buffer.append( " by " ).append( sourceHint ).append( ' ' );
                        }

                        buffer.append( ')' );
                    }
                }

                if ( ( node.getManagedBits() & DependencyNode.MANAGED_EXCLUSIONS )
                         == DependencyNode.MANAGED_EXCLUSIONS )
                {
                    final Collection<org.eclipse.aether.graph.Exclusion> premanagedExclusions =
                        DependencyManagerUtils.getPremanagedExclusions( node );

                    if ( premanagedExclusions != null && !premanagedExclusions.equals( dep.getExclusions() ) )
                    {
                        buffer.append( " (exclusions managed from " );
                        buffer.append( StringUtils.defaultString( premanagedExclusions, "default" ) );

                        final String sourceHint = DependencyManagerUtils.getExclusionsManagementSourceHint( node );
                        if ( sourceHint != null )
                        {
                            buffer.append( " by " ).append( sourceHint ).append( ' ' );
                        }

                        buffer.append( ')' );
                    }
                }

                if ( ( node.getManagedBits() & DependencyNode.MANAGED_PROPERTIES )
                         == DependencyNode.MANAGED_PROPERTIES )
                {
                    final Map<String, String> premanagedProperties =
                        DependencyManagerUtils.getPremanagedProperties( node );

                    if ( premanagedProperties != null && !premanagedProperties.equals( art.getProperties() ) )
                    {
                        buffer.append( " (properties managed from " );
                        buffer.append( StringUtils.defaultString( premanagedProperties, "default" ) );

                        final String sourceHint = DependencyManagerUtils.getPropertiesManagementSourceHint( node );
                        if ( sourceHint != null )
                        {
                            buffer.append( " by " ).append( sourceHint ).append( ' ' );
                        }

                        buffer.append( ')' );
                    }
                }
            }
            else
            {
                buffer.append( project.getGroupId() );
                buffer.append( ':' ).append( project.getArtifactId() );
                buffer.append( ':' ).append( project.getPackaging() );
                buffer.append( ':' ).append( project.getVersion() );
            }

            logger.debug( buffer.toString() );
            indent += "   ";
            return true;
        }

        public boolean visitLeave( DependencyNode node )
        {
            indent = indent.substring( 0, indent.length() - 3 );
            return true;
        }

    }

}
