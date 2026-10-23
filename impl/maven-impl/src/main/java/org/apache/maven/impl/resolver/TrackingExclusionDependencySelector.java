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
            return matchedExclusions;
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
                        trackingRecord.getMatchedExclusions().add(exclusion);
                    }
                }
            }
            return false;
        }
        return true;
    }

    private static boolean matches(Exclusion exclusion, Artifact artifact) {
        if (artifact == null) {
            return false;
        }
        return matches(exclusion.getGroupId(), artifact.getGroupId())
                && matches(exclusion.getArtifactId(), artifact.getArtifactId())
                && matches(exclusion.getExtension(), artifact.getExtension())
                && matches(exclusion.getClassifier(), artifact.getClassifier());
    }

    private static boolean matches(String coordinate, String value) {
        if ("*".equals(coordinate) || Objects.equals(coordinate, value)) {
            return true;
        }
        if (coordinate != null && value != null && (coordinate.contains("*") || coordinate.contains("?"))) {
            return matchesGlob(coordinate, value);
        }
        return false;
    }

    private static boolean matchesGlob(String pattern, String text) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if (c == '?') {
                sb.append(".");
            } else if ("\\.[]{}()+-^$|".indexOf(c) != -1) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        sb.append('$');
        return text.matches(sb.toString());
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        ExclusionDependencySelector childDelegate = (ExclusionDependencySelector) delegate.deriveChildSelector(context);

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
        Map<String, List<ExclusionTrackingRecord>> map =
                (Map<String, List<ExclusionTrackingRecord>>) session.getData().get(EXCLUSION_TRACKER_KEY);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            session.getData().set(EXCLUSION_TRACKER_KEY, map);
        }
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
            map.clear();
            return all;
        }
        List<ExclusionTrackingRecord> records = map.remove(projectKey);
        if (records == null) {
            records = map.remove("");
        }
        return records != null ? records : Collections.emptyList();
    }

    public static void clearTrackingRecords(RepositorySystemSession session) {
        if (session != null && session.getData() != null) {
            session.getData().set(EXCLUSION_TRACKER_KEY, null);
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
