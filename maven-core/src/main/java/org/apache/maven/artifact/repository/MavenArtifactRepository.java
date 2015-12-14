package org.apache.maven.artifact.repository;

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

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.Proxy;

//TODO completely separate local and remote artifact repositories
public class MavenArtifactRepository
    implements ArtifactRepository
{
    private String id;

    private String url;

    private String basedir;

    private String protocol;

    private ArtifactRepositoryLayout layout;

    private ArtifactRepositoryPolicy snapshots;

    private ArtifactRepositoryPolicy releases;

    private Authentication authentication;

    private Proxy proxy;

    private List<ArtifactRepository> mirroredRepositories = Collections.emptyList();

    public MavenArtifactRepository()
    {
    }

    /**
     * Create a remote download repository.
     *
     * @param id        the unique identifier of the repository
     * @param url       the URL of the repository
     * @param layout    the layout of the repository
     * @param snapshots the policies to use for snapshots
     * @param releases  the policies to use for releases
     */
    public MavenArtifactRepository( String id, String url, ArtifactRepositoryLayout layout,
                                    ArtifactRepositoryPolicy snapshots, ArtifactRepositoryPolicy releases )
    {
        this.id = id;
        this.url = url;
        this.layout = layout;
        this.snapshots = snapshots;
        this.releases = releases;
        //
        // Derive these from the URL
        //
        this.protocol = protocol( url );
        this.basedir = basedir( url );
    }

    public String pathOf( Artifact artifact )
    {
        return layout.pathOf( artifact );
    }

    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        return layout.pathOfRemoteRepositoryMetadata( artifactMetadata );
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return layout.pathOfLocalRepositoryMetadata( metadata, repository );
    }

    public void setLayout( ArtifactRepositoryLayout layout )
    {
        this.layout = layout;
    }

    public ArtifactRepositoryLayout getLayout()
    {
        return layout;
    }

    public void setSnapshotUpdatePolicy( ArtifactRepositoryPolicy snapshots )
    {
        this.snapshots = snapshots;
    }

    public ArtifactRepositoryPolicy getSnapshots()
    {
        return snapshots;
    }

    public void setReleaseUpdatePolicy( ArtifactRepositoryPolicy releases )
    {
        this.releases = releases;
    }

    public ArtifactRepositoryPolicy getReleases()
    {
        return releases;
    }

    public String getKey()
    {
        return getId();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder( 256 );

        sb.append( "      id: " ).append( getId() ).append( '\n' );
        sb.append( "      url: " ).append( getUrl() ).append( '\n' );
        sb.append( "   layout: " ).append( layout != null ? layout : "none" ).append( '\n' );

        if ( proxy != null )
        {
            sb.append( "    proxy: " ).append( proxy.getHost() ).append( ':' ).append( proxy.getPort() ).append( '\n' );
        }

        if ( snapshots != null )
        {
            sb.append( "snapshots: [enabled => " ).append( snapshots.isEnabled() );
            sb.append( ", update => " ).append( snapshots.getUpdatePolicy() ).append( "]\n" );
        }

        if ( releases != null )
        {
            sb.append( " releases: [enabled => " ).append( releases.isEnabled() );
            sb.append( ", update => " ).append( releases.getUpdatePolicy() ).append( "]\n" );
        }

        return sb.toString();
    }

    public Artifact find( Artifact artifact )
    {
        File artifactFile = new File( getBasedir(), pathOf( artifact ) );

        // We need to set the file here or the resolver will fail with an NPE, not fully equipped to deal
        // with multiple local repository implementations yet.
        artifact.setFile( artifactFile );

        return artifact;
    }

    public List<String> findVersions( Artifact artifact )
    {
        return Collections.emptyList();
    }

    public String getId()
    {
        return id;
    }

    public String getUrl()
    {
        return url;
    }

    public String getBasedir()
    {
        return basedir;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public void setUrl( String url )
    {
        this.url = url;

        this.protocol = protocol( url );
        this.basedir = basedir( url );
    }

    // Path Utils

    /**
     * Return the protocol name.
     * <br/>
     * E.g: for input
     * <code>http://www.codehaus.org</code> this method will return <code>http</code>
     *
     * @param url the url
     * @return the host name
     */
    private static String protocol( final String url )
    {
        final int pos = url.indexOf( ':' );

        if ( pos == -1 )
        {
            return "";
        }
        return url.substring( 0, pos ).trim();
    }

    /**
     * Derive the path portion of the given URL.
     *
     * @param url the repository URL
     * @return the basedir of the repository
     * TODO need to URL decode for spaces?
     */
    private String basedir( String url )
    {
        String retValue = null;

        if ( protocol.equalsIgnoreCase( "file" ) )
        {
            retValue = url.substring( protocol.length() + 1 );
            retValue = decode( retValue );
            // special case: if omitted // on protocol, keep path as is
            if ( retValue.startsWith( "//" ) )
            {
                retValue = retValue.substring( 2 );

                if ( retValue.length() >= 2 && ( retValue.charAt( 1 ) == '|' || retValue.charAt( 1 ) == ':' ) )
                {
                    // special case: if there is a windows drive letter, then keep the original return value
                    retValue = retValue.charAt( 0 ) + ":" + retValue.substring( 2 );
                }
                else
                {
                    // Now we expect the host
                    int index = retValue.indexOf( '/' );
                    if ( index >= 0 )
                    {
                        retValue = retValue.substring( index + 1 );
                    }

                    // special case: if there is a windows drive letter, then keep the original return value
                    if ( retValue.length() >= 2 && ( retValue.charAt( 1 ) == '|' || retValue.charAt( 1 ) == ':' ) )
                    {
                        retValue = retValue.charAt( 0 ) + ":" + retValue.substring( 2 );
                    }
                    else if ( index >= 0 )
                    {
                        // leading / was previously stripped
                        retValue = "/" + retValue;
                    }
                }
            }

            // special case: if there is a windows drive letter using |, switch to :
            if ( retValue.length() >= 2 && retValue.charAt( 1 ) == '|' )
            {
                retValue = retValue.charAt( 0 ) + ":" + retValue.substring( 2 );
            }

            // normalize separators
            retValue = new File( retValue ).getPath();
        }

        if ( retValue == null )
        {
            retValue = "/";
        }
        return retValue.trim();
    }

    /**
     * Decodes the specified (portion of a) URL. <strong>Note:</strong> This decoder assumes that ISO-8859-1 is used to
     * convert URL-encoded bytes to characters.
     *
     * @param url The URL to decode, may be <code>null</code>.
     * @return The decoded URL or <code>null</code> if the input was <code>null</code>.
     */
    private static String decode( String url )
    {
        String decoded = url;
        if ( url != null )
        {
            int pos = -1;
            while ( ( pos = decoded.indexOf( '%', pos + 1 ) ) >= 0 )
            {
                if ( pos + 2 < decoded.length() )
                {
                    String hexStr = decoded.substring( pos + 1, pos + 3 );
                    char ch = (char) Integer.parseInt( hexStr, 16 );
                    decoded = decoded.substring( 0, pos ) + ch + decoded.substring( pos + 3 );
                }
            }
        }
        return decoded;
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( getId() == null ) ? 0 : getId().hashCode() );
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        ArtifactRepository other = (ArtifactRepository) obj;

        return eq( getId(), other.getId() );
    }

    protected static <T> boolean eq( T s1, T s2 )
    {
        return s1 != null ? s1.equals( s2 ) : s2 == null;
    }

    public Authentication getAuthentication()
    {
        return authentication;
    }

    public void setAuthentication( Authentication authentication )
    {
        this.authentication = authentication;
    }

    public Proxy getProxy()
    {
        return proxy;
    }

    public void setProxy( Proxy proxy )
    {
        this.proxy = proxy;
    }

    public boolean isBlacklisted()
    {
        return false;
    }

    public void setBlacklisted( boolean blackListed )
    {
        // no op
    }

    public boolean isUniqueVersion()
    {
        return true;
    }

    public boolean isProjectAware()
    {
        return false;
    }

    public List<ArtifactRepository> getMirroredRepositories()
    {
        return mirroredRepositories;
    }

    public void setMirroredRepositories( List<ArtifactRepository> mirroredRepositories )
    {
        if ( mirroredRepositories != null )
        {
            this.mirroredRepositories = Collections.unmodifiableList( mirroredRepositories );
        }
        else
        {
            this.mirroredRepositories = Collections.emptyList();
        }
    }

}
