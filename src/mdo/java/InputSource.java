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
package ${package};

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the source of a model input, such as a POM file.
 * <p>
 * This class tracks the origin of model elements, providing location information
 * used primarily for error reporting and debugging to help identify where specific
 * model elements came from. The location typically represents a file path, URL,
 * or other identifier that describes the source of the input.
 * <p>
 * InputSource instances are immutable and can be safely shared across threads.
 * The class provides factory methods for convenient creation of instances.
 *
 * @since 4.0.0
 */
public final class InputSource implements Serializable {

#if ( $isMavenModel )
    private final String modelId;
#end
    private final String location;
    private final List<InputSource> inputs;
    private final InputLocation importedFrom;

#if ( $isMavenModel )
    private volatile int hashCode = 0; // Cached hashCode for performance
#end

#if ( $isMavenModel )
    public InputSource(String modelId, String location) {
        this(modelId, location, null);
    }

    public InputSource(String modelId, String location, InputLocation importedFrom) {
        this.modelId = modelId;
        this.location = location;
        this.inputs = null;
        this.importedFrom = importedFrom;
    }
#end

    /**
     * Creates a new InputSource with the specified location.
     *
     * @param location the path/URL of the input source, may be null
     */
    InputSource(String location) {
#if ( $isMavenModel )
        this.modelId = null;
#end
        this.location = location;
        this.inputs = null;
        this.importedFrom = null;
    }

    public InputSource(Collection<InputSource> inputs) {
#if ( $isMavenModel )
        this.modelId = null;
#end
        this.location = null;
        this.inputs = ImmutableCollections.copy(inputs);
        this.importedFrom = null;
    }

    /**
     * Creates a new InputSource with the specified location.
     * The location typically represents a file path, URL, or other identifier
     * that describes where the input originated from.
     *
     * @param location the path/URL of the input source, may be null
     * @return a new InputSource instance
     */
    public static InputSource of(String location) {
#if ( $isMavenModel )
        return ModelObjectProcessor.processObject(new InputSource(location));
#else
        return new InputSource(location);
#end
    }

#if ( $isMavenModel )
    /**
     * Creates a new InputSource with the specified model ID and location.
     * The created instance is processed through ModelObjectProcessor for optimization.
     *
     * @param modelId the model ID
     * @param location the location
     * @return a new InputSource instance
     */
    public static InputSource of(String modelId, String location) {
        return ModelObjectProcessor.processObject(new InputSource(modelId, location));
    }

    /**
     * Creates a new InputSource with the specified model ID, location, and imported from location.
     * The created instance is processed through ModelObjectProcessor for optimization.
     *
     * @param modelId the model ID
     * @param location the location
     * @param importedFrom the imported from location
     * @return a new InputSource instance
     */
    public static InputSource of(String modelId, String location, InputLocation importedFrom) {
        return ModelObjectProcessor.processObject(new InputSource(modelId, location, importedFrom));
    }

    /**
     * Creates a new InputSource from a collection of input sources.
     * The created instance is processed through ModelObjectProcessor for optimization.
     *
     * @param inputs the collection of input sources
     * @return a new InputSource instance
     */
    public static InputSource of(Collection<InputSource> inputs) {
        return ModelObjectProcessor.processObject(new InputSource(inputs));
    }
#end

    /**
     * Gets the path/URL of the input source or {@code null} if unknown.
     * <p>
     * The location typically represents a file path, URL, or other identifier
     * that describes where the input originated from. This information is
     * primarily used for error reporting and debugging purposes.
     *
     * @return the location string, or null if unknown
     */
    public String getLocation() {
        return this.location;
    }

#if ( $isMavenModel )
    /**
     * Get the identifier of the POM in the format {@code <groupId>:<artifactId>:<version>}.
     *
     * @return the model id
     */
    public String getModelId() {
        return this.modelId;
    }
#end

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

#if ( $isMavenModel )
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
                && Objects.equals(inputs, that.inputs)
                && Objects.equals(importedFrom, that.importedFrom);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = Objects.hash(modelId, location, inputs, importedFrom);
            hashCode = result;
        }
        return result;
    }
#else
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InputSource that = (InputSource) o;
        return Objects.equals(location, that.location)
                && Objects.equals(inputs, that.inputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, inputs);
    }
#end

    /**
     * Returns a stream of all input sources contained in this instance.
     * For merged sources, returns all constituent sources; for single sources, returns this instance.
     *
     * @return a stream of InputSource instances
     */
    Stream<InputSource> sources() {
        return inputs != null ? inputs.stream() : Stream.of(this);
    }

    @Override
    public String toString() {
        if (inputs != null) {
            return inputs.stream().map(InputSource::toString).collect(Collectors.joining(", ", "merged[", "]"));
        }
#if ( $isMavenModel )
        return getModelId() != null ? getModelId() + " " + getLocation() : getLocation();
#else
        return getLocation();
#end
    }

    /**
     * Merges two InputSource instances into a single merged InputSource.
     * The resulting InputSource will contain all distinct sources from both inputs.
     *
     * @param src1 the first input source to merge
     * @param src2 the second input source to merge
     * @return a new merged InputSource containing all distinct sources from both inputs
     */
    public static InputSource merge(InputSource src1, InputSource src2) {
#if ( $isMavenModel )
        return new InputSource(
                Stream.concat(src1.sources(), src2.sources()).distinct().toList());
#else
        return new InputSource(Stream.concat(src1.sources(), src2.sources()).collect(Collectors.toSet()));
#end
    }
}
