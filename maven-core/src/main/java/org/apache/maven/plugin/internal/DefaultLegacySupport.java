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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Helps to provide backward-compatibility with plugins that use legacy components. <strong>Warning:</strong> This is an
 * internal utility component that is only public for technical reasons, it is not part of the public API. In
 * particular, this component can be changed or deleted without prior notice.
 * 
 * @since 3.0
 * @author Benjamin Bentmann
 */
@Component( role = LegacySupport.class )
public class DefaultLegacySupport
    implements LegacySupport
{

    private static final ThreadLocal<MavenSession[]> session = new InheritableThreadLocal<MavenSession[]>();

    public void setSession( MavenSession session )
    {
        if ( session == null )
        {
            MavenSession[] oldSession = DefaultLegacySupport.session.get();
            if ( oldSession != null )
            {
                oldSession[0] = null;
                DefaultLegacySupport.session.remove();
            }
        }
        else
        {
            DefaultLegacySupport.session.set( new MavenSession[] { session } );
        }
    }

    public MavenSession getSession()
    {
        MavenSession[] currentSession = DefaultLegacySupport.session.get();
        return currentSession != null ? currentSession[0] : null;
    }

    public RepositorySystemSession getRepositorySession()
    {
        MavenSession session = getSession();
        return ( session != null ) ? session.getRepositorySession() : null;
    }

}
