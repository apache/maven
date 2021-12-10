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

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ThreadLocalBuffer
 */
public class ThreadLocalBuffer
{

    private static final ConcurrentMap<CloseableBuffer, Boolean> LOCALS = new ConcurrentHashMap<>();

    public static ByteBuffer get( ThreadLocal<CloseableBuffer> local, int capacity )
    {
        final CloseableBuffer buffer = local.get();
        if ( buffer == null )
        {
            return create( local, capacity );
        }

        if ( capacity( buffer ) < capacity )
        {
            close( buffer );
            return create( local, capacity * 2 );
        }

        return clear( buffer );
    }

    @Override
    public void finalize()
    {
        for ( CloseableBuffer buffer : LOCALS.keySet() )
        {
            buffer.close();
        }
    }

    private static ByteBuffer create( ThreadLocal<CloseableBuffer> local, int capacity )
    {
        final CloseableBuffer buffer = CloseableBuffer.directBuffer( capacity );
        local.set( buffer );
        LOCALS.put( buffer, false );
        return buffer.getBuffer();
    }

    private static int capacity( CloseableBuffer buffer )
    {
        return buffer.getBuffer().capacity();
    }

    private static ByteBuffer clear( CloseableBuffer buffer )
    {
        return ( ByteBuffer ) buffer.getBuffer().clear();
    }

    private static void close( CloseableBuffer buffer )
    {
        LOCALS.remove( buffer );
        buffer.close();
    }

    private ThreadLocalBuffer()
    {
    }
}
