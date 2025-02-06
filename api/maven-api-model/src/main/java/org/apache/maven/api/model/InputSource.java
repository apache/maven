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
package org.apache.maven.api.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the source of input for Maven model elements, typically a POM file.
 * This class maintains information about both the physical location (file path or URL)
 * and the logical identity (groupId:artifactId:version) of the source.
 *
 * <p>InputSource instances can represent either:</p>
 * <ul>
 *   <li>A single source file with its location and model ID</li>
 *   <li>A merged set of multiple input sources (e.g., from parent POMs)</li>
 * </ul>
 *
 * <p>The class is immutable and supports caching for performance optimization.
 * It provides factory methods for creating instances and utilities for merging
 * multiple sources.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * // Create a simple source
 * InputSource source = InputSource.source(
 *     "com.example:myproject:1.0",
 *     "path/to/pom.xml"
 * );
 *
 * // Create a merged source
 * InputSource merged = InputSource.merge(parentSource, childSource);
 * </pre>
 *
 * @see InputLocation
 */
public class InputSource implements Serializable, Cacheable {

    private final String modelId;
    private final String location;
    private final List<InputSource> inputs;
    private final InputLocation importedFrom;
    private final int hashCache;

    /**
     * Creates a new InputSource with model identification and location information.
     * This is the most common factory method for creating a simple input source.
     *
     * @param modelId the model identifier in the format "groupId:artifactId:version"
     * @param location the file path or URL where the model is located
     * @return a cached instance of InputSource
     */
    public static InputSource source(String modelId, String location) {
        return CacheManager.getInstance().cached(new InputSource(modelId, location, null, null));
    }

    /**
     * Creates a new InputSource with model identification, location, and import information.
     * This factory method is useful when tracking sources that have been imported from other files.
     *
     * @param modelId the model identifier in the format "groupId:artifactId:version"
     * @param location the file path or URL where the model is located
     * @param importedFrom the location from which this source was imported
     * @return a cached instance of InputSource
     */
    public static InputSource source(String modelId, String location, InputLocation importedFrom) {
        return CacheManager.getInstance().cached(new InputSource(modelId, location, null, importedFrom));
    }

    /**
     * Creates a new InputSource representing a merged set of input sources.
     * This factory method is used when combining multiple sources, such as when
     * dealing with parent POMs or aggregated models.
     *
     * @param inputs the list of input sources to merge
     * @return a cached instance of InputSource representing the merged sources
     */
    public static InputSource source(List<InputSource> inputs) {
        return CacheManager.getInstance().cached(new InputSource(null, null, inputs, null));
    }

    /**
     * Creates a new InputSource with complete information including model ID, location,
     * input sources, and import information. This is the most flexible factory method
     * that allows specifying all possible source attributes.
     *
     * @param modelId the model identifier in the format "groupId:artifactId:version"
     * @param location the file path or URL where the model is located
     * @param inputs the list of input sources to merge
     * @param importedFrom the location from which this source was imported
     * @return a cached instance of InputSource
     */
    public static InputSource source(
            String modelId, String location, List<InputSource> inputs, InputLocation importedFrom) {
        return CacheManager.getInstance().cached(new InputSource(modelId, location, inputs, importedFrom));
    }

    InputSource(String modelId, String location, List<InputSource> inputs, InputLocation importedFrom) {
        this.modelId = modelId;
        this.location = location;
        this.inputs = inputs != null ? ImmutableCollections.copy(inputs) : null;
        this.importedFrom = importedFrom;
        this.hashCache = CacheManager.getInstance().computeCacheHash(this);
    }

    @Override
    public int cacheIdentityHash() {
        return hashCache;
    }

    /**
     * Get the path/URL of the POM or {@code null} if unknown.
     *
     * @return the location
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Get the identifier of the POM in the format {@code <groupId>:<artifactId>:<version>}.
     *
     * @return the model id
     */
    public String getModelId() {
        return this.modelId;
    }

    /**
     * Gets the parent InputLocation where this InputLocation may have been imported from.
     * Can return {@code null}.
     *
     * @return InputLocation
     * @since 4.0.0
     */
    public InputLocation getImportedFrom() {
        return importedFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InputSource that = (InputSource) o;
        return Objects.equals(modelId, that.modelId)
                && Objects.equals(location, that.location)
                && Objects.equals(inputs, that.inputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, location, inputs);
    }

    Stream<InputSource> sources() {
        return inputs != null ? inputs.stream() : Stream.of(this);
    }

    @Override
    public String toString() {
        if (inputs != null) {
            return inputs.stream().map(InputSource::toString).collect(Collectors.joining(", ", "merged[", "]"));
        }
        return getModelId() + " " + getLocation();
    }

    /**
     * Merges two InputSource instances in a null-safe manner.
     *
     * @param src1 the first source
     * @param src2 the second source
     * @return merged InputSource, or one of the sources if the other is null
     */
    public static InputSource merge(InputSource src1, InputSource src2) {
        // If either source is null, return the other source
        if (src1 == null) {
            return src2;
        }
        if (src2 == null) {
            return src1;
        }

        // Create streams from both sources
        Stream<InputSource> stream1 = src1.sources();
        Stream<InputSource> stream2 = src2.sources();

        // Merge the streams and create a new InputSource
        List<InputSource> mergedSources = Stream.concat(stream1, stream2)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return new InputSource(null, null, mergedSources.isEmpty() ? null : mergedSources, null);
    }
}
