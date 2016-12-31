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
import org.apache.maven.artifact.versioning.ComparableVersion;
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
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;
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

    private static final String DEFAULT_PREREQUISITES = "2.0";

    private static final ComparableVersion DEFAULT_RESULTION_PREREQUISITES = new ComparableVersion( "3.4" );

    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repoSystem;

    private Artifact toArtifact( Plugin plugin, RepositorySystemSession session )
    {
        return new DefaultArtifact( plugin.getGroupId(), plugin.getArtifactId(), null, "jar", plugin.getVersion(),
                                    session.getArtifactTypeRegistry().get( "maven-plugin" ) );
    }

    @Override
    public Artifact resolve( Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session )
        throws PluginResolutionException
    {
        try
        {
            final Artifact pluginArtifact = this.createPluginArtifact( plugin, session, repositories );
            final ArtifactRequest request = new ArtifactRequest( pluginArtifact, repositories, REPOSITORY_CONTEXT );
            request.setTrace( RequestTrace.newChild( null, plugin ) );
            return this.repoSystem.resolveArtifact( session, request ).getArtifact();
        }
        catch ( ArtifactDescriptorException | ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }
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
        // This dependency manager delegates to the session's dependency manager but supports excluding plugin
        // dependency overrides from the plugins/plugin/dependencies POM element so that what is declared there will
        // not get changed due to any management performed.
        class PluginDependencyManager implements DependencyManager
        {

            private final int depth;

            private final DependencyManager delegate;

            private final List<Artifact> exclusions;

            PluginDependencyManager( final DependencyManager delegate )
            {
                this( 0, delegate, new LinkedList<Artifact>() );
            }

            private PluginDependencyManager( final int depth, final DependencyManager delegate,
                                             final List<Artifact> exclusions )
            {
                super();
                this.depth = depth;
                this.delegate = delegate;
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

                return !excluded && this.depth >= 2 && this.delegate != null
                           ? this.delegate.manageDependency( dependency )
                           : null;

            }

            @Override
            public DependencyManager deriveChildManager( final DependencyCollectionContext context )
            {
                return new PluginDependencyManager( this.depth + 1,
                                                    this.delegate != null
                                                        ? this.delegate.deriveChildManager( context )
                                                        : null,
                                                    this.exclusions );

            }

            public List<Artifact> getExclusions()
            {
                return this.exclusions;
            }

        }

        // This dependency selector matches the resolver's implementation before MRESOLVER-8 got fixed. It is
        // used for plugin's with prerequisites < 3.4 to mimic incorrect but backwards compatible behaviour.
        class ClassicScopeDependencySelector implements DependencySelector
        {

            private final boolean transitive;

            ClassicScopeDependencySelector()
            {
                this( false );
            }

            private ClassicScopeDependencySelector( final boolean transitive )
            {
                super();
                this.transitive = transitive;
            }

            @Override
            public boolean selectDependency( final org.eclipse.aether.graph.Dependency dependency )
            {
                return !this.transitive
                           || !( "test".equals( dependency.getScope() )
                                 || "provided".equals( dependency.getScope() ) );

            }

            @Override
            public DependencySelector deriveChildSelector( final DependencyCollectionContext context )
            {
                ClassicScopeDependencySelector child = this;

                if ( context.getDependency() != null && !child.transitive )
                {
                    child = new ClassicScopeDependencySelector( true );
                }
                if ( context.getDependency() == null && child.transitive )
                {
                    child = new ClassicScopeDependencySelector( false );
                }

                return child;
            }

            @Override
            public boolean equals( Object obj )
            {
                boolean equal = obj instanceof ClassicScopeDependencySelector;

                if ( equal )
                {
                    final ClassicScopeDependencySelector that = (ClassicScopeDependencySelector) obj;
                    equal = this.transitive == that.transitive;
                }

                return equal;
            }

            @Override
            public int hashCode()
            {
                int hash = 17;
                hash = hash * 31 + ( ( (Boolean) this.transitive ).hashCode() );
                return hash;
            }

        }

        final RequestTrace trace = RequestTrace.newChild( null, plugin );
        final DependencyFilter collectionFilter = new ScopeDependencyFilter( "provided", "test" );
        final DependencyFilter resolutionFilter = AndDependencyFilter.newInstance( collectionFilter, dependencyFilter );

        try
        {
            final Artifact pluginArtifact = artifact != null
                                                ? this.createPluginArtifact( artifact, session, repositories )
                                                : this.createPluginArtifact( plugin, session, repositories );

            final ComparableVersion prerequisites =
                new ComparableVersion( pluginArtifact.getProperty( "requiredMavenVersion", DEFAULT_PREREQUISITES ) );

            final boolean classicResolution = prerequisites.compareTo( DEFAULT_RESULTION_PREREQUISITES ) < 0;

            if ( this.logger.isDebugEnabled() )
            {
                if ( classicResolution )
                {
                    this.logger.debug( String.format(
                        "Constructing classic plugin classpath '%s' for prerequisites '%s'.",
                        pluginArtifact, prerequisites ) );

                }
                else
                {
                    this.logger.debug( String.format(
                        "Constructing default plugin classpath '%s' for prerequisites '%s'.",
                        pluginArtifact, prerequisites ) );

                }
            }

            final DependencySelector pluginDependencySelector =
                classicResolution
                    ? new AndDependencySelector( new ClassicScopeDependencySelector(), // incorrect - see MRESOLVER-8
                                                 new OptionalDependencySelector(),
                                                 new ExclusionDependencySelector(),
                                                 new WagonExcluder() )
                    : AndDependencySelector.newInstance( session.getDependencySelector(), new WagonExcluder() );

            final DependencyGraphTransformer pluginDependencyGraphTransformer =
                ChainedDependencyGraphTransformer.newInstance( session.getDependencyGraphTransformer(), transformer );

            final PluginDependencyManager pluginDependencyManager =
                    new PluginDependencyManager( classicResolution
                            ? new ClassicDependencyManager()
                            : session.getDependencyManager() );

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
        catch ( ArtifactDescriptorException | DependencyCollectionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }
        catch ( DependencyResolutionException e )
        {
            throw new PluginResolutionException( plugin, e.getCause() );
        }
    }

    private Artifact createPluginArtifact( final Plugin plugin,
                                           final RepositorySystemSession session,
                                           final List<RemoteRepository> repositories )
        throws ArtifactDescriptorException
    {
        return this.createPluginArtifact( toArtifact( plugin, session ), session, repositories );
    }

    private Artifact createPluginArtifact( final Artifact artifact,
                                           final RepositorySystemSession session,
                                           final List<RemoteRepository> repositories )
        throws ArtifactDescriptorException
    {
        Artifact pluginArtifact = artifact;
        final DefaultRepositorySystemSession pluginSession = new DefaultRepositorySystemSession( session );
        pluginSession.setArtifactDescriptorPolicy( new SimpleArtifactDescriptorPolicy( true, false ) );

        final ArtifactDescriptorRequest request =
            new ArtifactDescriptorRequest( pluginArtifact, repositories, REPOSITORY_CONTEXT );

        request.setTrace( RequestTrace.newChild( null, artifact ) );

        final ArtifactDescriptorResult result = this.repoSystem.readArtifactDescriptor( pluginSession, request );

        pluginArtifact = result.getArtifact();

        final String requiredMavenVersion = (String) result.getProperties().get( "prerequisites.maven" );

        if ( requiredMavenVersion != null )
        {
            final Map<String, String> props = new LinkedHashMap<>( pluginArtifact.getProperties() );
            props.put( "requiredMavenVersion", requiredMavenVersion );
            pluginArtifact = pluginArtifact.setProperties( props );
        }

        return pluginArtifact;
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

        final PreorderNodeListGenerator nodeListGenerator = new PreorderNodeListGenerator();
        final DependencyFilter scopeDependencyFilter = new ScopeDependencyFilter( "provided", "test" );
        node.accept( new FilteringDependencyVisitor( nodeListGenerator, scopeDependencyFilter ) );
        return nodeListGenerator.getArtifacts( true );
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
