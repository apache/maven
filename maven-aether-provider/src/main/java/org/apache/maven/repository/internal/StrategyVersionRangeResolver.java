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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
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

/**
 * This {@link VersionRangeResolver} implementation choose a named {@link VersionRangeResolver} based on system property
 * {@value StrategyVersionRangeResolver#STRATEGY_PROPERTY_KEY}.
 *
 * <p>
 * This implementation is the default implementation of {@link VersionRangeResolver}.
 * It choose the concrete strategy out of all registered {@link VersionRangeResolver} instances.
 * <br/>
 * If the strategy is unknown the {@link MavenDefaultVersionRangeResolver}
 * ({@value MavenDefaultVersionRangeResolver#VERSION_RANGE_RESOLVER_STRATEGY}) is used.
 * </p>
 */
@Named
@Component( role = VersionRangeResolver.class )
public class StrategyVersionRangeResolver
        implements VersionRangeResolver, Service
{

  /**
   * system property key providing the {@link VersionRangeResolver} strategy.
   */
  public static final String STRATEGY_PROPERTY_KEY = "maven.versionRangeResolver.strategy";

  @SuppressWarnings( "unused" )
  @Requirement( role = LoggerFactory.class )
  private Logger logger = NullLoggerFactory.LOGGER;

  @Requirement( role = VersionRangeResolver.class,
          hint = MavenDefaultVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY )
  private VersionRangeResolver defaultMavenVersionRangeResolver;

  private VersionRangeResolver actualVersionRangeResolver;

  // the key of the entry is the component`s hint.
  @Requirement( role = VersionRangeResolver.class )
  private Map<String, VersionRangeResolver> versionrangeResolverStrategies
          = new HashMap<String, VersionRangeResolver>();

  public StrategyVersionRangeResolver()
  {
    // enable default constructor
  }

  @Inject
  StrategyVersionRangeResolver( LoggerFactory loggerFactory,
          Map<String, VersionRangeResolver> versionRangeResolverStrategies )
  {
    setLoggerFactory( loggerFactory );
    setVersionRangeResolverStrategies( versionRangeResolverStrategies );
  }

  @Override
  public void initService( ServiceLocator locator )
  {
    setLoggerFactory( locator.getService( LoggerFactory.class ) );
    setVersionRangeResolverStrategies( locator.getServices( VersionRangeResolver.class ) );
  }

  public final StrategyVersionRangeResolver setLoggerFactory( LoggerFactory loggerFactory )
  {
    this.logger = NullLoggerFactory.getSafeLogger( loggerFactory, getClass() );
    return this;
  }

  void setLogger( LoggerFactory loggerFactory )
  {
    // plexus support
    setLoggerFactory( loggerFactory );
  }

  /**
   * Replace all {@link VersionRangeResolver} strategies with the passed list.
   *
   * @param versionRangeResolverStrategies must not be {@code null}
   *
   * @return this instance for fluent API
   *
   * @throws IllegalArgumentException if the passed list is {@code null}
   */
  public final StrategyVersionRangeResolver setVersionRangeResolverStrategies(
          List<VersionRangeResolver> versionRangeResolverStrategies )
  {
    if ( null == versionRangeResolverStrategies )
    {
      throw new IllegalArgumentException( "version range resolver strategy not been specified" );
    }
    this.versionrangeResolverStrategies.clear();
    for ( VersionRangeResolver versionRangeResolverStrategy : versionRangeResolverStrategies )
    {
      if ( null == versionRangeResolverStrategy )
      {
        continue;
      }
      final Component component = versionRangeResolverStrategy.getClass().getAnnotation( Component.class );
      if ( component != null && ! ! !versionrangeResolverStrategies.containsKey( component.hint() ) )
      {
        versionrangeResolverStrategies.put( component.hint(), versionRangeResolverStrategy );
      }
    }
    return this;
  }

  /**
   * Replace all {@link VersionRangeResolver} strategies with the passed map.
   *
   * @param versionRangeResolverStrategies must not be {@code null}
   *
   * @return this instance for fluent API
   *
   * @throws IllegalArgumentException if the passed map is {@code null}
   */
  public final StrategyVersionRangeResolver setVersionRangeResolverStrategies(
          Map<String, VersionRangeResolver> versionRangeResolverStrategies )
  {
    if ( null == versionRangeResolverStrategies )
    {
      throw new IllegalArgumentException( "version range resolver strategy map not been specified" );
    }
    this.versionrangeResolverStrategies.clear();
    this.versionrangeResolverStrategies.putAll( versionRangeResolverStrategies );
    return this;
  }

  /**
   *
   * @return unmodifiable map of {@link VersionRangeResolver} strategies.
   */
  Map<String, VersionRangeResolver> getVersionrangeResolverStrategies()
  {
    return Collections.unmodifiableMap( versionrangeResolverStrategies );
  }

  @Override
  public VersionRangeResult resolveVersionRange( RepositorySystemSession session, VersionRangeRequest request )
          throws VersionRangeResolutionException
  {
    return getVersionRangeResolverStrategy().resolveVersionRange( session, request );
  }

  /**
   *
   * @return version range resolver strategy never {@code null}
   *
   * @throws VersionRangeResolutionException if default version range strategy is unavailable.
   */
  VersionRangeResolver getVersionRangeResolverStrategy() throws VersionRangeResolutionException
  {
    if ( actualVersionRangeResolver != null )
    {
      return actualVersionRangeResolver;
    }
    final String strategyName = getStrategyName();
    final VersionRangeResolver versionRangeResolver = null == getVersionrangeResolverStrategies() ? null
            : getVersionrangeResolverStrategies().get( strategyName );
    if ( null == versionRangeResolver )
    {
      logger.debug( String.format( "Could not find a version range resolver strategy for name: %1$s", strategyName ) );
      if ( defaultMavenVersionRangeResolver != null )
      {
        return actualVersionRangeResolver = defaultMavenVersionRangeResolver;
      }
      throw new VersionRangeResolutionException( null, "Invalid version range resolver strategy." );
    }
    logger.debug( String.format( "Using version range resolver strategy: %1$s [%2$s]", versionRangeResolver.getClass().
            getName(), strategyName ) );
    return actualVersionRangeResolver = versionRangeResolver;
  }

  /**
   *
   * @return strategy name never {@code null}
   */
  String getStrategyName()
  {
    String strategyName = StringUtils.trim( System.getProperty( STRATEGY_PROPERTY_KEY,
            MavenDefaultVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY ) );
    return StringUtils.isBlank( strategyName )
            ? MavenDefaultVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY : strategyName;
  }

}
