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
package org.apache.maven.artifact;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Exception which is meant to occur when a layout specified for a particular
 * repository doesn't have a corresponding {@link org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout}
 * component in the current container.
 *
 * @author jdcasey
 */
public class UnknownRepositoryLayoutException extends InvalidRepositoryException {

    private final String layoutId;

    public UnknownRepositoryLayoutException(String repositoryId, String layoutId) {
        super("Cannot find ArtifactRepositoryLayout instance for: " + layoutId, repositoryId);
        this.layoutId = layoutId;
    }

    public UnknownRepositoryLayoutException(String repositoryId, String layoutId, ComponentLookupException e) {
        super("Cannot find ArtifactRepositoryLayout instance for: " + layoutId, repositoryId, e);
        this.layoutId = layoutId;
    }

    public String getLayoutId() {
        return layoutId;
    }
}
