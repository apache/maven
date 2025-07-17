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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the location of an element within a model source file.
 * <p>
 * This class tracks the line and column numbers of elements in source files like POM files.
 * It's used for error reporting and debugging to help identify where specific model elements
 * are defined in the source files. The class supports nested location tracking through a
 * locations map that can contain InputLocation instances for child elements.
 * <p>
 * InputLocation instances are immutable and can be safely shared across threads.
 * Factory methods are provided for convenient creation of instances with different
 * combinations of location information.
 *
 * @since 4.0.0
 */
public final class InputLocation implements Serializable, InputLocationTracker {
    private final int lineNumber;
    private final int columnNumber;
    private final InputSource source;
    private final Map<Object, InputLocation> locations;

    private static final InputLocation EMPTY = new InputLocation(-1, -1);

    /**
     * Creates an InputLocation with only a source, no line/column information.
     * The line and column numbers will be set to -1 (unknown).
     *
     * @param source the input source where this location originates from
     */
    InputLocation(InputSource source) {
        this.lineNumber = -1;
        this.columnNumber = -1;
        this.source = source;
        this.locations = Collections.singletonMap(0, this);
    }

    /**
     * Creates an InputLocation with line and column numbers but no source.
     *
     * @param lineNumber the line number in the source file (1-based)
     * @param columnNumber the column number in the source file (1-based)
     */
    InputLocation(int lineNumber, int columnNumber) {
        this(lineNumber, columnNumber, null, null);
    }

    /**
     * Creates an InputLocation with line number, column number, and source.
     *
     * @param lineNumber the line number in the source file (1-based)
     * @param columnNumber the column number in the source file (1-based)
     * @param source the input source where this location originates from
     */
    InputLocation(int lineNumber, int columnNumber, InputSource source) {
        this(lineNumber, columnNumber, source, null);
    }

