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
package org.apache.maven.model.merge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.model.BuildBase;
import org.apache.maven.api.model.CiManagement;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DeploymentRepository;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.Extension;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.IssueManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.ModelBase;
import org.apache.maven.api.model.Organization;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.ReportSet;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.RepositoryBase;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.model.Site;
import org.apache.maven.model.v4.MavenMerger;
import org.codehaus.plexus.util.StringUtils;

/**
 * The domain-specific model merger for the Maven POM, overriding generic code from parent class when necessary with
 * more adapted algorithms.
 *
 * @author Benjamin Bentmann
 */
public class MavenModelMerger extends MavenMerger {

    /**
     * The hint key for the child path adjustment used during inheritance for URL calculations.
     */
    public static final String CHILD_PATH_ADJUSTMENT = "child-path-adjustment";

    /**
     * The context key for the artifact id of the target model.
     */
    public static final String ARTIFACT_ID = "artifact-id";

    public MavenModelMerger() {
        super(false);
    }

    @Override
    protected Model mergeModel(Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        context.put(ARTIFACT_ID, target.getArtifactId());

        return super.mergeModel(target, source, sourceDominant, context);
    }

    @Override
    protected void mergeModel_Name(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getName();
        if (src != null) {
            if (sourceDominant) {
                builder.name(src);
                builder.location("name", source.getLocation("name"));
            }
        }
    }

    @Override
    protected void mergeModel_Url(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getUrl();
        if (src != null) {
            if (sourceDominant) {
                builder.url(src);
                builder.location("url", source.getLocation("url"));
            } else if (target.getUrl() == null) {
                builder.url(extrapolateChildUrl(src, source.isChildProjectUrlInheritAppendPath(), context));
                builder.location("url", source.getLocation("url"));
            }
        }
    }

    /*
     * TODO: Whether the merge continues recursively into an existing node or not could be an option for the generated
     * merger
     */
    @Override
    protected void mergeModel_Organization(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        Organization src = source.getOrganization();
        if (src != null) {
            Organization tgt = target.getOrganization();
            if (tgt == null) {
                builder.organization(src);
                builder.location("organisation", source.getLocation("organisation"));
            }
        }
    }

    @Override
    protected void mergeModel_IssueManagement(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        IssueManagement src = source.getIssueManagement();
        if (src != null) {
            IssueManagement tgt = target.getIssueManagement();
            if (tgt == null) {
                builder.issueManagement(src);
                builder.location("issueManagement", source.getLocation("issueManagement"));
            }
        }
    }

    @Override
    protected void mergeModel_CiManagement(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        CiManagement src = source.getCiManagement();
        if (src != null) {
            CiManagement tgt = target.getCiManagement();
            if (tgt == null) {
                builder.ciManagement(src);
                builder.location("ciManagement", source.getLocation("ciManagement"));
            }
        }
    }

    @Override
    protected void mergeModel_ModelVersion(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        // neither inherited nor injected
    }

    @Override
    protected void mergeModel_ArtifactId(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        // neither inherited nor injected
    }

    @Override
    protected void mergeModel_Profiles(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        // neither inherited nor injected
    }

    @Override
    protected void mergeModel_Prerequisites(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        // neither inherited nor injected
    }

    @Override
    protected void mergeModel_Licenses(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        builder.licenses(target.getLicenses().isEmpty() ? source.getLicenses() : target.getLicenses());
    }

    @Override
    protected void mergeModel_Developers(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        builder.developers(target.getDevelopers().isEmpty() ? source.getDevelopers() : target.getDevelopers());
    }

    @Override
    protected void mergeModel_Contributors(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        builder.contributors(target.getContributors().isEmpty() ? source.getContributors() : target.getContributors());
    }

    @Override
    protected void mergeModel_MailingLists(
            Model.Builder builder, Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        if (target.getMailingLists().isEmpty()) {
            builder.mailingLists(source.getMailingLists());
        }
    }

