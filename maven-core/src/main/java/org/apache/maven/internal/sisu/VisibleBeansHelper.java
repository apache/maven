package org.apache.maven.internal.sisu;

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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.Key;
import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.plexus.ClassRealmManager;

import static java.util.Objects.requireNonNull;

/**
 * Helper class for artificial lookup filtered by "visibility" (Plexus realm visibility) but not using Plexus but
 * pure Sisu.
 */
@Singleton
@Named
public final class VisibleBeansHelper
{
  private final BeanLocator beanLocator;

  @Inject
  public VisibleBeansHelper( final BeanLocator beanLocator )
  {
    this.beanLocator = requireNonNull( beanLocator );
  }

  public <T> Map<String, T> lookupMap( final Class<T> clazz )
  {
    final Key<T> key = Key.get( clazz, com.google.inject.name.Named.class );
    final Set<String> realmNames = ClassRealmManager.visibleRealmNames( ClassRealmManager.contextRealm() );
    HashMap<String, T> result = new HashMap<>();
    beanLocator.locate( key ).forEach( b ->
    {
      final String source = String.valueOf( b.getSource() );
      if ( !source.startsWith( "ClassRealm" ) || realmNames == null || realmNames.contains( source ) )
      {
        result.put( ( ( com.google.inject.name.Named ) b.getKey() ).value(), b.getProvider().get() );
      }
    } );
    return result;
  }
}
