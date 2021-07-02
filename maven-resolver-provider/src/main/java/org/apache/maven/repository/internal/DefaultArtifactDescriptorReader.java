package org.apache.maven.repository.internal;

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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.building.ArtifactModelSource;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicyRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

/**
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultArtifactDescriptorReader
    implements ArtifactDescriptorReader, Service
{
    private RemoteRepositoryManager remoteRepositoryManager;

    private VersionResolver versionResolver;

    private VersionRangeResolver versionRangeResolver;

    private ArtifactResolver artifactResolver;

    private RepositoryEventDispatcher repositoryEventDispatcher;

    private ModelBuilder modelBuilder;

    public DefaultArtifactDescriptorReader()
    {
        // enable no-arg constructor
    }

    @Inject
    DefaultArtifactDescriptorReader( RemoteRepositoryManager remoteRepositoryManager, VersionResolver versionResolver,
                                     VersionRangeResolver versionRangeResolver, ArtifactResolver artifactResolver,
                                     ModelBuilder modelBuilder, RepositoryEventDispatcher repositoryEventDispatcher )
    {
        setRemoteRepositoryManager( remoteRepositoryManager );
        setVersionResolver( versionResolver );
        setVersionRangeResolver( versionRangeResolver );
        setArtifactResolver( artifactResolver );
        setModelBuilder( modelBuilder );
        setRepositoryEventDispatcher( repositoryEventDispatcher );
    }

    public void initService( ServiceLocator locator )
    {
        setRemoteRepositoryManager( locator.getService( RemoteRepositoryManager.class ) );
        setVersionResolver( locator.getService( VersionResolver.class ) );
        setVersionRangeResolver( locator.getService( VersionRangeResolver.class ) );
        setArtifactResolver( locator.getService( ArtifactResolver.class ) );
        modelBuilder = locator.getService( ModelBuilder.class );
        if ( modelBuilder == null )
        {
            setModelBuilder( new DefaultModelBuilderFactory().newInstance() );
        }
        setRepositoryEventDispatcher( locator.getService( RepositoryEventDispatcher.class ) );
    }

    public DefaultArtifactDescriptorReader setRemoteRepositoryManager( RemoteRepositoryManager remoteRepositoryManager )
    {
        this.remoteRepositoryManager = Objects.requireNonNull( remoteRepositoryManager,
            "remoteRepositoryManager cannot be null" );
        return this;
    }

    public DefaultArtifactDescriptorReader setVersionResolver( VersionResolver versionResolver )
    {
        this.versionResolver = Objects.requireNonNull( versionResolver, "versionResolver cannot be null" );
        return this;
    }

    /** @since 3.2.2 */
    public DefaultArtifactDescriptorReader setVersionRangeResolver( VersionRangeResolver versionRangeResolver )
    {
        this.versionRangeResolver =
            Objects.requireNonNull( versionRangeResolver, "versionRangeResolver cannot be null" );
        return this;
    }

    public DefaultArtifactDescriptorReader setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = Objects.requireNonNull( artifactResolver, "artifactResolver cannot be null" );
        return this;
    }

    public DefaultArtifactDescriptorReader setRepositoryEventDispatcher(
        RepositoryEventDispatcher repositoryEventDispatcher )
    {
        this.repositoryEventDispatcher = Objects.requireNonNull( repositoryEventDispatcher,
            "repositoryEventDispatcher cannot be null" );
        return this;
    }

    public DefaultArtifactDescriptorReader setModelBuilder( ModelBuilder modelBuilder )
    {
        this.modelBuilder = Objects.requireNonNull( modelBuilder, "modelBuilder cannot be null" );
        return this;
    }

    public ArtifactDescriptorResult readArtifactDescriptor( RepositorySystemSession session,
                                                            ArtifactDescriptorRequest request )
        throws ArtifactDescriptorException
    {
        ArtifactDescriptorResult result = new ArtifactDescriptorResult( request );

        Model model = loadPom( session, request, result );
        if ( model != null )
        {
            Map<String, Object> config = session.getConfigProperties();
            ArtifactDescriptorReaderDelegate delegate =
                (ArtifactDescriptorReaderDelegate) config.get( ArtifactDescriptorReaderDelegate.class.getName() );

            if ( delegate == null )
            {
                delegate = new ArtifactDescriptorReaderDelegate();
            }

            delegate.populateResult( session, result, model );
        }

        return result;
    }

    private Model loadPom( RepositorySystemSession session, ArtifactDescriptorRequest request,
                           ArtifactDescriptorResult result )
        throws ArtifactDescriptorException
    {
        RequestTrace trace = RequestTrace.newChild( request.getTrace(), request );

        Set<String> visited = new LinkedHashSet<>();
        for ( Artifact a = request.getArtifact();; )
        {
            Artifact pomArtifact = ArtifactDescriptorUtils.toPomArtifact( a );
            try
            {
                VersionRequest versionRequest =
                    new VersionRequest( a, request.getRepositories(), request.getRequestContext() );
                versionRequest.setTrace( trace );
                VersionResult versionResult = versionResolver.resolveVersion( session, versionRequest );

                a = a.setVersion( versionResult.getVersion() );

                versionRequest =
                    new VersionRequest( pomArtifact, request.getRepositories(), request.getRequestContext() );
                versionRequest.setTrace( trace );
                versionResult = versionResolver.resolveVersion( session, versionRequest );

                pomArtifact = pomArtifact.setVersion( versionResult.getVersion() );
            }
            catch ( VersionResolutionException e )
            {
                result.addException( e );
                throw new ArtifactDescriptorException( result );
            }

            if ( !visited.add( a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getBaseVersion() ) )
            {
                RepositoryException exception =
                    new RepositoryException( "Artifact relocations form a cycle: " + visited );
                invalidDescriptor( session, trace, a, exception );
                if ( ( getPolicy( session, a, request ) & ArtifactDescriptorPolicy.IGNORE_INVALID ) != 0 )
                {
                    return null;
                }
                result.addException( exception );
                throw new ArtifactDescriptorException( result );
            }

            ArtifactResult resolveResult;
            try
            {
                ArtifactRequest resolveRequest =
                    new ArtifactRequest( pomArtifact, request.getRepositories(), request.getRequestContext() );
                resolveRequest.setTrace( trace );
                resolveResult = artifactResolver.resolveArtifact( session, resolveRequest );
                pomArtifact = resolveResult.getArtifact();
                result.setRepository( resolveResult.getRepository() );
            }
            catch ( ArtifactResolutionException e )
            {
                if ( e.getCause() instanceof ArtifactNotFoundException )
                {
                    missingDescriptor( session, trace, a, (Exception) e.getCause() );
                    if ( ( getPolicy( session, a, request ) & ArtifactDescriptorPolicy.IGNORE_MISSING ) != 0 )
                    {
                        return null;
                    }
                }
                result.addException( e );
                throw new ArtifactDescriptorException( result );
            }

            Model model;

            // TODO hack: don't rebuild model if it was already loaded during reactor resolution
            final WorkspaceReader workspace = session.getWorkspaceReader();
            if ( workspace instanceof MavenWorkspaceReader )
            {
                model = ( (MavenWorkspaceReader) workspace ).findModel( pomArtifact );
                if ( model != null )
                {
                    return model;
                }
            }

            try
            {
                ModelBuildingRequest modelRequest = new DefaultModelBuildingRequest();
                modelRequest.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
                modelRequest.setProcessPlugins( false );
                modelRequest.setTwoPhaseBuilding( false );
                modelRequest.setSystemProperties( toProperties( session.getUserProperties(),
                                                                session.getSystemProperties() ) );
                modelRequest.setModelCache( DefaultModelCache.newInstance( session ) );
                modelRequest.setModelResolver( new DefaultModelResolver( session, trace.newChild( modelRequest ),
                                                                         request.getRequestContext(), artifactResolver,
                                                                         versionRangeResolver, remoteRepositoryManager,
                                                                         request.getRepositories() ) );
                if ( resolveResult.getRepository() instanceof WorkspaceRepository )
                {
                    modelRequest.setPomFile( pomArtifact.getFile() );
                }
                else
                {
                    modelRequest.setModelSource( new ArtifactModelSource( pomArtifact.getFile(),
                                                                          pomArtifact.getGroupId(),
                                                                          pomArtifact.getArtifactId(),
                                                                          pomArtifact.getVersion() ) );
                }

                model = modelBuilder.build( modelRequest ).getEffectiveModel();
            }
            catch ( ModelBuildingException e )
            {
                for ( ModelProblem problem : e.getProblems() )
                {
                    if ( problem.getException() instanceof UnresolvableModelException )
                    {
                        result.addException( problem.getException() );
                        throw new ArtifactDescriptorException( result );
                    }
                }
                invalidDescriptor( session, trace, a, e );
                if ( ( getPolicy( session, a, request ) & ArtifactDescriptorPolicy.IGNORE_INVALID ) != 0 )
                {
                    return null;
                }
                result.addException( e );
                throw new ArtifactDescriptorException( result );
            }

            Relocation relocation = getRelocation( model );

            if ( relocation != null )
            {
                result.addRelocation( a );
                a =
                    new RelocatedArtifact( a, relocation.getGroupId(), relocation.getArtifactId(),
                                           relocation.getVersion() );
                result.setArtifact( a );
            }
            else
            {
                return model;
            }
        }
    }

    private Properties toProperties( Map<String, String> dominant, Map<String, String> recessive )
    {
        Properties props = new Properties();
        if ( recessive != null )
        {
            props.putAll( recessive );
        }
        if ( dominant != null )
        {
            props.putAll( dominant );
        }
        return props;
    }

    private Relocation getRelocation( Model model )
    {
        Relocation relocation = null;
        DistributionManagement distMgmt = model.getDistributionManagement();
        if ( distMgmt != null )
        {
            relocation = distMgmt.getRelocation();
        }
        return relocation;
    }

    private void missingDescriptor( RepositorySystemSession session, RequestTrace trace, Artifact artifact,
                                    Exception exception )
    {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder( session, EventType.ARTIFACT_DESCRIPTOR_MISSING );
        event.setTrace( trace );
        event.setArtifact( artifact );
        event.setException( exception );

        repositoryEventDispatcher.dispatch( event.build() );
    }

    private void invalidDescriptor( RepositorySystemSession session, RequestTrace trace, Artifact artifact,
                                    Exception exception )
    {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder( session, EventType.ARTIFACT_DESCRIPTOR_INVALID );
        event.setTrace( trace );
        event.setArtifact( artifact );
        event.setException( exception );

        repositoryEventDispatcher.dispatch( event.build() );
    }

    private int getPolicy( RepositorySystemSession session, Artifact a, ArtifactDescriptorRequest request )
    {
        ArtifactDescriptorPolicy policy = session.getArtifactDescriptorPolicy();
        if ( policy == null )
        {
            return ArtifactDescriptorPolicy.STRICT;
        }
        return policy.getPolicy( session, new ArtifactDescriptorPolicyRequest( a, request.getRequestContext() ) );
    }

}