    @Override
    protected void mergeModelBase_Modules(
            ModelBase.Builder builder,
            ModelBase target,
            ModelBase source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        List<String> src = source.getModules();
        if (!src.isEmpty() && sourceDominant) {
            List<Integer> indices = new ArrayList<>();
            List<String> tgt = target.getModules();
            Set<String> excludes = new LinkedHashSet<>(tgt);
            List<String> merged = new ArrayList<>(tgt.size() + src.size());
            merged.addAll(tgt);
            for (int i = 0, n = tgt.size(); i < n; i++) {
                indices.add(i);
            }
            for (int i = 0, n = src.size(); i < n; i++) {
                String s = src.get(i);
                if (!excludes.contains(s)) {
                    merged.add(s);
                    indices.add(~i);
                }
            }
            builder.modules(merged);
            builder.location(
                    "modules",
                    InputLocation.merge(target.getLocation("modules"), source.getLocation("modules"), indices));
        }
    }

    /*
     * TODO: The order of the merged list could be controlled by an attribute in the model association: target-first,
     * source-first, dominant-first, recessive-first
     */
    @Override
    protected void mergeModelBase_Repositories(
            ModelBase.Builder builder,
            ModelBase target,
            ModelBase source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        List<Repository> src = source.getRepositories();
        if (!src.isEmpty()) {
            List<Repository> tgt = target.getRepositories();
            Map<Object, Repository> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

            List<Repository> dominant, recessive;
            if (sourceDominant) {
                dominant = src;
                recessive = tgt;
            } else {
                dominant = tgt;
                recessive = src;
            }

            for (Repository element : dominant) {
                Object key = getRepositoryKey().apply(element);
                merged.put(key, element);
            }

            for (Repository element : recessive) {
                Object key = getRepositoryKey().apply(element);
                if (!merged.containsKey(key)) {
                    merged.put(key, element);
                }
            }

            builder.repositories(merged.values());
        }
    }

    @Override
    protected void mergeModelBase_PluginRepositories(
            ModelBase.Builder builder,
            ModelBase target,
            ModelBase source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        List<Repository> src = source.getPluginRepositories();
        if (!src.isEmpty()) {
            List<Repository> tgt = target.getPluginRepositories();
            Map<Object, Repository> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

            List<Repository> dominant, recessive;
            if (sourceDominant) {
                dominant = src;
                recessive = tgt;
            } else {
                dominant = tgt;
                recessive = src;
            }

            for (Repository element : dominant) {
                Object key = getRepositoryKey().apply(element);
                merged.put(key, element);
            }

            for (Repository element : recessive) {
                Object key = getRepositoryKey().apply(element);
                if (!merged.containsKey(key)) {
                    merged.put(key, element);
                }
            }

            builder.pluginRepositories(merged.values());
        }
    }

    /*
     * TODO: Whether duplicates should be removed looks like an option for the generated merger.
     */
    @Override
    protected void mergeBuildBase_Filters(
            BuildBase.Builder builder,
            BuildBase target,
            BuildBase source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        List<String> src = source.getFilters();
        if (!src.isEmpty()) {
            List<String> tgt = target.getFilters();
            Set<String> excludes = new LinkedHashSet<>(tgt);
            List<String> merged = new ArrayList<>(tgt.size() + src.size());
            merged.addAll(tgt);
            for (String s : src) {
                if (!excludes.contains(s)) {
                    merged.add(s);
                }
            }
            builder.filters(merged);
        }
    }

    @Override
    protected void mergeBuildBase_Resources(
            BuildBase.Builder builder,
            BuildBase target,
            BuildBase source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        if (sourceDominant || target.getResources().isEmpty()) {
            super.mergeBuildBase_Resources(builder, target, source, sourceDominant, context);
        }
    }

