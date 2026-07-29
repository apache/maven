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
package org.apache.maven.impl;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultTransportTest {

    @Test
    void testPutWithNonExistentFileThrows(@TempDir Path tempDir) {
        Transporter transporter = mock(Transporter.class);
        DefaultTransport transport = new DefaultTransport(URI.create("http://example.com/test/"), transporter);
        Path nonExistentFile = tempDir.resolve("missing.txt");
        assertThrows(IllegalArgumentException.class, () -> transport.put(nonExistentFile, URI.create("dest.txt")));
    }

    @Test
    void testPutWithExistingFileSucceeds(@TempDir Path tempDir) throws Exception {
        Path sourceFile = tempDir.resolve("source.txt");
        Files.writeString(sourceFile, "test content");

        Transporter transporter = mock(Transporter.class);
        DefaultTransport transport = new DefaultTransport(URI.create("http://example.com/test/"), transporter);
        URI dest = URI.create("dest.txt");
        transport.put(sourceFile, dest);
        verify(transporter).put(any(PutTask.class));
    }

    @Test
    void testPutBytesSucceeds() throws Exception {
        Transporter transporter = mock(Transporter.class);
        DefaultTransport transport = new DefaultTransport(URI.create("http://example.com/test/"), transporter);
        URI dest = URI.create("dest.txt");
        transport.putBytes("test content".getBytes(), dest);
        verify(transporter).put(any(PutTask.class));
    }
}
