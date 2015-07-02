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

import java.util.Iterator;
import javax.inject.Named;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.spi.log.Logger;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.eclipse.aether.spi.log.NullLoggerFactory;
import org.eclipse.aether.version.Version;

/**
 * This implementation resolve <b>only released</b> artifact versions in a version range.
 *
 */
@Named
@Component( role = VersionRangeResolver.class, hint = OnlyReleaseVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY )
public class OnlyReleaseVersionRangeResolver
        implements VersionRangeResolver, Service
{

  public static final String VERSION_RANGE_RESOLVER_STRATEGY = "onlyReleases";

  @SuppressWarnings( "unused" )
  @Requirement( role = LoggerFactory.class )
  private Logger logger = NullLoggerFactory.LOGGER;

  @Requirement( hint = MavenDefaultVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY )
  private VersionRangeResolver delegateVersionRangeResolver;

  public OnlyReleaseVersionRangeResolver()
  {
    // enable default constructor
  }

  @Override
  public void initService( ServiceLocator locator )
  {
    setLoggerFactory( locator.getService( LoggerFactory.class ) );
    MavenDefaultVersionRangeResolver mavenDefaultVersionRangeResolver = new MavenDefaultVersionRangeResolver();
    mavenDefaultVersionRangeResolver.initService( locator );
    setDelegateVersionRangeResolver( mavenDefaultVersionRangeResolver );
  }

  public final OnlyReleaseVersionRangeResolver setLoggerFactory( LoggerFactory loggerFactory )
  {
    this.logger = NullLoggerFactory.getSafeLogger( loggerFactory, getClass() );
    return this;
  }

  void setLogger( LoggerFactory loggerFactory )
  {
    // plexus support
    setLoggerFactory( loggerFactory );
  }

  public final OnlyReleaseVersionRangeResolver setDelegateVersionRangeResolver(
          VersionRangeResolver delegateVersionRangeResolver )
  {
    if ( delegateVersionRangeResolver == null )
    {
      throw new IllegalArgumentException( "all version range resolver has not been specified" );
    }
    this.delegateVersionRangeResolver = delegateVersionRangeResolver;
    return this;
  }

  @Override
  public VersionRangeResult resolveVersionRange( RepositorySystemSession session, VersionRangeRequest request )
          throws VersionRangeResolutionException
  {
    VersionRangeResult resolveVersionRange = delegateVersionRangeResolver.resolveVersionRange( session, request );
    for ( Iterator<Version> it = resolveVersionRange.getVersions().iterator(); it.hasNext(); )
    {
      String version = it.next().toString();
      // XXX: better way to identify a SNAPSHOT version
      if ( version.endsWith( "SNAPSHOT" ) )
      {
        it.remove();
      }
    }
    return resolveVersionRange;
  }

}
