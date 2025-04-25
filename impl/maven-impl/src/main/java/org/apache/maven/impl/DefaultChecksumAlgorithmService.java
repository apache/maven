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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ChecksumAlgorithmService;
import org.apache.maven.api.services.ChecksumAlgorithmServiceException;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;

@Named
@Singleton
public class DefaultChecksumAlgorithmService implements ChecksumAlgorithmService {
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    @Inject
    public DefaultChecksumAlgorithmService(ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.checksumAlgorithmFactorySelector = Objects.requireNonNull(
                checksumAlgorithmFactorySelector, "checksumAlgorithmFactorySelector cannot be null");
    }

    @Override
    public Collection<String> getChecksumAlgorithmNames() {
        return checksumAlgorithmFactorySelector.getChecksumAlgorithmFactories().stream()
                .map(ChecksumAlgorithmFactory::getName)
                .toList();
    }

    @Override
    public ChecksumAlgorithm select(String algorithmName) {
        Objects.requireNonNull(algorithmName, "algorithmName cannot be null");
        try {
            return new DefaultChecksumAlgorithm(checksumAlgorithmFactorySelector.select(algorithmName));
        } catch (IllegalArgumentException e) {
            throw new ChecksumAlgorithmServiceException("unsupported algorithm", e);
        }
    }

    @Override
    public Collection<ChecksumAlgorithm> select(Collection<String> algorithmNames) {
        Objects.requireNonNull(algorithmNames, "algorithmNames cannot be null");
        try {
            return checksumAlgorithmFactorySelector.selectList(new ArrayList<>(algorithmNames)).stream()
                    .map(a -> (ChecksumAlgorithm) new DefaultChecksumAlgorithm(a))
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new ChecksumAlgorithmServiceException("unsupported algorithm", e);
        }
    }

    @Override
    public Map<ChecksumAlgorithm, String> calculate(byte[] data, Collection<ChecksumAlgorithm> algorithms) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(algorithms, "algorithms cannot be null");
        try {
            return calculate(new ByteArrayInputStream(data), algorithms);
        } catch (IOException e) {
            throw new UncheckedIOException(e); // really unexpected
        }
    }

    @Override
    public Map<ChecksumAlgorithm, String> calculate(ByteBuffer data, Collection<ChecksumAlgorithm> algorithms) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(algorithms, "algorithms cannot be null");
        LinkedHashMap<ChecksumAlgorithm, ChecksumCalculator> algMap = new LinkedHashMap<>();
        algorithms.forEach(f -> algMap.put(f, f.getCalculator()));
        data.mark();
        for (ChecksumCalculator checksumCalculator : algMap.values()) {
            checksumCalculator.update(data);
            data.reset();
        }
        LinkedHashMap<ChecksumAlgorithm, String> result = new LinkedHashMap<>();
        algMap.forEach((k, v) -> result.put(k, v.checksum()));
        return result;
    }

    @Override
    public Map<ChecksumAlgorithm, String> calculate(Path file, Collection<ChecksumAlgorithm> algorithms)
            throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        Objects.requireNonNull(algorithms, "algorithms cannot be null");
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
            return calculate(inputStream, algorithms);
        }
    }

    @Override
    public Map<ChecksumAlgorithm, String> calculate(InputStream stream, Collection<ChecksumAlgorithm> algorithms)
            throws IOException {
        Objects.requireNonNull(stream, "stream cannot be null");
        Objects.requireNonNull(algorithms, "algorithms cannot be null");
        LinkedHashMap<ChecksumAlgorithm, ChecksumCalculator> algMap = new LinkedHashMap<>();
        algorithms.forEach(f -> algMap.put(f, f.getCalculator()));
        final byte[] buffer = new byte[1024 * 32];
        for (; ; ) {
            int read = stream.read(buffer);
            if (read < 0) {
                break;
            }
            for (ChecksumCalculator checksumCalculator : algMap.values()) {
                checksumCalculator.update(ByteBuffer.wrap(buffer, 0, read));
            }
        }
        LinkedHashMap<ChecksumAlgorithm, String> result = new LinkedHashMap<>();
        algMap.forEach((k, v) -> result.put(k, v.checksum()));
        return result;
    }

    private static class DefaultChecksumAlgorithm implements ChecksumAlgorithm {
        private final ChecksumAlgorithmFactory factory;

        DefaultChecksumAlgorithm(ChecksumAlgorithmFactory factory) {
            this.factory = factory;
        }

        @Override
        public String getName() {
            return factory.getName();
        }

        @Override
        public String getFileExtension() {
            return factory.getFileExtension();
        }

        @Override
        public ChecksumCalculator getCalculator() {
            return new DefaultChecksumCalculator(factory.getAlgorithm());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DefaultChecksumAlgorithm that = (DefaultChecksumAlgorithm) o;
            return Objects.equals(factory.getName(), that.factory.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(factory.getName());
        }
    }

    private static class DefaultChecksumCalculator implements ChecksumCalculator {
        private final org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithm algorithm;

        DefaultChecksumCalculator(org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithm algorithm) {
            this.algorithm = algorithm;
        }

        @Override
        public void update(ByteBuffer input) {
            algorithm.update(input);
        }

        @Override
        public String checksum() {
            return algorithm.checksum();
        }
    }
}
