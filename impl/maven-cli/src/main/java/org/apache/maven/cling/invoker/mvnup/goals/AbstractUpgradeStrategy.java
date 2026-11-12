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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.maven.Coordinates;
import eu.maveniverse.domtrip.maven.MavenPomElements;
import org.apache.maven.api.Session;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PARENT;

/**
 * Abstract base class for upgrade strategies that provides common functionality
 * and reduces code duplication across strategy implementations.
 *
 * <p>Strategies work with domtrip Documents for perfect formatting preservation.
 * Subclasses can create domtrip Editors from Documents as needed:
 * <pre>
 * Editor editor = new Editor(document);
 * // ... perform domtrip operations ...
 * // Document is automatically updated
 * </pre>
 *
 * <h2>Effective model building</h2>
 *
 * <p>Each {@link #apply} call pre-builds effective models for the whole reactor
 * in a single {@code BUILD_PROJECT} pass via {@link #prebuildReactorModels}.
 * This ensures that:
 * <ul>
 *   <li>Profile activation (file, property, condition) works correctly — not just
 *       OS/JDK/activeByDefault profiles;</li>
 *   <li>Maven 4 coordinate inference ({@code version}/{@code groupId} from reactor
 *       siblings) works because {@code mappedSources} is populated for the whole
 *       reactor before any individual POM is resolved;</li>
 *   <li>Remote parent resolution shares a single model-builder session, so artifacts
 *       fetched for one module are not re-fetched for siblings.</li>
 * </ul>
 *
 * <p>The resulting {@code Map<Path, Model>} cache is stored in
 * {@link #effectiveModelCache} and consulted by {@link #buildEffectiveModel}.
 * For paths not in the cache (external parents reached during a parent-walk),
 * a fallback {@code BUILD_EFFECTIVE} call is made on the same
 * {@link ModelBuilder.ModelBuilderSession}, which still benefits from the
 * already-populated {@code mappedSources}.</p>
 *
 * <p>Both the cache and the session are reset to {@code null} in the {@code finally}
 * block of {@link #apply} so that singleton strategy instances do not leak state
 * across invocations or test runs.</p>
 */
public abstract class AbstractUpgradeStrategy implements UpgradeStrategy {

    /**
     * Logs a change using dry-run wording when the goal is checking rather than saving.
     */
    protected void logChange(UpgradeContext context, String description) {
        if (context.isDryRun()) {
            context.action(description + " would be applied");
        } else {
            context.success(description + " applied");
        }
    }

    /**
     * DI-injected standalone Maven 4 API Session, produced by {@link MvnupSessionHolder}.
     * Sharing the session avoids recreating the heavyweight standalone DI container
     * for each strategy. The Session's {@link org.apache.maven.api.cache.RequestCache}
     * deduplicates effective model builds when the same POM path is resolved more
     * than once within a strategy.
     *
     * <p>When running outside a DI container (e.g. unit tests), this field is {@code null}
     * and {@link #getSession()} falls back to creating a session via
     * {@link MvnupSessionHolder#createSession()}.</p>
     */
    @Inject
    @Named("mvnup")
    private Session session;

    /**
     * Cache of effective models pre-built by {@link #prebuildReactorModels}.
     * Keys are the absolute, normalised POM paths from {@code pomMap}.
     * Reset to {@code null} at the end of each {@link #apply} call.
     */
    private Map<Path, Model> effectiveModelCache;

    /**
     * Shared model-builder session kept alive for the duration of one {@link #apply}
     * call. Created by {@link #prebuildReactorModels} (which uses it for the
     * {@code BUILD_PROJECT} pass) and reused by the {@code BUILD_EFFECTIVE} fallback
     * in {@link #buildEffectiveModel}. Reusing the same session means the
     * {@code mappedSources} map accumulated during the reactor build is available for
     * subsequent individual calls, so cross-module inference keeps working for any
     * POM that was not part of the original reactor (e.g. an external parent).
     * Reset to {@code null} at the end of each {@link #apply} call.
     */
    private ModelBuilder.ModelBuilderSession sharedModelBuilderSession;

