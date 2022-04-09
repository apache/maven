package org.apache.maven.internal.aether;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.transform.InstallRequestTransformer;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.Installer;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.sisu.Priority;

import static java.util.Objects.requireNonNull;

/**
 * Maven specific {@link Installer} that delegates to {@link DefaultInstaller}.
 */
@Singleton
@Named
@Priority( 100 )
public final class MavenInstaller
        implements Installer
{
    private final DefaultInstaller defaultInstaller;

    @Inject
    public MavenInstaller( DefaultInstaller defaultInstaller )
    {
        this.defaultInstaller = requireNonNull( defaultInstaller );
    }

    @Override
    public InstallResult install( RepositorySystemSession session, InstallRequest origRequest )
            throws InstallationException
    {
        InstallRequest request = origRequest;
        InstallRequestTransformer transformer =
                (InstallRequestTransformer) session.getData().get( InstallRequestTransformer.KEY );
        if ( transformer != null )
        {
            request = requireNonNull( transformer.transformInstallRequest( session, request ) );
        }
        return defaultInstaller.install( session, request );
    }
}
