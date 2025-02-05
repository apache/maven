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
 * Class InputSource.
 */
public class InputSource implements Serializable, Cacheable {

    private final String modelId;
    private final String location;
    private final List<InputSource> inputs;
    private final InputLocation importedFrom;
    private final int hashCache;

    public static InputSource source(String modelId, String location) {
        return CacheManager.getInstance().cached(new InputSource(modelId, location, null, null));
    }

    public static InputSource source(String modelId, String location, InputLocation importedFrom) {
        return CacheManager.getInstance().cached(new InputSource(modelId, location, null, importedFrom));
    }

    public static InputSource source(List<InputSource> inputs) {
        return CacheManager.getInstance().cached(new InputSource(null, null, inputs, null));
    }

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

    public static InputSource merge(InputSource src1, InputSource src2) {
        return new InputSource(
                null,
                null,
                Stream.concat(src1.sources(), src2.sources()).distinct().toList(),
                null);
    }
}
