package org.apache.maven.internal.impl;

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

import javax.inject.Inject;
import javax.inject.Named;

import java.nio.file.Path;

import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.model.Repository;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RepositoryPolicy;

@Named
public class DefaultRepositoryFactory implements RepositoryFactory
{

    private final RepositorySystem repositorySystem;

    @Inject
    public DefaultRepositoryFactory( RepositorySystem repositorySystem )
    {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public LocalRepository createLocal( Path path )
    {
        return new DefaultLocalRepository( new org.eclipse.aether.repository.LocalRepository( path.toFile() ) );
    }

    @Override
    public RemoteRepository createRemote( String id, String url )
    {
        return new DefaultRemoteRepository(
                new org.eclipse.aether.repository.RemoteRepository.Builder( id, "default", url )
                        .build() );
    }

    @Override
    public RemoteRepository createRemote( Repository repository )
            throws IllegalArgumentException
    {
        return new DefaultRemoteRepository(
                new org.eclipse.aether.repository.RemoteRepository.Builder(
                        repository.getId(), repository.getLayout(), repository.getUrl() )
                        .setReleasePolicy( buildRepositoryPolicy( repository.getReleases() ) )
                        .setSnapshotPolicy( buildRepositoryPolicy( repository.getSnapshots() ) )
                        .build() );
    }

    public static org.eclipse.aether.repository.RepositoryPolicy buildRepositoryPolicy(
            org.apache.maven.api.model.RepositoryPolicy policy )
    {
        boolean enabled = true;
        String updatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY;
        String checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL;
        if ( policy != null )
        {
            enabled = policy.isEnabled();
            if ( policy.getUpdatePolicy() != null )
            {
                updatePolicy = policy.getUpdatePolicy();
            }
            if ( policy.getChecksumPolicy() != null )
            {
                checksumPolicy = policy.getChecksumPolicy();
            }
        }
        return new org.eclipse.aether.repository.RepositoryPolicy(
                enabled, updatePolicy, checksumPolicy );
    }
}
