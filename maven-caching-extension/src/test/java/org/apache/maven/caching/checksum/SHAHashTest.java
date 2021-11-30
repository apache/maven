package org.apache.maven.caching.checksum;

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
import java.nio.charset.StandardCharsets;
import org.apache.maven.caching.hash.HashAlgorithm;
import org.apache.maven.caching.hash.HashChecksum;
import org.junit.jupiter.api.Test;

import static org.apache.maven.caching.hash.HashFactory.SHA256;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SHAHashTest
{

    private static final byte[] HELLO_ARRAY = "hello".getBytes( StandardCharsets.UTF_8 );
    private static final byte[] WORLD_ARRAY = "world".getBytes( StandardCharsets.UTF_8 );
    private static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String HELLO_HASH = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    private static final String WORLD_HASH = "486ea46224d1bb4fb680f34f7c9ad96a8f24ec88be73ea8e5a6c65260e9cb8a7";
    private static final String HELLO_CHECKSUM = "9595c9df90075148eb06860365df33584b75bff782a510c6cd4883a419833d50";
    private static final String WORLD_CHECKSUM = "63e5c163c81ee9a3ed99d365ff963ecea340cc455deeac7c4b63ac75b9cf4706";
    private static final String FULL_CHECKSUM = "7305db9b2abccd706c256db3d97e5ff48d677cfe4d3a5904afb7da0e3950e1e2";

    private static final HashAlgorithm ALGORITHM = SHA256.createAlgorithm();
    private static final HashChecksum CHECKSUM = SHA256.createChecksum( 0 );

    @Test
    public void testEmptyArray()
    {
        byte[] emptyArray = new byte[0];
        String hash = ALGORITHM.hash( emptyArray );
        assertEquals( EMPTY_HASH, hash );
    }

    @Test
    public void testSimpleHash()
    {
        String helloHash = ALGORITHM.hash( HELLO_ARRAY );
        assertEquals( HELLO_HASH, helloHash );

        String worldHash = ALGORITHM.hash( WORLD_ARRAY );
        assertEquals( WORLD_HASH, worldHash );
    }

    @Test
    public void testSimpleChecksum()
    {
        assertEquals( HELLO_HASH, CHECKSUM.update( HELLO_ARRAY ) );
        assertEquals( HELLO_CHECKSUM, CHECKSUM.digest() );

        assertEquals( WORLD_HASH, CHECKSUM.update( WORLD_ARRAY ) );
        assertEquals( WORLD_CHECKSUM, CHECKSUM.digest() );

        assertEquals( HELLO_HASH, CHECKSUM.update( HELLO_ARRAY ) );
        assertEquals( WORLD_HASH, CHECKSUM.update( WORLD_ARRAY ) );
        assertEquals( FULL_CHECKSUM, CHECKSUM.digest() );
    }
}
