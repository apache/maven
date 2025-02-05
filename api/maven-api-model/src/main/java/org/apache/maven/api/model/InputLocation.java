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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class InputLocation.
 */
public class InputLocation implements Serializable, InputLocationTracker, Cacheable {
    private final int lineNumber;
    private final int columnNumber;
    private final InputSource source;
    private final Map<Object, InputLocation> locations;
    private final int cacheHash;

    public static InputLocation location(InputSource source) {
        return CacheManager.getInstance().cached(new InputLocation(-1, -1, source, 0));
    }

    public static InputLocation location(int lineNumber, int columnNumber) {
        return CacheManager.getInstance().cached(new InputLocation(lineNumber, columnNumber, null, null));
    }

    public static InputLocation location(int lineNumber, int columnNumber, InputSource source) {
        return CacheManager.getInstance().cached(new InputLocation(lineNumber, columnNumber, source, null));
    }

    public static InputLocation location(int lineNumber, int columnNumber, InputSource source, Object selfLocationKey) {
        return CacheManager.getInstance().cached(new InputLocation(lineNumber, columnNumber, source, selfLocationKey));
    }

    InputLocation(int lineNumber, int columnNumber, InputSource source, Object selfLocationKey) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.source = source;
        this.locations = selfLocationKey != null
                ? ImmutableCollections.singletonMap(selfLocationKey, this)
                : ImmutableCollections.emptyMap();
        this.cacheHash = CacheManager.getInstance().computeCacheHash(this);
    }

    InputLocation(int lineNumber, int columnNumber, InputSource source, Map<Object, InputLocation> locations) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.source = source;
        this.locations = ImmutableCollections.copy(locations);
        this.cacheHash = CacheManager.getInstance().computeCacheHash(this);
    }

    @Override
    public int cacheIdentityHash() {
        return cacheHash;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public InputSource getSource() {
        return source;
    }

    public InputLocation getLocation(Object key) {
        return locations != null ? locations.get(key) : null;
    }

    public Map<Object, InputLocation> getLocations() {
        return locations;
    }

    /**
     * Gets the parent InputLocation where this InputLocation may have been imported from.
     * Can return {@code null}.
     *
     * @return InputLocation
     * @since 4.0.0
     */
    public InputLocation getImportedFrom() {
        return null;
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

        return new InputLocation(-1, -1, InputSource.merge(source.getSource(), target.getSource()), locations);
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

        return new InputLocation(-1, -1, InputSource.merge(source.getSource(), target.getSource()), locations);
    } // -- InputLocation merge( InputLocation, InputLocation, java.util.Collection )

    /**
     * Class StringFormatter.
     *
     * @version $Revision$ $Date$
     */
    public interface StringFormatter {

        // -----------/
        // - Methods -/
        // -----------/

        /**
         * Method toString.
         */
        String toString(InputLocation location);
    }

    @Override
    public String toString() {
        return String.format("%s @ %d:%d", source != null ? source.getLocation() : "n/a", lineNumber, columnNumber);
    }
}
