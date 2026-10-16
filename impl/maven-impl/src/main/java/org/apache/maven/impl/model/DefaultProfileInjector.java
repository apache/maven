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
package org.apache.maven.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.BuildBase;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.ModelBase;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginContainer;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.ReportSet;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.RepositoryBase;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ProfileInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles profile injection into the model.
 *
 * <p>In addition to standard profile property injection (where properties from active profiles
 * are merged into the model properties using last-write-wins), properties from active profiles
 * are also injected under the {@code profile.<id>.<property>} prefix. This allows expressions
 * in the POM to reference properties defined in a specific active profile without collision
 * (e.g., {@code ${profile.dev.server.port}}).</p>
 *
 * <h3>Conventions and Semantics</h3>
 * <ul>
 *   <li><b>Active Profiles Only:</b> Only active profiles have their properties injected. Inactive
 *       profiles do not contribute scoped properties.</li>
 *   <li><b>Reserved Namespace:</b> Property names of the form {@code profile.<id>.*} are reserved
 *       for profile-scoped resolution. If an explicit model property clashes with an injected scoped key,
 *       the profile value takes precedence and a warning is logged.</li>
 *   <li><b>Prefix Exclusion:</b> Profile properties that already start with {@code profile.} are not
 *       re-prefixed to prevent redundant double-scoping.</li>
 *   <li><b>Profile ID Naming:</b> It is recommended to use hyphens or underscores in profile IDs
 *       rather than dots (e.g., {@code my-profile} instead of {@code my.profile}) to avoid dot-delimiter
 *       ambiguity across profiles with overlapping names.</li>
 * </ul>
 */
