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
package org.apache.maven.repository.internal.reader;

import java.util.*;

import org.apache.maven.model.*;
import org.apache.maven.repository.internal.ArtifactDescriptorReaderSource;
import org.eclipse.aether.artifact.*;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * Support class for {@link ArtifactDescriptorReaderSource} implementations.
 *
 * @since TBD
 */
public abstract class ArtifactDescriptorReaderSourceSupport implements ArtifactDescriptorReaderSource {
    protected Dependency convert(org.apache.maven.model.Dependency dependency, ArtifactTypeRegistry stereotypes) {
        ArtifactType stereotype = stereotypes.get(dependency.getType());
        if (stereotype == null) {
            stereotype = new DefaultArtifactType(dependency.getType());
        }

        boolean system = dependency.getSystemPath() != null
                && !dependency.getSystemPath().isEmpty();

        Map<String, String> props = null;
        if (system) {
            props = Collections.singletonMap(ArtifactProperties.LOCAL_PATH, dependency.getSystemPath());
        }

        Artifact artifact = new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getClassifier(),
                null,
                dependency.getVersion(),
                props,
                stereotype);

        List<Exclusion> exclusions = new ArrayList<>(dependency.getExclusions().size());
        for (org.apache.maven.model.Exclusion exclusion : dependency.getExclusions()) {
            exclusions.add(convert(exclusion));
        }

        return new Dependency(
                artifact,
                dependency.getScope(),
                dependency.getOptional() != null ? dependency.isOptional() : null,
                exclusions);
    }

    protected Exclusion convert(org.apache.maven.model.Exclusion exclusion) {
        return new Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*");
    }

    protected void setArtifactProperties(ArtifactDescriptorResult result, Model model) {
        String downloadUrl = null;
        DistributionManagement distMgmt = model.getDistributionManagement();
        if (distMgmt != null) {
            downloadUrl = distMgmt.getDownloadUrl();
        }
        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            Artifact artifact = result.getArtifact();
            Map<String, String> props = new HashMap<>(artifact.getProperties());
            props.put(ArtifactProperties.DOWNLOAD_URL, downloadUrl);
            result.setArtifact(artifact.setProperties(props));
        }
    }
}