    /**
     * Template method that handles common logging and error handling.
     * Subclasses implement the actual upgrade logic in doApply().
     *
     * <p>Before delegating to {@link #doApply}, pre-builds effective models for every
     * POM in {@code pomMap} via a single {@code BUILD_PROJECT} reactor pass so that
     * {@link #buildEffectiveModel} can serve from cache for the common case.</p>
     */
    @Override
    public final UpgradeResult apply(UpgradeContext context, Map<Path, Document> pomMap) {
        context.info(getDescription());
        context.indent();

        try {
            prebuildReactorModels(context, pomMap.keySet());
            UpgradeResult result = doApply(context, pomMap);

            // Log summary
            logSummary(context, result);

            return result;
        } catch (Exception e) {
            context.failure("Strategy execution failed: " + e.getMessage());
            return UpgradeResult.failure(pomMap.keySet(), Set.of());
        } finally {
            effectiveModelCache = null;
            sharedModelBuilderSession = null;
            context.unindent();
        }
    }

    /**
     * Pre-builds effective models for every POM in the reactor with a single
     * {@code BUILD_PROJECT} pass and stores them in {@link #effectiveModelCache}.
     *
     * <p>{@code BUILD_PROJECT} is used because it:
     * <ul>
     *   <li>calls {@code loadFromRoot}, which populates {@code mappedSources} for the
     *       whole reactor before any single effective model is assembled — this is what
     *       makes Maven 4 coordinate inference work across modules;</li>
     *   <li>applies full profile activation (file, property, condition) rather than
     *       the reduced set used by {@code BUILD_CONSUMER};</li>
     *   <li>produces child results (via {@code getChildren()}) when {@code recursive=true}
     *       is set on the request, mapping 1-to-1 to the reactor modules — this lets the
     *       cache be populated in a single pass without additional per-module requests;</li>
     * </ul>
     *
     * <p>The root POM is identified as the shallowest (fewest name parts) path in
     * {@code pomPaths}.</p>
     *
     * <p>If the build fails (e.g. the project has no network access and an external
     * parent cannot be resolved), the cache is left empty and
     * {@link #buildEffectiveModel} falls back to individual {@code BUILD_EFFECTIVE}
     * calls.</p>
     *
     * @param context  the upgrade context (used for debug/warning logging)
     * @param pomPaths the set of POM paths that make up the reactor
     */
    private void prebuildReactorModels(UpgradeContext context, Set<Path> pomPaths) {
        if (pomPaths.isEmpty()) {
            return;
        }
        Session s = getSession();
        ModelBuilder modelBuilder = s.getService(ModelBuilder.class);
        sharedModelBuilderSession = modelBuilder.newSession();

        // Use the shallowest path as the root (fewest name elements).
        Path rootPom = pomPaths.stream()
                .min(java.util.Comparator.comparingInt(Path::getNameCount))
                .orElseThrow();

        context.debug("Pre-building reactor effective models from root: " + rootPom);

        try {
            ModelBuilderRequest request = ModelBuilderRequest.builder()
                    .session(s)
                    .source(Sources.buildSource(rootPom))
                    .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                    .recursive(true)
                    .build();

            ModelBuilderResult result = sharedModelBuilderSession.build(request);

            // Walk the result tree and populate the cache.
            Map<Path, Model> cache = new HashMap<>();
            allResults(result).forEach(r -> {
                Path path = r.getSource().getPath();
                if (path != null) {
                    cache.put(path.toAbsolutePath().normalize(), r.getEffectiveModel());
                }
            });
            effectiveModelCache = cache;
            context.debug("Reactor pre-build complete: cached " + cache.size() + " effective model(s)");

        } catch (Exception e) {
            context.warning("Reactor pre-build failed, falling back to per-POM resolution: " + e.getMessage());
            context.debug("Reactor pre-build exception detail: " + e);
            effectiveModelCache = new HashMap<>();
        }
    }

    /**
     * Recursively flattens a {@link ModelBuilderResult} tree (root + all descendants).
     */
    private static Stream<ModelBuilderResult> allResults(ModelBuilderResult root) {
        return Stream.concat(Stream.of(root), root.getChildren().stream().flatMap(AbstractUpgradeStrategy::allResults));
    }

