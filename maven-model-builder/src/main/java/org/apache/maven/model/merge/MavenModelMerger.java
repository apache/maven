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

import org.apache.maven.model.BuildBase;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Extension;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.codehaus.plexus.util.StringUtils;

/**
 * The domain-specific model merger for the Maven POM, overriding generic code from parent class when necessary with
 * more adapted algorithms.
 *
 * @author Benjamin Bentmann
 */
public class MavenModelMerger extends ModelMerger {

    /**
     * The hint key for the child path adjustment used during inheritance for URL calculations.
     */
    public static final String CHILD_PATH_ADJUSTMENT = "child-path-adjustment";

    /**
     * The context key for the artifact id of the target model.
     */
    public static final String ARTIFACT_ID = "artifact-id";

    @Override
    protected void mergeModel(Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        context.put(ARTIFACT_ID, target.getArtifactId());

        super.mergeModel(target, source, sourceDominant, context);
    }

    @Override
    protected void mergeModel_Name(Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getName();
        if (src != null) {
            if (sourceDominant) {
                target.setName(src);
                target.setLocation("name", source.getLocation("name"));
            }
        }
    }

    @Override
    protected void mergeModel_Url(Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getUrl();
        if (src != null) {
            if (sourceDominant) {
                target.setUrl(src);
                target.setLocation("url", source.getLocation("url"));
            } else if (target.getUrl() == null) {
                target.setUrl(extrapolateChildUrl(src, source.isChildProjectUrlInheritAppendPath(), context));
                target.setLocation("url", source.getLocation("url"));
            }
        }
    }

    /*
     * TODO: Whether the merge continues recursively into an existing node or not could be an option for the generated
     * merger
     */
    @Override
    protected void mergeModel_Organization(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        Organization src = source.getOrganization();
        if (src != null) {
            Organization tgt = target.getOrganization();
            if (tgt == null) {
                tgt = new Organization();
                tgt.setLocation("", src.getLocation(""));
                target.setOrganization(tgt);
                mergeOrganization(tgt, src, sourceDominant, context);
            }
        }
    }

    @Override
    protected void mergeModel_IssueManagement(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        IssueManagement src = source.getIssueManagement();
        if (src != null) {
            IssueManagement tgt = target.getIssueManagement();
            if (tgt == null) {
                tgt = new IssueManagement();
                tgt.setLocation("", src.getLocation(""));
                target.setIssueManagement(tgt);
                mergeIssueManagement(tgt, src, sourceDominant, context);
            }
        }
    }

    @Override
    protected void mergeModel_CiManagement(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        CiManagement src = source.getCiManagement();
        if (src != null) {
            CiManagement tgt = target.getCiManagement();
            if (tgt == null) {
                tgt = new CiManagement();
                tgt.setLocation("", src.getLocation(""));
                target.setCiManagement(tgt);
                mergeCiManagement(tgt, src, sourceDominant, context);
            }
        }
    }

    @Override
    protected void mergeModel_ModelVersion(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        // neither inherited nor injected
    }

    @Override
    protected void mergeModel_ArtifactId(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        // neither inherited nor injected
    }

    @Override
    protected void mergeModel_Profiles(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        // neither inherited nor injected
    }

    @Override
    protected void mergeModel_Prerequisites(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        // neither inherited nor injected
    }

    @Override
    protected void mergeModel_Licenses(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        if (target.getLicenses().isEmpty()) {
            target.setLicenses(new ArrayList<>(source.getLicenses()));
        }
    }

    @Override
    protected void mergeModel_Developers(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        if (target.getDevelopers().isEmpty()) {
            target.setDevelopers(new ArrayList<>(source.getDevelopers()));
        }
    }

    @Override
    protected void mergeModel_Contributors(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        if (target.getContributors().isEmpty()) {
            target.setContributors(new ArrayList<>(source.getContributors()));
        }
    }

    @Override
    protected void mergeModel_MailingLists(
            Model target, Model source, boolean sourceDominant, Map<Object, Object> context) {
        if (target.getMailingLists().isEmpty()) {
            target.setMailingLists(new ArrayList<>(source.getMailingLists()));
        }
    }

