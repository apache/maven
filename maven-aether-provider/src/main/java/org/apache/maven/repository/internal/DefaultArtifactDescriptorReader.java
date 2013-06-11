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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.VersionResolver;
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
import org.eclipse.aether.spi.log.Logger;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.eclipse.aether.spi.log.NullLoggerFactory;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

/**
 * @author Benjamin Bentmann
 */
@Named
@Component( role = ArtifactDescriptorReader.class )
public class DefaultArtifactDescriptorReader
    implements ArtifactDescriptorReader, Service
{

    @SuppressWarnings( "unused" )
    @Requirement( role = LoggerFactory.class )
    private Logger logger = NullLoggerFactory.LOGGER;

    @Requirement
    private RemoteRepositoryManager remoteRepositoryManager;

    @Requirement
    private VersionResolver versionResolver;

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement
    private RepositoryEventDispatcher repositoryEventDispatcher;

    @Requirement
    private ModelBuilder modelBuilder;

    public DefaultArtifactDescriptorReader()
    {
        // enable no-arg constructor
    }

    @Inject
    DefaultArtifactDescriptorReader( RemoteRepositoryManager remoteRepositoryManager, VersionResolver versionResolver,
                                     ArtifactResolver artifactResolver, ModelBuilder modelBuilder,
                                     RepositoryEventDispatcher repositoryEventDispatcher, LoggerFactory loggerFactory )
    {
        setRemoteRepositoryManager( remoteRepositoryManager );
        setVersionResolver( versionResolver );
        setArtifactResolver( artifactResolver );
        setModelBuilder( modelBuilder );
        setLoggerFactory( loggerFactory );
        setRepositoryEventDispatcher( repositoryEventDispatcher );
    }

    public void initService( ServiceLocator locator )
    {
        setLoggerFactory( locator.getService( LoggerFactory.class ) );
        setRemoteRepositoryManager( locator.getService( RemoteRepositoryManager.class ) );
        setVersionResolver( locator.getService( VersionResolver.class ) );
        setArtifactResolver( locator.getService( ArtifactResolver.class ) );
        setRepositoryEventDispatcher( locator.getService( RepositoryEventDispatcher.class ) );
        modelBuilder = locator.getService( ModelBuilder.class );
        if ( modelBuilder == null )
        {
            setModelBuilder( new DefaultModelBuilderFactory().newInstance() );
        }
    }

    public DefaultArtifactDescriptorReader setLoggerFactory( LoggerFactory loggerFactory )
    {
        this.logger = NullLoggerFactory.getSafeLogger( loggerFactory, getClass() );
        return this;
    }

    void setLogger( LoggerFactory loggerFactory )
    {
        // plexus support
        setLoggerFactory( loggerFactory );
    }

    public DefaultArtifactDescriptorReader setRemoteRepositoryManager( RemoteRepositoryManager remoteRepositoryManager )
    {
        if ( remoteRepositoryManager == null )
        {
            throw new IllegalArgumentException( "remote repository manager has not been specified" );
        }
        this.remoteRepositoryManager = remoteRepositoryManager;
        return this;
    }

    public DefaultArtifactDescriptorReader setVersionResolver( VersionResolver versionResolver )
    {
        if ( versionResolver == null )
        {
            throw new IllegalArgumentException( "version resolver has not been specified" );
        }
        this.versionResolver = versionResolver;
        return this;
    }

    public DefaultArtifactDescriptorReader setArtifactResolver( ArtifactResolver artifactResolver )
    {
        if ( artifactResolver == null )
        {
            throw new IllegalArgumentException( "artifact resolver has not been specified" );
        }
        this.artifactResolver = artifactResolver;
        return this;
    }

    public DefaultArtifactDescriptorReader setRepositoryEventDispatcher( RepositoryEventDispatcher repositoryEventDispatcher )
    {
        if ( repositoryEventDispatcher == null )
        {
            throw new IllegalArgumentException( "repository event dispatcher has not been specified" );
        }
        this.repositoryEventDispatcher = repositoryEventDispatcher;
        return this;
    }

    public DefaultArtifactDescriptorReader setModelBuilder( ModelBuilder modelBuilder )
    {
        if ( modelBuilder == null )
        {
            throw new IllegalArgumentException( "model builder has not been specified" );
        }
        this.modelBuilder = modelBuilder;
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
            ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

            for ( Repository r : model.getRepositories() )
            {
                result.addRepository( ArtifactDescriptorUtils.toRemoteRepository( r ) );
            }

            for ( org.apache.maven.model.Dependency dependency : model.getDependencies() )
            {
                result.addDependency( convert( dependency, stereotypes ) );
            }

            DependencyManagement mngt = model.getDependencyManagement();
            if ( mngt != null )
            {
                for ( org.apache.maven.model.Dependency dependency : mngt.getDependencies() )
                {
                    result.addManagedDependency( convert( dependency, stereotypes ) );
                }
            }

            Map<String, Object> properties = new LinkedHashMap<String, Object>();

            Prerequisites prerequisites = model.getPrerequisites();
            if ( prerequisites != null )
            {
                properties.put( "prerequisites.maven", prerequisites.getMaven() );
            }

            List<License> licenses = model.getLicenses();
            properties.put( "license.count", licenses.size() );
            for ( int i = 0; i < licenses.size(); i++ )
            {
                License license = licenses.get( i );
                properties.put( "license." + i + ".name", license.getName() );
                properties.put( "license." + i + ".url", license.getUrl() );
                properties.put( "license." + i + ".comments", license.getComments() );
                properties.put( "license." + i + ".distribution", license.getDistribution() );
            }

            result.setProperties( properties );

            setArtifactProperties( result, model );
        }

        return result;
    }

    private Model loadPom( RepositorySystemSession session, ArtifactDescriptorRequest request,
                           ArtifactDescriptorResult result )
        throws ArtifactDescriptorException
    {
        RequestTrace trace = RequestTrace.newChild( request.getTrace(), request );

        Set<String> visited = new LinkedHashSet<String>();
        for ( Artifact artifact = request.getArtifact();; )
        {
            try
            {
                VersionRequest versionRequest =
                    new VersionRequest( artifact, request.getRepositories(), request.getRequestContext() );
                versionRequest.setTrace( trace );
                VersionResult versionResult = versionResolver.resolveVersion( session, versionRequest );

                artifact = artifact.setVersion( versionResult.getVersion() );
            }
            catch ( VersionResolutionException e )
            {
                result.addException( e );
                throw new ArtifactDescriptorException( result );
            }

            if ( !visited.add( artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getBaseVersion() ) )
            {
                RepositoryException exception =
                    new RepositoryException( "Artifact relocations form a cycle: " + visited );
                invalidDescriptor( session, trace, artifact, exception );
                if ( ( getPolicy( session, artifact, request ) & ArtifactDescriptorPolicy.IGNORE_INVALID ) != 0 )
                {
                    return null;
                }
                result.addException( exception );
                throw new ArtifactDescriptorException( result );
            }

            Artifact pomArtifact = ArtifactDescriptorUtils.toPomArtifact( artifact );

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
                    missingDescriptor( session, trace, artifact, (Exception) e.getCause() );
                    if ( ( getPolicy( session, artifact, request ) & ArtifactDescriptorPolicy.IGNORE_MISSING ) != 0 )
                    {
                        return null;
                    }
                }
                result.addException( e );
                throw new ArtifactDescriptorException( result );
            }

            Model model;
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
                                                                         remoteRepositoryManager,
                                                                         request.getRepositories() ) );
                if ( resolveResult.getRepository() instanceof WorkspaceRepository )
                {
                    modelRequest.setPomFile( pomArtifact.getFile() );
                }
                else
                {
                    modelRequest.setModelSource( new FileModelSource( pomArtifact.getFile() ) );
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
                invalidDescriptor( session, trace, artifact, e );
                if ( ( getPolicy( session, artifact, request ) & ArtifactDescriptorPolicy.IGNORE_INVALID ) != 0 )
                {
                    return null;
                }
                result.addException( e );
                throw new ArtifactDescriptorException( result );
            }

            Relocation relocation = getRelocation( model );

            if ( relocation != null )
            {
                result.addRelocation( artifact );
                artifact =
                    new RelocatedArtifact( artifact, relocation.getGroupId(), relocation.getArtifactId(),
                                           relocation.getVersion() );
                result.setArtifact( artifact );
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
        DistributionManagement distMngt = model.getDistributionManagement();
        if ( distMngt != null )
        {
            relocation = distMngt.getRelocation();
        }
        return relocation;
    }

    private void setArtifactProperties( ArtifactDescriptorResult result, Model model )
    {
        String downloadUrl = null;
        DistributionManagement distMngt = model.getDistributionManagement();
        if ( distMngt != null )
        {
            downloadUrl = distMngt.getDownloadUrl();
        }
        if ( downloadUrl != null && downloadUrl.length() > 0 )
        {
            Artifact artifact = result.getArtifact();
            Map<String, String> props = new HashMap<String, String>( artifact.getProperties() );
            props.put( ArtifactProperties.DOWNLOAD_URL, downloadUrl );
            result.setArtifact( artifact.setProperties( props ) );
        }
    }

    private Dependency convert( org.apache.maven.model.Dependency dependency, ArtifactTypeRegistry stereotypes )
    {
        ArtifactType stereotype = stereotypes.get( dependency.getType() );
        if ( stereotype == null )
        {
            stereotype = new DefaultArtifactType( dependency.getType() );
        }

        boolean system = dependency.getSystemPath() != null && dependency.getSystemPath().length() > 0;

        Map<String, String> props = null;
        if ( system )
        {
            props = Collections.singletonMap( ArtifactProperties.LOCAL_PATH, dependency.getSystemPath() );
        }

        Artifact artifact =
            new DefaultArtifact( dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), null,
                                 dependency.getVersion(), props, stereotype );

        List<Exclusion> exclusions = new ArrayList<Exclusion>( dependency.getExclusions().size() );
        for ( org.apache.maven.model.Exclusion exclusion : dependency.getExclusions() )
        {
            exclusions.add( convert( exclusion ) );
        }

        Dependency result = new Dependency( artifact, dependency.getScope(), dependency.isOptional(), exclusions );

        return result;
    }

    private Exclusion convert( org.apache.maven.model.Exclusion exclusion )
    {
        return new Exclusion( exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*" );
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

    private int getPolicy( RepositorySystemSession session, Artifact artifact, ArtifactDescriptorRequest request )
    {
        ArtifactDescriptorPolicy policy = session.getArtifactDescriptorPolicy();
        if ( policy == null )
        {
            return ArtifactDescriptorPolicy.STRICT;
        }
        return policy.getPolicy( session, new ArtifactDescriptorPolicyRequest( artifact, request.getRequestContext() ) );
    }

}
