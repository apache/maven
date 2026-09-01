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

import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void testRelativeUriResolvesInsideBaseWithoutTrailingSlash(@TempDir Path tempDir) throws Exception {
        Transporter transporter = mock(Transporter.class);
        // base deliberately configured without a trailing slash
        DefaultTransport transport = new DefaultTransport(URI.create("http://example.com/repo"), transporter);
        transport.get(URI.create("repo-other/artifact.jar"), tempDir.resolve("out.jar"));

        ArgumentCaptor<GetTask> task = ArgumentCaptor.forClass(GetTask.class);
        verify(transporter).get(task.capture());
        // must resolve under the repository root, not to the sibling ".../repo-other/" tree
        assertEquals(
                "http://example.com/repo/repo-other/artifact.jar",
                task.getValue().getLocation().toString());
    }

    @Test
    void testRelativeUriOutsideBaseIsRejected(@TempDir Path tempDir) throws Exception {
        Transporter transporter = mock(Transporter.class);
        DefaultTransport transport = new DefaultTransport(URI.create("http://example.com/repo/"), transporter);
        assertThrows(
                IllegalArgumentException.class,
                () -> transport.get(URI.create("../other/artifact.jar"), tempDir.resolve("out.jar")));
        assertThrows(
                IllegalArgumentException.class,
                () -> transport.get(URI.create("//other.example.com/artifact.jar"), tempDir.resolve("out.jar")));

        Path source = tempDir.resolve("source.txt");
        Files.writeString(source, "content");
        assertThrows(IllegalArgumentException.class, () -> transport.put(source, URI.create("../other/artifact.jar")));
    }
}
