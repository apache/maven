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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Properties;

/** @plexus.component */
public class DefaultUpdateCheckManager
    extends AbstractLogEnabled
    implements UpdateCheckManager
{

    public DefaultUpdateCheckManager()
    {

    }

    public DefaultUpdateCheckManager( Logger logger )
    {
        enableLogging( logger );
    }

    public static final String LAST_UPDATE_TAG = ".lastUpdated";

    private static final String TOUCHFILE_NAME = "resolver-status.properties";

    public boolean isUpdateRequired( Artifact artifact, ArtifactRepository repository )
    {
        // Update intervals are never used for release artifacts. These intervals
        // only exist on the release section of the repository definition in the POM for one reason:
        // to specify how often artifact METADATA is checked. Here, we simply shortcut for non-snapshot
        // artifacts.
        if ( !artifact.isSnapshot() )
        {
            return false;
        }

        // we can safely assume that we're calculating based on the snapshot policy here if we've made it past the
        // release-artifact short circuit above.
        ArtifactRepositoryPolicy policy = repository.getSnapshots();

        return isUpdateRequired( artifact, repository, policy );
    }

    private boolean isUpdateRequired( Artifact artifact, ArtifactRepository repository, ArtifactRepositoryPolicy policy )
    {
        File file = artifact.getFile();

        if ( !policy.isEnabled() )
        {
            return false;
        }

        if ( file == null )
        {
            // TODO throw something instead?
            return true;
        }

        Date lastCheckDate;

        if ( file.exists() )
        {
            lastCheckDate = new Date ( file.lastModified() );
        }
        else
        {
            File touchfile = getTouchfile( artifact );
            lastCheckDate = readLastUpdated( touchfile, repository.getId() );
        }

        return ( lastCheckDate == null ) || policy.checkOutOfDate( lastCheckDate );
    }

    public boolean isUpdateRequired( RepositoryMetadata metadata, ArtifactRepository repository, File file )
    {
        // Here, we need to determine which policy to use. Release updateInterval will be used when
        // the metadata refers to a release artifact or meta-version, and snapshot updateInterval will be used when
        // it refers to a snapshot artifact or meta-version.
        // NOTE: Release metadata includes version information about artifacts that have been released, to allow
        // meta-versions like RELEASE and LATEST to resolve, and also to allow retrieval of the range of valid, released
        // artifacts available.
        ArtifactRepositoryPolicy policy = metadata.isSnapshot() ? repository.getSnapshots() : repository.getReleases();

        if ( !policy.isEnabled() )
        {
            return false;
        }

        if ( file == null )
        {
            // TODO throw something instead?
            return true;
        }

        Date lastCheckDate = readLastUpdated( metadata, repository, file );

        return ( lastCheckDate == null ) || policy.checkOutOfDate( lastCheckDate );
    }

    public Date readLastUpdated( RepositoryMetadata metadata, ArtifactRepository repository, File file )
    {
        File touchfile = getTouchfile( metadata, file );

        String key = getMetadataKey( repository, file );

        return readLastUpdated( touchfile, key );
    }

    public void touch( Artifact artifact, ArtifactRepository repository )
    {
        File file = artifact.getFile();

        File touchfile = getTouchfile( artifact );

        if ( file.exists() )
        {
            touchfile.delete();
        }
        else
        {
            writeLastUpdated( touchfile, repository.getId() );
        }

    }

    public void touch( RepositoryMetadata metadata, ArtifactRepository repository, File file )
    {
        File touchfile = getTouchfile( metadata, file );

        String key = getMetadataKey( repository, file );

        writeLastUpdated( touchfile, key );
    }

    public String getMetadataKey( ArtifactRepository repository, File file )
    {
        return repository.getId() + "." + file.getName() + LAST_UPDATE_TAG;
    }

    private void writeLastUpdated( File touchfile, String key )
    {
        synchronized ( touchfile.getAbsolutePath().intern() )
        {
            if ( !touchfile.getParentFile().exists() && !touchfile.getParentFile().mkdirs() )
            {
                getLogger().debug( "Failed to create directory: " + touchfile.getParent() +
                                       " for tracking artifact metadata resolution." );
                return;
            }

            FileChannel channel = null;
            FileLock lock = null;
            try
            {
                Properties props = new Properties();

                channel = new RandomAccessFile( touchfile, "rw" ).getChannel();
                lock = channel.lock( 0, channel.size(), false );

                if ( touchfile.canRead() )
                {
                    getLogger().debug( "Reading resolution-state from: " + touchfile );
                    ByteBuffer buffer = ByteBuffer.allocate( (int) channel.size() );

                    channel.read( buffer );
                    buffer.flip();

                    ByteArrayInputStream stream = new ByteArrayInputStream( buffer.array() );
                    props.load( stream );
                }

                props.setProperty( key, Long.toString( System.currentTimeMillis() ) );

                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                getLogger().debug( "Writing resolution-state to: " + touchfile );
                props.store( stream, "Last modified on: " + new Date() );

                byte[] data = stream.toByteArray();
                ByteBuffer buffer = ByteBuffer.allocate( data.length );
                buffer.put( data );
                buffer.flip();

                channel.position( 0 );
                channel.write( buffer );
            }
            catch ( IOException e )
            {
                getLogger().debug( "Failed to record lastUpdated information for resolution.\nFile: " +
                                       touchfile.toString() + "; key: " + key, e );
            }
            finally
            {
                if ( lock != null )
                {
                    try
                    {
                        lock.release();
                    }
                    catch ( IOException e )
                    {
                        getLogger().debug( "Error releasing exclusive lock for resolution tracking file: " +
                                               touchfile, e );
                    }
                }

                if ( channel != null )
                {
                    try
                    {
                        channel.close();
                    }
                    catch ( IOException e )
                    {
                        getLogger().debug( "Error closing FileChannel for resolution tracking file: " +
                                               touchfile, e );
                    }
                }
            }
        }
    }

    public Date readLastUpdated( File touchfile, String key )
    {
        if ( !touchfile.canRead() )
        {
            return null;
        }

        synchronized ( touchfile.getAbsolutePath().intern() )
        {
            getLogger().debug( "Searching for: " + key + " in touchfile." );

            Date result = null;
            FileInputStream stream = null;
            FileLock lock = null;
            FileChannel channel = null;
            try
            {
                Properties props = new Properties();

                stream = new FileInputStream( touchfile );
                channel = stream.getChannel();
                lock = channel.lock( 0, channel.size(), true );

                getLogger().debug( "Reading resolution-state from: " + touchfile );
                props.load( stream );

                String rawVal = props.getProperty( key );
                if ( rawVal != null )
                {
                    try
                    {
                        result = new Date( Long.parseLong( rawVal ) );
                    }
                    catch ( NumberFormatException e )
                    {
                        getLogger().debug( "Cannot parse lastUpdated date: \'" + rawVal + "\'. Ignoring.", e );
                        result = null;
                    }
                }
            }
            catch ( IOException e )
            {
                getLogger().debug( "Failed to read lastUpdated information.\nFile: " +
                                       touchfile.toString() + "; key: " + key, e );
            }
            finally
            {
                if ( lock != null )
                {
                    try
                    {
                        lock.release();
                    }
                    catch ( IOException e )
                    {
                        getLogger().debug( "Error releasing shared lock for resolution tracking file: " +
                                               touchfile, e );
                    }
                }

                if ( channel != null )
                {
                    try
                    {
                        channel.close();
                    }
                    catch ( IOException e )
                    {
                        getLogger().debug( "Error closing FileChannel for resolution tracking file: " +
                                           touchfile, e );
                    }
                }
            }

            return result;
        }
    }

    public File getTouchfile( Artifact artifact )
    {
        StringBuffer sb = new StringBuffer();
        sb.append( artifact.getArtifactId() );
        sb.append( '-' ).append( artifact.getBaseVersion() );
        if ( artifact.getClassifier() != null )
        {
            sb.append( '-' ).append( artifact.getClassifier() );
        }
        sb.append( '.' ).append( artifact.getType() ).append( LAST_UPDATE_TAG );
        return new File( artifact.getFile().getParentFile(), sb.toString() );
    }

    public File getTouchfile( RepositoryMetadata metadata, File file )
    {
        return new File( file.getParent(), TOUCHFILE_NAME );
    }

    public boolean isPomUpdateRequired( Artifact artifact, ArtifactRepository repository )
    {
        return isUpdateRequired( artifact, repository, repository.getReleases() );
    }

}
