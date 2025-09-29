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

/**
 * Class InputSource.
 *
 * @version $Revision$ $Date$
 */
@SuppressWarnings("all")
public class InputSource implements java.io.Serializable, Cloneable {

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     *
     *
     *             The identifier of the POM in the format {@code
     * <groupId>:<artifactId>:<version>}.
     *
     *
     */
    private String modelId;

    /**
     *
     *
     *             The path/URL of the POM or {@code null} if
     * unknown.
     *
     *
     */
    private String location;

    /**
     *
     *
     *             The location of the POM from which this POM was
     * imported from or {@code null} if unknown.
     */
    private InputLocation importedFrom;

    /**
     * Cached hashCode for performance.
     */
    private volatile int hashCode = 0;

    // ----------------/
    // - Constructors -/
    // ----------------/

    /**
     * Default constructor for InputSource.
     */
    public InputSource() {}

    /**
     * Creates a new InputSource from an API model InputSource.
     * This constructor is used for converting between the API model and the compat model.
     *
     * @param source the API model InputSource to convert from
     */
    public InputSource(org.apache.maven.api.model.InputSource source) {
        this.modelId = source.getModelId();
        this.location = source.getLocation();
        this.importedFrom = source.getImportedFrom() != null ? new InputLocation(source.getImportedFrom()) : null;
    }

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Method clone.
     *
     * @return InputSource
     */
    public InputSource clone() {
        try {
            InputSource copy = (InputSource) super.clone();

            return copy;
        } catch (Exception ex) {
            throw (RuntimeException)
                    new UnsupportedOperationException(getClass().getName() + " does not support clone()").initCause(ex);
        }
    } // -- InputSource clone()

    /**
     * Get the path/URL of the POM or {@code null} if unknown.
     *
     * @return String
     */
    public String getLocation() {
        return this.location;
    } // -- String getLocation()

    /**
     * Get the identifier of the POM in the format {@code
     * <groupId>:<artifactId>:<version>}.
     *
     * @return String
     */
    public String getModelId() {
        return this.modelId;
    } // -- String getModelId()

    /**
     * Set the path/URL of the POM or {@code null} if unknown.
     *
     * @param location
     */
    public void setLocation(String location) {
        this.location = location;
    } // -- void setLocation( String )

    /**
     * Set the identifier of the POM in the format {@code
     * <groupId>:<artifactId>:<version>}.
     *
     * @param modelId
     */
    public void setModelId(String modelId) {
        this.modelId = modelId;
    } // -- void setModelId( String )

    /**
     * Get the location of the POM from which this POM was imported from.
     * Can return {@code null} if this POM was not imported.
     *
     * @return the InputLocation where this POM was imported from, or null if not imported
     */
    public InputLocation getImportedFrom() {
        return importedFrom;
    }

    /**
     * Set the location of the POM from which this POM was imported from.
     *
     * @param importedFrom the InputLocation where this POM was imported from, or null if not imported
     */
    public void setImportedFrom(InputLocation importedFrom) {
        this.importedFrom = importedFrom;
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
        return java.util.Objects.equals(modelId, that.modelId)
                && java.util.Objects.equals(location, that.location)
                && java.util.Objects.equals(importedFrom, that.importedFrom);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = java.util.Objects.hash(modelId, location, importedFrom);
            hashCode = result;
        }
        return result;
    }

    @Override
    public String toString() {
        return getModelId() + " " + getLocation();
    }

    /**
     * Converts this compat model InputSource to an API model InputSource.
     * This method is used for converting between the compat model and the API model.
     *
     * @return the equivalent API model InputSource
     */
    public org.apache.maven.api.model.InputSource toApiSource() {
        return org.apache.maven.api.model.InputSource.of(modelId, location);
    }
}
