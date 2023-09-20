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
package org.apache.maven.lifecycle.internal;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

/**
 * Default component responsible for creation of MavenProject#dependencyArtifacts instances.
 */
@SuppressWarnings("deprecation")
@Named
public class DefaultProjectArtifactFactory implements ProjectArtifactFactory {
    private final ArtifactFactory artifactFactory;

    @Inject
    public DefaultProjectArtifactFactory(ArtifactFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
    }

    @Override
    public Set<Artifact> createArtifacts(MavenProject project) throws InvalidDependencyVersionException {
        return createArtifacts(artifactFactory, project.getDependencies(), null, null, project);
    }

    public static Set<Artifact> createArtifacts(
            ArtifactFactory artifactFactory,
            List<Dependency> dependencies,
            String inheritedScope,
            ArtifactFilter dependencyFilter,
            MavenProject project)
            throws InvalidDependencyVersionException {
        Set<Artifact> artifacts = new LinkedHashSet<>();

        for (Dependency d : dependencies) {
            Artifact dependencyArtifact;
            try {
                dependencyArtifact = createDependencyArtifact(artifactFactory, d, inheritedScope, dependencyFilter);
            } catch (InvalidVersionSpecificationException e) {
                throw new InvalidDependencyVersionException(project.getId(), d, project.getFile(), e);
            }

            if (dependencyArtifact != null) {
                artifacts.add(dependencyArtifact);
            }
        }

        return artifacts;
    }

    private static Artifact createDependencyArtifact(
            ArtifactFactory factory, Dependency dependency, String inheritedScope, ArtifactFilter inheritedFilter)
            throws InvalidVersionSpecificationException {
        String effectiveScope = getEffectiveScope(dependency.getScope(), inheritedScope);

        if (effectiveScope == null) {
            return null;
        }

        VersionRange versionRange = VersionRange.createFromVersionSpec(dependency.getVersion());

        Artifact dependencyArtifact = factory.createDependencyArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                versionRange,
                dependency.getType(),
                dependency.getClassifier(),
                effectiveScope,
                dependency.isOptional());

        if (inheritedFilter != null && !inheritedFilter.include(dependencyArtifact)) {
            return null;
        }

        if (Artifact.SCOPE_SYSTEM.equals(effectiveScope)) {
            dependencyArtifact.setFile(new File(dependency.getSystemPath()));
        }

        dependencyArtifact.setDependencyFilter(createDependencyFilter(dependency, inheritedFilter));

        return dependencyArtifact;
    }

    private static String getEffectiveScope(String originalScope, String inheritedScope) {
        String effectiveScope = Artifact.SCOPE_RUNTIME;

        if (originalScope == null) {
            originalScope = Artifact.SCOPE_COMPILE;
        }

        if (inheritedScope == null) {
            // direct dependency retains its scope
            effectiveScope = originalScope;
        } else if (Artifact.SCOPE_TEST.equals(originalScope) || Artifact.SCOPE_PROVIDED.equals(originalScope)) {
            // test and provided are not transitive, so exclude them
            effectiveScope = null;
        } else if (Artifact.SCOPE_SYSTEM.equals(originalScope)) {
            // system scope come through unchanged...
            effectiveScope = Artifact.SCOPE_SYSTEM;
        } else if (Artifact.SCOPE_COMPILE.equals(originalScope) && Artifact.SCOPE_COMPILE.equals(inheritedScope)) {
            // added to retain compile scope. Remove if you want compile inherited as runtime
            effectiveScope = Artifact.SCOPE_COMPILE;
        } else if (Artifact.SCOPE_TEST.equals(inheritedScope)) {
            effectiveScope = Artifact.SCOPE_TEST;
        } else if (Artifact.SCOPE_PROVIDED.equals(inheritedScope)) {
            effectiveScope = Artifact.SCOPE_PROVIDED;
        }

        return effectiveScope;
    }

    private static ArtifactFilter createDependencyFilter(Dependency dependency, ArtifactFilter inheritedFilter) {
        ArtifactFilter effectiveFilter = inheritedFilter;

        if (!dependency.getExclusions().isEmpty()) {
            effectiveFilter = new ExclusionArtifactFilter(dependency.getExclusions());

            if (inheritedFilter != null) {
                effectiveFilter = new AndArtifactFilter(Arrays.asList(inheritedFilter, effectiveFilter));
            }
        }

        return effectiveFilter;
    }
}
