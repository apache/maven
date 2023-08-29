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
package org.apache.maven.model.building;

import org.apache.maven.model.locator.ModelLocator;

/**
 * Enhancement to the {@link ModelSource2} to support locating POM files using the {@link ModelLocator}
 * when pointing to a directory.
 */
public interface ModelSource3 extends ModelSource2 {
    /**
     * Returns model source identified by a path relative to this model source POM. Implementation <strong>MUST</strong>
     * accept <code>relPath</code> parameter values that
     * <ul>
     * <li>use either / or \ file path separator</li>
     * <li>have .. parent directory references</li>
     * <li>point either at file or directory</li>
     * </ul>
     * If the given path points at a directory, the provided {@code ModelLocator} will be used
     * to find the POM file, else if no locator is provided, a file named 'pom.xml' needs to be
     * used by the requested model source.
     *
     * @param locator locator used to locate the pom file
     * @param relPath path of the requested model source relative to this model source POM
     * @return related model source or <code>null</code> if no such model source
     */
    ModelSource3 getRelatedSource(ModelLocator locator, String relPath);

    /**
     * When using a ModelSource3, the method with a {@code ModelLocator} argument should
     * be used instead.
     *
     * @deprecated use {@link #getRelatedSource(ModelLocator, String)} instead
     */
    @Deprecated
    default ModelSource3 getRelatedSource(String relPath) {
        return getRelatedSource(null, relPath);
    }
}
