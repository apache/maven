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
package org.apache.maven.cli.internal.extension.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Extensions to load.
 *
 * @deprecated Use {@link org.apache.maven.api.cli.extensions.CoreExtension} instead
 */
@Deprecated
@SuppressWarnings("all")
public class CoreExtensions implements Serializable {

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field extensions.
     */
    private List<CoreExtension> extensions;

    /**
     * Field modelEncoding.
     */
    private String modelEncoding = "UTF-8";

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Method addExtension.
     *
     * @param coreExtension a coreExtension object.
     */
    public void addExtension(CoreExtension coreExtension) {
        getExtensions().add(coreExtension);
    } // -- void addExtension( CoreExtension )

    /**
     * Method getExtensions.
     *
     * @return List
     */
    public List<CoreExtension> getExtensions() {
        if (this.extensions == null) {
            this.extensions = new ArrayList<CoreExtension>();
        }

        return this.extensions;
    } // -- List<CoreExtension> getExtensions()

    /**
     * Get the modelEncoding field.
     *
     * @return String
     */
    public String getModelEncoding() {
        return this.modelEncoding;
    } // -- String getModelEncoding()

    /**
     * Method removeExtension.
     *
     * @param coreExtension a coreExtension object.
     */
    public void removeExtension(CoreExtension coreExtension) {
        getExtensions().remove(coreExtension);
    } // -- void removeExtension( CoreExtension )

    /**
     * Set a set of build extensions to use from this project.
     *
     * @param extensions a extensions object.
     */
    public void setExtensions(List<CoreExtension> extensions) {
        this.extensions = extensions;
    } // -- void setExtensions( List )

    /**
     * Set the modelEncoding field.
     *
     * @param modelEncoding a modelEncoding object.
     */
    public void setModelEncoding(String modelEncoding) {
        this.modelEncoding = modelEncoding;
    } // -- void setModelEncoding( String )
}
