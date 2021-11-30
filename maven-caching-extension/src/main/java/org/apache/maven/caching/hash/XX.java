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
package org.apache.maven.caching.hash;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.openhft.hashing.LongHashFunction;

/**
 * XX
 */
public class XX implements Hash.Factory
{

    static final LongHashFunction INSTANCE = LongHashFunction.xx();

    @Override
    public String getAlgorithm()
    {
        return "XX";
    }

    @Override
    public Hash.Algorithm algorithm()
    {
        return new XX.Algorithm();
    }

    @Override
    public Hash.Checksum checksum( int count )
    {
        return new XX.Checksum( ByteBuffer.allocate( capacity( count ) ) );
    }

    static int capacity( int count )
    {
        // Java 8: Long.BYTES
        return count * Long.SIZE / Byte.SIZE;
    }

    static class Algorithm implements Hash.Algorithm
    {

        @Override
        public byte[] hash( byte[] array )
        {
            return HexUtils.toByteArray( INSTANCE.hashBytes( array ) );
        }

        @Override
        public byte[] hash( Path path ) throws IOException
        {
            return hash( Files.readAllBytes( path ) );
        }
    }

    static class Checksum implements Hash.Checksum
    {

        private final ByteBuffer buffer;

        Checksum( ByteBuffer buffer )
        {
            this.buffer = buffer;
        }

        @Override
        public void update( byte[] hash )
        {
            buffer.put( hash );
        }

        @Override
        public byte[] digest()
        {
            return HexUtils.toByteArray( INSTANCE.hashBytes( buffer, 0, buffer.position() ) );
        }
    }

}