    @Override
    protected void mergeModelBase_Modules(
            ModelBase target, ModelBase source, boolean sourceDominant, Map<Object, Object> context) {
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
            target.setModules(merged);
            target.setLocation(
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
            ModelBase target, ModelBase source, boolean sourceDominant, Map<Object, Object> context) {
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
                Object key = getRepositoryKey(element);
                merged.put(key, element);
            }

            for (Repository element : recessive) {
                Object key = getRepositoryKey(element);
                if (!merged.containsKey(key)) {
                    merged.put(key, element);
                }
            }

            target.setRepositories(new ArrayList<>(merged.values()));
        }
    }

    @Override
    protected void mergeModelBase_PluginRepositories(
            ModelBase target, ModelBase source, boolean sourceDominant, Map<Object, Object> context) {
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
                Object key = getRepositoryKey(element);
                merged.put(key, element);
            }

            for (Repository element : recessive) {
                Object key = getRepositoryKey(element);
                if (!merged.containsKey(key)) {
                    merged.put(key, element);
                }
            }

            target.setPluginRepositories(new ArrayList<>(merged.values()));
        }
    }

    /*
     * TODO: Whether duplicates should be removed looks like an option for the generated merger.
     */
    @Override
    protected void mergeBuildBase_Filters(
            BuildBase target, BuildBase source, boolean sourceDominant, Map<Object, Object> context) {
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
            target.setFilters(merged);
        }
    }

    @Override
    protected void mergeBuildBase_Resources(
            BuildBase target, BuildBase source, boolean sourceDominant, Map<Object, Object> context) {
        if (sourceDominant || target.getResources().isEmpty()) {
            super.mergeBuildBase_Resources(target, source, sourceDominant, context);
        }
    }

    @Override
    protected void mergeBuildBase_TestResources(
            BuildBase target, BuildBase source, boolean sourceDominant, Map<Object, Object> context) {
        if (sourceDominant || target.getTestResources().isEmpty()) {
            super.mergeBuildBase_TestResources(target, source, sourceDominant, context);
        }
    }

    @Override
    protected void mergeDistributionManagement_Repository(
            DistributionManagement target,
            DistributionManagement source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        DeploymentRepository src = source.getRepository();
        if (src != null) {
            DeploymentRepository tgt = target.getRepository();
            if (sourceDominant || tgt == null) {
                tgt = new DeploymentRepository();
                tgt.setLocation("", src.getLocation(""));
                target.setRepository(tgt);
                mergeDeploymentRepository(tgt, src, sourceDominant, context);
            }
        }
    }

    @Override
    protected void mergeDistributionManagement_SnapshotRepository(
            DistributionManagement target,
            DistributionManagement source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        DeploymentRepository src = source.getSnapshotRepository();
        if (src != null) {
            DeploymentRepository tgt = target.getSnapshotRepository();
            if (sourceDominant || tgt == null) {
                tgt = new DeploymentRepository();
                tgt.setLocation("", src.getLocation(""));
                target.setSnapshotRepository(tgt);
                mergeDeploymentRepository(tgt, src, sourceDominant, context);
            }
        }
    }

    @Override
    protected void mergeDistributionManagement_Site(
            DistributionManagement target,
            DistributionManagement source,
            boolean sourceDominant,
            Map<Object, Object> context) {
        Site src = source.getSite();
        if (src != null) {
            Site tgt = target.getSite();
            if (sourceDominant || tgt == null || isSiteEmpty(tgt)) {
                if (tgt == null) {
                    tgt = new Site();
                }
                tgt.setLocation("", src.getLocation(""));
                target.setSite(tgt);
                mergeSite(tgt, src, sourceDominant, context);
            }
            mergeSite_ChildSiteUrlInheritAppendPath(tgt, src, sourceDominant, context);
        }
    }

    @Override
    protected void mergeSite(Site target, Site source, boolean sourceDominant, Map<Object, Object> context) {
        mergeSite_Id(target, source, sourceDominant, context);
        mergeSite_Name(target, source, sourceDominant, context);
        mergeSite_Url(target, source, sourceDominant, context);
    }

    protected boolean isSiteEmpty(Site site) {
        return StringUtils.isEmpty(site.getId())
                && StringUtils.isEmpty(site.getName())
                && StringUtils.isEmpty(site.getUrl());
    }

    @Override
    protected void mergeSite_Url(Site target, Site source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getUrl();
        if (src != null) {
            if (sourceDominant) {
                target.setUrl(src);
                target.setLocation("url", source.getLocation("url"));
            } else if (target.getUrl() == null) {
                target.setUrl(extrapolateChildUrl(src, source.isChildSiteUrlInheritAppendPath(), context));
                target.setLocation("url", source.getLocation("url"));
            }
        }
    }

    @Override
    protected void mergeScm_Url(Scm target, Scm source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getUrl();
        if (src != null) {
            if (sourceDominant) {
                target.setUrl(src);
                target.setLocation("url", source.getLocation("url"));
            } else if (target.getUrl() == null) {
                target.setUrl(extrapolateChildUrl(src, source.isChildScmUrlInheritAppendPath(), context));
                target.setLocation("url", source.getLocation("url"));
            }
        }
    }

    @Override
    protected void mergeScm_Connection(Scm target, Scm source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getConnection();
        if (src != null) {
            if (sourceDominant) {
                target.setConnection(src);
                target.setLocation("connection", source.getLocation("connection"));
            } else if (target.getConnection() == null) {
                target.setConnection(extrapolateChildUrl(src, source.isChildScmConnectionInheritAppendPath(), context));
                target.setLocation("connection", source.getLocation("connection"));
            }
        }
    }

    @Override
    protected void mergeScm_DeveloperConnection(
            Scm target, Scm source, boolean sourceDominant, Map<Object, Object> context) {
        String src = source.getDeveloperConnection();
        if (src != null) {
            if (sourceDominant) {
                target.setDeveloperConnection(src);
                target.setLocation("developerConnection", source.getLocation("developerConnection"));
            } else if (target.getDeveloperConnection() == null) {
                String e = extrapolateChildUrl(src, source.isChildScmDeveloperConnectionInheritAppendPath(), context);
                target.setDeveloperConnection(e);
                target.setLocation("developerConnection", source.getLocation("developerConnection"));
            }
        }
    }

    @Override
    protected void mergePlugin_Executions(
            Plugin target, Plugin source, boolean sourceDominant, Map<Object, Object> context) {
        List<PluginExecution> src = source.getExecutions();
        if (!src.isEmpty()) {
            List<PluginExecution> tgt = target.getExecutions();
            Map<Object, PluginExecution> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

            for (PluginExecution element : src) {
                if (sourceDominant || (element.getInherited() != null ? element.isInherited() : source.isInherited())) {
                    Object key = getPluginExecutionKey(element);
                    merged.put(key, element);
                }
            }

            for (PluginExecution element : tgt) {
                Object key = getPluginExecutionKey(element);
                PluginExecution existing = merged.get(key);
                if (existing != null) {
                    mergePluginExecution(element, existing, sourceDominant, context);
                }
                merged.put(key, element);
            }

            target.setExecutions(new ArrayList<>(merged.values()));
        }
    }

    @Override
    protected void mergePluginExecution_Goals(
            PluginExecution target, PluginExecution source, boolean sourceDominant, Map<Object, Object> context) {
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
            target.setGoals(merged);
        }
    }

    @Override
    protected void mergeReportPlugin_ReportSets(
            ReportPlugin target, ReportPlugin source, boolean sourceDominant, Map<Object, Object> context) {
        List<ReportSet> src = source.getReportSets();
        if (!src.isEmpty()) {
            List<ReportSet> tgt = target.getReportSets();
            Map<Object, ReportSet> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

            for (ReportSet rset : src) {
                if (sourceDominant || (rset.getInherited() != null ? rset.isInherited() : source.isInherited())) {
                    Object key = getReportSetKey(rset);
                    merged.put(key, rset);
                }
            }

            for (ReportSet element : tgt) {
                Object key = getReportSetKey(element);
                ReportSet existing = merged.get(key);
                if (existing != null) {
                    mergeReportSet(element, existing, sourceDominant, context);
                }
                merged.put(key, element);
            }

            target.setReportSets(new ArrayList<>(merged.values()));
        }
    }

    @Override
    protected Object getDependencyKey(Dependency dependency) {
        return dependency.getManagementKey();
    }

    @Override
    protected Object getPluginKey(Plugin plugin) {
        return plugin.getKey();
    }

    @Override
    protected Object getPluginExecutionKey(PluginExecution pluginExecution) {
        return pluginExecution.getId();
    }

    @Override
    protected Object getReportPluginKey(ReportPlugin reportPlugin) {
        return reportPlugin.getKey();
    }

    @Override
    protected Object getReportSetKey(ReportSet reportSet) {
        return reportSet.getId();
    }

    @Override
    protected Object getRepositoryBaseKey(RepositoryBase repositoryBase) {
        return repositoryBase.getId();
    }

    @Override
    protected Object getExtensionKey(Extension extension) {
        return extension.getGroupId() + ':' + extension.getArtifactId();
    }

    @Override
    protected Object getExclusionKey(Exclusion exclusion) {
        return exclusion.getGroupId() + ':' + exclusion.getArtifactId();
    }

    protected String extrapolateChildUrl(String parentUrl, boolean appendPath, Map<Object, Object> context) {
        return parentUrl;
    }
}
