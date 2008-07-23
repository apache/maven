package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Site;
import org.apache.maven.project.inheritance.DefaultModelInheritanceAssembler;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public final class ModelUtils
{

    /**
     * Given this plugin list:
     *
     * A1 -> B -> C -> A2 -> D
     *
     * Rearrange it to this:
     *
     * A(A1 + A2) -> B -> C -> D
     *
     * In cases of overlapping definitions, A1 is overridden by A2
     *
     */
    public static void mergeDuplicatePluginDefinitions( PluginContainer pluginContainer )
    {
        if ( pluginContainer == null )
        {
            return;
        }

        List originalPlugins = pluginContainer.getPlugins();

        if ( ( originalPlugins == null ) || originalPlugins.isEmpty() )
        {
            return;
        }

        List normalized = new ArrayList( originalPlugins.size() );

        for ( Iterator it = originalPlugins.iterator(); it.hasNext(); )
        {
            Plugin currentPlugin = (Plugin) it.next();

            if ( normalized.contains( currentPlugin ) )
            {
                int idx = normalized.indexOf( currentPlugin );
                Plugin firstPlugin = (Plugin) normalized.get( idx );

                mergePluginDefinitions( firstPlugin, currentPlugin, false );
            }
            else
            {
                normalized.add( currentPlugin );
            }
        }

        pluginContainer.setPlugins( normalized );
    }

    /**
     * This should be the resulting ordering of plugins after merging:
     *
     * Given:
     *
     *   parent: X -> A -> B -> D -> E
     *   child: Y -> A -> C -> D -> F
     *
     * Result:
     *
     *   X -> Y -> A -> B -> C -> D -> E -> F
     */
    public static void mergePluginLists( PluginContainer childContainer, PluginContainer parentContainer,
                                         boolean handleAsInheritance )
    {
        if ( ( childContainer == null ) || ( parentContainer == null ) )
        {
            // nothing to do.
            return;
        }

        List parentPlugins = parentContainer.getPlugins();

        if ( ( parentPlugins != null ) && !parentPlugins.isEmpty() )
        {
            parentPlugins = new ArrayList( parentPlugins );

            // If we're processing this merge as an inheritance, we have to build up a list of
            // plugins that were considered for inheritance.
            if ( handleAsInheritance )
            {
                for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
                {
                    Plugin plugin = (Plugin) it.next();

                    String inherited = plugin.getInherited();

                    if ( ( inherited != null ) && !Boolean.valueOf( inherited ).booleanValue() )
                    {
                        it.remove();
                    }
                }
            }

            List assembledPlugins = new ArrayList();

            Map childPlugins = childContainer.getPluginsAsMap();

            for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
            {
                Plugin parentPlugin = (Plugin) it.next();

                String parentInherited = parentPlugin.getInherited();

                // only merge plugin definition from the parent if at least one
                // of these is true:
                // 1. we're not processing the plugins in an inheritance-based merge
                // 2. the parent's <inherited/> flag is not set
                // 3. the parent's <inherited/> flag is set to true
                if ( !handleAsInheritance || ( parentInherited == null ) ||
                    Boolean.valueOf( parentInherited ).booleanValue() )
                {
                    Plugin childPlugin = (Plugin) childPlugins.get( parentPlugin.getKey() );

                    if ( ( childPlugin != null ) && !assembledPlugins.contains( childPlugin ) )
                    {
                        Plugin assembledPlugin = childPlugin;

                        mergePluginDefinitions( childPlugin, parentPlugin, handleAsInheritance );

                        // fix for MNG-2221 (assembly cache was not being populated for later reference):
                        assembledPlugins.add( assembledPlugin );
                    }

                    // if we're processing this as an inheritance-based merge, and
                    // the parent's <inherited/> flag is not set, then we need to
                    // clear the inherited flag in the merge result.
                    if ( handleAsInheritance && ( parentInherited == null ) )
                    {
                        parentPlugin.unsetInheritanceApplied();
                    }
                }

                // very important to use the parentPlugins List, rather than parentContainer.getPlugins()
                // since this list is a local one, and may have been modified during processing.
                List results = ModelUtils.orderAfterMerge( assembledPlugins, parentPlugins,
                                                                        childContainer.getPlugins() );


                childContainer.setPlugins( results );

                childContainer.flushPluginMap();
            }
        }
    }

    public static List orderAfterMerge( List merged, List highPrioritySource, List lowPrioritySource )
    {
        List results = new ArrayList();

        if ( !merged.isEmpty() )
        {
            results.addAll( merged );
        }

        List missingFromResults = new ArrayList();

        List sources = new ArrayList();

        sources.add( highPrioritySource );
        sources.add( lowPrioritySource );

        for ( Iterator sourceIterator = sources.iterator(); sourceIterator.hasNext(); )
        {
            List source = (List) sourceIterator.next();

            for ( Iterator it = source.iterator(); it.hasNext(); )
            {
                Object item = it.next();

                if ( results.contains( item ) )
                {
                    if ( !missingFromResults.isEmpty() )
                    {
                        int idx = results.indexOf( item );

                        if ( idx < 0 )
                        {
                            idx = 0;
                        }

                        results.addAll( idx, missingFromResults );

                        missingFromResults.clear();
                    }
                }
                else
                {
                    missingFromResults.add( item );
                }
            }

            if ( !missingFromResults.isEmpty() )
            {
                results.addAll( missingFromResults );

                missingFromResults.clear();
            }
        }

        return results;
    }

    public static void mergeReportPluginLists( Reporting child, Reporting parent, boolean handleAsInheritance )
    {
        if ( ( child == null ) || ( parent == null ) )
        {
            // nothing to do.
            return;
        }

        List parentPlugins = parent.getPlugins();

        if ( ( parentPlugins != null ) && !parentPlugins.isEmpty() )
        {
            Map assembledPlugins = new TreeMap();

            Map childPlugins = child.getReportPluginsAsMap();

            for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
            {
                ReportPlugin parentPlugin = (ReportPlugin) it.next();

                String parentInherited = parentPlugin.getInherited();

                if ( !handleAsInheritance || ( parentInherited == null ) ||
                    Boolean.valueOf( parentInherited ).booleanValue() )
                {

                    ReportPlugin assembledPlugin = parentPlugin;

                    ReportPlugin childPlugin = (ReportPlugin) childPlugins.get( parentPlugin.getKey() );

                    if ( childPlugin != null )
                    {
                        assembledPlugin = childPlugin;

                        mergeReportPluginDefinitions( childPlugin, parentPlugin, handleAsInheritance );
                    }

                    if ( handleAsInheritance && ( parentInherited == null ) )
                    {
                        assembledPlugin.unsetInheritanceApplied();
                    }

                    assembledPlugins.put( assembledPlugin.getKey(), assembledPlugin );
                }
            }

            for ( Iterator it = childPlugins.values().iterator(); it.hasNext(); )
            {
                ReportPlugin childPlugin = (ReportPlugin) it.next();

                if ( !assembledPlugins.containsKey( childPlugin.getKey() ) )
                {
                    assembledPlugins.put( childPlugin.getKey(), childPlugin );
                }
            }

            child.setPlugins( new ArrayList( assembledPlugins.values() ) );

            child.flushReportPluginMap();
        }
    }

    public static void mergePluginDefinitions( Plugin child, Plugin parent, boolean handleAsInheritance )
    {
        if ( ( child == null ) || ( parent == null ) )
        {
            // nothing to do.
            return;
        }

        if ( parent.isExtensions() )
        {
            child.setExtensions( true );
        }

        if ( ( child.getVersion() == null ) && ( parent.getVersion() != null ) )
        {
            child.setVersion( parent.getVersion() );
        }

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );

        child.setDependencies( mergeDependencyList( child.getDependencies(), parent.getDependencies() ) );

        // from here to the end of the method is dealing with merging of the <executions/> section.
        String parentInherited = parent.getInherited();

        boolean parentIsInherited = ( parentInherited == null ) || Boolean.valueOf( parentInherited ).booleanValue();

        List parentExecutions = parent.getExecutions();

        if ( ( parentExecutions != null ) && !parentExecutions.isEmpty() )
        {
            List mergedExecutions = new ArrayList();

            Map assembledExecutions = new TreeMap();

            Map childExecutions = child.getExecutionsAsMap();

            for ( Iterator it = parentExecutions.iterator(); it.hasNext(); )
            {
                PluginExecution parentExecution = (PluginExecution) it.next();

                String inherited = parentExecution.getInherited();

                boolean parentExecInherited = parentIsInherited && ( ( inherited == null ) || Boolean.valueOf( inherited ).booleanValue() );

                if ( !handleAsInheritance || parentExecInherited )
                {
                    PluginExecution assembled = parentExecution;

                    PluginExecution childExecution = (PluginExecution) childExecutions.get( parentExecution.getId() );

                    if ( childExecution != null )
                    {
                        mergePluginExecutionDefinitions( childExecution, parentExecution );

                        assembled = childExecution;
                    }
                    else if ( handleAsInheritance && ( parentInherited == null ) )
                    {
                        parentExecution.unsetInheritanceApplied();
                    }

                    assembledExecutions.put( assembled.getId(), assembled );
                    mergedExecutions.add(assembled);
                }
            }

            for ( Iterator it = child.getExecutions().iterator(); it.hasNext(); )
            {
                PluginExecution childExecution = (PluginExecution)it.next();

                if ( !assembledExecutions.containsKey( childExecution.getId() ) )
                {
                    mergedExecutions.add(childExecution);
                }
            }

            child.setExecutions(mergedExecutions);

            child.flushExecutionMap();
        }

    }

    public static void mergeReportPluginDefinitions( ReportPlugin child, ReportPlugin parent,
                                                     boolean handleAsInheritance )
    {
        if ( ( child == null ) || ( parent == null ) )
        {
            // nothing to do.
            return;
        }

        if ( ( child.getVersion() == null ) && ( parent.getVersion() != null ) )
        {
            child.setVersion( parent.getVersion() );
        }

        // from here to the end of the method is dealing with merging of the <executions/> section.
        String parentInherited = parent.getInherited();

        boolean parentIsInherited = ( parentInherited == null ) || Boolean.valueOf( parentInherited ).booleanValue();

        List parentReportSets = parent.getReportSets();

        if ( ( parentReportSets != null ) && !parentReportSets.isEmpty() )
        {
            Map assembledReportSets = new TreeMap();

            Map childReportSets = child.getReportSetsAsMap();

            for ( Iterator it = parentReportSets.iterator(); it.hasNext(); )
            {
                ReportSet parentReportSet = (ReportSet) it.next();

                if ( !handleAsInheritance || parentIsInherited )
                {
                    ReportSet assembledReportSet = parentReportSet;

                    ReportSet childReportSet = (ReportSet) childReportSets.get( parentReportSet.getId() );

                    if ( childReportSet != null )
                    {
                        mergeReportSetDefinitions( childReportSet, parentReportSet );

                        assembledReportSet = childReportSet;
                    }
                    else if ( handleAsInheritance && ( parentInherited == null ) )
                    {
                        parentReportSet.unsetInheritanceApplied();
                    }

                    assembledReportSets.put( assembledReportSet.getId(), assembledReportSet );
                }
            }

            for ( Iterator it = childReportSets.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();

                String id = (String) entry.getKey();

                if ( !assembledReportSets.containsKey( id ) )
                {
                    assembledReportSets.put( id, entry.getValue() );
                }
            }

            child.setReportSets( new ArrayList( assembledReportSets.values() ) );

            child.flushReportSetMap();
        }

    }

    private static void mergePluginExecutionDefinitions( PluginExecution child, PluginExecution parent )
    {
        if ( child.getPhase() == null )
        {
            child.setPhase( parent.getPhase() );
        }

        List parentGoals = parent.getGoals();
        List childGoals = child.getGoals();

        List goals = new ArrayList();

        if ( ( childGoals != null ) && !childGoals.isEmpty() )
        {
            goals.addAll( childGoals );
        }

        if ( parentGoals != null )
        {
            for ( Iterator goalIterator = parentGoals.iterator(); goalIterator.hasNext(); )
            {
                String goal = (String) goalIterator.next();

                if ( !goals.contains( goal ) )
                {
                    goals.add( goal );
                }
            }
        }

        child.setGoals( goals );

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );
    }

    private static void mergeReportSetDefinitions( ReportSet child, ReportSet parent )
    {
        List parentReports = parent.getReports();
        List childReports = child.getReports();

        List reports = new ArrayList();

        if ( ( childReports != null ) && !childReports.isEmpty() )
        {
            reports.addAll( childReports );
        }

        if ( parentReports != null )
        {
            for ( Iterator i = parentReports.iterator(); i.hasNext(); )
            {
                String report = (String) i.next();

                if ( !reports.contains( report ) )
                {
                    reports.add( report );
                }
            }
        }

        child.setReports( reports );

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );
    }

    public static Model cloneModel( Model model )
    {
        // TODO: would be nice for the modello:java code to generate this as a copy constructor
        // FIXME: Fix deep cloning issues with existing plugin instances (setting 
        //       a version when resolved will pollute the original model instance)
        Model newModel = new Model();
        ModelInheritanceAssembler assembler = new DefaultModelInheritanceAssembler();
        newModel.setModelVersion( model.getModelVersion() );
        newModel.setName( model.getName() );
        newModel.setParent( cloneParent( model.getParent() ) );
        newModel.setVersion( model.getVersion() );
        newModel.setArtifactId( model.getArtifactId() );
        newModel.setProperties( new Properties( model.getProperties() ) );
        newModel.setGroupId( model.getGroupId() );
        newModel.setPackaging( model.getPackaging() );
        newModel.setModules( cloneModules( model.getModules() ) );

        newModel.setProfiles( cloneProfiles( model.getProfiles() ) );

        assembler.copyModel( newModel, model );

        return newModel;
    }

    public static Build cloneBuild( Build build )
    {
        ModelInheritanceAssembler assembler = new DefaultModelInheritanceAssembler();

        Build clone = new Build();

        assembler.assembleBuildInheritance( clone, build, false );

        return clone;
    }

    private static List cloneProfiles( List profiles )
    {
        if ( profiles == null )
        {
            return profiles;
        }

        List newProfiles = new ArrayList( profiles.size() );

        for ( Iterator it = profiles.iterator(); it.hasNext(); )
        {
            Profile profile = (Profile) it.next();

            Profile newProfile = new Profile();

            newProfile.setId( profile.getId() );

            newProfile.setActivation( cloneProfileActivation( profile.getActivation() ) );

            newProfile.setBuild( cloneProfileBuild( profile.getBuild() ) );

            newProfile.setDependencies( cloneProfileDependencies( profile.getDependencies() ) );

            DependencyManagement dm = profile.getDependencyManagement();

            if ( dm != null )
            {
                DependencyManagement newDM = new DependencyManagement();

                newDM.setDependencies( cloneProfileDependencies( dm.getDependencies() ) );

                newProfile.setDependencyManagement( newDM );
            }

            newProfile.setDistributionManagement( cloneProfileDistributionManagement( profile
                .getDistributionManagement() ) );

            List modules = profile.getModules();

            if ( ( modules != null ) && !modules.isEmpty() )
            {
                newProfile.setModules( new ArrayList( modules ) );
            }

//            newProfile.setPluginRepositories( cloneProfileRepositories( profile.getPluginRepositories() ) );

            Properties props = profile.getProperties();

            if ( props != null )
            {
                Properties newProps = new Properties();
                newProps.putAll( props );

                newProfile.setProperties( newProps );
            }

            newProfile.setReporting( cloneProfileReporting( profile.getReporting() ) );

            newProfile.setReports( profile.getReports() );

            newProfile.setRepositories( cloneProfileRepositories( profile.getRepositories() ) );

            newProfile.setSource( profile.getSource() );

            newProfiles.add( newProfile );
        }

        return newProfiles;
    }

    private static Reporting cloneProfileReporting( Reporting reporting )
    {
        Reporting newR = null;

        if ( reporting != null )
        {
            newR = new Reporting();

            newR.setOutputDirectory( reporting.getOutputDirectory() );

            List plugins = reporting.getPlugins();

            if ( plugins != null )
            {
                List newP = new ArrayList( plugins.size() );

                for ( Iterator it = plugins.iterator(); it.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) it.next();

                    ReportPlugin newPlugin = new ReportPlugin();

                    newPlugin.setArtifactId( plugin.getArtifactId() );
                    newPlugin.setGroupId( plugin.getGroupId() );
                    newPlugin.setVersion( plugin.getVersion() );
                    newPlugin.setInherited( plugin.getInherited() );
                    newPlugin.setReportSets( cloneReportSets( plugin.getReportSets() ) );

                    // TODO: Implement deep-copy of configuration.
                    newPlugin.setConfiguration( plugin.getConfiguration() );

                    newP.add( newPlugin );
                }

                newR.setPlugins( newP );
            }
        }

        return newR;
    }

    private static List cloneReportSets( List sets )
    {
        List newSets = null;

        if ( sets != null )
        {
            newSets = new ArrayList( sets.size() );

            for ( Iterator it = sets.iterator(); it.hasNext(); )
            {
                ReportSet set = (ReportSet) it.next();

                ReportSet newSet = new ReportSet();

                // TODO: Deep-copy config.
                newSet.setConfiguration( set.getConfiguration() );

                newSet.setId( set.getId() );
                newSet.setInherited( set.getInherited() );

                newSet.setReports( new ArrayList( set.getReports() ) );

                newSets.add( newSet );
            }
        }

        return newSets;
    }

    private static List cloneProfileRepositories( List repos )
    {
        List newRepos = null;

        if ( repos != null )
        {
            newRepos = new ArrayList( repos.size() );

            for ( Iterator it = repos.iterator(); it.hasNext(); )
            {
                Repository repo = (Repository) it.next();

                Repository newRepo = new Repository();

                newRepo.setId( repo.getId() );
                newRepo.setLayout( repo.getLayout() );
                newRepo.setName( repo.getName() );

                RepositoryPolicy releasePolicy = repo.getReleases();

                if ( releasePolicy != null )
                {
                    RepositoryPolicy newPolicy = new RepositoryPolicy();
                    newPolicy.setEnabled( releasePolicy.isEnabled() );
                    newPolicy.setChecksumPolicy( releasePolicy.getChecksumPolicy() );
                    newPolicy.setUpdatePolicy( releasePolicy.getUpdatePolicy() );

                    newRepo.setReleases( newPolicy );
                }

                RepositoryPolicy snapPolicy = repo.getSnapshots();

                if ( snapPolicy != null )
                {
                    RepositoryPolicy newPolicy = new RepositoryPolicy();
                    newPolicy.setEnabled( snapPolicy.isEnabled() );
                    newPolicy.setChecksumPolicy( snapPolicy.getChecksumPolicy() );
                    newPolicy.setUpdatePolicy( snapPolicy.getUpdatePolicy() );

                    newRepo.setSnapshots( newPolicy );
                }

                newRepo.setUrl( repo.getUrl() );

                newRepos.add( newRepo );
            }
        }

        return newRepos;
    }

    private static DistributionManagement cloneProfileDistributionManagement( DistributionManagement dm )
    {
        DistributionManagement newDM = null;

        if ( dm != null )
        {
            newDM = new DistributionManagement();

            newDM.setDownloadUrl( dm.getDownloadUrl() );
            newDM.setStatus( dm.getStatus() );

            Relocation relocation = dm.getRelocation();

            if ( relocation != null )
            {
                Relocation newR = new Relocation();

                newR.setArtifactId( relocation.getArtifactId() );
                newR.setGroupId( relocation.getGroupId() );
                newR.setMessage( relocation.getMessage() );
                newR.setVersion( relocation.getVersion() );

                newDM.setRelocation( newR );
            }

            RepositoryBase repo = dm.getRepository();

            if ( repo != null )
            {
                DeploymentRepository newRepo = new DeploymentRepository();

                newRepo.setId( repo.getId() );
                newRepo.setLayout( repo.getLayout() );
                newRepo.setName( repo.getName() );
                newRepo.setUrl( repo.getUrl() );

                newDM.setRepository( newRepo );
            }

            Site site = dm.getSite();

            if ( site != null )
            {
                Site newSite = new Site();

                newSite.setId( site.getId() );
                newSite.setName( site.getName() );
                newSite.setUrl( site.getUrl() );

                newDM.setSite( newSite );
            }

            RepositoryBase sRepo = dm.getSnapshotRepository();

            if ( sRepo != null )
            {
                DeploymentRepository newRepo = new DeploymentRepository();

                newRepo.setId( sRepo.getId() );
                newRepo.setLayout( sRepo.getLayout() );
                newRepo.setName( sRepo.getName() );
                newRepo.setUrl( sRepo.getUrl() );

                newDM.setSnapshotRepository( newRepo );
            }
        }

        return newDM;
    }

    private static List cloneProfileDependencies( List dependencies )
    {
        List newDependencies = null;

        if ( dependencies != null )
        {
            newDependencies = new ArrayList( dependencies.size() );

            for ( Iterator it = dependencies.iterator(); it.hasNext(); )
            {
                Dependency dep = (Dependency) it.next();

                Dependency newDep = new Dependency();

                newDep.setArtifactId( dep.getArtifactId() );
                newDep.setClassifier( dep.getClassifier() );
                newDep.setExclusions( cloneDependencyExclusions( dep.getExclusions() ) );
                newDep.setGroupId( dep.getGroupId() );
                newDep.setScope( dep.getScope() );
                newDep.setSystemPath( dep.getSystemPath() );
                newDep.setType( dep.getType() );
                newDep.setVersion( dep.getVersion() );

                newDependencies.add( newDep );
            }
        }

        return newDependencies;
    }

    private static List cloneDependencyExclusions( List ex )
    {
        List newEx = null;

        if ( ex != null )
        {
            newEx = new ArrayList( ex.size() );

            for ( Iterator it = ex.iterator(); it.hasNext(); )
            {
                Exclusion exclusion = (Exclusion) it.next();

                Exclusion newExclusion = new Exclusion();

                newExclusion.setArtifactId( exclusion.getArtifactId() );
                newExclusion.setGroupId( exclusion.getGroupId() );

                newEx.add( newExclusion );
            }
        }

        return newEx;
    }

    private static BuildBase cloneProfileBuild( BuildBase build )
    {
        BuildBase newBuild = null;
        if ( build != null )
        {
            newBuild = new BuildBase();

            newBuild.setDefaultGoal( build.getDefaultGoal() );
            newBuild.setDirectory( build.getDirectory() );
            newBuild.setFinalName( build.getFinalName() );

            newBuild.setPluginManagement( cloneProfilePluginManagement( build.getPluginManagement() ) );
            newBuild.setPlugins( cloneProfilePlugins( build.getPlugins() ) );
            newBuild.setResources( cloneProfileResources( build.getResources() ) );
            newBuild.setTestResources( cloneProfileResources( build.getTestResources() ) );
        }

        return newBuild;
    }

    private static List cloneProfileResources( List resources )
    {
        List newResources = null;

        if ( resources != null )
        {
            newResources = new ArrayList( resources.size() );

            for ( Iterator it = resources.iterator(); it.hasNext(); )
            {
                Resource resource = (Resource) it.next();

                Resource newResource = new Resource();

                newResource.setDirectory( resource.getDirectory() );
                newResource.setExcludes( new ArrayList( resource.getExcludes() ) );
                newResource.setFiltering( resource.isFiltering() );
                newResource.setIncludes( new ArrayList( resource.getIncludes() ) );
                newResource.setTargetPath( resource.getTargetPath() );

                newResources.add( newResource );
            }
        }

        return newResources;
    }

    private static PluginManagement cloneProfilePluginManagement( PluginManagement pluginManagement )
    {
        PluginManagement newPM = null;

        if ( pluginManagement != null )
        {
            newPM = new PluginManagement();

            List plugins = pluginManagement.getPlugins();

            newPM.setPlugins( cloneProfilePlugins( plugins ) );
        }

        return newPM;
    }

    private static List cloneProfilePlugins( List plugins )
    {
        List newPlugins = null;

        if ( plugins != null )
        {
            newPlugins = new ArrayList( plugins.size() );

            for ( Iterator it = plugins.iterator(); it.hasNext(); )
            {
                Plugin plugin = (Plugin) it.next();

                Plugin newPlugin = new Plugin();

                newPlugin.setArtifactId( plugin.getArtifactId() );
                newPlugin.setExtensions( plugin.isExtensions() );
                newPlugin.setGroupId( plugin.getGroupId() );
                newPlugin.setInherited( plugin.getInherited() );
                newPlugin.setVersion( plugin.getVersion() );

                // TODO: Deep-copy this!
                newPlugin.setConfiguration( plugin.getConfiguration() );

                newPlugin.setExecutions( cloneExecutions( plugin.getExecutions() ) );

                newPlugins.add( newPlugin );
            }
        }

        return newPlugins;
    }

    private static List cloneExecutions( List executions )
    {
        List newExecs = null;

        if ( executions != null )
        {
            newExecs = new ArrayList( executions.size() );

            for ( Iterator it = executions.iterator(); it.hasNext(); )
            {
                PluginExecution exec = (PluginExecution) it.next();

                PluginExecution newExec = new PluginExecution();

                // TODO: Deep-copy configs.
                newExec.setConfiguration( exec.getConfiguration() );

                newExec.setId( exec.getId() );
                newExec.setInherited( exec.getInherited() );
                newExec.setPhase( exec.getPhase() );

                List goals = exec.getGoals();

                if ( ( goals != null ) && !goals.isEmpty() )
                {
                    newExec.setGoals( new ArrayList( goals ) );
                }

                newExecs.add( newExec );
            }
        }

        return newExecs;
    }

    private static Activation cloneProfileActivation( Activation activation )
    {
        Activation newActivation = null;
        if ( activation != null )
        {
            newActivation = new Activation();

            newActivation.setActiveByDefault( activation.isActiveByDefault() );

            ActivationFile af = activation.getFile();

            if ( af != null )
            {
                ActivationFile afNew = new ActivationFile();
                afNew.setExists( af.getExists() );
                afNew.setMissing( af.getMissing() );

                newActivation.setFile( afNew );
            }

            newActivation.setJdk( activation.getJdk() );

            ActivationProperty ap = activation.getProperty();

            if ( ap != null )
            {
                ActivationProperty newAp = new ActivationProperty();

                newAp.setName( ap.getName() );
                newAp.setValue( ap.getValue() );

                newActivation.setProperty( newAp );
            }
        }

        return newActivation;
    }

    private static List cloneModules( List modules )
    {
        if ( modules == null )
        {
            return modules;
        }
        return new ArrayList( modules );
    }

    private static Parent cloneParent( Parent parent )
    {
        if ( parent == null )
        {
            return parent;
        }

        Parent newParent = new Parent();
        newParent.setArtifactId( parent.getArtifactId() );
        newParent.setGroupId( parent.getGroupId() );
        newParent.setRelativePath( parent.getRelativePath() );
        newParent.setVersion( parent.getVersion() );
        return newParent;
    }

    public static List mergeRepositoryLists( List dominant, List recessive )
    {
        List repositories = new ArrayList();

        for ( Iterator it = dominant.iterator(); it.hasNext(); )
        {
            Repository repository = (Repository) it.next();

            repositories.add( repository );
        }

        for ( Iterator it = recessive.iterator(); it.hasNext(); )
        {
            Repository repository = (Repository) it.next();

            if ( !repositories.contains( repository ) )
            {
                repositories.add( repository );
            }
        }

        return repositories;
    }

    public static void mergeExtensionLists( Build childBuild, Build parentBuild )
    {
        for ( Iterator i = parentBuild.getExtensions().iterator(); i.hasNext(); )
        {
            Extension e = (Extension) i.next();
            if ( !childBuild.getExtensions().contains( e ) )
            {
                childBuild.addExtension( e );
            }
        }
    }

    public static void mergeResourceLists( List childResources, List parentResources )
    {
        for ( Iterator i = parentResources.iterator(); i.hasNext(); )
        {
            Resource r = (Resource) i.next();
            if ( !childResources.contains( r ) )
            {
                childResources.add( r );
            }
        }
    }

    public static void mergeFilterLists( List childFilters, List parentFilters )
    {
        for ( Iterator i = parentFilters.iterator(); i.hasNext(); )
        {
            String f = (String) i.next();
            if ( !childFilters.contains( f ) )
            {
                childFilters.add( f );
            }
        }
    }

    public static List mergeDependencyList( List child, List parent )
    {
        Map depsMap = new HashMap();

        if ( parent != null )
        {
            for ( Iterator it = parent.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }

        if ( child != null )
        {
            for ( Iterator it = child.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }

        return new ArrayList( depsMap.values() );
    }

    public static String getGroupId( Model model )
    {
        Parent parent = model.getParent();

        String groupId = model.getGroupId();
        if ( ( parent != null ) && ( groupId == null ) )
        {
            groupId = parent.getGroupId();
        }

        return groupId;
    }

    public static String getVersion( Model model )
    {
        Parent parent = model.getParent();

        String version = model.getVersion();
        if ( ( parent != null ) && ( version == null ) )
        {
            version = parent.getVersion();
        }

        return version;
    }

}
