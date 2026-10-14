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
package org.apache.maven.internal.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.model.Plugin;

/**
 * A {@link Lifecycle} decorator that injects custom phases into the lifecycle phase tree.
 *
 * <p>Maven 4 lifecycles carry two parallel phase trees:
 * <ul>
 *   <li>{@link Lifecycle#phases()} — the native Maven 4 hierarchical tree.</li>
 *   <li>{@link Lifecycle#v3phases()} — a flatter, Maven-3-compatible tree used exclusively by
 *       {@link DefaultLifecycleRegistry#computePhases} to produce the ordered phase list.</li>
 * </ul>
 *
 * <p>Injection strategy:
 * <ul>
 *   <li>In {@code phases()}: the new phase is inserted as a child of the named {@code parent}
 *       container, positioned by {@code after}/{@code before} among that container's children.
 *       The injected phase's {@code AFTER} link points to its immediate left sibling so that
 *       {@code computePhases()} can build a correct DAG edge.</li>
 *   <li>In {@code v3phases()}: the anchor ({@code after} or {@code before}) is searched
 *       recursively; the new phase is inserted as a sibling with an explicit {@code AFTER} link
 *       to that anchor.  This is sufficient for {@code computePhases()} which resolves order
 *       from DAG edges.  Falls back to top-level append if the anchor is not found.</li>
 * </ul>
 *
 * <p>Only {@code AFTER} links are emitted; {@code BEFORE} links are omitted to avoid
 * introducing cycles when {@code addPhases()} also adds implicit sibling-ordering edges.
 *
 * <p>Example {@code reactor.xml} declaration:
 * <pre>{@code
 * <phase name="pre-integration" parent="verify" after="unit-test" before="integration-test"/>
 * }</pre>
 *
 * @since 4.1.0
 */
public class PhaseEnrichedLifecycle implements Lifecycle {

    /**
     * Descriptor of a single phase to inject.
     *
     * @param name   name of the new phase
     * @param parent name of the parent container phase (in the Maven 4 tree) whose children
     *               list receives the injection
     * @param after  sibling anchor: insert after this phase (takes precedence over
     *               {@code before}); also used as the DAG anchor in v3phases (nullable)
     * @param before sibling anchor: insert before this phase (nullable; used when
     *               {@code after} is absent)
     */
    public record InjectedPhase(String name, String parent, String after, String before) {}

    private final Lifecycle delegate;
    private final Collection<Phase> enrichedPhases;
    private final Collection<Phase> enrichedV3Phases;

    public PhaseEnrichedLifecycle(Lifecycle delegate, List<InjectedPhase> injections) {
        this.delegate = delegate;
        this.enrichedPhases = injectAll(new ArrayList<>(delegate.phases()), injections, false);
        this.enrichedV3Phases = injectAll(new ArrayList<>(delegate.v3phases()), injections, true);
    }

    // -------------------------------------------------------------------------
    // Lifecycle delegation
    // -------------------------------------------------------------------------

    @Override
    public String id() {
        return delegate.id();
    }

    @Override
    public Collection<Phase> phases() {
        return enrichedPhases;
    }

    @Override
    public Collection<Phase> v3phases() {
        return enrichedV3Phases;
    }

    @Override
    public Stream<Phase> allPhases() {
        return enrichedPhases.stream().flatMap(Phase::allPhases);
    }

    @Override
    public Collection<Alias> aliases() {
        return delegate.aliases();
    }

    // -------------------------------------------------------------------------
    // Top-level dispatcher
    // -------------------------------------------------------------------------

    /**
     * Applies all injections to the given phase tree.
     *
     * @param phases      mutable top-level phase list to enrich
     * @param injections  phases to inject
     * @param v3Mode      when {@code true} uses v3-style injection (sibling of anchor with
     *                    explicit AFTER link); when {@code false} uses the parent-based injection
     * @return unmodifiable enriched list
     */
    private static Collection<Phase> injectAll(List<Phase> phases, List<InjectedPhase> injections, boolean v3Mode) {
        for (InjectedPhase inj : injections) {
            if (v3Mode) {
                phases = injectV3(phases, inj);
            } else {
                phases = injectMaven4(phases, inj);
            }
        }
        return Collections.unmodifiableList(phases);
    }

    // -------------------------------------------------------------------------
    // Maven 4 tree injection (phases())
    // -------------------------------------------------------------------------

