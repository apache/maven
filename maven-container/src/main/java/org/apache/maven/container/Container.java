package org.apache.maven.container;

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

import java.util.List;
import java.util.Map;

import com.google.inject.Module;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.logging.LoggerManager;

public interface Container
{

    <T> T lookup( Class<T> role );

    <T> T lookup( Class<T> role, String name );

    <T> Map<String, T> lookupMap( Class<T> role );

    <T> List<T> lookupList( Class<T> role );

    <R, I extends R> void addComponent( Class<R> role, I impl );

    <R, I extends R> void addComponent( Class<R> role, Class<I> impl );

    void addComponent( ComponentDescriptor<?> componentDescriptor );

    void release( Object component );

    ClassRealm setLookupRealm( ClassRealm realm );

    void discoverComponents( ClassRealm realm, Module... modules );

    ClassWorld getClassWorld();

    ClassRealm getContainerRealm();

    void dispose();

    // configuration

    void setLoggerManager( LoggerManager loggerManager );

}
