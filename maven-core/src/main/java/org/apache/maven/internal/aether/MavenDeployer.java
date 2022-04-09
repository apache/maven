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

import org.apache.maven.artifact.transform.DeployRequestTransformer;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.internal.impl.DefaultDeployer;
import org.eclipse.sisu.Priority;

import static java.util.Objects.requireNonNull;

/**
 * Maven specific {@link Deployer} that delegates to {@link DefaultDeployer}.
 */
@Singleton
@Named
@Priority( 100 )
public final class MavenDeployer
        implements Deployer
{
    private final DefaultDeployer defaultDeployer;

    @Inject
    public MavenDeployer( DefaultDeployer defaultDeployer )
    {
        this.defaultDeployer = requireNonNull( defaultDeployer );
    }

    @Override
    public DeployResult deploy( RepositorySystemSession session, DeployRequest origRequest ) throws DeploymentException
    {
        DeployRequest request = origRequest;
        DeployRequestTransformer transformer =
                (DeployRequestTransformer) session.getData().get( DeployRequestTransformer.KEY );
        if ( transformer != null )
        {
            request = requireNonNull( transformer.transformDeployRequest( session, request ) );
        }
        return defaultDeployer.deploy( session, request );
    }
}