    /**
     * Subclasses implement the actual upgrade logic here.
     *
     * @param context the upgrade context
     * @param pomMap map of all POM files in the project
     * @return the result of the upgrade operation
     */
    protected abstract UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap);

    /**
     * Gets the upgrade options from the context.
     *
     * @param context the upgrade context
     * @return the upgrade options
     */
    protected final UpgradeOptions getOptions(UpgradeContext context) {
        return context.options();
    }

    /**
     * Logs a summary of the upgrade results.
     *
     * @param context the upgrade context
     * @param result the upgrade result
     */
    protected void logSummary(UpgradeContext context, UpgradeResult result) {
        context.println();
        context.info(getDescription() + " Summary:");
        context.indent();
        context.info(result.modifiedCount() + (context.isDryRun() ? " POM(s) would be modified" : " POM(s) modified"));
        context.info(result.unmodifiedCount() + " POM(s) needed no changes");
        if (result.errorCount() > 0) {
            context.info(result.errorCount() + " POM(s) had errors");
        }
        context.unindent();
    }

    /**
     * Extracts an Artifact from a POM document with parent resolution.
     * If groupId or version are missing, attempts to resolve from parent.
     *
     * <p>This method handles Maven's inheritance mechanism where groupId and version
     * can be inherited from the parent POM.
     *
     * @param context the upgrade context for logging
     * @param pomDocument the POM document
     * @return the Artifact or null if it cannot be determined
     */
    public static Coordinates extractArtifactCoordinatesWithParentResolution(
            UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.root();

        // Extract direct values
        String groupId = root.childTextTrimmed(MavenPomElements.Elements.GROUP_ID);
        String artifactId = root.childTextTrimmed(MavenPomElements.Elements.ARTIFACT_ID);
        String version = root.childTextTrimmed(MavenPomElements.Elements.VERSION);

        // If groupId or version is missing, try to get from parent
        if (groupId == null || version == null) {
            Element parentElement = root.childElement(PARENT).orElse(null);
            if (parentElement != null) {
                if (groupId == null) {
                    groupId = parentElement.childTextTrimmed(MavenPomElements.Elements.GROUP_ID);
                }
                if (version == null) {
                    version = parentElement.childTextTrimmed(MavenPomElements.Elements.VERSION);
                }
            }
        }

        // ArtifactId is required and cannot be inherited
        if (artifactId == null || artifactId.isEmpty()) {
            context.debug("Cannot determine artifactId for POM");
            return null;
        }

        // GroupId and version can be inherited, but if still null, we can't create a valid Artifact
        if (groupId == null || groupId.isEmpty() || version == null || version.isEmpty()) {
            context.debug("Cannot determine complete GAV for artifactId: " + artifactId);
            return null;
        }

        return Coordinates.of(groupId, artifactId, version);
    }

    /**
     * Computes all artifacts from all POMs in a multi-module project.
     * This includes resolving parent inheritance.
     *
     * @param context the upgrade context for logging
     * @param pomMap map of all POM files in the project
     * @return set of all Artifacts in the project
     */
    public static Set<Coordinates> computeAllArtifactCoordinates(UpgradeContext context, Map<Path, Document> pomMap) {
        Map<String, Coordinates> coordinatesByGAV = new HashMap<>();

        context.info("Computing artifacts for inference from " + pomMap.size() + " POM(s)...");

        // Extract artifact from all POMs in the project
        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();

            Coordinates coordinate =
                    AbstractUpgradeStrategy.extractArtifactCoordinatesWithParentResolution(context, pomDocument);
            if (coordinate != null) {
                coordinatesByGAV.putIfAbsent(coordinate.toGAV(), coordinate);
                context.debug("Found artifact: " + coordinate.toGAV() + " from " + pomPath);
            }
        }

        context.info("Computed " + coordinatesByGAV.size() + " unique artifact(s) for inference");
        return new HashSet<>(coordinatesByGAV.values());
    }

    /**
     * Fallback session for unit tests that instantiate strategies directly (without DI).
     */
    private static volatile Session fallbackSession;

    protected Session getSession() {
        Session s = session;
        if (s != null) {
            return s;
        }
        // Fallback for unit tests that instantiate strategies directly (without DI)
        s = fallbackSession;
        if (s == null) {
            synchronized (AbstractUpgradeStrategy.class) {
                s = fallbackSession;
                if (s == null) {
                    s = MvnupSessionHolder.createSession();
                    fallbackSession = s;
                }
            }
        }
        return s;
    }

    /**
     * Returns the reason why remote resolution cannot honor the operator's configured
     * repository posture, or {@code null} if remote resolution may proceed.
     *
     * <p>The standalone resolver session used by mvnup does not apply mirrors, proxies or
     * offline mode from the effective settings. Rather than silently resolving remote POMs
     * while ignoring that configuration, strategies must call this method and skip the
     * remote-model-dependent work whenever a posture is configured that the standalone
     * session cannot honor.</p>
     *
     * <p>Blocked mirrors (such as the default {@code external:http:*} blocker shipped in the
     * Maven installation settings) do not redirect traffic and therefore do not disable
     * remote resolution by themselves.</p>
     *
     * @param context the upgrade context
     * @return a human-readable reason to skip remote resolution, or {@code null} if allowed
     */
    protected static String remoteResolutionUnsupportedReason(UpgradeContext context) {
        Settings settings = context.effectiveSettings;
        if (settings == null) {
            // Settings were never loaded (embedded or test use): there is no operator
            // repository posture declared that could be violated.
            return null;
        }
        if (settings.isOffline()) {
            return "offline mode is enabled in settings";
        }
        boolean hasRedirectingMirror = settings.getMirrors().stream().anyMatch(mirror -> !mirror.isBlocked());
        if (hasRedirectingMirror) {
            return "settings declare mirror(s) that the mvnup standalone resolver cannot honor";
        }
        boolean hasActiveProxy = settings.getProxies().stream().anyMatch(Proxy::isActive);
        if (hasActiveProxy) {
            return "settings declare an active proxy that the mvnup standalone resolver cannot honor";
        }
        return null;
    }

    protected Path findCommonRoot(Set<Path> pomPaths) {
        Path commonRoot = null;
        for (Path pomPath : pomPaths) {
            Path parent = pomPath.getParent();
            if (parent == null) {
                parent = Path.of(".");
            }
            if (commonRoot == null) {
                commonRoot = parent;
            } else {
                while (!parent.startsWith(commonRoot)) {
                    commonRoot = commonRoot.getParent();
                    if (commonRoot == null) {
                        break;
                    }
                }
            }
        }
        return commonRoot;
    }

    /**
     * Returns the effective model for the given POM path.
     *
     * <p>Consults {@link #effectiveModelCache} first (populated by the {@code BUILD_PROJECT}
     * reactor pass in {@link #prebuildReactorModels}). If the path is not in the cache
     * (e.g. an external parent that was not part of the reactor), falls back to a
     * {@code BUILD_EFFECTIVE} call on {@link #sharedModelBuilderSession}, which still
     * benefits from the {@code mappedSources} populated during the reactor build.</p>
     *
     * @param context the upgrade context (used for debug logging)
     * @param pomPath the path to the POM file
     * @return the effective model, never {@code null}
     */
    protected Model buildEffectiveModel(UpgradeContext context, Path pomPath) {
        Path key = pomPath.toAbsolutePath().normalize();

        // Fast path: reactor pre-build already has this model.
        Map<Path, Model> cache = effectiveModelCache;
        if (cache != null) {
            Model cached = cache.get(key);
            if (cached != null) {
                context.debug("Effective model cache hit: " + pomPath);
                return cached;
            }
        }

        // Fallback: individual BUILD_EFFECTIVE on the shared session (inherits mappedSources).
        context.debug("Effective model cache miss, building individually: " + pomPath);
        Session s = getSession();
        ModelBuilder modelBuilder = s.getService(ModelBuilder.class);

        if (sharedModelBuilderSession == null) {
            sharedModelBuilderSession = modelBuilder.newSession();
        }

        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(s)
                .source(Sources.buildSource(pomPath))
                .requestType(ModelBuilderRequest.RequestType.BUILD_EFFECTIVE)
                .build();

        ModelBuilderResult result = sharedModelBuilderSession.build(request);
        Model model = result.getEffectiveModel();

        // Populate cache for potential subsequent calls to the same path.
        if (cache != null) {
            cache.put(key, model);
        }

        return model;
    }
}
