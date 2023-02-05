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
package org.apache.maven.repository.legacy.resolver.conflict;

import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Resolves conflicting artifacts by always selecting the <em>newest</em> declaration. Newest is defined as the
 * declaration whose version is greater according to <code>ArtifactVersion.compareTo</code>.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @see ArtifactVersion#compareTo
 * @since 3.0
 */
@Component(role = ConflictResolver.class, hint = "newest")
public class NewestConflictResolver implements ConflictResolver {
    // ConflictResolver methods -----------------------------------------------

    /*
     * @see org.apache.maven.artifact.resolver.conflict.ConflictResolver#resolveConflict(org.apache.maven.artifact.resolver.ResolutionNode,
     *      org.apache.maven.artifact.resolver.ResolutionNode)
     */

    public ResolutionNode resolveConflict(ResolutionNode node1, ResolutionNode node2) {
        try {
            ArtifactVersion version1 = node1.getArtifact().getSelectedVersion();
            ArtifactVersion version2 = node2.getArtifact().getSelectedVersion();

            return version1.compareTo(version2) > 0 ? node1 : node2;
        } catch (OverConstrainedVersionException exception) {
            // TODO log message or throw exception?

            return null;
        }
    }
}
