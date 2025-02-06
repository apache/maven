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

import org.apache.maven.api.annotations.Nullable;

/**
 * Provides a contract for tracking the source location of model elements.
 * This interface is implemented by classes that need to maintain information
 * about where their data was defined in source files.
 *
 * <p>The interface supports hierarchical location tracking, where elements can
 * have both their own location and locations for their child elements. It also
 * supports tracking locations of elements that have been imported from other
 * files.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * public class ModelElement implements InputLocationTracker {
 *     private InputLocation location;
 *     private Map&lt;Object, InputLocation&gt; locations;
 *
 *     public InputLocation getLocation(Object field) {
 *         return locations.get(field);
 *     }
 *
 *     public InputLocation getImportedFrom() {
 *         return location.getImportedFrom();
 *     }
 * }
 * </pre>
 */
public interface InputLocationTracker {

    /**
     * Retrieves the location information for a specific field within the implementing class.
     *
     * @param field the identifier for the field whose location should be retrieved
     * @return the InputLocation for the specified field, or null if not found
     */
    InputLocation getLocation(Object field);

    /**
     * Retrieves the original location information when the current element was imported
     * from another source. This is particularly useful for tracking the origin of
     * inherited or merged model elements.
     *
     * @return the InputLocation from which this element was imported, or null if not imported
     * @since 4.0.0
     */
    @Nullable
    InputLocation getImportedFrom();
}
