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

    private final String location;

    /**
     * Creates a new InputSource with the specified location.
     *
     * @param location the path/URL of the input source, may be null
     */
    InputSource(String location) {
        this.location = location;
    }

    /**
     * Creates a new InputSource with the specified location.
     *
     * @param location the path/URL of the input source, may be null
     * @deprecated Use {@link #of(String)} instead. This constructor will become package-protected in Maven 4.1.0.
     */
    @Deprecated
    public InputSource(String location) {
        this.location = location;
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
        return new InputSource(location);
    }

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

    /**
     * Returns a string representation of this InputSource.
     * The string representation is the location if available, or null.
     *
     * @return the location string, or null if no location is set
     */
    @Override
    public String toString() {
        return getLocation();
    }
}
