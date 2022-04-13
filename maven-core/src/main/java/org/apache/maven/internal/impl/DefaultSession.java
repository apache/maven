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

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Listener;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.Metadata;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.ArtifactDeployer;
import org.apache.maven.api.services.ArtifactDeployerException;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.ArtifactInstaller;
import org.apache.maven.api.services.ArtifactInstallerException;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.DependencyCollector;
import org.apache.maven.api.services.DependencyCollectorException;
import org.apache.maven.api.services.DependencyFactory;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.LocalRepositoryManager;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.ToolchainManager;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.services.xml.ToolchainsXmlFactory;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.DefaultToolchainManagerPrivate;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultSession implements Session
{

    private final MavenSession mavenSession;
    private final RepositorySystemSession session;
    private final RepositorySystem repositorySystem;
    private final List<RemoteRepository> repositories;
    private final org.apache.maven.project.ProjectBuilder projectBuilder;
    private final MavenRepositorySystem mavenRepositorySystem;
    private final DefaultToolchainManagerPrivate toolchainManagerPrivate;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final ArtifactManager artifactManager = new DefaultArtifactManager();
    private final ProjectManager projectManager = new DefaultProjectManager( artifactManager );

    private final Map<org.eclipse.aether.graph.DependencyNode, Node> allNodes
            = Collections.synchronizedMap( new WeakHashMap<>() );
    private final Map<org.eclipse.aether.artifact.Artifact, Artifact> allArtifacts
            = Collections.synchronizedMap( new WeakHashMap<>() );
    private final Map<org.eclipse.aether.repository.RemoteRepository, RemoteRepository> allRepositories
            = Collections.synchronizedMap( new WeakHashMap<>() );
    private final Map<String, Project> allProjects
            = Collections.synchronizedMap( new WeakHashMap<>() );
    private final Map<org.eclipse.aether.graph.Dependency, Dependency> allDependencies
            = Collections.synchronizedMap( new WeakHashMap<>() );

    public DefaultSession( @Nonnull MavenSession session,
                           @Nonnull RepositorySystem repositorySystem,
                           @Nullable List<RemoteRepository> repositories,
                           @Nonnull org.apache.maven.project.ProjectBuilder projectBuilder,
                           @Nonnull MavenRepositorySystem mavenRepositorySystem,
                           @Nonnull DefaultToolchainManagerPrivate toolchainManagerPrivate )
    {
        this.mavenSession = nonNull( session );
        this.session = mavenSession.getRepositorySession();
        this.repositorySystem = nonNull( repositorySystem );
        this.repositories = repositories != null
                ? repositories
                : mavenSession.getRequest().getRemoteRepositories().stream()
                .map( RepositoryUtils::toRepo ).map( this::getRemoteRepository ).collect( Collectors.toList() );
        this.projectBuilder = projectBuilder;
        this.mavenRepositorySystem = mavenRepositorySystem;
        this.toolchainManagerPrivate = toolchainManagerPrivate;
    }

    MavenSession getMavenSession()
    {
        return null;
    }

    @Nonnull
    @Override
    public LocalRepository getLocalRepository()
    {
        return new DefaultLocalRepository( session.getLocalRepository() );
    }

    @Nonnull
    @Override
    public List<RemoteRepository> getRemoteRepositories()
    {
        return Collections.unmodifiableList( repositories );
    }

    @Nonnull
    @Override
    public Settings getSettings()
    {
        return mavenSession.getSettings().getDelegate();
    }

    @Nonnull
    @Override
    public Properties getUserProperties()
    {
        return mavenSession.getUserProperties();
    }

    @Nonnull
    @Override
    public Properties getSystemProperties()
    {
        return mavenSession.getSystemProperties();
    }

    @Nonnull
    @Override
    public SessionData getData()
    {
        org.eclipse.aether.SessionData data = session.getData();
        return new SessionData()
        {
            @Override
            public void set( @Nonnull Object key, @Nullable Object value )
            {
                data.set( key, value );
            }

            @Override
            public boolean set( @Nonnull Object key, @Nullable Object oldValue, @Nullable Object newValue )
            {
                return data.set( key, oldValue, newValue );
            }

            @Nullable
            @Override
            public Object get( @Nonnull Object key )
            {
                return data.get( key );
            }

            @Nullable
            @Override
            public Object computeIfAbsent( @Nonnull Object key, @Nonnull Supplier<Object> supplier )
            {
                // return data.computeIfAbsent( key, supplier );
                Object value = data.get( key );
                if ( value == null )
                {
                    if ( !data.set( key, null, supplier.get() ) )
                    {
                        value = data.get( key );
                    }
                }
                return value;
            }
        };
    }

    @Nonnull
    @Override
    public Session withLocalRepository( @Nonnull LocalRepository localRepository )
    {
        nonNull( localRepository, "localRepository" );
        if ( session.getLocalRepository() != null
                && Objects.equals( session.getLocalRepository().getBasedir().toPath(),
                localRepository.getPath() ) )
        {
            return this;
        }
        org.eclipse.aether.repository.LocalRepository repository = toRepository( localRepository );
        org.eclipse.aether.repository.LocalRepositoryManager localRepositoryManager
                = repositorySystem.newLocalRepositoryManager( session, repository );

        RepositorySystemSession repoSession = new DefaultRepositorySystemSession( session )
                .setLocalRepositoryManager( localRepositoryManager );
        MavenSession newSession = new MavenSession( mavenSession.getContainer(), repoSession,
                mavenSession.getRequest(), mavenSession.getResult() );
        return new DefaultSession( newSession, repositorySystem,
                repositories, projectBuilder, mavenRepositorySystem, toolchainManagerPrivate );
    }

    @Nonnull
    @Override
    public Session withRemoteRepositories( @Nonnull List<RemoteRepository> repositories )
    {
        return new DefaultSession( mavenSession, repositorySystem,
                repositories, projectBuilder, mavenRepositorySystem, toolchainManagerPrivate );
    }

    @Nonnull
    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends Service> T getService( Class<T> clazz ) throws NoSuchElementException
    {
        if ( clazz == ArtifactFactory.class )
        {
            return (T) new DefaultArtifactFactory();
        }
        else if ( clazz == ArtifactResolver.class )
        {
            return (T) new DefaultArtifactResolver( repositorySystem );
        }
        else if ( clazz == ArtifactDeployer.class )
        {
            return (T) new DefaultArtifactDeployer( repositorySystem );
        }
        else if ( clazz == ArtifactInstaller.class )
        {
            return (T) new DefaultArtifactInstaller( repositorySystem );
        }
        else if ( clazz == ArtifactManager.class )
        {
            return (T) artifactManager;
        }
        else if ( clazz == DependencyFactory.class )
        {
            return (T) new DefaultDependencyFactory();
        }
        else if ( clazz == DependencyCollector.class )
        {
            return (T) new DefaultDependencyCollector( repositorySystem );
        }
        else if ( clazz == DependencyResolver.class )
        {
            return (T) new DefaultDependencyResolver( repositorySystem );
        }
        else if ( clazz == ProjectBuilder.class )
        {
            return (T) new DefaultProjectBuilder( projectBuilder );
        }
        else if ( clazz == ProjectManager.class )
        {
            return (T) projectManager;
        }
        else if ( clazz == LocalRepositoryManager.class )
        {
            return (T) new DefaultLocalRepositoryManager();
        }
        else if ( clazz == RepositoryFactory.class )
        {
            return (T) new DefaultRepositoryFactory( repositorySystem );
        }
        else if ( clazz == ToolchainManager.class )
        {
            return (T) new DefaultToolchainManager( toolchainManagerPrivate );
        }
        else if ( clazz == ModelXmlFactory.class )
        {
            return (T) new DefaultModelXmlFactory();
        }
        else if ( clazz == SettingsXmlFactory.class )
        {
            return (T) new DefaultSettingsXmlFactory();
        }
        else if ( clazz == ToolchainsXmlFactory.class )
        {
            return (T) new DefaultToolchainsXmlFactory();
        }
        throw new NoSuchElementException( clazz.getName() );
    }

    @Nonnull
    public RepositorySystemSession getSession()
    {
        return session;
    }

    @Nonnull
    public RepositorySystem getRepositorySystem()
    {
        return repositorySystem;
    }

    public RemoteRepository getRemoteRepository( org.eclipse.aether.repository.RemoteRepository repository )
    {
        return allRepositories.computeIfAbsent( repository, DefaultRemoteRepository::new );
    }

    public Node getNode( org.eclipse.aether.graph.DependencyNode node )
    {
        return allNodes.computeIfAbsent( node, n -> new DefaultNode( this, n ) );
    }

    @Nonnull
    public Artifact getArtifact( @Nonnull org.eclipse.aether.artifact.Artifact artifact )
    {
        return allArtifacts.computeIfAbsent( artifact, a -> new DefaultArtifact( this, a ) );
    }

    @Nonnull
    public Dependency getDependency( @Nonnull org.eclipse.aether.graph.Dependency dependency )
    {
        return allDependencies.computeIfAbsent( dependency, d -> new DefaultDependency( this, d ) );
    }

    public List<Project> getProjects( List<MavenProject> projects )
    {
        return projects == null ? null : projects.stream()
                .map( this::getProject )
                .collect( Collectors.toList() );
    }

    public Project getProject( MavenProject project )
    {
        return allProjects.computeIfAbsent( project.getId(), id -> new DefaultProject( this, project ) );
    }

    public List<org.eclipse.aether.repository.RemoteRepository> toRepositories( List<RemoteRepository> repositories )
    {
        return repositories == null ? null : repositories.stream()
                .map( this::toRepository )
                .collect( Collectors.toList() );
    }

    public org.eclipse.aether.repository.RemoteRepository toRepository( RemoteRepository repository )
    {
        if ( repository instanceof DefaultRemoteRepository )
        {
            return ( (DefaultRemoteRepository) repository ).getRepository();
        }
        else
        {
            // TODO
            throw new UnsupportedOperationException( "Not implemented yet" );
        }
    }

    public org.eclipse.aether.repository.LocalRepository toRepository( LocalRepository repository )
    {
        if ( repository instanceof DefaultLocalRepository )
        {
            return ( (DefaultLocalRepository) repository ).getRepository();
        }
        else
        {
            // TODO
            throw new UnsupportedOperationException( "Not implemented yet" );
        }
    }

    public List<ArtifactRepository> toArtifactRepositories( List<RemoteRepository> repositories )
    {
        return repositories == null ? null : repositories.stream()
                .map( this::toArtifactRepository )
                .collect( Collectors.toList() );
    }

    private ArtifactRepository toArtifactRepository( RemoteRepository repository )
    {
        if ( repository instanceof DefaultRemoteRepository )
        {
            org.eclipse.aether.repository.RemoteRepository rr
                    = ( (DefaultRemoteRepository) repository ).getRepository();

            try
            {
                return mavenRepositorySystem.createRepository(
                        rr.getUrl(),
                        rr.getId(),
                        rr.getPolicy( false ).isEnabled(),
                        rr.getPolicy( false ).getUpdatePolicy(),
                        rr.getPolicy( true ).isEnabled(),
                        rr.getPolicy( true ).getUpdatePolicy(),
                        rr.getPolicy( false ).getChecksumPolicy()

                );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( "Unable to create repository", e );
            }
        }
        else
        {
            // TODO
            throw new UnsupportedOperationException( "Not yet implemented" );
        }
    }

    public List<org.eclipse.aether.graph.Dependency> toDependencies( Collection<Dependency> dependencies )
    {
        return dependencies == null ? null : dependencies.stream()
                .map( this::toDependency )
                .collect( Collectors.toList() );
    }

    public org.eclipse.aether.graph.Dependency toDependency( Dependency dependency )
    {
        if ( dependency instanceof DefaultDependency )
        {
            return ( (DefaultDependency) dependency ).getDependency();
        }
        else
        {
            String typeId = dependency.getType();
            org.eclipse.aether.artifact.ArtifactType type = typeId != null
                    ? session.getArtifactTypeRegistry().get( typeId ) : null;
            String extension = type != null ? type.getExtension() : null;
            return new org.eclipse.aether.graph.Dependency(
                    new org.eclipse.aether.artifact.DefaultArtifact(
                            dependency.getGroupId(),
                            dependency.getArtifactId(),
                            dependency.getClassifier(),
                            extension,
                            dependency.getVersion(),
                            type
                    ), null );
        }
    }

    public List<org.eclipse.aether.artifact.Artifact> toArtifacts( Collection<Artifact> artifacts )
    {
        return artifacts == null ? null : artifacts.stream()
                .map( this::toArtifact )
                .collect( Collectors.toList() );
    }

    public org.eclipse.aether.artifact.Artifact toArtifact( Artifact artifact )
    {
        File file = getService( ArtifactManager.class ).getPath( artifact ).map( Path::toFile ).orElse( null );
        if ( artifact instanceof DefaultArtifact )
        {
            org.eclipse.aether.artifact.Artifact a = ( (DefaultArtifact) artifact ).getArtifact();
            if ( Objects.equals( file, a.getFile() ) )
            {
                return a;
            }
        }
        return new org.eclipse.aether.artifact.DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getClassifier(),
                artifact.getExtension(),
                artifact.getVersion(),
                null,
                file
        );
    }

    public org.eclipse.aether.metadata.Metadata toMetadata( Metadata metadata )
    {
        /*
        if ( metadata instanceof ProjectArtifactMetadata )
        {
            Artifact pomArtifact = new SubArtifact( mainArtifact, "", "pom" );
            pomArtifact = pomArtifact.setFile( ( (ProjectArtifactMetadata) metadata ).getFile() );
            request.addArtifact( pomArtifact );
        }
        else if ( // metadata instanceof SnapshotArtifactRepositoryMetadata ||
                metadata instanceof ArtifactRepositoryMetadata )
        {
            // eaten, handled by repo system
        }
        else if ( metadata instanceof org.apache.maven.shared.transfer.metadata.ArtifactMetadata )
        {
            org.apache.maven.shared.transfer.metadata.ArtifactMetadata transferMetadata =
                    (org.apache.maven.shared.transfer.metadata.ArtifactMetadata) metadata;

            request.addMetadata( new Maven31MetadataBridge( metadata ).setFile( transferMetadata.getFile() ) );
        }

         */
        // TODO
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    @Override
    public void registerListener( @Nonnull Listener listener )
    {
        listeners.add( nonNull( listener ) );
    }

    @Override
    public void unregisterListener( @Nonnull Listener listener )
    {
        listeners.remove( nonNull( listener ) );
    }

    @Nonnull
    @Override
    public Collection<Listener> getListeners()
    {
        return Collections.unmodifiableCollection( listeners );
    }

    //
    // Shortcut implementations
    //

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createLocal(...)</code>
     *
     * @see RepositoryFactory#createLocal(Path)
     */
    @Override
    public LocalRepository createLocalRepository( Path path )
    {
        return getService( RepositoryFactory.class ).createLocal( path );
    }

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createRemote(...)</code>
     *
     * @see RepositoryFactory#createRemote(String, String)
     */
    @Nonnull
    @Override
    public RemoteRepository createRemoteRepository( @Nonnull String id, @Nonnull String url )
    {
        return getService( RepositoryFactory.class )
                .createRemote( id, url );
    }

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createRemote(...)</code>
     *
     * @see RepositoryFactory#createRemote(Repository)
     */
    @Nonnull
    @Override
    public RemoteRepository createRemoteRepository( @Nonnull Repository repository )
    {
        return getService( RepositoryFactory.class )
                .createRemote( repository );
    }

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     *
     * @see ArtifactFactory#create(Session, String, String, String, String)
     */
    @Override
    public Artifact createArtifact( String groupId, String artifactId, String version, String extension )
    {
        return getService( ArtifactFactory.class )
                .create( this, groupId, artifactId, version, extension );
    }

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     *
     * @see ArtifactFactory#create(Session, String, String, String, String, String, String)
     */
    @Override
    public Artifact createArtifact( String groupId, String artifactId, String version, String classifier,
                                    String extension, String type )
    {
        return getService( ArtifactFactory.class )
                .create( this, groupId, artifactId, version, classifier, extension, type );
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Artifact)
     */
    @Override
    public Artifact resolveArtifact( Artifact artifact )
    {
        return getService( ArtifactResolver.class )
                .resolve( this, artifact )
                .getArtifact();
    }

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     *
     * @throws ArtifactInstallerException if the artifacts installation failed
     * @see ArtifactInstaller#install(Session, Collection)
     */
    @Override
    public void installArtifacts( Artifact... artifacts )
    {
        installArtifacts( Arrays.asList( artifacts ) );
    }

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     *
     * @throws ArtifactInstallerException if the artifacts installation failed
     * @see ArtifactInstaller#install(Session, Collection)
     */
    @Override
    public void installArtifacts( Collection<Artifact> artifacts )
    {
        getService( ArtifactInstaller.class )
                .install( this, artifacts );
    }

    /**
     * Shortcut for <code>getService(ArtifactDeployer.class).deploy(...)</code>
     *
     * @throws ArtifactDeployerException if the artifacts deployment failed
     * @see ArtifactDeployer#deploy(Session, RemoteRepository, Collection)
     */
    @Override
    public void deployArtifact( RemoteRepository repository, Artifact... artifacts )
    {
        getService( ArtifactDeployer.class )
                .deploy( this, repository, Arrays.asList( artifacts ) );
    }

    /**
     * Shortcut for <code>getService(ArtifactManager.class).setPath(...)</code>
     *
     * @see ArtifactManager#setPath(Artifact, Path)
     */
    @Override
    public void setArtifactPath( @Nonnull Artifact artifact, @Nonnull Path path )
    {
        getService( ArtifactManager.class )
                .setPath( artifact, path );
    }

    /**
     * Shortcut for <code>getService(ArtifactManager.class).getPath(...)</code>
     *
     * @see ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    @Override
    public Optional<Path> getArtifactPath( @Nonnull Artifact artifact )
    {
        return getService( ArtifactManager.class )
                .getPath( artifact );
    }

    /**
     * Shortcut for <code>getService(ArtifactManager.class).isSnapshot(...)</code>
     *
     * @see ArtifactManager#isSnapshot(String)
     */
    @Override
    public boolean isVersionSnapshot( @Nonnull String version )
    {
        return getService( ArtifactManager.class )
                .isSnapshot( version );
    }

    /**
     * Shortcut for <code>getService(DependencyFactory.class).create(...)</code>
     *
     * @see DependencyFactory#create(Session, Artifact)
     */
    @Nonnull
    @Override
    public Dependency createDependency( @Nonnull Artifact artifact )
    {
        return getService( DependencyFactory.class )
                .create( this, artifact );
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     *
     * @throws DependencyCollectorException if the dependency collection failed
     * @see DependencyCollector#collect(Session, Artifact)
     */
    @Nonnull
    @Override
    public Node collectDependencies( @Nonnull Artifact artifact )
    {
        return getService( DependencyCollector.class )
                .collect( this, artifact )
                .getRoot();
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     *
     * @throws DependencyCollectorException if the dependency collection failed
     * @see DependencyCollector#collect(Session, Project)
     */
    @Nonnull
    @Override
    public Node collectDependencies( @Nonnull Project project )
    {
        return getService( DependencyCollector.class )
                .collect( this, project )
                .getRoot();
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     *
     * @throws DependencyCollectorException if the dependency collection failed
     * @see DependencyCollector#collect(Session, Dependency)
     */
    @Nonnull
    @Override
    public Node collectDependencies( @Nonnull Dependency dependency )
    {
        return getService( DependencyCollector.class )
                .collect( this, dependency )
                .getRoot();
    }

    /**
     * Shortcut for <code>getService(DependencyResolver.class).resolve(...)</code>
     *
     * @throws DependencyResolverException if the dependency resolution failed
     * @see DependencyResolver#resolve(Session, Dependency, Predicate)
     */
    @Nonnull
    @Override
    public Node resolveDependencies( @Nonnull Dependency dependency )
    {
        return getService( DependencyResolver.class )
                .resolve( this, dependency, null )
                .getRoot();
    }

    @Override
    public Path getPathForLocalArtifact( @Nonnull Artifact artifact )
    {
        return getService( LocalRepositoryManager.class )
                .getPathForLocalArtifact( this, getLocalRepository(), artifact );
    }

    @Override
    public Path getPathForLocalMetadata( Metadata metadata )
    {
        return getService( LocalRepositoryManager.class )
                .getPathForLocalMetadata( this, getLocalRepository(), metadata );
    }

    @Override
    public Path getPathForRemoteArtifact( RemoteRepository remote, Artifact artifact )
    {
        return getService( LocalRepositoryManager.class )
                .getPathForRemoteArtifact( this, getLocalRepository(), remote, artifact );
    }

    @Override
    public Path getPathForRemoteMetadata( RemoteRepository remote, Metadata metadata )
    {
        return getService( LocalRepositoryManager.class )
                .getPathForRemoteMetadata( this, getLocalRepository(), remote, metadata );
    }

}
