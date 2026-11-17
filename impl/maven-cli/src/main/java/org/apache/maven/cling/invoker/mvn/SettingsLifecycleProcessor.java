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
package org.apache.maven.cling.invoker.mvn;

import java.util.List;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.settings.PhaseInjection;
import org.apache.maven.api.spi.LifecycleProcessor;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.PhaseEnrichedLifecycle;
import org.apache.maven.plugin.LegacySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LifecycleProcessor} that injects custom phases declared in the project-scoped
 * {@code .mvn/settings.xml} into the lifecycle DAG.
 *
 * <p>Phases are declared under {@code <phases>} in the project {@code settings.xml}:
 * <pre>{@code
 * <settings>
 *   <phases>
 *     <phase name="pre-integration" parent="verify" after="unit-test" before="integration-test"/>
 *     <phase name="post-all" parent="verify" after="unit-test,integration-test"/>
 *   </phases>
 * </settings>
 * }</pre>
 *
 * <p>Both {@code after} and {@code before} accept comma-separated lists and can be
 * combined to constrain the insertion from both sides.
 *
 * <p>This processor only enriches the {@code default} lifecycle. Injected phases have no
 * plugin bindings by default — mojos must bind to them explicitly in their POM via
 * {@code <phase>pre-integration</phase>}.
 *
 * <p>Phase injections are only respected from project-scoped settings. User and installation
 * settings {@code <phases>} are silently ignored (and emit a warning during settings loading).
 *
 * @since 4.2.0
 */
@Named
public class SettingsLifecycleProcessor implements LifecycleProcessor {

    /**
     * Key under which {@code MavenInvoker.populateRequest()} stores the project-scoped phase injections
     * in the {@link org.apache.maven.execution.MavenExecutionRequest} data map.
     */
    public static final String PHASES_DATA_KEY = SettingsLifecycleProcessor.class.getName() + ".phases";

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsLifecycleProcessor.class);

    private final LegacySupport legacySupport;

    @Inject
    public SettingsLifecycleProcessor(@Nullable LegacySupport legacySupport) {
        this.legacySupport = legacySupport;
    }

    @Override
    public Lifecycle process(Lifecycle lifecycle) {
        // Only enrich the default lifecycle
        if (!Lifecycle.DEFAULT.equals(lifecycle.id())) {
            return lifecycle;
        }

        List<PhaseEnrichedLifecycle.InjectedPhase> injections = resolveInjections();
        if (injections.isEmpty()) {
            return lifecycle;
        }

        LOGGER.debug(
                "Injecting {} custom phase(s) from project settings.xml into lifecycle '{}': {}",
                injections.size(),
                lifecycle.id(),
                injections.stream()
                        .map(PhaseEnrichedLifecycle.InjectedPhase::name)
                        .toList());

        return new PhaseEnrichedLifecycle(lifecycle, injections);
    }

    @SuppressWarnings("unchecked")
    private List<PhaseEnrichedLifecycle.InjectedPhase> resolveInjections() {
        if (legacySupport == null) {
            return List.of();
        }
        MavenSession session = legacySupport.getSession();
        if (session == null) {
            return List.of();
        }

        List<PhaseInjection> phases =
                (List<PhaseInjection>) session.getRequest().getData().get(PHASES_DATA_KEY);
        if (phases == null || phases.isEmpty()) {
            return List.of();
        }

        return phases.stream()
                .map(p -> {
                    validate(p);
                    return new PhaseEnrichedLifecycle.InjectedPhase(
                            p.getName(), p.getParent(), p.getAfter(), p.getBefore());
                })
                .toList();
    }

    /**
     * Validates a {@link PhaseInjection} declaration from project {@code settings.xml}.
     *
     * <p>Rules:
     * <ul>
     *   <li>The {@code name} attribute must be present and non-blank.</li>
     *   <li>When {@code parent} is absent, at least one of {@code after} or {@code before} must be
     *       specified to anchor the phase in the lifecycle DAG.</li>
     *   <li>When {@code parent} is present, {@code after} and {@code before} are optional.</li>
     * </ul>
     *
     * @throws IllegalStateException if any rule is violated
     */
    static void validate(PhaseInjection p) {
        if (p.getName() == null || p.getName().isBlank()) {
            throw new IllegalStateException("settings.xml: <phase> element must have a non-blank 'name' attribute");
        }
        boolean hasParent = p.getParent() != null && !p.getParent().isBlank();
        boolean hasAfter = p.getAfter() != null && !p.getAfter().isBlank();
        boolean hasBefore = p.getBefore() != null && !p.getBefore().isBlank();
        if (!hasParent && !hasAfter && !hasBefore) {
            throw new IllegalStateException("settings.xml: custom phase '"
                    + p.getName()
                    + "' must specify 'parent' and/or at least one of 'after' or 'before'"
                    + " to anchor it in the lifecycle DAG");
        }
    }
}
