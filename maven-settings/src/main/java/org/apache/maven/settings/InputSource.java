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
package org.apache.maven.settings;

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
     *             The path/URL of the POM or {@code null} if
     * unknown.
     *
     *
     */
    private String location;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public InputSource() {}

    public InputSource(org.apache.maven.api.settings.InputSource source) {
        this.location = source.getLocation();
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
     * Set the path/URL of the POM or {@code null} if unknown.
     *
     * @param location
     */
    public void setLocation(String location) {
        this.location = location;
    } // -- void setLocation( String )

    @Override
    public String toString() {
        return getLocation();
    }

    public org.apache.maven.api.settings.InputSource toApiSource() {
        return new org.apache.maven.api.settings.InputSource(location);
    }
}
