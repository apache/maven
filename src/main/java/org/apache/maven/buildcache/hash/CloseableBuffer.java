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
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.PrivilegedAction;

import static java.security.AccessController.doPrivileged;
import static org.apache.maven.buildcache.hash.ReflectionUtils.getField;
import static org.apache.maven.buildcache.hash.ReflectionUtils.getMethod;

/**
 * CloseableBuffer https://stackoverflow.com/a/54046774
 */
public class CloseableBuffer implements AutoCloseable
{

    private static final Cleaner CLEANER = doPrivileged( new PrivilegedAction<Cleaner>()
    {

        @Override
        public Cleaner run()
        {
            final String jsv = System.getProperty( "java.specification.version", "9" );
            if ( jsv.startsWith( "1." ) )
            {
                return DirectCleaner.isSupported() ? new DirectCleaner() : new NoopCleaner();
            }
            else
            {
                return UnsafeCleaner.isSupported() ? new UnsafeCleaner() : new NoopCleaner();
            }
        }
    } );

    public static CloseableBuffer directBuffer( int capacity )
    {
        return new CloseableBuffer( ByteBuffer.allocateDirect( capacity ) );
    }

    public static CloseableBuffer mappedBuffer( FileChannel channel, MapMode mode ) throws IOException
    {
        return new CloseableBuffer( channel.map( mode, 0, channel.size() ) );
    }

    private ByteBuffer buffer;

    /**
     * Unmap only DirectByteBuffer and MappedByteBuffer
     */
    private CloseableBuffer( ByteBuffer buffer )
    {
        // Java 8: buffer.isDirect()
        this.buffer = buffer;
    }

    /**
     * Do not use buffer after close
     */
    public ByteBuffer getBuffer()
    {
        return buffer;
    }

    @Override
    public void close()
    {
        // Java 8: () -> CLEANER.clean(buffer)
        boolean done = doPrivileged( new PrivilegedAction<Boolean>()
        {

            @Override
            public Boolean run()
            {
                return CLEANER.clean( buffer );
            }
        } );
        if ( done )
        {
            buffer = null;
        }
    }

    // Java 8: @FunctionalInterface
    private interface Cleaner
    {

        boolean clean( ByteBuffer buffer );
    }

    private static class NoopCleaner implements Cleaner
    {

        @Override
        public boolean clean( ByteBuffer buffer )
        {
            return false;
        }
    }

    private static class DirectCleaner implements Cleaner
    {

        private static final Method ATTACHMENT = getMethod( "sun.nio.ch.DirectBuffer",
                "attachment" );
        private static final Method CLEANER = getMethod( "sun.nio.ch.DirectBuffer", "cleaner" );
        private static final Method CLEAN = getMethod( "sun.misc.Cleaner", "clean" );

        public static boolean isSupported()
        {
            return ATTACHMENT != null && CLEAN != null && CLEANER != null;
        }

        /**
         * Make sure duplicates and slices are not cleaned, since this can result in duplicate attempts to clean the
         * same buffer, which trigger a crash with: "A fatal error has been detected by the Java Runtime Environment:
         * EXCEPTION_ACCESS_VIOLATION" See: https://stackoverflow.com/a/31592947/3950982
         */
        @Override
        public boolean clean( ByteBuffer buffer )
        {
            try
            {
                if ( ATTACHMENT.invoke( buffer ) == null )
                {
                    CLEAN.invoke( CLEANER.invoke( buffer ) );
                    return true;
                }
            }
            catch ( Exception ignore )
            {
            }
            return false;
        }
    }

    private static class UnsafeCleaner implements Cleaner
    {

        // Java 9: getMethod("jdk.internal.misc.Unsafe", "invokeCleaner", ByteBuffer.class);
        private static final Method INVOKE_CLEANER = getMethod( "sun.misc.Unsafe", "invokeCleaner", ByteBuffer.class );
        private static final Object UNSAFE = getField( "sun.misc.Unsafe", "theUnsafe" );

        public static boolean isSupported()
        {
            return UNSAFE != null && INVOKE_CLEANER != null;
        }

        /**
         * Calling the above code in JDK9+ gives a reflection warning on stderr,
         * Unsafe.theUnsafe.invokeCleaner(byteBuffer)
         * makes the same call, but does not print the reflection warning
         */
        @Override
        public boolean clean( ByteBuffer buffer )
        {
            try
            {
                // throws IllegalArgumentException if buffer is a duplicate or slice
                INVOKE_CLEANER.invoke( UNSAFE, buffer );
                return true;
            }
            catch ( Exception ignore )
            {
            }
            return false;
        }
    }
}
