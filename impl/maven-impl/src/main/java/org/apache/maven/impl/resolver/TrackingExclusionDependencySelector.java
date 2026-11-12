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
package org.apache.maven.impl.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.slf4j.Logger;

/**
 * A dependency selector that wraps {@link ExclusionDependencySelector} to track which declared
 * exclusions on direct dependencies are actually encountered and applied during dependency collection.
 * This allows detecting obsolete or invalid exclusions that have no impact on the dependency tree.
 */
public class TrackingExclusionDependencySelector implements DependencySelector {

    public static final String EXCLUSION_TRACKER_KEY = TrackingExclusionDependencySelector.class.getName();

    public static class ExclusionTrackingRecord {
        private final Dependency directDependency;
        private final Collection<Exclusion> declaredExclusions;
        private final Set<Exclusion> matchedExclusions = ConcurrentHashMap.newKeySet();

        public ExclusionTrackingRecord(Dependency directDependency, Collection<Exclusion> declaredExclusions) {
            this.directDependency = directDependency;
            this.declaredExclusions = declaredExclusions;
        }

        public Dependency getDirectDependency() {
            return directDependency;
        }

        public Collection<Exclusion> getDeclaredExclusions() {
            return declaredExclusions;
        }

        public Set<Exclusion> getMatchedExclusions() {
            return Collections.unmodifiableSet(matchedExclusions);
        }

        void addMatchedExclusion(Exclusion exclusion) {
            matchedExclusions.add(exclusion);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ExclusionTrackingRecord that = (ExclusionTrackingRecord) o;
            return Objects.equals(directDependency, that.directDependency)
                    && Objects.equals(declaredExclusions, that.declaredExclusions)
                    && Objects.equals(matchedExclusions, that.matchedExclusions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(directDependency, declaredExclusions, matchedExclusions);
        }
    }

    private final ExclusionDependencySelector delegate;
    private final int depth;
    private final ExclusionTrackingRecord trackingRecord;

    public TrackingExclusionDependencySelector() {
        this(new ExclusionDependencySelector(), 0, null);
    }

    public TrackingExclusionDependencySelector(Collection<Exclusion> exclusions) {
        this(new ExclusionDependencySelector(exclusions), 0, null);
    }

    private TrackingExclusionDependencySelector(
            ExclusionDependencySelector delegate, int depth, ExclusionTrackingRecord trackingRecord) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.depth = depth;
        this.trackingRecord = trackingRecord;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        if (!delegate.selectDependency(dependency)) {
            if (trackingRecord != null && dependency != null) {
                Artifact artifact = dependency.getArtifact();
                for (Exclusion exclusion : trackingRecord.getDeclaredExclusions()) {
                    if (matches(exclusion, artifact)) {
                        trackingRecord.addMatchedExclusion(exclusion);
                    }
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Mirrors {@link ExclusionDependencySelector} matching logic exactly:
     * {@code "*"} matches any value, otherwise requires exact string equality.
     */
    private static boolean matches(Exclusion exclusion, Artifact artifact) {
        if (artifact == null) {
            return false;
        }
        return matches(exclusion.getGroupId(), artifact.getGroupId())
                && matches(exclusion.getArtifactId(), artifact.getArtifactId());
    }

    private static boolean matches(String pattern, String value) {
        return "*".equals(pattern) || Objects.equals(pattern, value);
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        DependencySelector childDelegateRaw = delegate.deriveChildSelector(context);
        // deriveChildSelector() is declared to return DependencySelector; guard against decoration
        ExclusionDependencySelector childDelegate =
                childDelegateRaw instanceof ExclusionDependencySelector eds ? eds : new ExclusionDependencySelector();

        Dependency dependency = context.getDependency();
        ExclusionTrackingRecord childRecord = this.trackingRecord;

        if (depth == 0) {
            if (dependency != null
                    && dependency.getExclusions() != null
                    && !dependency.getExclusions().isEmpty()) {
                childRecord = new ExclusionTrackingRecord(dependency, dependency.getExclusions());
                registerTrackingRecord(context.getSession(), context.getArtifact(), childRecord);
            } else {
                childRecord = null;
            }
        }

        return new TrackingExclusionDependencySelector(childDelegate, depth + 1, childRecord);
    }

    @SuppressWarnings("unchecked")
    private static void registerTrackingRecord(
            RepositorySystemSession session, Artifact rootArtifact, ExclusionTrackingRecord record) {
        if (session == null || session.getData() == null) {
            return;
        }
        String key = rootArtifact != null ? rootArtifact.getGroupId() + ":" + rootArtifact.getArtifactId() : "";
        Map<String, List<ExclusionTrackingRecord>> map = (Map<String, List<ExclusionTrackingRecord>>)
                session.getData().computeIfAbsent(EXCLUSION_TRACKER_KEY, () -> new ConcurrentHashMap<>());
        map.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(record);
    }

    @SuppressWarnings("unchecked")
    public static List<ExclusionTrackingRecord> getTrackingRecords(RepositorySystemSession session, String projectKey) {
        if (session == null || session.getData() == null) {
            return Collections.emptyList();
        }
        Map<String, List<ExclusionTrackingRecord>> map =
                (Map<String, List<ExclusionTrackingRecord>>) session.getData().get(EXCLUSION_TRACKER_KEY);
        if (map == null) {
            return Collections.emptyList();
        }
        if (projectKey == null) {
            List<ExclusionTrackingRecord> all = new ArrayList<>();
            for (List<ExclusionTrackingRecord> list : map.values()) {
                all.addAll(list);
            }
            return all;
        }
        List<ExclusionTrackingRecord> records = map.get(projectKey);
        if (records == null) {
            records = map.get("");
        }
        return records != null ? records : Collections.emptyList();
    }

    public static void clearTrackingRecords(RepositorySystemSession session) {
        if (session != null && session.getData() != null) {
            session.getData().set(EXCLUSION_TRACKER_KEY, null);
        }
    }

    /**
     * Logs obsolete exclusion warnings for a given project.
     * Shared helper used by both {@code DefaultDependencyResolver} and {@code DefaultProjectDependenciesResolver}.
     *
     * @param session    the repository session
     * @param projectKey the {@code groupId:artifactId} key used to look up tracking records
     * @param projectId  the full project identifier shown in the warning message
     * @param logger     the logger to write warnings to
     */
    public static void warnObsoleteExclusions(
            RepositorySystemSession session, String projectKey, String projectId, Logger logger) {
        if (!logger.isWarnEnabled()) {
            return;
        }
        List<ExclusionTrackingRecord> records = getTrackingRecords(session, projectKey);
        for (ExclusionTrackingRecord record : records) {
            org.eclipse.aether.graph.Dependency directDep = record.getDirectDependency();
            String depCoord = directDep.getArtifact().getGroupId() + ":"
                    + directDep.getArtifact().getArtifactId();
            for (Exclusion exclusion : record.getDeclaredExclusions()) {
                if ("*".equals(exclusion.getGroupId()) && "*".equals(exclusion.getArtifactId())) {
                    continue;
                }
                if (!record.getMatchedExclusions().contains(exclusion)) {
                    logger.warn(
                            "The exclusion of {}:{} for {} in {} is obsolete - there is no dependency on this exclusion",
                            exclusion.getGroupId(),
                            exclusion.getArtifactId(),
                            depCoord,
                            projectId != null ? projectId : "project");
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TrackingExclusionDependencySelector that = (TrackingExclusionDependencySelector) o;
        return Objects.equals(delegate, that.delegate) && Objects.equals(trackingRecord, that.trackingRecord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, trackingRecord);
    }
}
