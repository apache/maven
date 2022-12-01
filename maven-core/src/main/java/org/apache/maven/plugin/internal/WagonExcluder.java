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

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

/**
 * Assists in detecting wagon providers brought into the plugin class path via legacy Maven core artifacts (e.g.
 * maven-core:2.0.6) and excluding them. A plugin should be able to explicitly declare dependencies on specific wagons
 * for its use. However, the (old) wagons pulled in transitively via legacy Maven core artifacts are usually not
 * intended as dependencies and more importantly screw up artifact resolution because they would get preferred over the
 * core wagon versions. This is a hack to provide backward-compat with Maven 2 (MNG-4528, MNG-4561).
 *
 * @author Benjamin Bentmann
 */
class WagonExcluder implements DependencySelector {

    private final boolean coreArtifact;

    WagonExcluder() {
        this(false);
    }

    private WagonExcluder(boolean coreArtifact) {
        this.coreArtifact = coreArtifact;
    }

    public boolean selectDependency(Dependency dependency) {
        return !coreArtifact || !isWagonProvider(dependency.getArtifact());
    }

    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        if (coreArtifact || !isLegacyCoreArtifact(context.getDependency().getArtifact())) {
            return this;
        } else {
            return new WagonExcluder(true);
        }
    }

    private boolean isLegacyCoreArtifact(Artifact artifact) {
        String version = artifact.getVersion();
        return version != null
                && version.startsWith("2.")
                && artifact.getArtifactId().startsWith("maven-")
                && artifact.getGroupId().equals("org.apache.maven");
    }

    private boolean isWagonProvider(Artifact artifact) {
        if ("org.apache.maven.wagon".equals(artifact.getGroupId())) {
            return artifact.getArtifactId().startsWith("wagon-");
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }

        WagonExcluder that = (WagonExcluder) obj;
        return coreArtifact == that.coreArtifact;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = hash * 31 + (coreArtifact ? 1 : 0);
        return hash;
    }
}
