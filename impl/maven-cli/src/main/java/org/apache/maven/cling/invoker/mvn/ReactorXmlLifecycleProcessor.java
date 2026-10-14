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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.reactor.PhaseInjection;
import org.apache.maven.api.reactor.ReactorConfig;
import org.apache.maven.api.spi.LifecycleProcessor;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.PhaseEnrichedLifecycle;
import org.apache.maven.plugin.LegacySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LifecycleProcessor} that injects custom phases declared in {@code .mvn/reactor.xml}
 * into the lifecycle DAG.
 *
 * <p>Phases are declared in the {@code <phases>} section of {@code reactor.xml}:
 * <pre>{@code
 * <reactor>
 *   <phases>
 *     <phase name="pre-integration" parent="verify" after="unit-test" before="integration-test"/>
 *   </phases>
 * </reactor>
 * }</pre>
 *
 * <p>This processor only enriches the {@code default} lifecycle. Injected phases have no
 * plugin bindings by default — mojos must bind to them explicitly in their POM via
 * {@code <phase>pre-integration</phase>}.
 *
 * <p>The {@link ReactorConfig} is resolved from the current {@link MavenSession} on every
 * call, so no state is cached between sessions.
 *
 * @since 4.1.0
 */
@Named
@Singleton
public class ReactorXmlLifecycleProcessor implements LifecycleProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorXmlLifecycleProcessor.class);

    private final LegacySupport legacySupport;

    @Inject
    public ReactorXmlLifecycleProcessor(LegacySupport legacySupport) {
        this.legacySupport = legacySupport;
    }

    @Override
    public Lifecycle process(Lifecycle lifecycle) {
        // Only enrich the default lifecycle for now
        if (!Lifecycle.DEFAULT.equals(lifecycle.id())) {
            return lifecycle;
        }

        List<PhaseEnrichedLifecycle.InjectedPhase> injections = resolveInjections();
        if (injections.isEmpty()) {
            return lifecycle;
        }

        LOGGER.debug(
                "Injecting {} custom phase(s) from reactor.xml into lifecycle '{}': {}",
                injections.size(),
                lifecycle.id(),
                injections.stream()
                        .map(PhaseEnrichedLifecycle.InjectedPhase::name)
                        .toList());

        return new PhaseEnrichedLifecycle(lifecycle, injections);
    }

    /**
     * Reads the {@link ReactorConfig} from the current session's execution request data map
     * and converts its {@link PhaseInjection} entries to {@link PhaseEnrichedLifecycle.InjectedPhase}.
     *
     * <p>Returns an empty list when no session is active, when no {@code reactor.xml} was
     * found, or when the config has no {@code <phases>} declarations.
     */
    private List<PhaseEnrichedLifecycle.InjectedPhase> resolveInjections() {
        MavenSession session = legacySupport.getSession();
        if (session == null) {
            return List.of();
        }

        ReactorConfig config = (ReactorConfig) session.getRequest().getData().get(ReactorConfig.class.getName());
        if (config == null || config.getPhases() == null || config.getPhases().isEmpty()) {
            return List.of();
        }

        return config.getPhases().stream()
                .map(p -> {
                    validate(p);
                    return new PhaseEnrichedLifecycle.InjectedPhase(
                            p.getName(), p.getParent(), p.getAfter(), p.getBefore());
                })
                .toList();
    }

    /**
     * Validates a {@link PhaseInjection} declaration from {@code reactor.xml}.
     *
     * <p>Rules:
     * <ul>
     *   <li>The {@code name} attribute must be present and non-blank.</li>
     *   <li>When {@code parent} is absent, at least one of {@code after} or {@code before} must be
     *       specified to anchor the phase in the lifecycle DAG.</li>
     *   <li>When {@code parent} is present, {@code after} and {@code before} are optional
     *       (the phase is appended to the parent's children when both are absent).</li>
     * </ul>
     *
     * @throws IllegalStateException if any rule is violated
     */
    private static void validate(PhaseInjection p) {
        if (p.getName() == null || p.getName().isBlank()) {
            throw new IllegalStateException("reactor.xml: <phase> element must have a non-blank 'name' attribute");
        }
        boolean hasParent = p.getParent() != null && !p.getParent().isBlank();
        boolean hasAfter = p.getAfter() != null && !p.getAfter().isBlank();
        boolean hasBefore = p.getBefore() != null && !p.getBefore().isBlank();
        if (!hasParent && !hasAfter && !hasBefore) {
            throw new IllegalStateException("reactor.xml: custom phase '"
                    + p.getName()
                    + "' must specify 'parent' and/or at least one of 'after' or 'before'"
                    + " to anchor it in the lifecycle DAG");
        }
    }
}