    @Override
    protected void mergeBuildBase_TestResources(
            BuildBase.Builder builder,
            BuildBase target,
            BuildBase source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        if (sourceDominant || target.getTestResources().isEmpty()) {
            super.mergeBuildBase_TestResources(builder, target, source, sourceDominant, context);
        }
    }

    @Override
    protected void mergeDistributionManagement_Relocation(
            DistributionManagement.Builder builder,
            DistributionManagement target,
            DistributionManagement source,
            boolean sourceDominant,
            Map<Object, Object> context) {}

    @Override
    protected void mergeDistributionManagement_Repository(
            DistributionManagement.Builder builder,
            DistributionManagement target,
            DistributionManagement source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        DeploymentRepository src = source.getRepository();
        if (src != null) {
            DeploymentRepository tgt = target.getRepository();
            if (sourceDominant || tgt == null) {
                tgt = DeploymentRepository.newInstance(false);
                builder.repository(mergeDeploymentRepository(tgt, src, sourceDominant, context));
            }
        }
    }

    @Override
    protected void mergeDistributionManagement_SnapshotRepository(
            DistributionManagement.Builder builder,
            DistributionManagement target,
            DistributionManagement source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        DeploymentRepository src = source.getSnapshotRepository();
        if (src != null) {
            DeploymentRepository tgt = target.getSnapshotRepository();
            if (sourceDominant || tgt == null) {
                tgt = DeploymentRepository.newInstance(false);
                builder.snapshotRepository(mergeDeploymentRepository(tgt, src, sourceDominant, context));
            }
        }
    }

    @Override
    protected void mergeDistributionManagement_Site(
            DistributionManagement.Builder builder,
            DistributionManagement target,
            DistributionManagement source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        Site src = source.getSite();
        if (src != null) {
            Site tgt = target.getSite();
            if (tgt == null) {
                tgt = Site.newBuilder(false).build();
            }
            Site.Builder sbuilder = Site.newBuilder(tgt);
            if (sourceDominant || tgt == null || isSiteEmpty(tgt)) {
                mergeSite(sbuilder, tgt, src, sourceDominant, context);
            }
            super.mergeSite_ChildSiteUrlInheritAppendPath(sbuilder, tgt, src, sourceDominant, context);
            builder.site(sbuilder.build());
        }
    }

    @Override
    protected void mergeSite_ChildSiteUrlInheritAppendPath(
            Site.Builder builder, Site target, Site source, boolean sourceDominant, Map<Object, Object> context) {}

    protected boolean isSiteEmpty(Site site) {
        return StringUtils.isEmpty(site.getId())
                && StringUtils.isEmpty(site.getName())
                && StringUtils.isEmpty(site.getUrl());
    }

    @Override
    protected void mergeSite_Url(
            Site.Builder builder, Site target, Site source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getUrl();
        if (src != null) {
            if (sourceDominant) {
                builder.url(src);
                builder.location("url", source.getLocation("url"));
            } else if (target.getUrl() == null) {
                builder.url(extrapolateChildUrl(src, source.isChildSiteUrlInheritAppendPath(), context));
                builder.location("url", source.getLocation("url"));
            }
        }
    }

    @Override
    protected void mergeScm_Url(
            Scm.Builder builder, Scm target, Scm source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getUrl();
        if (src != null) {
            if (sourceDominant) {
                builder.url(src);
                builder.location("url", source.getLocation("url"));
            } else if (target.getUrl() == null) {
                builder.url(extrapolateChildUrl(src, source.isChildScmUrlInheritAppendPath(), context));
                builder.location("url", source.getLocation("url"));
            }
        }
    }

    @Override
    protected void mergeScm_Connection(
            Scm.Builder builder, Scm target, Scm source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getConnection();
        if (src != null) {
            if (sourceDominant) {
                builder.connection(src);
                builder.location("connection", source.getLocation("connection"));
            } else if (target.getConnection() == null) {
                builder.connection(extrapolateChildUrl(src, source.isChildScmConnectionInheritAppendPath(), context));
                builder.location("connection", source.getLocation("connection"));
            }
        }
    }

