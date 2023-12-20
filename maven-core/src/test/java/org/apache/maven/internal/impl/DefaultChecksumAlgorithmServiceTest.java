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
package org.apache.maven.internal.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.services.ChecksumAlgorithmService;
import org.eclipse.aether.internal.impl.checksum.*;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultChecksumAlgorithmServiceTest {
    private static Map<String, ChecksumAlgorithmFactory> getChecksumAlgorithmFactories() {
        HashMap<String, ChecksumAlgorithmFactory> result = new HashMap<>();
        result.put(Sha512ChecksumAlgorithmFactory.NAME, new Sha512ChecksumAlgorithmFactory());
        result.put(Sha256ChecksumAlgorithmFactory.NAME, new Sha256ChecksumAlgorithmFactory());
        result.put(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory());
        result.put(Md5ChecksumAlgorithmFactory.NAME, new Md5ChecksumAlgorithmFactory());
        return result;
    }

    private final DefaultChecksumAlgorithmService service = new DefaultChecksumAlgorithmService(
            new DefaultChecksumAlgorithmFactorySelector(getChecksumAlgorithmFactories()));

    @Test
    void smokeTest() {
        Collection<String> algNames = service.getChecksumAlgorithmNames();
        assertEquals(4, algNames.size());
    }

    @Test
    void emptySha1Calculator() {
        ChecksumAlgorithmService.ChecksumCalculator calculator =
                service.select("SHA-1").getCalculator();
        calculator.update(ByteBuffer.allocate(0));
        assertEquals(calculator.checksum(), "da39a3ee5e6b4b0d3255bfef95601890afd80709");
    }

    @Test
    void calculateByte() throws IOException {
        Map<ChecksumAlgorithmService.ChecksumAlgorithm, String> checksums = service.calculate(
                "test".getBytes(StandardCharsets.UTF_8), service.select(Arrays.asList("SHA-1", "MD5")));
        assertNotNull(checksums);
        assertEquals(2, checksums.size());
        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", checksums.get(service.select("SHA-1")));
        assertEquals("098f6bcd4621d373cade4e832627b4f6", checksums.get(service.select("MD5")));
    }

    @Test
    void calculateByteBuffer() throws IOException {
        Map<ChecksumAlgorithmService.ChecksumAlgorithm, String> checksums = service.calculate(
                ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)),
                service.select(Arrays.asList("SHA-1", "MD5")));
        assertNotNull(checksums);
        assertEquals(2, checksums.size());
        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", checksums.get(service.select("SHA-1")));
        assertEquals("098f6bcd4621d373cade4e832627b4f6", checksums.get(service.select("MD5")));
    }

    @Test
    void calculateStream() throws IOException {
        Map<ChecksumAlgorithmService.ChecksumAlgorithm, String> checksums = service.calculate(
                new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)),
                service.select(Arrays.asList("SHA-1", "MD5")));
        assertNotNull(checksums);
        assertEquals(2, checksums.size());
        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", checksums.get(service.select("SHA-1")));
        assertEquals("098f6bcd4621d373cade4e832627b4f6", checksums.get(service.select("MD5")));
    }
}
