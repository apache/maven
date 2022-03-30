package org.codehaus.modello.plugin.velocity;

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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Caching OutputStream to avoid overwriting a file with
 * the same content.
 */
public class CachingOutputStream extends OutputStream
{
    private final Path path;
    private FileChannel channel;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private boolean modified;

    public CachingOutputStream( File path ) throws IOException
    {
        this( Objects.requireNonNull( path ).toPath() );
    }

    public CachingOutputStream( Path path ) throws IOException
    {
        this( path, 32 * 1024 );
    }

    public CachingOutputStream( Path path, int bufferSize ) throws IOException
    {
        this.path = Objects.requireNonNull( path );
        this.channel = FileChannel.open( path,
                StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE );
        this.readBuffer = ByteBuffer.allocate( bufferSize );
        this.writeBuffer = ByteBuffer.allocate( bufferSize );
    }

    @Override
    public void write( int b ) throws IOException
    {
        if ( writeBuffer.remaining() < 1 )
        {
            ( ( Buffer ) writeBuffer ).flip();
            flushBuffer( writeBuffer );
            ( ( Buffer ) writeBuffer ).clear();
        }
        writeBuffer.put( ( byte ) b );
    }

    @Override
    public void write( byte[] b ) throws IOException
    {
        write( b, 0, b.length );
    }

    @Override
    public void write( byte[] b, int off, int len ) throws IOException
    {
        if ( writeBuffer.remaining() < len )
        {
            ( ( Buffer ) writeBuffer ).flip();
            flushBuffer( writeBuffer );
            ( ( Buffer ) writeBuffer ).clear();
        }
        int capacity = writeBuffer.capacity();
        while ( len >= capacity )
        {
            flushBuffer( ByteBuffer.wrap( b, off, capacity ) );
            off += capacity;
            len -= capacity;
        }
        if ( len > 0 )
        {
            writeBuffer.put( b, off, len );
        }
    }

    @Override
    public void flush() throws IOException
    {
        ( ( Buffer ) writeBuffer ).flip();
        flushBuffer( writeBuffer );
        ( ( Buffer ) writeBuffer ).clear();
        super.flush();
    }

    private void flushBuffer( ByteBuffer writeBuffer ) throws IOException
    {
        if ( modified )
        {
            channel.write( writeBuffer );
        }
        else
        {
            int len = writeBuffer.remaining();
            ByteBuffer readBuffer;
            if ( this.readBuffer.capacity() >= len )
            {
                readBuffer = this.readBuffer;
                ( ( Buffer ) readBuffer ).clear();
            }
            else
            {
                readBuffer = ByteBuffer.allocate( len );
            }
            while ( len > 0 )
            {
                int read = channel.read( readBuffer );
                if ( read <= 0 )
                {
                    modified = true;
                    channel.position( channel.position() - readBuffer.position() );
                    channel.write( writeBuffer );
                    return;
                }
                len -= read;
            }
            ( ( Buffer ) readBuffer ).flip();
            if ( readBuffer.compareTo( writeBuffer ) != 0 )
            {
                modified = true;
                channel.position( channel.position() - readBuffer.remaining() );
                channel.write( writeBuffer );
            }
        }
    }

    @Override
    public void close() throws IOException
    {
        flush();
        long position = channel.position();
        if ( position != channel.size() )
        {
            modified = true;
            channel.truncate( position );
        }
        channel.close();
    }

    public boolean isModified()
    {
        return modified;
    }
}