    @Override
    protected void mergeScm_DeveloperConnection(
            Scm.Builder builder, Scm target, Scm source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getDeveloperConnection();
        if (src != null) {
            if (sourceDominant) {
                builder.developerConnection(src);
                builder.location("developerConnection", source.getLocation("developerConnection"));
            } else if (target.getDeveloperConnection() == null) {
                String e = extrapolateChildUrl(src, source.isChildScmDeveloperConnectionInheritAppendPath(), context);
                builder.developerConnection(e);
                builder.location("developerConnection", source.getLocation("developerConnection"));
            }
        }
    }

    @Override
    protected void mergePlugin_Executions(
            Plugin.Builder builder, Plugin target, Plugin source, boolean sourceDominant, Map<Object, Object> context) {
        List<PluginExecution> src = source.getExecutions();
        if (!src.isEmpty()) {
            List<PluginExecution> tgt = target.getExecutions();
            Map<Object, PluginExecution> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

            for (PluginExecution element : src) {
                if (sourceDominant || (element.getInherited() != null ? element.isInherited() : source.isInherited())) {
                    Object key = getPluginExecutionKey().apply(element);
                    merged.put(key, element);
                }
            }

            for (PluginExecution element : tgt) {
                Object key = getPluginExecutionKey().apply(element);
                PluginExecution existing = merged.get(key);
                if (existing != null) {
                    element = mergePluginExecution(element, existing, sourceDominant, context);
                }
                merged.put(key, element);
            }

            builder.executions(merged.values());
        }
    }

    @Override
    protected void mergePluginExecution_Goals(
            PluginExecution.Builder builder,
            PluginExecution target,
            PluginExecution source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        List<String> src = source.getGoals();
        if (!src.isEmpty()) {
            List<String> tgt = target.getGoals();
            Set<String> excludes = new LinkedHashSet<>(tgt);
            List<String> merged = new ArrayList<>(tgt.size() + src.size());
            merged.addAll(tgt);
            for (String s : src) {
                if (!excludes.contains(s)) {
                    merged.add(s);
                }
            }
            builder.goals(merged);
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

            for (ReportSet rset : src) {
                if (sourceDominant || (rset.getInherited() != null ? rset.isInherited() : source.isInherited())) {
                    Object key = getReportSetKey().apply(rset);
                    merged.put(key, rset);
                }
            }

            for (ReportSet element : tgt) {
                Object key = getReportSetKey().apply(element);
                ReportSet existing = merged.get(key);
                if (existing != null) {
                    mergeReportSet(element, existing, sourceDominant, context);
                }
                merged.put(key, element);
            }

            builder.reportSets(merged.values());
        }
    }

    @Override
    protected KeyComputer<Dependency> getDependencyKey() {
        return Dependency::getManagementKey;
    }

    @Override
    protected KeyComputer<Plugin> getPluginKey() {
        return Plugin::getKey;
    }

    @Override
    protected KeyComputer<PluginExecution> getPluginExecutionKey() {
        return PluginExecution::getId;
    }

    @Override
    protected KeyComputer<ReportPlugin> getReportPluginKey() {
        return ReportPlugin::getKey;
    }

    @Override
    protected KeyComputer<ReportSet> getReportSetKey() {
        return ReportSet::getId;
    }

    @Override
    protected KeyComputer<RepositoryBase> getRepositoryBaseKey() {
        return RepositoryBase::getId;
    }

    @Override
    protected KeyComputer<Extension> getExtensionKey() {
        return e -> e.getGroupId() + ':' + e.getArtifactId();
    }

    @Override
    protected KeyComputer<Exclusion> getExclusionKey() {
        return e -> e.getGroupId() + ':' + e.getArtifactId();
    }

    protected String extrapolateChildUrl(String parentUrl, boolean appendPath, Map<Object, Object> context) {
        return parentUrl;
    }
}