@Named
@Singleton
public class DefaultProfileInjector implements ProfileInjector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProfileInjector.class);

    private static final Map<Model, Map<List<Profile>, Model>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    // In order for the weak hash map to work correctly, we must not hold any reference to
    // the model used as the key.  So we use a dummy model as a placeholder to indicate that
    // we want to store the model used as they key.
    private static final Model KEY = Model.newInstance();

    private final ProfileModelMerger merger = new ProfileModelMerger();

    @Override
    public Model injectProfiles(
            Model model, List<Profile> profiles, ModelBuilderRequest request, ModelProblemCollector problems) {
        Model result = CACHE.computeIfAbsent(model, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(profiles, l -> doInjectProfiles(model, profiles));
        return result == KEY ? model : result;
    }

    private Model doInjectProfiles(Model model, List<Profile> profiles) {
        Model orgModel = model;
        for (Profile profile : profiles) {
            if (profile != null) {
                Model.Builder builder = Model.newBuilder(model);
                merger.mergeModelBase(builder, model, profile);

                if (profile.getBuild() != null) {
                    Build build = model.getBuild() != null ? model.getBuild() : Build.newInstance();
                    Build.Builder bbuilder = Build.newBuilder(build);
                    merger.mergeBuildBase(bbuilder, build, profile.getBuild());
                    builder.build(bbuilder.build());
                }

                model = builder.build();
            }
        }
        return model == orgModel ? KEY : model;
    }

    /**
     * ProfileModelMerger
     */
    protected static class ProfileModelMerger extends MavenModelMerger {

        public void mergeModelBase(ModelBase.Builder builder, ModelBase target, ModelBase source) {
            mergeModelBase(builder, target, source, true, Collections.emptyMap());
        }

        public void mergeBuildBase(BuildBase.Builder builder, BuildBase target, BuildBase source) {
            mergeBuildBase(builder, target, source, true, Collections.emptyMap());
        }

        @Override
        protected void mergeModelBase_Properties(
                ModelBase.Builder builder,
                ModelBase target,
                ModelBase source,
                boolean sourceDominant,
                Map<Object, Object> context) {
            Map<String, String> src = source.getProperties();
            Map<String, String> tgt = target.getProperties();
            Map<String, String> merged = new LinkedHashMap<>(tgt.size() + src.size() * 2);
            if (sourceDominant) {
                merged.putAll(tgt);
                merged.putAll(src);
            } else {
                merged.putAll(src);
                merged.putAll(tgt);
            }

            if (src.isEmpty()) {
                builder.properties(merged);
                builder.location(
                        "properties",
                        InputLocation.merge(
                                target.getLocation("properties"), source.getLocation("properties"), sourceDominant));
                return;
            }

            InputLocation propsLoc = source.getLocation("properties");
            Map<Object, InputLocation> scopedLocs = new LinkedHashMap<>();
            if (source instanceof Profile profile) {
                String profileId = profile.getId();
                if (profileId != null && !profileId.isEmpty()) {
                    String prefix = "profile." + profileId + ".";
                    // Note: Profile IDs containing dots (e.g., "foo.bar") may cause key collisions
                    // if another profile (e.g., "foo") defines a property that shares the same prefix
                    // (e.g., "bar.myprop"). In such cases, last-write-wins applies. Profile IDs without
                    // dots or using hyphens are recommended.
                    // Property names of the form "profile.<id>.*" are reserved for profile-scoped resolution.
                    // If an existing model property matches a scoped key, the profile's value takes precedence
                    // and a warning is logged.
                    for (Map.Entry<String, String> entry : src.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        // Skip re-prefixing: properties already in the profile.* namespace
                        // are user-declared scoped references — don't double-prefix them.
                        if (!key.startsWith("profile.")) {
                            String scopedKey = prefix + key;
                            if (tgt.containsKey(scopedKey)) {
                                LOGGER.warn(
                                        "Profile-scoped property '{}' from profile '{}' overrides an existing model property",
                                        scopedKey,
                                        profileId);
                            }
                            merged.put(scopedKey, value);
                            if (propsLoc != null) {
                                InputLocation loc = propsLoc.getLocation(key);
                                if (loc != null) {
                                    scopedLocs.put(scopedKey, loc);
                                }
                            }
                        }
                    }
                }
            }

            builder.properties(merged);
            InputLocation mergedPropsLoc = InputLocation.merge(
                    target.getLocation("properties"), source.getLocation("properties"), sourceDominant);
            if (!scopedLocs.isEmpty()) {
                if (mergedPropsLoc != null) {
                    Map<Object, InputLocation> allLocs = new LinkedHashMap<>(
                            mergedPropsLoc.getLocations() != null
                                    ? mergedPropsLoc.getLocations()
                                    : Collections.emptyMap());
                    allLocs.putAll(scopedLocs);
                    mergedPropsLoc = InputLocation.of(
                            mergedPropsLoc.getLineNumber(),
                            mergedPropsLoc.getColumnNumber(),
                            mergedPropsLoc.getSource(),
                            allLocs);
                } else {
                    mergedPropsLoc = InputLocation.of(-1, -1, null, scopedLocs);
                }
            }
            builder.location("properties", mergedPropsLoc);
        }

        @Override
        protected void mergePluginContainer_Plugins(
                PluginContainer.Builder builder,
                PluginContainer target,
                PluginContainer source,
                boolean sourceDominant,
                Map<Object, Object> context) {
            List<Plugin> src = source.getPlugins();
            if (!src.isEmpty()) {
                List<Plugin> tgt = target.getPlugins();
                Map<Object, Plugin> master = new LinkedHashMap<>(tgt.size() * 2);

                for (Plugin element : tgt) {
                    Object key = getPluginKey().apply(element);
                    master.put(key, element);
                }

                Map<Object, List<Plugin>> predecessors = new LinkedHashMap<>();
                List<Plugin> pending = new ArrayList<>();
                for (Plugin element : src) {
                    Object key = getPluginKey().apply(element);
                    Plugin existing = master.get(key);
                    if (existing != null) {
                        existing = mergePlugin(existing, element, sourceDominant, context);
                        master.put(key, existing);
                        if (!pending.isEmpty()) {
                            predecessors.put(key, pending);
                            pending = new ArrayList<>();
                        }
                    } else {
                        pending.add(element);
                    }
                }

                List<Plugin> result = new ArrayList<>(src.size() + tgt.size());
                for (Map.Entry<Object, Plugin> entry : master.entrySet()) {
                    List<Plugin> pre = predecessors.get(entry.getKey());
                    if (pre != null) {
                        result.addAll(pre);
                    }
                    result.add(entry.getValue());
                }
                result.addAll(pending);

                builder.plugins(result);
            }
        }

        @Override
        protected void mergePlugin_Executions(
                Plugin.Builder builder,
                Plugin target,
                Plugin source,
                boolean sourceDominant,
                Map<Object, Object> context) {
            List<PluginExecution> src = source.getExecutions();
            if (!src.isEmpty()) {
                List<PluginExecution> tgt = target.getExecutions();
                Map<Object, PluginExecution> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

                for (PluginExecution element : tgt) {
                    Object key = getPluginExecutionKey().apply(element);
                    merged.put(key, element);
                }

                for (PluginExecution element : src) {
                    Object key = getPluginExecutionKey().apply(element);
                    PluginExecution existing = merged.get(key);
                    if (existing != null) {
                        element = mergePluginExecution(existing, element, sourceDominant, context);
                    }
                    merged.put(key, element);
                }

                builder.executions(merged.values());
            }
        }

        @Override
        protected void mergeReporting_Plugins(
                Reporting.Builder builder,
                Reporting target,
                Reporting source,
                boolean sourceDominant,
                Map<Object, Object> context) {
            List<ReportPlugin> src = source.getPlugins();
            if (!src.isEmpty()) {
                List<ReportPlugin> tgt = target.getPlugins();
                Map<Object, ReportPlugin> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

                for (ReportPlugin element : tgt) {
                    Object key = getReportPluginKey().apply(element);
                    merged.put(key, element);
                }

                for (ReportPlugin element : src) {
                    Object key = getReportPluginKey().apply(element);
                    ReportPlugin existing = merged.get(key);
                    if (existing != null) {
                        element = mergeReportPlugin(existing, element, sourceDominant, context);
                    }
                    merged.put(key, element);
                }

                builder.plugins(merged.values());
            }
        }

        @Override
        protected void mergeReportPlugin_ReportSets(
                ReportPlugin.Builder builder,
                ReportPlugin target,
                ReportPlugin source,
                boolean sourceDominant,
                Map<Object, Object> context) {
            List<ReportSet> src = source.getReportSets();
            if (!src.isEmpty()) {
                List<ReportSet> tgt = target.getReportSets();
                Map<Object, ReportSet> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

                for (ReportSet element : tgt) {
                    Object key = getReportSetKey().apply(element);
                    merged.put(key, element);
                }

                for (ReportSet element : src) {
                    Object key = getReportSetKey().apply(element);
                    ReportSet existing = merged.get(key);
                    if (existing != null) {
                        element = mergeReportSet(existing, element, sourceDominant, context);
                    }
                    merged.put(key, element);
                }

                builder.reportSets(merged.values());
            }
        }

        @Override
        protected KeyComputer<Repository> getRepositoryKey() {
            return RepositoryBase::getId;
        }
    }
}
