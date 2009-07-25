package org.apache.maven.plugin;

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

import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * For the most part plugins do not specify their own dependencies so the {@link ClassLoader} used to
 * execute a {@link Mojo} remains the same across projects. But we do need to account for the case where
 * plugin dependencies are specified. Maven has a default implementation and integrators can create their
 * own implementations to deal with different environments like an IDE.
 * 
 * @author Jason van Zyl
 *
 */
public interface PluginClassLoaderCache
{
    void put( String key, ClassRealm pluginClassLoader );
    
    ClassRealm get( String key );
    
    int size();
}
