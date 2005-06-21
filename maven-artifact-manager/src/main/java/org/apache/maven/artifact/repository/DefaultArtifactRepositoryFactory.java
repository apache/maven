package org.apache.maven.artifact.repository;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * @author jdcasey
 */
public class DefaultArtifactRepositoryFactory
    implements ArtifactRepositoryFactory
{
    // TODO: use settings?
    private String globalSnapshotPolicy = null;
    
    private String globalChecksumPolicy = null;

    public ArtifactRepository createArtifactRepository( String id, String url,
                                                        ArtifactRepositoryLayout repositoryLayout,
                                                        String snapshotPolicy, String checksumPolicy )
    {
        ArtifactRepository repo = null;
        
        String snapPolicy = snapshotPolicy;

        if ( globalSnapshotPolicy != null )
        {
            snapPolicy = globalSnapshotPolicy;
        }
        
        if ( snapPolicy == null )
        {
            snapPolicy = ArtifactRepository.SNAPSHOT_POLICY_NEVER;
        }
        
        String csumPolicy = checksumPolicy;
        
        if ( globalChecksumPolicy != null )
        {
            csumPolicy = globalChecksumPolicy;
        }
        
        if ( csumPolicy == null )
        {
            csumPolicy = ArtifactRepository.CHECKSUM_POLICY_FAIL;
        }
        
        repo = new DefaultArtifactRepository( id, url, repositoryLayout, snapPolicy, csumPolicy );

        return repo;
    }

    public void setGlobalSnapshotPolicy( String snapshotPolicy )
    {
        this.globalSnapshotPolicy = snapshotPolicy;
    }
    
    public void setGlobalChecksumPolicy( String checksumPolicy )
    {
        this.globalChecksumPolicy = checksumPolicy;
    }
}
