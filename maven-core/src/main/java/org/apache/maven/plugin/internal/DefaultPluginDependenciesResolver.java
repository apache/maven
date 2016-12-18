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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManagement;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

/**
 * Assists in resolving the dependencies of a plugin. <strong>Warning:</strong> This is an internal utility class that
 * is only public for technical reasons, it is not part of the public API. In particular, this class can be changed or
 * deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
@Component( role = PluginDependenciesResolver.class )
public class DefaultPluginDependenciesResolver
    implements PluginDependenciesResolver
{

    private static final String REPOSITORY_CONTEXT = "plugin";

    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repoSystem;

    private Artifact toArtifact( Plugin plugin, RepositorySystemSession session )
    {
        return new DefaultArtifact( plugin.getGroupId(), plugin.getArtifactId(), null, "jar", plugin.getVersion(),
                                    session.getArtifactTypeRegistry().get( "maven-plugin" ) );
    }

    public Artifact resolve( Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session )
        throws PluginResolutionException
    {
        RequestTrace trace = RequestTrace.newChild( null, plugin );

        Artifact pluginArtifact = toArtifact( plugin, session );

        try
        {
            DefaultRepositorySystemSession pluginSession = new DefaultRepositorySystemSession( session );
            pluginSession.setArtifactDescriptorPolicy( new SimpleArtifactDescriptorPolicy( true, false ) );

            ArtifactDescriptorRequest request =
                new ArtifactDescriptorRequest( pluginArtifact, repositories, REPOSITORY_CONTEXT );
            request.setTrace( trace );
            ArtifactDescriptorResult result = repoSystem.readArtifactDescriptor( pluginSession, request );

            pluginArtifact = result.getArtifact();

            String requiredMavenVersion = (String) result.getProperties().get( "prerequisites.maven" );
            if ( requiredMavenVersion != null )
            {
                Map<String, String> props = new LinkedHashMap<>( pluginArtifact.getProperties() );
                props.put( "requiredMavenVersion", requiredMavenVersion );
                pluginArtifact = pluginArtifact.setProperties( props );
            }
        }
        catch ( ArtifactDescriptorException e )
        {
            throw new PluginResolutionException( plugin, e );
        }

        try
        {
            ArtifactRequest request = new ArtifactRequest( pluginArtifact, repositories, REPOSITORY_CONTEXT );
            request.setTrace( trace );
            pluginArtifact = repoSystem.resolveArtifact( session, request ).getArtifact();
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }

        return pluginArtifact;
    }

    /**
     * @since 3.3.0
     */
    public DependencyNode resolveCoreExtension( Plugin plugin, DependencyFilter dependencyFilter,
                                                List<RemoteRepository> repositories, RepositorySystemSession session )
        throws PluginResolutionException
    {
        return resolveInternal( plugin, null /* pluginArtifact */, dependencyFilter, null /* transformer */,
                                repositories, session );
    }

    public DependencyNode resolve( Plugin plugin, Artifact pluginArtifact, DependencyFilter dependencyFilter,
                                   List<RemoteRepository> repositories, RepositorySystemSession session )
        throws PluginResolutionException
    {
        return resolveInternal( plugin, pluginArtifact, dependencyFilter, new PlexusUtilsInjector(), repositories,
                                session );
    }

    private DependencyNode resolveInternal( final Plugin plugin, final Artifact artifact,
                                            final DependencyFilter dependencyFilter,
                                            final DependencyGraphTransformer transformer,
                                            final List<RemoteRepository> repositories,
                                            final RepositorySystemSession session )
        throws PluginResolutionException
    {
        // This selector is a combination of the ScopeDependencySelector and the OptionalDependencySelector
        // simulating the POM resolution case for the dependency resolution case we are going to perform below.
        class PluginDependencySelector implements DependencySelector
        {

            private final int depth;

            PluginDependencySelector()
            {
                this( 0 );
            }

            private PluginDependencySelector( final int depth )
            {
                super();
                this.depth = depth;
            }

            @Override
            public boolean selectDependency( final org.eclipse.aether.graph.Dependency dependency )
            {
                return this.depth < 2 || !( dependency.isOptional()
                                            || "test".equalsIgnoreCase( dependency.getScope() )
                                            || "provided".equalsIgnoreCase( dependency.getScope() ) );

            }

            @Override
            public DependencySelector deriveChildSelector( final DependencyCollectionContext context )
            {
                assert context.getDependency() != null : "Unexpected POM resolution.";
                return this.depth >= 2
                           ? this
                           : new PluginDependencySelector( this.depth + 1 );

            }

        }

        // This dependency manager delegates to the session's dependency manager but supports excluding plugin
        // dependency overrides from the plugins/plugin/dependencies POM element.
        class PluginDependencyManager implements DependencyManager
        {

            private final int depth;

            private final DependencyManager defaultManager;

            private final List<Artifact> exclusions;

            PluginDependencyManager()
            {
                this( 0, session.getDependencyManager(), new LinkedList<Artifact>() );
            }

            private PluginDependencyManager( final int depth, final DependencyManager defaultManager,
                                             final List<Artifact> exclusions )
            {
                super();
                this.depth = depth;
                this.defaultManager = defaultManager;
                this.exclusions = exclusions;
            }

            @Override
            public DependencyManagement manageDependency( final org.eclipse.aether.graph.Dependency dependency )
            {
                boolean excluded = false;

                for ( final Artifact exclusion : this.getExclusions() )
                {
                    final Artifact artifact = dependency.getArtifact();

                    if ( exclusion.getGroupId().equals( artifact.getGroupId() )
                             && exclusion.getArtifactId().equals( artifact.getArtifactId() )
                             && exclusion.getExtension().equals( artifact.getExtension() )
                             && exclusion.getClassifier() != null
                             ? exclusion.getClassifier().equals( artifact.getClassifier() )
                             : dependency.getArtifact().getClassifier() == null )
                    {
                        excluded = true;
                        break;
                    }
                }

                return !excluded && this.depth >= 2 && this.defaultManager != null
                           ? this.defaultManager.manageDependency( dependency )
                           : null;

            }

            @Override
            public DependencyManager deriveChildManager( final DependencyCollectionContext context )
            {
                assert context.getDependency() != null : "Unexpected POM resolution.";
                return new PluginDependencyManager( this.depth + 1,
                                                    this.defaultManager != null
                                                        ? this.defaultManager.deriveChildManager( context )
                                                        : null,
                                                    this.exclusions );

            }

            public List<Artifact> getExclusions()
            {
                return this.exclusions;
            }

        }

        final RequestTrace trace = RequestTrace.newChild( null, plugin );
        final DependencyFilter collectionFilter = new ScopeDependencyFilter( "provided", "test" );
        final DependencyFilter resolutionFilter = AndDependencyFilter.newInstance( collectionFilter, dependencyFilter );
        final Artifact pluginArtifact = artifact != null
                                            ? artifact
                                            : toArtifact( plugin, session );

        try
        {
            final DependencySelector pluginDependencySelector =
                new AndDependencySelector( new PluginDependencySelector(), new ExclusionDependencySelector(),
                                           new WagonExcluder() );

            final DependencyGraphTransformer pluginDependencyGraphTransformer =
                ChainedDependencyGraphTransformer.newInstance( session.getDependencyGraphTransformer(), transformer );

            final PluginDependencyManager pluginDependencyManager = new PluginDependencyManager();
            DefaultRepositorySystemSession pluginSession = new DefaultRepositorySystemSession( session );
            pluginSession.setDependencySelector( pluginDependencySelector );
            pluginSession.setDependencyGraphTransformer( pluginDependencyGraphTransformer );
            pluginSession.setDependencyManager( pluginDependencyManager );

            CollectRequest request = new CollectRequest();
            request.setRequestContext( REPOSITORY_CONTEXT );
            request.setRepositories( repositories );

            for ( Dependency dependency : plugin.getDependencies() )
            {
                org.eclipse.aether.graph.Dependency pluginDep =
                    RepositoryUtils.toDependency( dependency, session.getArtifactTypeRegistry() );
                if ( !JavaScopes.SYSTEM.equals( pluginDep.getScope() ) )
                {
                    pluginDep = pluginDep.setScope( JavaScopes.RUNTIME );
                }
                request.addDependency( pluginDep );

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( String.format( "Collecting plugin dependency %s from project.", pluginDep ) );
                }

                pluginDependencyManager.getExclusions().
                    addAll( this.collectPluginDependencyArtifacts( session, repositories, pluginDep ) );

            }

            // [MNG-6135] Maven plugins and core extensions are not dependencies, they should be resolved the same way
            //            as projects.
            // We would need to perform 'request.setRootArtifact' here to request POM resolution. This would not resolve
            // the plugin JAR file later. So we perform dependency resolution and provide our own 'DependencySelector'
            // and 'DependencyManager' (see above) simulating the POM resolution case.
            request.setRoot( new org.eclipse.aether.graph.Dependency( pluginArtifact, null ) );

            DependencyRequest depRequest = new DependencyRequest( request, resolutionFilter );
            depRequest.setTrace( trace );

            request.setTrace( RequestTrace.newChild( trace, depRequest ) );

            final DependencyNode node = repoSystem.collectDependencies( pluginSession, request ).getRoot();

            if ( logger.isDebugEnabled() )
            {
                node.accept( new GraphLogger() );
            }

            depRequest.setRoot( node );
            repoSystem.resolveDependencies( session, depRequest );
            return node;
        }
        catch ( DependencyCollectionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }
        catch ( DependencyResolutionException e )
        {
            throw new PluginResolutionException( plugin, e.getCause() );
        }
    }

    private List<org.eclipse.aether.artifact.Artifact> collectPluginDependencyArtifacts(
        final RepositorySystemSession session, final List<RemoteRepository> repositories,
        final org.eclipse.aether.graph.Dependency pluginDependency )
        throws DependencyCollectionException
    {
        final CollectRequest request = new CollectRequest();
        request.setRequestContext( REPOSITORY_CONTEXT );
        request.setRepositories( repositories );
        request.setRoot( pluginDependency );
        request.setTrace( RequestTrace.newChild( null, pluginDependency ) );

        final DependencyNode node = repoSystem.collectDependencies( session, request ).getRoot();

        if ( logger.isDebugEnabled() )
        {
            node.accept( new GraphLogger() );
        }

        final PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept( nlg );
        return nlg.getArtifacts( true );
    }

    // Keep this class in sync with org.apache.maven.project.DefaultProjectDependenciesResolver.GraphLogger
    class GraphLogger
        implements DependencyVisitor
    {

        private String indent = "";

        GraphLogger()
        {
            super();
        }

        public boolean visitEnter( DependencyNode node )
        {
            assert node.getDependency() != null : "Unexpected POM resolution result.";

            StringBuilder buffer = new StringBuilder( 128 );
            buffer.append( indent );
            org.eclipse.aether.graph.Dependency dep = node.getDependency();
            org.eclipse.aether.artifact.Artifact art = dep.getArtifact();

            buffer.append( art );
            buffer.append( ':' ).append( dep.getScope() );

            String premanagedScope = DependencyManagerUtils.getPremanagedScope( node );
            if ( premanagedScope != null && !premanagedScope.equals( dep.getScope() ) )
            {
                buffer.append( " (scope managed from " ).append( premanagedScope ).append( ')' );
            }

            String premanagedVersion = DependencyManagerUtils.getPremanagedVersion( node );
            if ( premanagedVersion != null && !premanagedVersion.equals( art.getVersion() ) )
            {
                buffer.append( " (version managed from " ).append( premanagedVersion ).append( ')' );
            }

            Boolean premanagedOptional = DependencyManagerUtils.getPremanagedOptional( node );
            if ( premanagedOptional != null && !premanagedOptional.equals( dep.getOptional() ) )
            {
                buffer.append( " (optionality managed from " ).append( premanagedOptional ).append( ')' );
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
