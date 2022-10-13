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
package org.apache.maven.lifecycle.providers.packaging;

import javax.inject.Provider;

import java.util.Collections;
import java.util.HashMap;

import org.apache.maven.lifecycle.mapping.DefaultLifecycleMapping;
import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

import static java.util.Objects.requireNonNull;

/**
 * Base lifecycle mapping provider, ie per-packaging plugin bindings for {@code default} lifecycle.
 */
public abstract class AbstractLifecycleMappingProvider
    implements Provider<LifecycleMapping>
{
    protected static final String RESOURCES_PLUGIN_VERSION = "3.2.0";

    protected static final String COMPILER_PLUGIN_VERSION = "3.8.1";

    protected static final String SUREFIRE_PLUGIN_VERSION = "3.0.0-M5";

    protected static final String INSTALL_PLUGIN_VERSION = "3.0.0-M1";

    protected static final String DEPLOY_PLUGIN_VERSION = "3.0.0-M2";

    // packaging

    protected static final String JAR_PLUGIN_VERSION = "3.2.0";

    protected static final String EAR_PLUGIN_VERSION = "3.1.2";

    protected static final String EJB_PLUGIN_VERSION = "3.1.0";

    protected static final String PLUGIN_PLUGIN_VERSION = "3.6.0";

    protected static final String RAR_PLUGIN_VERSION = "2.4"; // TODO: Update!!!

    protected static final String WAR_PLUGIN_VERSION = "3.3.1";

    private final LifecycleMapping lifecycleMapping;

    protected AbstractLifecycleMappingProvider( String[] pluginBindings )
    {
        requireNonNull( pluginBindings );
        final int len = pluginBindings.length;
        if ( len < 2 || len % 2 != 0 )
        {
            throw new IllegalArgumentException( "Plugin bindings must have more than 0, even count of elements" );
        }

        HashMap<String, LifecyclePhase> lifecyclePhaseBindings = new HashMap<>( len / 2 );
        for ( int i = 0; i < len; i = i + 2 )
        {
            lifecyclePhaseBindings.put( pluginBindings[i], new LifecyclePhase( pluginBindings[i + 1] ) );
        }

        Lifecycle lifecycle = new Lifecycle();
        lifecycle.setId( "default" );
        lifecycle.setLifecyclePhases( Collections.unmodifiableMap( lifecyclePhaseBindings ) );

        this.lifecycleMapping = new DefaultLifecycleMapping( Collections.singletonList( lifecycle ) );
    }

    @Override
    public LifecycleMapping get()
    {
        return lifecycleMapping;
    }
}