    /**
     * Injects into the Maven 4 tree by finding the named {@code parent} node and inserting
     * the new phase among its direct children.
     *
     * <p>Falls back to appending at the top level (no link) if the parent is not found.
     */
    private static List<Phase> injectMaven4(List<Phase> phases, InjectedPhase inj) {
        if (inj.parent() == null) {
            return insertAmongSiblings(phases, inj, null);
        }
        Optional<List<Phase>> result = tryInjectIntoParent(phases, inj);
        if (result.isPresent()) {
            return result.get();
        }
        // Parent not found — append at top level without a link
        List<Phase> fallback = new ArrayList<>(phases);
        fallback.add(buildPhase(inj.name(), null));
        return fallback;
    }

    /**
     * Recursively searches for {@code inj.parent()} in the tree rooted at {@code phases} and,
     * when found, inserts the new phase among that node's children.
     *
     * @return the updated top-level list wrapped in an {@code Optional}, or empty if the parent
     *         was not found at this level or any descendant level
     */
    private static Optional<List<Phase>> tryInjectIntoParent(List<Phase> phases, InjectedPhase inj) {
        for (int i = 0; i < phases.size(); i++) {
            Phase phase = phases.get(i);
            if (inj.parent().equals(phase.name())) {
                // Found the parent: insert among its children
                String leftSiblingLink = null; // computed by insertAmongSiblings
                List<Phase> newChildren = insertAmongSiblings(new ArrayList<>(phase.phases()), inj, leftSiblingLink);
                List<Phase> result = new ArrayList<>(phases);
                result.set(i, withChildren(phase, newChildren));
                return Optional.of(result);
            }
            // Recurse into descendants
            if (!phase.phases().isEmpty()) {
                Optional<List<Phase>> sub = tryInjectIntoParent(new ArrayList<>(phase.phases()), inj);
                if (sub.isPresent()) {
                    List<Phase> result = new ArrayList<>(phases);
                    result.set(i, withChildren(phase, sub.get()));
                    return Optional.of(result);
                }
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // v3 tree injection (v3phases() / computePhases)
    // -------------------------------------------------------------------------

    /**
     * Injects into the v3 tree by searching for the {@code after}/{@code before} anchor anywhere
     * in the tree and inserting the new phase as its sibling with an explicit {@code AFTER} link.
     *
     * <p>The explicit link is what {@code computePhases()} uses for DAG ordering — the exact
     * position in the v3 tree is secondary.  Falls back to top-level append when the anchor is
     * not found in the v3 tree.
     */
    private static List<Phase> injectV3(List<Phase> phases, InjectedPhase inj) {
        String anchor = inj.after() != null ? inj.after() : inj.before();
        boolean insertAfter = inj.after() != null;

        Optional<List<Phase>> result = tryInjectV3Sibling(phases, anchor, inj.name(), insertAfter);
        if (result.isPresent()) {
            return result.get();
        }
        // Anchor not found — append at top level.
        // Use an AFTER link only for 'after' anchors; 'before' anchors can't safely be used
        // as AFTER links since we'd be after integration-test but claiming to be before it.
        String fallbackLink = insertAfter ? anchor : null;
        List<Phase> fallback = new ArrayList<>(phases);
        fallback.add(buildPhase(inj.name(), fallbackLink));
        return fallback;
    }

    /**
     * Recursively searches for {@code anchor} and inserts the new phase as its sibling.
     *
     * @param phases      current sibling list
     * @param anchor      name of the anchor phase ({@code after} or {@code before} target)
     * @param newName     name of the phase to inject
     * @param insertAfter {@code true} to insert after the anchor, {@code false} to insert before
     * @return updated list wrapped in {@code Optional}, or empty if not found
     */
    private static Optional<List<Phase>> tryInjectV3Sibling(
            List<Phase> phases, String anchor, String newName, boolean insertAfter) {
        if (anchor == null) {
            return Optional.empty();
        }
        // Search in direct siblings
        for (int i = 0; i < phases.size(); i++) {
            if (anchor.equals(phases.get(i).name())) {
                int insertAt = insertAfter ? i + 1 : i;
                // For AFTER anchor: link points to the anchor (= left sibling).
                // For BEFORE anchor: link points to the left sibling of insertAt (may be null
                // when inserting at position 0), NOT the anchor — pointing to the anchor would
                // create a cycle since addPhases() already adds an edge from the anchor to us.
                String afterLink = insertAt > 0 ? phases.get(insertAt - 1).name() : null;
                Phase newPhase = buildPhase(newName, afterLink);
                List<Phase> result = new ArrayList<>(phases);
                result.add(insertAt, newPhase);
                return Optional.of(result);
            }
        }
        // Recurse into sub-phases
        for (int i = 0; i < phases.size(); i++) {
            Phase phase = phases.get(i);
            if (!phase.phases().isEmpty()) {
                Optional<List<Phase>> sub =
                        tryInjectV3Sibling(new ArrayList<>(phase.phases()), anchor, newName, insertAfter);
                if (sub.isPresent()) {
                    List<Phase> result = new ArrayList<>(phases);
                    result.set(i, withChildren(phase, sub.get()));
                    return Optional.of(result);
                }
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Shared helper: sibling insertion for Maven 4 tree
    // -------------------------------------------------------------------------

    /**
     * Inserts the new phase into {@code siblings} at the position given by
     * {@code inj.after()}/{@code inj.before()}, defaulting to append.
     *
     * <p>The injected phase's {@code AFTER} link points to its immediate left sibling so that,
     * together with the implicit sibling-ordering edges added by {@code addPhases()}, the DAG
     * correctly orders the new phase relative to its neighbours.
     *
     * @param siblings         the mutable sibling list to insert into
     * @param inj              the injection descriptor
     * @param ignoredLinkHint  reserved for future use; pass {@code null}
     */
    private static List<Phase> insertAmongSiblings(List<Phase> siblings, InjectedPhase inj, String ignoredLinkHint) {
        int insertAt = siblings.size(); // default: append

        if (inj.after() != null) {
            for (int i = 0; i < siblings.size(); i++) {
                if (inj.after().equals(siblings.get(i).name())) {
                    insertAt = i + 1;
                    break;
                }
            }
        } else if (inj.before() != null) {
            for (int i = 0; i < siblings.size(); i++) {
                if (inj.before().equals(siblings.get(i).name())) {
                    insertAt = i;
                    break;
                }
            }
        }

        // AFTER link = left sibling in the final list
        String afterAnchor = insertAt > 0 ? siblings.get(insertAt - 1).name() : null;
        Phase newPhase = buildPhase(inj.name(), afterAnchor);
        List<Phase> result = new ArrayList<>(siblings);
        result.add(insertAt, newPhase);
        return result;
    }

    // -------------------------------------------------------------------------
    // Phase builder and utilities
    // -------------------------------------------------------------------------

    /**
     * Returns a copy of {@code parent} with its sub-phases replaced by {@code newChildren}.
     */
    private static Phase withChildren(Phase parent, List<Phase> newChildren) {
        return new Phase() {
            @Override
            public String name() {
                return parent.name();
            }

            @Override
            public List<Plugin> plugins() {
                return parent.plugins();
            }

            @Override
            public Collection<Link> links() {
                return parent.links();
            }

            @Override
            public List<Phase> phases() {
                return newChildren;
            }

            @Override
            public Stream<Phase> allPhases() {
                return Stream.concat(Stream.of(this), newChildren.stream().flatMap(Phase::allPhases));
            }
        };
    }

    /**
     * Builds a leaf phase with no plugins and no sub-phases.
     * Carries a single {@link Lifecycle.Link.Kind#AFTER} link when {@code afterAnchor} is non-null.
     */
    private static Phase buildPhase(String name, String afterAnchor) {
        Collection<Link> links = afterAnchor != null ? List.of(afterLink(afterAnchor)) : Collections.emptyList();
        return new InjectedPhaseImpl(name, links);
    }

    private static Link afterLink(String phaseName) {
        return new Link() {
            @Override
            public Kind kind() {
                return Kind.AFTER;
            }

            @Override
            public Lifecycle.Pointer pointer() {
                return new Lifecycle.PhasePointer() {
                    @Override
                    public String phase() {
                        return phaseName;
                    }

                    @Override
                    public String toString() {
                        return "phase(" + phaseName + ")";
                    }
                };
            }

            @Override
            public String toString() {
                return "after(phase(" + phaseName + "))";
            }
        };
    }

    // -------------------------------------------------------------------------
    // Phase implementation
    // -------------------------------------------------------------------------

    private static class InjectedPhaseImpl implements Phase {
        private final String name;
        private final Collection<Link> links;

        InjectedPhaseImpl(String name, Collection<Link> links) {
            this.name = name;
            this.links = links;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<Plugin> plugins() {
            return List.of();
        }

        @Override
        public Collection<Link> links() {
            return links;
        }

        @Override
        public List<Phase> phases() {
            return List.of();
        }

        @Override
        public Stream<Phase> allPhases() {
            return Stream.of(this);
        }

        @Override
        public String toString() {
            return "InjectedPhase(" + name + ", links=" + links + ")";
        }
    }
}
