package org.apache.maven.plugin.internal;

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

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Helps to provide backward-compatibility with plugins that use legacy components. <strong>Warning:</strong> This is an
 * internal utility component that is only public for technical reasons, it is not part of the public API. In
 * particular, this component can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultLegacySupport
    implements LegacySupport
{

    private static final ThreadLocal<AtomicReference<MavenSession>> SESSION =
        new InheritableThreadLocal<>();

    public void setSession( MavenSession session )
    {
        AtomicReference<MavenSession> reference = DefaultLegacySupport.SESSION.get();
        if ( reference != null )
        {
            reference.set( null );
        }

        if ( session == null && reference != null )
        {
            DefaultLegacySupport.SESSION.remove();
        }
        else
        {
            DefaultLegacySupport.SESSION.set( new AtomicReference<>( session ) );
        }
    }

    public MavenSession getSession()
    {
        AtomicReference<MavenSession> currentSession = DefaultLegacySupport.SESSION.get();
        return currentSession != null ? currentSession.get() : null;
    }

    public RepositorySystemSession getRepositorySession()
    {
        MavenSession session = getSession();
        return ( session != null ) ? session.getRepositorySession() : null;
    }

}
