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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * SHA
 */
public class SHA implements Hash.Factory
{

    private static final ThreadLocal<MessageDigest> ALGORITHM = new ThreadLocal<>();
    private static final ThreadLocal<MessageDigest> CHECKSUM = new ThreadLocal<>();

    private final String algorithm;

    SHA( String algorithm )
    {
        this.algorithm = algorithm;
    }

    @Override
    public String getAlgorithm()
    {
        return algorithm;
    }

    @Override
    public Hash.Algorithm algorithm()
    {
        return new SHA.Algorithm( ThreadLocalDigest.get( ALGORITHM, algorithm ) );
    }

    @Override
    public Hash.Checksum checksum( int count )
    {
        return new SHA.Checksum( ThreadLocalDigest.get( CHECKSUM, algorithm ) );
    }

    private static class Algorithm implements Hash.Algorithm
    {

        private final MessageDigest digest;

        private Algorithm( MessageDigest digest )
        {
            this.digest = digest;
        }

        @Override
        public byte[] hash( byte[] array )
        {
            return digest.digest( array );
        }

        @Override
        public byte[] hash( Path path ) throws IOException
        {
            return hash( Files.readAllBytes( path ) );
        }
    }

    private static class Checksum implements Hash.Checksum
    {

        private final MessageDigest digest;

        private Checksum( MessageDigest digest )
        {
            this.digest = digest;
        }

        @Override
        public void update( byte[] hash )
        {
            digest.update( hash );
        }

        @Override
        public byte[] digest()
        {
            return digest.digest();
        }
    }
}
