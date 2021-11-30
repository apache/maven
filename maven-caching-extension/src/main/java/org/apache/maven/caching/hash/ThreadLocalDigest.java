package org.apache.maven.caching.hash;

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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * ThreadLocalDigest
 */
public class ThreadLocalDigest
{
    public static MessageDigest get( ThreadLocal<MessageDigest> local, String algorithm )
    {
        final MessageDigest digest = local.get();
        if ( digest == null )
        {
            return create( local, algorithm );
        }

        if ( Objects.equals( digest.getAlgorithm(), algorithm ) )
        {
            return reset( digest );
        }

        reset( digest );
        return create( local, algorithm );
    }

    private static MessageDigest create( ThreadLocal<MessageDigest> local, String algorithm )
    {
        try
        {
            final MessageDigest digest = MessageDigest.getInstance( algorithm );
            local.set( digest );
            return digest;
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RuntimeException( "Cannot create message digest with algorithm: " + algorithm, e );
        }
    }

    private static MessageDigest reset( MessageDigest digest )
    {
        digest.reset();
        return digest;
    }

    private ThreadLocalDigest()
    {
    }
}
