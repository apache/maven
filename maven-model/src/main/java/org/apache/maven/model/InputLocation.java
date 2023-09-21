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
package org.apache.maven.model;

import java.util.stream.Collectors;

/**
 * Class InputLocation.
 *
 * @version $Revision$ $Date$
 */
@SuppressWarnings("all")
public final class InputLocation implements java.io.Serializable, Cloneable, InputLocationTracker {

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * The one-based line number. The value will be non-positive if
     * unknown.
     */
    private int lineNumber = -1;

    /**
     * The one-based column number. The value will be non-positive
     * if unknown.
     */
    private int columnNumber = -1;

    /**
     * Field source.
     */
    private InputSource source;

    /**
     * Field locations.
     */
    private java.util.Map<Object, InputLocation> locations;

    /**
     * Field location.
     */
    private InputLocation location;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public InputLocation(org.apache.maven.api.model.InputLocation location) {
        this.lineNumber = location.getLineNumber();
        this.columnNumber = location.getColumnNumber();
        this.source = location.getSource() != null ? new InputSource(location.getSource()) : null;
        this.locations = location.getLocations().isEmpty()
                ? null
                : location.getLocations().entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> e.getValue() == location ? this : new InputLocation(e.getValue())));
    }

    public InputLocation(int lineNumber, int columnNumber) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    } // -- org.apache.maven.model.InputLocation(int, int)

    public InputLocation(int lineNumber, int columnNumber, InputSource source) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.source = source;
    } // -- org.apache.maven.model.InputLocation(int, int, InputSource)

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Method clone.
     *
     * @return InputLocation
     */
    public InputLocation clone() {
        try {
            InputLocation copy = (InputLocation) super.clone();

            if (copy.locations != null) {
                copy.locations = new java.util.LinkedHashMap(copy.locations);
            }

            return copy;
        } catch (Exception ex) {
            throw (RuntimeException)
                    new UnsupportedOperationException(getClass().getName() + " does not support clone()").initCause(ex);
        }
    } // -- InputLocation clone()

    /**
     * Get the one-based column number. The value will be
     * non-positive if unknown.
     *
     * @return int
     */
    public int getColumnNumber() {
        return this.columnNumber;
    } // -- int getColumnNumber()

    /**
     * Get the one-based line number. The value will be
     * non-positive if unknown.
     *
     * @return int
     */
    public int getLineNumber() {
        return this.lineNumber;
    } // -- int getLineNumber()

    /**
     *
     *
     * @param key
     * @return InputLocation
     */
    public InputLocation getLocation(Object key) {
        if (key instanceof String) {
            switch ((String) key) {
                case "": {
                    return this.location;
                }
                default: {
                    return getOtherLocation(key);
                }
            }
        } else {
            return getOtherLocation(key);
        }
    } // -- InputLocation getLocation( Object )

    /**
     *
     *
     * @return Map
     */
    public java.util.Map<Object, InputLocation> getLocations() {
        return locations;
    } // -- java.util.Map<Object, InputLocation> getLocations()

    /**
     *
     *
     * @param key
     * @param location
     */
    public void setLocation(Object key, InputLocation location) {
        if (key instanceof String) {
            switch ((String) key) {
                case "": {
                    this.location = location;
                    return;
                }
                default: {
                    setOtherLocation(key, location);
                    return;
                }
            }
        } else {
            setOtherLocation(key, location);
        }
    } // -- void setLocation( Object, InputLocation )

    /**
     *
     *
     * @param key
     * @param location
     */
    public void setOtherLocation(Object key, InputLocation location) {
        if (location != null) {
            if (this.locations == null) {
                this.locations = new java.util.LinkedHashMap<Object, InputLocation>();
            }
            this.locations.put(key, location);
        }
    } // -- void setOtherLocation( Object, InputLocation )

    /**
     *
     *
     * @param key
     * @return InputLocation
     */
    private InputLocation getOtherLocation(Object key) {
        return (locations != null) ? locations.get(key) : null;
    } // -- InputLocation getOtherLocation( Object )

    /**
     * Get the source field.
     *
     * @return InputSource
     */
    public InputSource getSource() {
        return this.source;
    } // -- InputSource getSource()

    /**
     * Method merge.
     *
     * @param target
     * @param sourceDominant
     * @param source
     * @return InputLocation
     */
    public static InputLocation merge(InputLocation target, InputLocation source, boolean sourceDominant) {
        if (source == null) {
            return target;
        } else if (target == null) {
            return source;
        }

        InputLocation result = new InputLocation(target.getLineNumber(), target.getColumnNumber(), target.getSource());

        java.util.Map<Object, InputLocation> locations;
        java.util.Map<Object, InputLocation> sourceLocations = source.getLocations();
        java.util.Map<Object, InputLocation> targetLocations = target.getLocations();
        if (sourceLocations == null) {
            locations = targetLocations;
        } else if (targetLocations == null) {
            locations = sourceLocations;
        } else {
            locations = new java.util.LinkedHashMap();
            locations.putAll(sourceDominant ? targetLocations : sourceLocations);
            locations.putAll(sourceDominant ? sourceLocations : targetLocations);
        }
        result.setLocations(locations);

        return result;
    } // -- InputLocation merge( InputLocation, InputLocation, boolean )

    /**
     * Method merge.
     *
     * @param target
     * @param indices
     * @param source
     * @return InputLocation
     */
    public static InputLocation merge(
            InputLocation target, InputLocation source, java.util.Collection<Integer> indices) {
        if (source == null) {
            return target;
        } else if (target == null) {
            return source;
        }

        InputLocation result = new InputLocation(target.getLineNumber(), target.getColumnNumber(), target.getSource());

        java.util.Map<Object, InputLocation> locations;
        java.util.Map<Object, InputLocation> sourceLocations = source.getLocations();
        java.util.Map<Object, InputLocation> targetLocations = target.getLocations();
        if (sourceLocations == null) {
            locations = targetLocations;
        } else if (targetLocations == null) {
            locations = sourceLocations;
        } else {
            locations = new java.util.LinkedHashMap<Object, InputLocation>();
            for (java.util.Iterator<Integer> it = indices.iterator(); it.hasNext(); ) {
                InputLocation location;
                Integer index = it.next();
                if (index.intValue() < 0) {
                    location = sourceLocations.get(Integer.valueOf(~index.intValue()));
                } else {
                    location = targetLocations.get(index);
                }
                locations.put(Integer.valueOf(locations.size()), location);
            }
        }
        result.setLocations(locations);

        return result;
    } // -- InputLocation merge( InputLocation, InputLocation, java.util.Collection )

    /**
     *
     *
     * @param locations
     */
    public void setLocations(java.util.Map<Object, InputLocation> locations) {
        this.locations = locations;
    } // -- void setLocations( java.util.Map )

    public org.apache.maven.api.model.InputLocation toApiLocation() {
        if (locations != null && locations.values().contains(this)) {
            if (locations.size() == 1 && locations.values().iterator().next() == this) {
                return new org.apache.maven.api.model.InputLocation(
                        lineNumber,
                        columnNumber,
                        source != null ? source.toApiSource() : null,
                        locations.keySet().iterator().next());
            } else {
                return new org.apache.maven.api.model.InputLocation(
                        lineNumber, columnNumber, source != null ? source.toApiSource() : null);
            }
        } else {
            return new org.apache.maven.api.model.InputLocation(
                    lineNumber,
                    columnNumber,
                    source != null ? source.toApiSource() : null,
                    locations != null
                            ? locations.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()
                                    .toApiLocation()))
                            : null);
        }
    }

    // -----------------/
    // - Inner Classes -/
    // -----------------/

    /**
     * Class StringFormatter.
     *
     * @version $Revision$ $Date$
     */
    public abstract static class StringFormatter {

        // -----------/
        // - Methods -/
        // -----------/

        /**
         * Method toString.
         *
         * @param location
         * @return String
         */
        public abstract String toString(InputLocation location);
    }

    @Override
    public String toString() {
        return getLineNumber() + " : " + getColumnNumber() + ", " + getSource();
    }
}
