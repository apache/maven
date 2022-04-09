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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.artifact.transform.InstallRequestTransformer;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.installation.InstallRequest;

import static java.util.Objects.requireNonNull;

/**
 * A {@link InstallRequestTransformer} that delegates to a chain of other transformers.
 */
public final class ChainedInstallRequestTransformer implements InstallRequestTransformer
{
    private final List<InstallRequestTransformer> transformers;

    public ChainedInstallRequestTransformer( List<InstallRequestTransformer> transformers )
    {
        this.transformers = requireNonNull( transformers );
    }

    @Override
    public InstallRequest transformInstallRequest( RepositorySystemSession session, InstallRequest installRequest )
    {
        InstallRequest result = installRequest;
        for ( InstallRequestTransformer transformer : transformers )
        {
            result = transformer.transformInstallRequest( session, result );
        }
        return result;
    }
}
