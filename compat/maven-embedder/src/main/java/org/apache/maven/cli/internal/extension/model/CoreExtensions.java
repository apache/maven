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

import java.util.ArrayList;
import java.util.List;

/**
 * Extensions to load.
 *
 * @deprecated Use {@link org.apache.maven.api.cli.extensions.CoreExtension} instead
 */
@Deprecated
public class CoreExtensions implements java.io.Serializable {

    private List<CoreExtension> extensions;

    /**
     * Gets a set of build extensions to use from this project.
     *
     * @return the list of extensions
     */
    public List<CoreExtension> getExtensions() {
        if (this.extensions == null) {
            this.extensions = new ArrayList<>();
        }
        return this.extensions;
    }

    /**
     * Sets a set of build extensions to use from this project.
     *
     * @param extensions the list of extensions
     */
    public void setExtensions(List<CoreExtension> extensions) {
        this.extensions = extensions;
    }

    /**
     * Adds an extension to the list.
     *
     * @param extension the extension to add
     */
    public void addExtension(CoreExtension extension) {
        getExtensions().add(extension);
    }

    /**
     * Removes an extension from the list.
     *
     * @param extension the extension to remove
     */
    public void removeExtension(CoreExtension extension) {
        getExtensions().remove(extension);
    }
}