    /**
     * Creates an InputLocation with line number, column number, source, and a self-location key.
     *
     * @param lineNumber the line number in the source file (1-based)
     * @param columnNumber the column number in the source file (1-based)
     * @param source the input source where this location originates from
     * @param selfLocationKey the key to map this location to itself in the locations map
     */
    InputLocation(int lineNumber, int columnNumber, InputSource source, Object selfLocationKey) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.source = source;
        this.locations =
                selfLocationKey != null ? Collections.singletonMap(selfLocationKey, this) : Collections.emptyMap();
    }

    /**
     * Creates an InputLocation with line number, column number, source, and a complete locations map.
     *
     * @param lineNumber the line number in the source file (1-based)
     * @param columnNumber the column number in the source file (1-based)
     * @param source the input source where this location originates from
     * @param locations a map of keys to InputLocation instances for nested elements
     */
    InputLocation(int lineNumber, int columnNumber, InputSource source, Map<Object, InputLocation> locations) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.source = source;
        this.locations = ImmutableCollections.copy(locations);
    }

    /**
     * Creates an empty InputLocation with no line/column information.
     * This is a singleton instance representing an unknown or unspecified location.
     *
     * @return an empty InputLocation instance
     */
    public static InputLocation of() {
        return EMPTY;
    }

    /**
     * Creates an InputLocation with the specified line and column numbers.
     * The source and locations map will be null.
     *
     * @param lineNumber the line number in the source file (1-based)
     * @param columnNumber the column number in the source file (1-based)
     * @return a new InputLocation instance
     */
    public static InputLocation of(int lineNumber, int columnNumber) {
        return new InputLocation(lineNumber, columnNumber);
    }

    /**
     * Creates an InputLocation with the specified line number, column number, and source.
     * The locations map will be empty.
     *
     * @param lineNumber the line number in the source file (1-based)
     * @param columnNumber the column number in the source file (1-based)
     * @param source the input source where this location originates from
     * @return a new InputLocation instance
     */
    public static InputLocation of(int lineNumber, int columnNumber, InputSource source) {
        return new InputLocation(lineNumber, columnNumber, source);
    }

    /**
     * Creates an InputLocation with the specified line number, column number, source,
     * and a self-location key. The locations map will contain a single entry mapping
     * the selfLocationKey to this location.
     *
     * @param lineNumber the line number in the source file (1-based)
     * @param columnNumber the column number in the source file (1-based)
     * @param source the input source where this location originates from
     * @param selfLocationKey the key to map this location to itself in the locations map
     * @return a new InputLocation instance
     */
    public static InputLocation of(int lineNumber, int columnNumber, InputSource source, Object selfLocationKey) {
        return new InputLocation(lineNumber, columnNumber, source, selfLocationKey);
    }

    /**
     * Creates an InputLocation with the specified line number, column number, source,
     * and a complete locations map. This is typically used when merging or combining
     * location information from multiple sources.
     *
     * @param lineNumber the line number in the source file (1-based)
     * @param columnNumber the column number in the source file (1-based)
     * @param source the input source where this location originates from
     * @param locations a map of keys to InputLocation instances for nested elements
     * @return a new InputLocation instance
     */
    public static InputLocation of(int lineNumber, int columnNumber, InputSource source, Map<Object, InputLocation> locations) {
        return new InputLocation(lineNumber, columnNumber, source, locations);
    }

    /**
     * Gets the one-based line number where this element is located in the source file.
     *
     * @return the line number, or -1 if unknown
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Gets the one-based column number where this element is located in the source file.
     *
     * @return the column number, or -1 if unknown
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Gets the input source where this location originates from.
     *
     * @return the input source, or null if unknown
     */
    public InputSource getSource() {
        return source;
    }

    /**
     * Gets the InputLocation for a specific nested element key.
     *
     * @param key the key to look up
     * @return the InputLocation for the specified key, or null if not found
     */
    @Override
    public InputLocation getLocation(Object key) {
        return locations != null ? locations.get(key) : null;
    }

    /**
     * Gets the map of nested element locations within this location.
     *
     * @return an immutable map of keys to InputLocation instances for nested elements
     */
    public Map<Object, InputLocation> getLocations() {
        return locations;
    }

    /**
     * Merges the {@code source} location into the {@code target} location.
     *
     * @param target the target location
     * @param source the source location
     * @param sourceDominant the boolean indicating of {@code source} is dominant compared to {@code target}
     * @return the merged location
     */
    public static InputLocation merge(InputLocation target, InputLocation source, boolean sourceDominant) {
        if (source == null) {
            return target;
        } else if (target == null) {
            return source;
        }

        Map<Object, InputLocation> locations;
        Map<Object, InputLocation> sourceLocations = source.locations;
        Map<Object, InputLocation> targetLocations = target.locations;
        if (sourceLocations == null) {
            locations = targetLocations;
        } else if (targetLocations == null) {
            locations = sourceLocations;
        } else {
            locations = new LinkedHashMap<>();
            locations.putAll(sourceDominant ? targetLocations : sourceLocations);
            locations.putAll(sourceDominant ? sourceLocations : targetLocations);
        }

        return new InputLocation(target.getLineNumber(), target.getColumnNumber(), target.getSource(), locations);
    } // -- InputLocation merge( InputLocation, InputLocation, boolean )

    /**
     * Merges the {@code source} location into the {@code target} location.
     * This method is used when the locations refer to lists and also merges the indices.
     *
     * @param target the target location
     * @param source the source location
     * @param indices the list of integers for the indices
     * @return the merged location
     */
    public static InputLocation merge(InputLocation target, InputLocation source, Collection<Integer> indices) {
        if (source == null) {
            return target;
        } else if (target == null) {
            return source;
        }

        Map<Object, InputLocation> locations;
        Map<Object, InputLocation> sourceLocations = source.locations;
        Map<Object, InputLocation> targetLocations = target.locations;
        if (sourceLocations == null) {
            locations = targetLocations;
        } else if (targetLocations == null) {
            locations = sourceLocations;
        } else {
            locations = new LinkedHashMap<>();
            for (int index : indices) {
                InputLocation location;
                if (index < 0) {
                    location = sourceLocations.get(~index);
                } else {
                    location = targetLocations.get(index);
                }
                locations.put(locations.size(), location);
            }
        }

        return new InputLocation(target.getLineNumber(), target.getColumnNumber(), target.getSource(), locations);
    } // -- InputLocation merge( InputLocation, InputLocation, java.util.Collection )
}
