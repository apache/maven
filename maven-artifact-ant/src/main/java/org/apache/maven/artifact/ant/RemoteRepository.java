package org.apache.maven.artifact.ant;

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

/**
 * Remote repository type.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class RemoteRepository
    extends Repository
{
    private String url;

    private Authentication authentication;

    /** @deprecated use &lt;snapshots/&gt; and &lt;updatePolicy&gt; instead. */
    private String snapshotPolicy;
    
    /** @deprecated use &lt;snapshots/&gt; and &lt;checksumPolicy&gt; instead. */
    private String checksumPolicy;
    
    private Proxy proxy;

    private RepositoryPolicy snapshots;

    private RepositoryPolicy releases;

    public String getUrl()
    {
        return ( (RemoteRepository) getInstance() ).url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public Authentication getAuthentication()
    {
        return authentication;
    }

    public void addAuthentication( Authentication authentication )
    {
        this.authentication = authentication;
    }

    public void setSnapshotPolicy( String snapshotPolicy )
    {
        this.snapshotPolicy = snapshotPolicy;
    }

    public String getSnapshotPolicy()
    {
        return snapshotPolicy;
    }

    public void addProxy( Proxy proxy )
    {
        this.proxy = proxy;
    }

    public Proxy getProxy()
    {
        return proxy;
    }

    public String getChecksumPolicy()
    {
        return checksumPolicy;
    }

    public void setChecksumPolicy( String checksumPolicy )
    {
        this.checksumPolicy = checksumPolicy;
    }

    public RepositoryPolicy getSnapshots()
    {
        return snapshots;
    }

    public void setSnapshots( RepositoryPolicy snapshots )
    {
        this.snapshots = snapshots;
    }

    public RepositoryPolicy getReleases()
    {
        return releases;
    }

    public void setReleases( RepositoryPolicy releases )
    {
        this.releases = releases;
    }
}
