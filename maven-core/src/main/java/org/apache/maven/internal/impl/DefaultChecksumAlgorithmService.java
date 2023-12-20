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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.maven.api.services.*;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmHelper;

import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultChecksumAlgorithmService implements ChecksumAlgorithmService {
    private final ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    @Inject
    public DefaultChecksumAlgorithmService(ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        this.checksumAlgorithmFactorySelector =
                nonNull(checksumAlgorithmFactorySelector, "checksumAlgorithmFactorySelector");
    }

    @Override
    public ChecksumAlgorithm select(String algorithmName) {
        nonNull(algorithmName, "algorithmName");
        return new DefaultChecksumAlgorithm(checksumAlgorithmFactorySelector.select(algorithmName));
    }

    @Override
    public Collection<ChecksumAlgorithm> select(Collection<String> algorithmNames) {
        nonNull(algorithmNames, "algorithmNames");
        return checksumAlgorithmFactorySelector.selectList(new ArrayList<>(algorithmNames)).stream()
                .map(DefaultChecksumAlgorithm::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getChecksumAlgorithmNames() {
        return checksumAlgorithmFactorySelector.getChecksumAlgorithmFactories().stream()
                .map(ChecksumAlgorithmFactory::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Map<ChecksumAlgorithm, String> calculate(byte[] data, Collection<ChecksumAlgorithm> algorithms)
            throws IOException {
        nonNull(data, "data");
        nonNull(algorithms, "algorithms");
        Map<String, String> checksums = ChecksumAlgorithmHelper.calculate(
                data,
                algorithms.stream()
                        .map(a -> ((DefaultChecksumAlgorithm) a).factory)
                        .collect(Collectors.toList()));
        return remap(algorithms, checksums);
    }

    @Override
    public Map<ChecksumAlgorithm, String> calculate(ByteBuffer data, Collection<ChecksumAlgorithm> algorithms)
            throws IOException {
        nonNull(data, "data");
        nonNull(algorithms, "algorithms");
        LinkedHashMap<ChecksumAlgorithm, ChecksumCalculator> algMap = new LinkedHashMap<>();
        algorithms.forEach(f -> algMap.put(f, f.getCalculator()));
        for (ChecksumCalculator checksumCalculator : algMap.values()) {
            checksumCalculator.update(data);
        }
        LinkedHashMap<ChecksumAlgorithm, String> result = new LinkedHashMap<>();
        algMap.forEach((k, v) -> result.put(k, v.checksum()));
        return result;
    }

    @Override
    public Map<ChecksumAlgorithm, String> calculate(Path file, Collection<ChecksumAlgorithm> algorithms)
            throws IOException {
        nonNull(file, "file");
        nonNull(algorithms, "algorithms");
        Map<String, String> checksums = ChecksumAlgorithmHelper.calculate(
                file.toFile(),
                algorithms.stream()
                        .map(a -> ((DefaultChecksumAlgorithm) a).factory)
                        .collect(Collectors.toList()));
        return remap(algorithms, checksums);
    }

    @Override
    public Map<ChecksumAlgorithm, String> calculate(InputStream stream, Collection<ChecksumAlgorithm> algorithms)
            throws IOException {
        nonNull(stream, "stream");
        nonNull(algorithms, "algorithms");
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

    private Map<ChecksumAlgorithm, String> remap(
            Collection<ChecksumAlgorithm> algorithms, Map<String, String> checksums) {
        LinkedHashMap<ChecksumAlgorithm, String> result = new LinkedHashMap<>();
        for (ChecksumAlgorithm alg : algorithms) {
            String checksum = nonNull(checksums.get(alg.getName()), "bug: alg asked but not present");
            result.put(alg, checksum);
        }
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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
