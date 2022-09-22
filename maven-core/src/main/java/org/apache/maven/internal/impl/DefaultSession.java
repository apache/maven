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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.ArtifactDeployer;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.ArtifactInstaller;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.CoordinateFactory;
import org.apache.maven.api.services.DependencyCollector;
import org.apache.maven.api.services.DependencyFactory;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.LocalRepositoryManager;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.api.services.Prompter;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.ToolchainManager;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.services.xml.ToolchainsXmlFactory;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.toolchain.DefaultToolchainManagerPrivate;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultSession extends AbstractSession
{

    private final MavenSession mavenSession;
    private final RepositorySystemSession session;
    private final RepositorySystem repositorySystem;
    private final List<RemoteRepository> repositories;
    private final org.apache.maven.project.ProjectBuilder projectBuilder;
    private final MavenRepositorySystem mavenRepositorySystem;
    private final DefaultToolchainManagerPrivate toolchainManagerPrivate;
    private final ArtifactManager artifactManager;
    private final ProjectManager projectManager;
    private final PlexusContainer container;
    private final MojoExecutionScope mojoExecutionScope;
    private final RuntimeInformation runtimeInformation;
    private final ArtifactHandlerManager artifactHandlerManager;

    @SuppressWarnings( "checkstyle:ParameterNumber" )
    public DefaultSession( @Nonnull MavenSession session,
                           @Nonnull RepositorySystem repositorySystem,
                           @Nullable List<RemoteRepository> repositories,
                           @Nonnull org.apache.maven.project.ProjectBuilder projectBuilder,
                           @Nonnull MavenRepositorySystem mavenRepositorySystem,
                           @Nonnull DefaultToolchainManagerPrivate toolchainManagerPrivate,
                           @Nonnull PlexusContainer container,
                           @Nonnull MojoExecutionScope mojoExecutionScope,
                           @Nonnull RuntimeInformation runtimeInformation,
                           @Nonnull ArtifactHandlerManager artifactHandlerManager )
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
        this.container = container;
        this.mojoExecutionScope = mojoExecutionScope;
        this.runtimeInformation = runtimeInformation;
        this.artifactManager = new DefaultArtifactManager( this );
        this.projectManager = new DefaultProjectManager( this, artifactManager, container );
        this.artifactHandlerManager = artifactHandlerManager;
    }

    public MavenSession getMavenSession()
    {
        return mavenSession;
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
    public String getMavenVersion()
    {
        return runtimeInformation.getMavenVersion();
    }

    @Override
    public int getDegreeOfConcurrency()
    {
        return mavenSession.getRequest().getDegreeOfConcurrency();
    }

    @Nonnull
    @Override
    public Instant getStartTime()
    {
        return mavenSession.getStartTime().toInstant();
    }

    @Nonnull
    @Override
    public Path getMultiModuleProjectDirectory()
    {
        return mavenSession.getRequest().getMultiModuleProjectDirectory().toPath();
    }

    @Nonnull
    @Override
    public Path getExecutionRootDirectory()
    {
        return Paths.get( mavenSession.getRequest().getBaseDirectory() );
    }

    @Nonnull
    @Override
    public List<Project> getProjects()
    {
        return getProjects( mavenSession.getProjects() );
    }

    @Nonnull
    @Override
    public Map<String, Object> getPluginContext( Project project )
    {
        nonNull( project, "project" );
        try
        {
            MojoExecution mojoExecution = container.lookup( MojoExecution.class );
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
            PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
            return mavenSession.getPluginContext( pluginDescriptor, ( ( DefaultProject ) project ).getProject() );
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenException( "The PluginContext is only available during a mojo execution", e );
        }
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
        return new DefaultSession( newSession, repositorySystem, repositories, projectBuilder, mavenRepositorySystem,
                toolchainManagerPrivate, container, mojoExecutionScope, runtimeInformation, artifactHandlerManager );
    }

    @Nonnull
    @Override
    public Session withRemoteRepositories( @Nonnull List<RemoteRepository> repositories )
    {
        return new DefaultSession( mavenSession, repositorySystem, repositories, projectBuilder, mavenRepositorySystem,
                toolchainManagerPrivate, container, mojoExecutionScope, runtimeInformation, artifactHandlerManager );
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
        else if ( clazz == Prompter.class )
        {
            return (T) new DefaultPrompter( container );
        }
        else if ( clazz == MessageBuilderFactory.class )
        {
            return (T) new DefaultMessageBuilderFactory();
        }
        else if ( clazz == VersionParser.class )
        {
            return (T) new DefaultVersionParser();
        }
        else if ( clazz == CoordinateFactory.class )
        {
            return (T) new DefaultCoordinateFactory();
        }
        else if ( clazz == TypeRegistry.class )
        {
            return (T) new DefaultTypeRegistry( artifactHandlerManager );
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

    public ArtifactRepository toArtifactRepository( RemoteRepository repository )
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

    public org.eclipse.aether.graph.Dependency toDependency( Dependency dependency )
    {
        if ( dependency instanceof DefaultDependency )
        {
            return ( (DefaultDependency) dependency ).getDependency();
        }
        else
        {
            return new org.eclipse.aether.graph.Dependency(
                    new org.eclipse.aether.artifact.DefaultArtifact(
                            dependency.getGroupId(),
                            dependency.getArtifactId(),
                            dependency.getClassifier(),
                            dependency.getType().getExtension(),
                            dependency.getVersion().toString(),
                            null ),
                    dependency.getScope().id() );
        }
    }

}
