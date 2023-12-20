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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

import java.nio.ByteBuffer;

/**
 * Checksum algorithm, a factory for checksum calculator.
 *
 * @since 4.0.0
 */
@Experimental
public interface ChecksumAlgorithm {
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

    /**
     * The calculator instance.
     */
    interface ChecksumCalculator {
        /**
         * Updates the checksum algorithm inner state with input.
         */
        void update(ByteBuffer input);

        /**
         * Returns the algorithm end result as string, never {@code null}. After invoking this method, this instance should
         * be discarded and not reused. For new checksum calculation you have to get new instance.
         */
        @Nonnull
        String checksum();
    }

}
