/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */

package org.apache.maven.repository.mercury;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.Util;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class MercuryAdaptor
{
    public static List<Repository> toMercuryRepos( ArtifactRepository localRepository,
                                                   List<ArtifactRepository> remoteRepositories,
                                                   DependencyProcessor dependencyProcessor )
    {
        if ( localRepository == null && Util.isEmpty( remoteRepositories ) )
            return null;

        int nRepos =
            ( localRepository == null ? 0 : 1 ) + ( Util.isEmpty( remoteRepositories ) ? 0 : remoteRepositories.size() );

        List<Repository> res = new ArrayList<Repository>( nRepos );

        if ( localRepository != null )
        {
            LocalRepositoryM2 lr =
                new LocalRepositoryM2( localRepository.getId(), new File( localRepository.getBasedir() ),
                                       dependencyProcessor );
            res.add( lr );
        }

        if ( !Util.isEmpty( remoteRepositories ) )
        {
            for ( ArtifactRepository ar : remoteRepositories )
            {
                Server server;
                try
                {
                    server = new Server( ar.getId(), new URL( ar.getUrl() ) );
                }
                catch ( MalformedURLException e )
                {
                    throw new IllegalArgumentException( e );
                }
                RemoteRepositoryM2 rr = new RemoteRepositoryM2( server, dependencyProcessor );

                res.add( rr );
            }
        }

        return res;
    }

    public static ArtifactBasicMetadata toMercuryBasicMetadata( Artifact a )
    {
        ArtifactBasicMetadata md = new ArtifactBasicMetadata();
        md.setGroupId( a.getGroupId() );
        md.setArtifactId( a.getArtifactId() );
        md.setVersion( a.getVersion() );
        md.setType( a.getType() );
        md.setScope( a.getScope() );

        return md;
    }

    public static ArtifactMetadata toMercuryMetadata( Artifact a )
    {
        ArtifactMetadata md = new ArtifactMetadata();
        md.setGroupId( a.getGroupId() );
        md.setArtifactId( a.getArtifactId() );
        md.setVersion( a.getVersion() );
        md.setType( a.getType() );
        md.setScope( a.getScope() );

        return md;
    }

    public static Artifact toMavenArtifact( org.apache.maven.mercury.artifact.Artifact a )
    {
        VersionRange vr = VersionRange.createFromVersion( a.getVersion() );
        Artifact ma =
            new DefaultArtifact( a.getGroupId(), a.getArtifactId(), vr, a.getScope(), a.getType(), a.getClassifier(),
                                 null );

        return ma;
    }

}
