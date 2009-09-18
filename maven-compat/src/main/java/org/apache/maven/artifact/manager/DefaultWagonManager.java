package org.apache.maven.artifact.manager;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.component.annotations.Component;

@Component(role=WagonManager.class) 
public class DefaultWagonManager
    extends org.apache.maven.repository.legacy.DefaultWagonManager
    implements WagonManager
{
    // only here for backward compat project-info-reports:dependencies
    public AuthenticationInfo getAuthenticationInfo( String id )
    {
       // empty one to prevent NPE
       return new AuthenticationInfo();
    }

    public ProxyInfo getProxy( String protocol )
    {
        return null;
    }

    public void getArtifact( Artifact artifact, ArtifactRepository repository )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        getArtifact( artifact, repository, null, false );
    }

    public void getArtifact( Artifact artifact, List<ArtifactRepository> remoteRepositories )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        getArtifact( artifact, remoteRepositories, null, false );
    }

}
