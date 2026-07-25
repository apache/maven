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

/**
 * Handles profile injection into the model.
 *
 */
@Named
@Singleton
public class DefaultProfileInjector implements ProfileInjector {

    /**
     * Cache to detect no-change profile injections (avoids redundant merge work).
     * Maps model → (profiles → true if profiles produce changes).
     */
    private static final Map<Model, Map<List<Profile>, Boolean>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final ProfileModelMerger merger = new ProfileModelMerger();

    @Override
    public void injectProfiles(
            Model model,
            Model.Builder builder,
            List<Profile> profiles,
            ModelBuilderRequest request,
            ModelProblemCollector problems) {
        // Check cache for known no-op
        Boolean hasChanges =
                CACHE.computeIfAbsent(model, k -> new ConcurrentHashMap<>()).get(profiles);
        if (hasChanges != null && !hasChanges) {
            return;
        }
        boolean changed = doInjectProfiles(model, builder, profiles);
        CACHE.get(model).putIfAbsent(profiles, changed);
    }

    /**
     * Merges profiles into the builder. For multiple profiles, intermediate results are built
     * into temporary models; the last profile writes directly to the passed builder.
     *
     * @return true if any profile actually changed the model
     */
    private boolean doInjectProfiles(Model model, Model.Builder builder, List<Profile> profiles) {
        // Find the last non-null profile index
        int lastIdx = -1;
        for (int i = profiles.size() - 1; i >= 0; i--) {
            if (profiles.get(i) != null) {
                lastIdx = i;
                break;
            }
        }
        if (lastIdx < 0) {
            return false;
        }

        Model current = model;
        boolean changed = false;
        for (int i = 0; i <= lastIdx; i++) {
            Profile profile = profiles.get(i);
            if (profile == null) {
                continue;
            }
            boolean isLast = (i == lastIdx);
            Model.Builder b = isLast ? builder : Model.newBuilder(current);
            merger.mergeModelBase(b, current, profile);

            if (profile.getBuild() != null) {
                Build build = current.getBuild() != null ? current.getBuild() : Build.newInstance();
                Build.Builder bbuilder = Build.newBuilder(build);
                merger.mergeBuildBase(bbuilder, build, profile.getBuild());
                Build newBuild = bbuilder.build();
                if (newBuild != build) {
                    b.build(newBuild);
                }
            }

            if (!isLast) {
                Model next = b.build();
                if (next != current) {
                    changed = true;
                }
                current = next;
            } else {
                // For the last profile, we can't easily detect change without building,
                // but if we got here, at least one profile was non-null and merge was attempted
                changed = true;
            }
        }
        return changed;
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
