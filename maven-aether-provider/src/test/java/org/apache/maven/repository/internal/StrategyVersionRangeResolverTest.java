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
import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;

/**
 */
public class StrategyVersionRangeResolverTest
        extends AbstractRepositoryTestCase
{

  private final VersionScheme versionScheme = new GenericVersionScheme();

  private StrategyVersionRangeResolver sut;

  private VersionRangeRequest request;

  @Override
  protected void setUp()
          throws Exception
  {
    super.setUp();
    // be sure we're testing the right class, i.e. DefaultVersionRangeResolver.class
    final VersionRangeResolver resolver = lookup( VersionRangeResolver.class );
    assertTrue( StrategyVersionRangeResolver.class.isInstance( resolver ) );
    sut = (StrategyVersionRangeResolver) resolver;
    request = new VersionRangeRequest();
    request.addRepository( newTestRepository() );
  }

  @Override
  protected void tearDown()
          throws Exception
  {
    sut = null;
    super.tearDown();
  }

  public void testVersionRangeStrategies()
  {
    assertNotNull( sut.getVersionrangeResolverStrategies() );
    assertFalse( sut.getVersionrangeResolverStrategies().isEmpty() );
  }

  public void testVersionRangeStrategies_mavenDefault()
  {
    assertNotNull( sut.getVersionrangeResolverStrategies().get(
            MavenDefaultVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY ) );
  }

  public void testVersionRangeStrategies_onlyReleases()
  {
    assertNotNull( sut.getVersionrangeResolverStrategies().get(
            OnlyReleaseVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY ) );
  }

  public void testVersionRangeStrategies_releaseOnBoundaries()
  {
    assertNotNull( sut.getVersionrangeResolverStrategies().get(
            ReleaseOnBoundariesVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY ) );
  }

  public void testGetStrategyName_notSet()
  {
    assertEquals( MavenDefaultVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY, sut.getStrategyName( session.
            getSystemProperties() ) );
  }

  public void testGetStrategyName_empty()
  {
    final Map<String, String> systemProperties = new HashMap<String, String>();
    systemProperties.putAll( session.getSystemProperties() );
    systemProperties.put( StrategyVersionRangeResolver.STRATEGY_PROPERTY_KEY, "" );
    assertEquals( MavenDefaultVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY, sut.getStrategyName(
            systemProperties ) );
  }

  public void testGetStrategyName_blank()
  {
    final Map<String, String> systemProperties = new HashMap<String, String>();
    systemProperties.putAll( session.getSystemProperties() );
    systemProperties.put( StrategyVersionRangeResolver.STRATEGY_PROPERTY_KEY, "      " );
    assertEquals( MavenDefaultVersionRangeResolver.VERSION_RANGE_RESOLVER_STRATEGY, sut.getStrategyName(
            systemProperties ) );
  }

  public void testGetStrategyName()
  {
    final Map<String, String> systemProperties = new HashMap<String, String>();
    systemProperties.putAll( session.getSystemProperties() );
    systemProperties.put( StrategyVersionRangeResolver.STRATEGY_PROPERTY_KEY, "expectedStrategy" );
    assertEquals( "expectedStrategy", sut.getStrategyName(
            systemProperties ) );
  }

}
