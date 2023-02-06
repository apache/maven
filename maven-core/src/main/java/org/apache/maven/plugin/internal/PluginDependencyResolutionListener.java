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
package org.apache.maven.plugin.internal;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Assists in detecting wagon providers brought into the plugin class path via legacy Maven core artifacts (e.g.
 * maven-core:2.0.6) and excluding them. A plugin should be able to explicitly declare dependencies on specific wagons
 * for its use. However, the (old) wagons pulled in transitively via legacy Maven core artifacts are usually not
 * intended as dependencies and more importantly screw up artifact resolution because they would get preferred over the
 * core wagon versions. This is a hack to provide backward-compat with Maven 2 (MNG-4528, MNG-4561).
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
class PluginDependencyResolutionListener implements ResolutionListener {

    private ArtifactFilter coreFilter;

    private LinkedList<Artifact> coreArtifacts = new LinkedList<>();

    private Artifact wagonProvider;

    private Map<Artifact, Object> bannedArtifacts = new IdentityHashMap<>();

    PluginDependencyResolutionListener(ArtifactFilter coreFilter) {
        this.coreFilter = coreFilter;
    }

    public void removeBannedDependencies(Collection<Artifact> artifacts) {
        if (!bannedArtifacts.isEmpty() && artifacts != null) {
            for (Iterator<Artifact> it = artifacts.iterator(); it.hasNext(); ) {
                Artifact artifact = it.next();
                if (bannedArtifacts.containsKey(artifact)) {
                    it.remove();
                }
            }
        }
    }

    public void startProcessChildren(Artifact artifact) {
        if (wagonProvider == null) {
            if (isLegacyCoreArtifact(artifact)) {
                coreArtifacts.addFirst(artifact);
            } else if (!coreArtifacts.isEmpty() && isWagonProvider(artifact)) {
                wagonProvider = artifact;
                bannedArtifacts.put(artifact, null);
            }
        }
    }

    private boolean isLegacyCoreArtifact(Artifact artifact) {
        String version = artifact.getVersion();
        return version != null && version.startsWith("2.") && !coreFilter.include(artifact);
    }

    public void endProcessChildren(Artifact artifact) {
        if (wagonProvider == artifact) {
            wagonProvider = null;
        } else if (coreArtifacts.peek() == artifact) {
            coreArtifacts.removeFirst();
        }
    }

    public void includeArtifact(Artifact artifact) {
        if (wagonProvider != null) {
            bannedArtifacts.put(artifact, null);
        }
    }

    private boolean isWagonProvider(Artifact artifact) {
        if ("org.apache.maven.wagon".equals(artifact.getGroupId())) {
            return artifact.getArtifactId().startsWith("wagon-");
        }
        return false;
    }

    public void manageArtifact(Artifact artifact, Artifact replacement) {}

    public void omitForCycle(Artifact artifact) {}

    public void omitForNearer(Artifact omitted, Artifact kept) {}

    public void restrictRange(Artifact artifact, Artifact replacement, VersionRange newRange) {}

    public void selectVersionFromRange(Artifact artifact) {}

    public void testArtifact(Artifact node) {}

    public void updateScope(Artifact artifact, String scope) {}

    public void updateScopeCurrentPom(Artifact artifact, String ignoredScope) {}
}
