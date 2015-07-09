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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.spi.log.Logger;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.eclipse.aether.spi.log.NullLoggerFactory;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

/**
 * This implementation ({@value #VERSION_RANGE_RESOLVER_STRATEGY}) resolve all artifact versions in a version range.
 * <p>
 * version range: (,3.0.0]<br/>
 * found versions: 1.0.0-SNAPSHOT,1.0.0, 1.0.1-SNAPSHOT, 2.0.0-SNAPSHOT, 2.0.0, 2.1.0-SNAPSHOT, 3.0.0-SNAPSHOT<br/>
 * returned versions: 1.0.0-SNAPSHOT,1.0.0, 1.0.1-SNAPSHOT, 2.0.0-SNAPSHOT, 2.0.0, 2.1.0-SNAPSHOT, 3.0.0-SNAPSHOT
 * </p>
 *
 * <p>
 * Note: This implementation is the default version range resolve behavior of Maven.</p>
 * <p>
 * Note: This class was formally known as <code>DefaultVersionRangeResolver</code>.</p>
 *
 * @author Benjamin Bentmann
 */

@Named
@Component( role = VersionRangeResolver.class, hint = MavenDefaultVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY )
public class MavenDefaultVersionRangeResolver
        implements VersionRangeResolver, Service
{

  public static final String VERSION_RANGE_RESOLVER_STRATEGY = "mavenDefault";

  private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

  @SuppressWarnings( "unused" )
  @Requirement( role = LoggerFactory.class )
  private Logger logger = NullLoggerFactory.LOGGER;

  @Requirement
  private MetadataResolver metadataResolver;

  @Requirement
  private SyncContextFactory syncContextFactory;

  @Requirement
  private RepositoryEventDispatcher repositoryEventDispatcher;

  public MavenDefaultVersionRangeResolver()
  {
    // enable default constructor
  }

  @Inject
  MavenDefaultVersionRangeResolver( final MetadataResolver metadataResolver,
          final SyncContextFactory syncContextFactory, final RepositoryEventDispatcher repositoryEventDispatcher,
          final LoggerFactory loggerFactory )
  {
    setMetadataResolver( metadataResolver );
    setSyncContextFactory( syncContextFactory );
    setLoggerFactory( loggerFactory );
    setRepositoryEventDispatcher( repositoryEventDispatcher );
  }

  @Override
  public void initService( final ServiceLocator locator )
  {
    setLoggerFactory( locator.getService( LoggerFactory.class ) );
    setMetadataResolver( locator.getService( MetadataResolver.class ) );
    setSyncContextFactory( locator.getService( SyncContextFactory.class ) );
    setRepositoryEventDispatcher( locator.getService( RepositoryEventDispatcher.class ) );
  }

  public final MavenDefaultVersionRangeResolver setLoggerFactory( final LoggerFactory loggerFactory )
  {
    this.logger = NullLoggerFactory.getSafeLogger( loggerFactory, getClass() );
    return this;
  }

  void setLogger( final LoggerFactory loggerFactory )
  {
    // plexus support
    setLoggerFactory( loggerFactory );
  }

  public final MavenDefaultVersionRangeResolver setMetadataResolver( final MetadataResolver metadataResolver )
  {
    if ( metadataResolver == null )
    {
      throw new IllegalArgumentException( "metadata resolver has not been specified" );
    }
    this.metadataResolver = metadataResolver;
    return this;
  }

  public final MavenDefaultVersionRangeResolver setSyncContextFactory( final SyncContextFactory syncContextFactory )
  {
    if ( syncContextFactory == null )
    {
      throw new IllegalArgumentException( "sync context factory has not been specified" );
    }
    this.syncContextFactory = syncContextFactory;
    return this;
  }

  public final MavenDefaultVersionRangeResolver setRepositoryEventDispatcher( final RepositoryEventDispatcher red )
  {
    if ( red == null )
    {
      throw new IllegalArgumentException( "repository event dispatcher has not been specified" );
    }
    this.repositoryEventDispatcher = red;
    return this;
  }

  @Override
  public VersionRangeResult resolveVersionRange( final RepositorySystemSession session,
          final VersionRangeRequest request )
          throws VersionRangeResolutionException
  {
    final VersionRangeResult result = new VersionRangeResult( request );

    final VersionScheme versionScheme = new GenericVersionScheme();

    final VersionConstraint versionConstraint;
    try
    {
      versionConstraint = versionScheme.parseVersionConstraint( request.getArtifact().getVersion() );
    }
    catch ( InvalidVersionSpecificationException e )
    {
      result.addException( e );
      throw new VersionRangeResolutionException( result );
    }

    result.setVersionConstraint( versionConstraint );

    if ( versionConstraint.getRange() == null )
    {
      result.addVersion( versionConstraint.getVersion() );
    }
    else
    {
      final Map<String, ArtifactRepository> versionIndex = getVersions( session, result, request );

      final List<Version> versions = new ArrayList<Version>();
      for ( Map.Entry<String, ArtifactRepository> v : versionIndex.entrySet() )
      {
        try
        {
          final Version ver = versionScheme.parseVersion( v.getKey() );
          if ( versionConstraint.containsVersion( ver ) )
          {
            versions.add( ver );
            result.setRepository( ver, v.getValue() );
          }
        }
        catch ( InvalidVersionSpecificationException e )
        {
          result.addException( e );
        }
      }

      Collections.sort( versions );
      result.setVersions( versions );
    }

    return result;
  }

  private Map<String, ArtifactRepository> getVersions( final RepositorySystemSession session,
          final VersionRangeResult result, final VersionRangeRequest request )
  {
    final RequestTrace trace = RequestTrace.newChild( request.getTrace(), request );

    final Map<String, ArtifactRepository> versionIndex = new HashMap<String, ArtifactRepository>();

    final Metadata metadata
            = new DefaultMetadata( request.getArtifact().getGroupId(), request.getArtifact().getArtifactId(),
                    MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT );

    final List<MetadataRequest> metadataRequests = new ArrayList<MetadataRequest>( request.getRepositories().size() );

    metadataRequests.add( new MetadataRequest( metadata, null, request.getRequestContext() ) );

    for ( final RemoteRepository repository : request.getRepositories() )
    {
      final MetadataRequest metadataRequest = new MetadataRequest( metadata, repository, request.getRequestContext() );
      metadataRequest.setDeleteLocalCopyIfMissing( true );
      metadataRequest.setTrace( trace );
      metadataRequests.add( metadataRequest );
    }

    final List<MetadataResult> metadataResults = metadataResolver.resolveMetadata( session, metadataRequests );

    final WorkspaceReader workspace = session.getWorkspaceReader();
    if ( workspace != null )
    {
      final List<String> versions = workspace.findVersions( request.getArtifact() );
      for ( final String version : versions )
      {
        versionIndex.put( version, workspace.getRepository() );
      }
    }

    for ( final MetadataResult metadataResult : metadataResults )
    {
      result.addException( metadataResult.getException() );

      ArtifactRepository repository = metadataResult.getRequest().getRepository();
      if ( repository == null )
      {
        repository = session.getLocalRepository();
      }

      final Versioning versioning = readVersions( session, trace, metadataResult.getMetadata(), repository, result );
      for ( final String version : versioning.getVersions() )
      {
        if ( !versionIndex.containsKey( version ) )
        {
          versionIndex.put( version, repository );
        }
      }
    }

    return versionIndex;
  }

  private Versioning readVersions( final RepositorySystemSession session, final RequestTrace trace,
          final Metadata metadata, final ArtifactRepository repository, final VersionRangeResult result )
  {
    Versioning versioning = null;

    FileInputStream fis = null;
    try
    {
      if ( metadata != null )
      {
        final SyncContext syncContext = syncContextFactory.newInstance( session, true );

        try
        {
          syncContext.acquire( null, Collections.singleton( metadata ) );

          if ( metadata.getFile() != null && metadata.getFile().exists() )
          {
            fis = new FileInputStream( metadata.getFile() );
            org.apache.maven.artifact.repository.metadata.Metadata m
                    = new MetadataXpp3Reader().read( fis, false );
            versioning = m.getVersioning();
          }
        }
        finally
        {
          syncContext.close();
        }
      }
    }
    catch ( Exception e )
    {
      invalidMetadata( session, trace, metadata, repository, e );
      result.addException( e );
    }
    finally
    {
      IOUtil.close( fis );
    }

    return ( versioning != null ) ? versioning : new Versioning();
  }

  private void invalidMetadata( final RepositorySystemSession session, final RequestTrace trace,
          final Metadata metadata, final ArtifactRepository repository, final Exception exception )
  {
    final RepositoryEvent.Builder event = new RepositoryEvent.Builder( session, EventType.METADATA_INVALID );
    event.setTrace( trace );
    event.setMetadata( metadata );
    event.setException( exception );
    event.setRepository( repository );

    repositoryEventDispatcher.dispatch( event.build() );
  }

}
