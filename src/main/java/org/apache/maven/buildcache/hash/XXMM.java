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
package org.apache.maven.buildcache.hash;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.file.StandardOpenOption.READ;

/**
 * XXMM
 */
public class XXMM implements Hash.Factory
{

    private static final ThreadLocal<CloseableBuffer> BUFFER = new ThreadLocal<>();

    @Override
    public String getAlgorithm()
    {
        return "XXMM";
    }

    @Override
    public Hash.Algorithm algorithm()
    {
        return new Algorithm();
    }

    @Override
    public Hash.Checksum checksum( int count )
    {
        return new XX.Checksum( ThreadLocalBuffer.get( BUFFER, XX.capacity( count ) ) );
    }

    private static class Algorithm extends XX.Algorithm
    {

        @Override
        public byte[] hash( Path path ) throws IOException
        {
            try ( FileChannel channel = FileChannel.open( path, READ );
                    CloseableBuffer buffer = CloseableBuffer.mappedBuffer( channel, READ_ONLY ) )
            {
                return HexUtils.toByteArray( XX.INSTANCE.hashBytes( buffer.getBuffer() ) );
            }
        }
    }
}
