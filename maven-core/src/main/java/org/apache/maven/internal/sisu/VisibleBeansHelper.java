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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.sisu.BeanEntry;
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

  /**
   * Performs a component lookup obeying "plexus visibility rules". Returns {@code null} if no component found.
   */
  public <T> T lookup( final Class<T> clazz, final String name )
  {
    final Key<T> key = Key.get( clazz, Names.named( name ) );
    final Set<String> realmNames = ClassRealmManager.visibleRealmNames( ClassRealmManager.contextRealm() );
    for ( BeanEntry<Annotation, T> beanEntry : beanLocator.locate( key ) ) 
    {
      final Object source = beanEntry.getSource();
      if ( !( source instanceof ClassRealm ) || realmNames == null || realmNames.contains( source.toString() ) )
      {
        return beanEntry.getProvider().get();
      }
    }
    return null;
  }

  /**
   * Performs a map component lookup obeying "plexus visibility rules". Never returns {@code null}.
   */
  public <T> Map<String, T> lookupMap( final Class<T> clazz )
  {
    final Key<T> key = Key.get( clazz, com.google.inject.name.Named.class );
    final Set<String> realmNames = ClassRealmManager.visibleRealmNames( ClassRealmManager.contextRealm() );
    HashMap<String, T> result = new HashMap<>();
    beanLocator.locate( key ).forEach( b ->
    {
      final Object source = b.getSource();
      if ( !( source instanceof ClassRealm ) || realmNames == null || realmNames.contains( source.toString() ) )
      {
        result.put( ( ( com.google.inject.name.Named ) b.getKey() ).value(), b.getProvider().get() );
      }
    } );
    return result;
  }
}
