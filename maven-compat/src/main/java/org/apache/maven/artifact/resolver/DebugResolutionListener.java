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
package org.apache.maven.artifact.resolver;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.logging.Logger;

/**
 * Send resolution events to the debug log.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DebugResolutionListener implements ResolutionListener, ResolutionListenerForDepMgmt {
    private Logger logger;

    private String indent = "";

    private static Set<Artifact> ignoredArtifacts = new HashSet<>();

    public DebugResolutionListener(Logger logger) {
        this.logger = logger;
    }

    public void testArtifact(Artifact node) {}

    public void startProcessChildren(Artifact artifact) {
        indent += "  ";
    }

    public void endProcessChildren(Artifact artifact) {
        indent = indent.substring(2);
    }

    public void includeArtifact(Artifact artifact) {
        logger.debug(indent + artifact + " (selected for " + artifact.getScope() + ")");
    }

    public void omitForNearer(Artifact omitted, Artifact kept) {
        String omittedVersion = omitted.getVersion();
        String keptVersion = kept.getVersion();

        if (!Objects.equals(omittedVersion, keptVersion)) {
            logger.debug(indent + omitted + " (removed - nearer found: " + keptVersion + ")");
        }
    }

    public void omitForCycle(Artifact omitted) {
        logger.debug(indent + omitted + " (removed - causes a cycle in the graph)");
    }

    public void updateScopeCurrentPom(Artifact artifact, String ignoredScope) {
        logger.debug(indent + artifact + " (not setting artifactScope to: " + ignoredScope + "; local artifactScope "
                + artifact.getScope() + " wins)");

        // TODO better way than static? this might hide messages in a reactor
        if (!ignoredArtifacts.contains(artifact)) {
            logger.warn("\n\tArtifact " + artifact + " retains local artifactScope '" + artifact.getScope()
                    + "' overriding broader artifactScope '" + ignoredScope + "'\n"
                    + "\tgiven by a dependency. If this is not intended, modify or remove the local artifactScope.\n");
            ignoredArtifacts.add(artifact);
        }
    }

    public void updateScope(Artifact artifact, String scope) {
        logger.debug(indent + artifact + " (setting artifactScope to: " + scope + ")");
    }

    public void selectVersionFromRange(Artifact artifact) {
        logger.debug(indent + artifact + " (setting version to: " + artifact.getVersion() + " from range: "
                + artifact.getVersionRange() + ")");
    }

    public void restrictRange(Artifact artifact, Artifact replacement, VersionRange newRange) {
        logger.debug(indent + artifact + " (range restricted from: " + artifact.getVersionRange() + " and: "
                + replacement.getVersionRange() + " to: " + newRange + " )");
    }

    /**
     * The logic used here used to be a copy of the logic used in the DefaultArtifactCollector, and this method was
     * called right before the actual version/artifactScope changes were done. However, a different set of conditionals
     * (and more information) is needed to be able to determine when and if the version and/or artifactScope changes.
     * See the two added methods, manageArtifactVersion and manageArtifactScope.
     */
    public void manageArtifact(Artifact artifact, Artifact replacement) {
        String msg = indent + artifact;
        msg += " (";
        if (replacement.getVersion() != null) {
            msg += "applying version: " + replacement.getVersion() + ";";
        }
        if (replacement.getScope() != null) {
            msg += "applying artifactScope: " + replacement.getScope();
        }
        msg += ")";
        logger.debug(msg);
    }

    public void manageArtifactVersion(Artifact artifact, Artifact replacement) {
        // only show msg if a change is actually taking place
        if (!replacement.getVersion().equals(artifact.getVersion())) {
            String msg = indent + artifact + " (applying version: " + replacement.getVersion() + ")";
            logger.debug(msg);
        }
    }

    public void manageArtifactScope(Artifact artifact, Artifact replacement) {
        // only show msg if a change is actually taking place
        if (!replacement.getScope().equals(artifact.getScope())) {
            String msg = indent + artifact + " (applying artifactScope: " + replacement.getScope() + ")";
            logger.debug(msg);
        }
    }

    public void manageArtifactSystemPath(Artifact artifact, Artifact replacement) {
        // only show msg if a change is actually taking place
        if (!replacement.getScope().equals(artifact.getScope())) {
            String msg = indent + artifact + " (applying system path: " + replacement.getFile() + ")";
            logger.debug(msg);
        }
    }
}
