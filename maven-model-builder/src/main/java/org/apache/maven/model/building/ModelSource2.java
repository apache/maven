package org.apache.maven.model.building;

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

import java.net.URI;

/**
 * Provides access to the contents of a POM independently of the backing store (e.g. file system, database, memory).
 * <p>
 * Unlike {@link ModelSource}, this interface supports loading of parent POM(s) from the same backing store and allows
 * construction of MavenProject instances without the need to have parent POM(s) available from local or remote
 * repositories.
 * <p>
 * ModelSource2 instances are cached in {@link ModelBuildingRequest#getModelCache()}. Implementations must guarantee
 * that the connection to the backing store remains active until request's {@link ModelCache} is discarded or flushed.
 */
public interface ModelSource2
    extends ModelSource
{
    /**
     * Returns model source identified by a path relative to this model source POM. Implementation <strong>MUST</strong>
     * be able to accept <code>relPath</code> parameter values that
     * <ul>
     * <li>use either / or \ file path separator</li>
     * <li>have .. parent directory references</li>
     * <li>point either at file or directory, in the latter case POM file name 'pom.xml' needs to be used by the
     * requested model source.</li>
     * </ul>
     *
     * @param relPath is the path of the requested model source relative to this model source POM.
     * @return related model source or <code>null</code> if no such model source.
     */
    ModelSource2 getRelatedSource( String relPath );

    /**
     * Returns location of the POM, never <code>null</code>.
     */
    URI getLocationURI();
}
