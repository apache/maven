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

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.api.services.Transport;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.connector.transport.Transporter;

import static java.util.Objects.requireNonNull;

@Named
@Singleton
public class DefaultTransport implements Transport {
    private final URI baseURI;
    private final Transporter transporter;

    public DefaultTransport(URI baseURI, Transporter transporter) {
        this.baseURI = requireNonNull(baseURI);
        this.transporter = requireNonNull(transporter);
    }

    @Override
    public boolean get(URI relativeSource, Path target) {
        requireNonNull(relativeSource, "relativeSource is null");
        requireNonNull(target, "target is null");
        if (relativeSource.isAbsolute()) {
            throw new IllegalArgumentException("Supplied URI is not relative");
        }
        URI source = baseURI.resolve(relativeSource);
        if (!source.toASCIIString().startsWith(baseURI.toASCIIString())) {
            throw new IllegalArgumentException("Supplied relative URI escapes baseUrl");
        }
        GetTask getTask = new GetTask(source);
        getTask.setDataFile(target.toFile());
        try {
            transporter.get(getTask);
            return true;
        } catch (Exception e) {
            if (Transporter.ERROR_NOT_FOUND != transporter.classify(e)) {
                throw new RuntimeException(e);
            }
            return false;
        }
    }

    @Override
    public Optional<byte[]> getBytes(URI relativeSource) {
        try {
            Path tempPath = null;
            try {
                tempPath = Files.createTempFile("transport-get", "tmp");
                if (get(relativeSource, tempPath)) {
                    // TODO: check file size and prevent OOM?
                    return Optional.of(Files.readAllBytes(tempPath));
                }
                return Optional.empty();
            } finally {
                if (tempPath != null) {
                    Files.deleteIfExists(tempPath);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<String> getString(URI relativeSource, Charset charset) {
        requireNonNull(charset, "charset is null");
        Optional<byte[]> data = getBytes(relativeSource);
        return data.map(bytes -> new String(bytes, charset));
    }

    @Override
    public void put(Path source, URI relativeTarget) {
        requireNonNull(source, "source is null");
        requireNonNull(relativeTarget, "relativeTarget is null");
        if (Files.isRegularFile(source)) {
            throw new IllegalArgumentException("source file does not exist or is not a file");
        }
        if (relativeTarget.isAbsolute()) {
            throw new IllegalArgumentException("Supplied URI is not relative");
        }
        URI target = baseURI.resolve(relativeTarget);
        if (!target.toASCIIString().startsWith(baseURI.toASCIIString())) {
            throw new IllegalArgumentException("Supplied relative URI escapes baseUrl");
        }

        PutTask putTask = new PutTask(target);
        putTask.setDataFile(source.toFile());
        try {
            transporter.put(putTask);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putBytes(byte[] source, URI relativeTarget) {
        requireNonNull(source, "source is null");
        try {
            Path tempPath = null;
            try {
                tempPath = Files.createTempFile("transport-get", "tmp");
                Files.write(tempPath, source);
                put(tempPath, relativeTarget);
            } finally {
                if (tempPath != null) {
                    Files.deleteIfExists(tempPath);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void putString(String source, Charset charset, URI relativeTarget) {
        requireNonNull(source, "source string is null");
        requireNonNull(charset, "charset is null");
        putBytes(source.getBytes(charset), relativeTarget);
    }

    @Override
    public void close() {
        transporter.close();
    }
}
