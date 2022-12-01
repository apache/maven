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
package org.apache.maven.repository.metadata;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;

/**
 * @author <a href="mailto:oleg@codehaus.org">Oleg Gusakov</a>
 *
 */
@Component(role = GraphConflictResolutionPolicy.class)
public class DefaultGraphConflictResolutionPolicy implements GraphConflictResolutionPolicy {
    /**
     * artifact, closer to the entry point, is selected
     */
    @Configuration(name = "closer-first", value = "true")
    private boolean closerFirst = true;

    /**
     * newer artifact is selected
     */
    @Configuration(name = "newer-first", value = "true")
    private boolean newerFirst = true;

    public MetadataGraphEdge apply(MetadataGraphEdge e1, MetadataGraphEdge e2) {
        int depth1 = e1.getDepth();
        int depth2 = e2.getDepth();

        if (depth1 == depth2) {
            ArtifactVersion v1 = new DefaultArtifactVersion(e1.getVersion());
            ArtifactVersion v2 = new DefaultArtifactVersion(e2.getVersion());

            if (newerFirst) {
                return v1.compareTo(v2) > 0 ? e1 : e2;
            }

            return v1.compareTo(v2) > 0 ? e2 : e1;
        }

        if (closerFirst) {
            return depth1 < depth2 ? e1 : e2;
        }

        return depth1 < depth2 ? e2 : e1;
    }
}
