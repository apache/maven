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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.maven.caching.hash.HashAlgorithm;
import org.apache.maven.caching.hash.HashChecksum;
import org.junit.Test;

import static org.apache.maven.caching.hash.HashFactory.XX;
import static org.apache.maven.caching.hash.HashFactory.XXMM;
import static org.junit.Assert.assertEquals;

public class XXHashTest {
    private static final byte[] HELLO_ARRAY = "hello".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WORLD_ARRAY = "world".getBytes(StandardCharsets.UTF_8);
    private static final long HELLO_LONG = 2794345569481354659L;
    private static final long WORLD_LONG = -1767385783675760145L;
    private static final String EMPTY_HASH = "ef46db3751d8e999";
    private static final String HELLO_HASH = "26c7827d889f6da3";
    private static final String WORLD_HASH = "e778fbfe66ee51ef";
    private static final String HELLO_CHECKSUM = "c07c10338a825a5d";
    private static final String WORLD_CHECKSUM = "cb21505d7a714523";
    private static final String FULL_CHECKSUM = "b8ca8fa824d335e9";

    private static final HashAlgorithm ALGORITHM = XX.createAlgorithm();

    @Test
    public void testEmptyArray() {
        byte[] emptyArray = new byte[0];
        String hash = ALGORITHM.hash(emptyArray);
        assertEquals(EMPTY_HASH, hash);
    }

    @Test
    public void testSimpleHash() {
        String helloHash = ALGORITHM.hash(HELLO_ARRAY);
        assertEquals(HELLO_HASH, helloHash);

        String worldHash = ALGORITHM.hash(WORLD_ARRAY);
        assertEquals(WORLD_HASH, worldHash);
    }

    @Test
    public void testSimpleChecksum() {
        String helloChecksum = ALGORITHM.hash(longToBytes(1, HELLO_LONG));
        assertEquals(HELLO_CHECKSUM, helloChecksum);

        String worldChecksum = ALGORITHM.hash(longToBytes(1, WORLD_LONG));
        assertEquals(WORLD_CHECKSUM, worldChecksum);

        String checksum = ALGORITHM.hash(longToBytes(2, HELLO_LONG, WORLD_LONG));
        assertEquals(FULL_CHECKSUM, checksum);
    }

    @Test
    public void testEmptyBuffer() {
        assertEmptyBuffer(XX.createChecksum(0));
        assertEmptyBuffer(XXMM.createChecksum(0));
    }

    @Test
    public void testSingleHash() {
        assertSingleHash(XX.createChecksum(1));
        assertSingleHash(XXMM.createChecksum(1));
    }

    @Test
    public void testSingleChecksum() {
        assertSingleChecksum(XX.createChecksum(1));
        assertSingleChecksum(XXMM.createChecksum(1));
    }

    @Test
    public void testNotFullChecksum() {
        assertSingleChecksum(XX.createChecksum(2));
        assertSingleChecksum(XXMM.createChecksum(2));
    }

    @Test
    public void testFullChecksum() {
        assertFullChecksum(XX.createChecksum(2));
        assertFullChecksum(XXMM.createChecksum(2));
    }

    private void assertEmptyBuffer(HashChecksum checksum) {
        assertEquals(EMPTY_HASH, checksum.digest());
    }

    private void assertSingleHash(HashChecksum checksum) {
        assertEquals(HELLO_HASH, checksum.update(HELLO_ARRAY));
        assertEquals(HELLO_CHECKSUM, checksum.digest());
    }

    private void assertSingleChecksum(HashChecksum checksum) {
        assertEquals(HELLO_HASH, checksum.update(HELLO_HASH));
        assertEquals(HELLO_CHECKSUM, checksum.digest());
    }

    private void assertFullChecksum(HashChecksum checksum) {
        assertEquals(HELLO_HASH, checksum.update(HELLO_HASH));
        assertEquals(WORLD_HASH, checksum.update(WORLD_HASH));
        assertEquals(FULL_CHECKSUM, checksum.digest());
    }

    private byte[] longToBytes(int size, long... values) {
        final ByteBuffer buffer = ByteBuffer.allocate(size * Long.SIZE / Byte.SIZE);
        for (long value : values) {
            buffer.putLong(value);
        }
        return buffer.array();
    }
}
