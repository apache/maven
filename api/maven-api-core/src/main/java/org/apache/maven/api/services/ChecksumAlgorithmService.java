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
package org.apache.maven.api.services;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Checksum algorithms service.
 *
 * @since 4.0.0
 */
@Experimental
public interface ChecksumAlgorithmService extends Service {

    /**
     * Returns immutable collection of all supported algorithm names.
     */
    @Nonnull
    Collection<String> getChecksumAlgorithmNames();

    /**
     * Returns {@link ChecksumAlgorithm} for given algorithm name, or throws if algorithm not supported.
     *
     * @throws ChecksumAlgorithmServiceException if asked algorithm name is not supported.
     * @throws NullPointerException if passed in name is {@code null}.
     */
    @Nonnull
    ChecksumAlgorithm select(@Nonnull String algorithmName);

    /**
     * Returns a collection of {@link ChecksumAlgorithm} in same order as algorithm names are ordered, or throws if
     * any of the algorithm name is not supported. The returned collection has equal count of elements as passed in
     * collection of names, and if names contains duplicated elements, the returned list of algorithms will have
     * duplicates as well.
     *
     * @throws ChecksumAlgorithmServiceException if any asked algorithm name is not supported.
     * @throws NullPointerException if passed in list of names is {@code null}.
     */
    @Nonnull
    Collection<ChecksumAlgorithm> select(@Nonnull Collection<String> algorithmNames);

    /**
     * Calculates checksums for specified data.
     *
     * @param data        The content for which to calculate checksums, must not be {@code null}.
     * @param algorithms  The checksum algorithms to use, must not be {@code null}.
     * @return The calculated checksums, indexed by algorithms, never {@code null}.
     * @throws NullPointerException if passed in any parameter is {@code null}.
     */
    @Nonnull
    Map<ChecksumAlgorithm, String> calculate(@Nonnull byte[] data, @Nonnull Collection<ChecksumAlgorithm> algorithms);

    /**
     * Calculates checksums for specified data.
     *
     * @param data        The content for which to calculate checksums, must not be {@code null}.
     * @param algorithms  The checksum algorithms to use, must not be {@code null}.
     * @return The calculated checksums, indexed by algorithms, never {@code null}.
     * @throws NullPointerException if passed in any parameter is {@code null}.
     */
    @Nonnull
    Map<ChecksumAlgorithm, String> calculate(
            @Nonnull ByteBuffer data, @Nonnull Collection<ChecksumAlgorithm> algorithms);

    /**
     * Calculates checksums for specified file.
     *
     * @param file        The file for which to calculate checksums, must not be {@code null}.
     * @param algorithms  The checksum algorithms to use, must not be {@code null}.
     * @return The calculated checksums, indexed by algorithms, never {@code null}.
     * @throws NullPointerException if passed in any parameter is {@code null}.
     * @throws IOException In case of any IO problem.
     */
    @Nonnull
    Map<ChecksumAlgorithm, String> calculate(@Nonnull Path file, @Nonnull Collection<ChecksumAlgorithm> algorithms)
            throws IOException;

    /**
     * Calculates checksums for specified stream. Upon this method returns, the stream will be depleted (fully read)
     * but not closed.
     *
     * @param stream      The stream for which to calculate checksums, must not be {@code null}.
     * @param algorithms  The checksum algorithms to use, must not be {@code null}.
     * @return The calculated checksums, indexed by algorithms, never {@code null}.
     * @throws NullPointerException if passed in any parameter is {@code null}.
     * @throws IOException In case of any IO problem.
     */
    @Nonnull
    Map<ChecksumAlgorithm, String> calculate(
            @Nonnull InputStream stream, @Nonnull Collection<ChecksumAlgorithm> algorithms) throws IOException;

    /**
     * The checksum algorithm.
     */
    interface ChecksumAlgorithm {
        /**
         * Returns the algorithm name, usually used as key, never {@code null} value. The name is a standard name of
         * algorithm (if applicable) or any other designator that is algorithm commonly referred with. Example: "SHA-1".
         */
        @Nonnull
        String getName();

        /**
         * Returns the file extension to be used for given checksum file (without leading dot), never {@code null}. The
         * extension should be file and URL path friendly, and may differ from algorithm name.
         * The checksum extension SHOULD NOT contain dot (".") character.
         * Example: "sha1".
         */
        @Nonnull
        String getFileExtension();

        /**
         * Each invocation of this method returns a new instance of calculator, never {@code null} value.
         */
        @Nonnull
        ChecksumCalculator getCalculator();
    }

    /**
     * The checksum calculator.
     */
    interface ChecksumCalculator {
        /**
         * Updates the checksum algorithm inner state with input.
         *
         * @throws NullPointerException if passed in buffer is {@code null}.
         */
        void update(@Nonnull ByteBuffer input);

        /**
         * Returns the algorithm end result as string, never {@code null}. After invoking this method, this instance should
         * be discarded and not reused. For new checksum calculation you have to get new instance.
         */
        @Nonnull
        String checksum();
    }
}
