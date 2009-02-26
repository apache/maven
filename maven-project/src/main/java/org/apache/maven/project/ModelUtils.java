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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Extension;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Notifier;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public final class ModelUtils
{

    private static final ModelPartCloner DEPENDENCY_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneDependency( (Dependency) src );
        }
    };

    private static final ModelPartCloner PLUGIN_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.clonePlugin( (Plugin) src );
        }
    };

    private static final ModelPartCloner EXTENSION_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneExtension( (Extension) src );
        }
    };

    private static final ModelPartCloner RESOURCE_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneResource( (Resource) src );
        }
    };

    private static final ModelPartCloner NOTIFIER_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneNotifier( (Notifier) src );
        }
    };

    private static final ModelPartCloner CONTRIBUTOR_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneContributor( (Contributor) src );
        }
    };

    private static final ModelPartCloner DEVELOPER_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneDeveloper( (Developer) src );
        }
    };

    private static final ModelPartCloner LICENSE_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneLicense( (License) src );
        }
    };

    private static final ModelPartCloner MAILING_LIST_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneMailingList( (MailingList) src );
        }
    };

    private static final ModelPartCloner REPOSITORY_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneRepository( (Repository) src );
        }
    };

    private static final ModelPartCloner PROFILE_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneProfile( (Profile) src );
        }
    };

    private static final ModelPartCloner REPORT_PLUGIN_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneReportPlugin( (ReportPlugin) src );
        }
    };

    private static final ModelPartCloner REPORT_SET_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneReportSet( (ReportSet) src );
        }
    };

    private static final ModelPartCloner DEPENDENCY_EXCLUSION_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.cloneExclusion( (Exclusion) src );
        }
    };

    private static final ModelPartCloner PLUGIN_EXECUTION_CLONER = new ModelPartCloner()
    {
        public Object cloneModelPart( Object src )
        {
            return ModelUtils.clonePluginExecution( (PluginExecution) src );
        }
    };

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

                // MNG-3719: merge currentPlugin with firstPlugin as parent,
                // then use updated currentPlugin as new parent
                mergePluginDefinitions( currentPlugin, firstPlugin, false );
                normalized.set(idx, currentPlugin);
            }
            else
            {
                normalized.add( currentPlugin );
            }
        }

        pluginContainer.setPlugins( normalized );
    }

    public static ReportSet cloneReportSet( ReportSet src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ReportSet result = new ReportSet();
        
        result.setConfiguration( cloneConfiguration( src.getConfiguration() ) );
        result.setId( src.getId() );
        result.setInherited( src.getInherited() );
        result.setReports( cloneListOfStrings( src.getReports() ) );
        
        return result;
    }

    public static ReportPlugin cloneReportPlugin( ReportPlugin src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ReportPlugin result = new ReportPlugin();
        
        result.setArtifactId( src.getArtifactId() );
        result.setConfiguration( cloneConfiguration( src.getConfiguration() ) );
        result.setGroupId( src.getGroupId() );
        result.setInherited( src.getInherited() );
        result.setReportSets( cloneList( src.getReportSets(), REPORT_SET_CLONER ) );
        result.setVersion( src.getVersion() );
        
        return result;
    }

    public static Profile cloneProfile( Profile src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Profile result = new Profile();
        
        cloneModelBaseFields( src, result );
        
        result.setActivation( cloneActivation( src.getActivation() ) );
        
        BuildBase resultBuild = null;
        if ( src.getBuild() != null )
        {
            resultBuild = new BuildBase();
            cloneBuildBaseFields( src.getBuild(), resultBuild );
        }
        result.setBuild( resultBuild );
        
        result.setId( src.getId() );
        result.setSource( src.getSource() );
        
        return result;
    }

    private static void cloneModelBaseFields( ModelBase src, ModelBase result )
    {
        result.setDependencies( cloneList( src.getDependencies(), DEPENDENCY_CLONER ) );
        result.setDependencyManagement( cloneDependencyManagement( src.getDependencyManagement() ) );
        result.setDistributionManagement( cloneDistributionManagement( src.getDistributionManagement() ) );
        
        result.setModules( cloneListOfStrings( src.getModules() ) );

        result.setPluginRepositories( cloneList( src.getPluginRepositories(), REPOSITORY_CLONER ) );
        result.setProperties( cloneProperties( src.getProperties() ) );
        result.setReporting( cloneReporting( src.getReporting() ) );
        result.setRepositories( cloneList( src.getRepositories(), REPOSITORY_CLONER ) );
    }

    public static Reporting cloneReporting( Reporting src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Reporting result = new Reporting();
        
        result.setExcludeDefaults( src.isExcludeDefaults() );
        result.setOutputDirectory( src.getOutputDirectory() );
        result.setPlugins( cloneList( src.getPlugins(), REPORT_PLUGIN_CLONER ) );
        
        return result;
    }

    public static Activation cloneActivation( Activation src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Activation result = new Activation();
        result.setActiveByDefault( src.isActiveByDefault() );
        result.setFile( cloneActivationFile( src.getFile() ) );
        result.setJdk( src.getJdk() );
        result.setOs( cloneActivationOs( src.getOs() ) );
        result.setProperty( cloneActivationProperty( src.getProperty() ) );
        
        return result;
    }

    public static ActivationProperty cloneActivationProperty( ActivationProperty src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ActivationProperty result = new ActivationProperty();
        
        result.setName( src.getName() );
        result.setValue( src.getValue() );
        
        return result;
    }

    public static ActivationOS cloneActivationOs( ActivationOS src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ActivationOS result = new ActivationOS();
        
        result.setArch( src.getArch() );
        result.setFamily( src.getFamily() );
        result.setName( src.getName() );
        result.setVersion( src.getVersion() );
        
        return result;
    }

    public static ActivationFile cloneActivationFile( ActivationFile src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ActivationFile result = new ActivationFile();
        
        result.setExists( src.getExists() );
        result.setMissing( src.getMissing() );

        return result;
    }

    public static Repository cloneRepository( Repository src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Repository result = new Repository();
        
        result.setReleases( cloneRepositoryPolicy( src.getReleases() ) );
        result.setSnapshots( cloneRepositoryPolicy( src.getSnapshots() ) );
        
        cloneRepositoryBaseFields( src, result );
        
        return result;
    }

    public static RepositoryPolicy cloneRepositoryPolicy( RepositoryPolicy src )
    {
        if ( src == null )
        {
            return null;
        }
        
        RepositoryPolicy result = new RepositoryPolicy();
        
        result.setChecksumPolicy( src.getChecksumPolicy() );
        result.setEnabled( src.isEnabled() );
        result.setUpdatePolicy( src.getUpdatePolicy() );
        
        return result;
    }

    public static MailingList cloneMailingList( MailingList src )
    {
        if ( src == null )
        {
            return null;
        }
        
        MailingList result = new MailingList();
        
        result.setArchive( src.getArchive() );
        result.setName( src.getName() );
        result.setOtherArchives( src.getOtherArchives() );
        result.setPost( src.getPost() );
        result.setSubscribe( src.getSubscribe() );
        result.setUnsubscribe( src.getUnsubscribe() );
        
        return result;
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
    public static void mergePluginLists( PluginContainer child, PluginContainer parent,
                                         boolean handleAsInheritance )
    {
        if ( ( child == null ) || ( parent == null ) )
        {
            // nothing to do.
            return;
        }

        List parentPlugins = parent.getPlugins();

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

            Map childPlugins = child.getPluginsAsMap();

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
                                                                        child.getPlugins() );


                child.setPlugins( results );

                child.flushPluginMap();
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

    /**
     * Merge the list of reporting plugins from parent pom and child pom
     * TODO it's pretty much a copy of {@link #mergePluginLists(PluginContainer, PluginContainer, boolean)}
     * 
     * @param child
     * @param parent
     * @param handleAsInheritance
     */
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
            parentPlugins = new ArrayList( parentPlugins );

            // If we're processing this merge as an inheritance, we have to build up a list of
            // plugins that were considered for inheritance.
            if ( handleAsInheritance )
            {
                for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) it.next();

                    String inherited = plugin.getInherited();

                    if ( ( inherited != null ) && !Boolean.valueOf( inherited ).booleanValue() )
                    {
                        it.remove();
                    }
                }
            }

            List assembledPlugins = new ArrayList();

            Map childPlugins = child.getReportPluginsAsMap();

            for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
            {
                ReportPlugin parentPlugin = (ReportPlugin) it.next();

                String parentInherited = parentPlugin.getInherited();

                // only merge plugin definition from the parent if at least one
                // of these is true:
                // 1. we're not processing the plugins in an inheritance-based merge
                // 2. the parent's <inherited/> flag is not set
                // 3. the parent's <inherited/> flag is set to true
                if ( !handleAsInheritance || ( parentInherited == null ) ||
                    Boolean.valueOf( parentInherited ).booleanValue() )
                {
                    ReportPlugin childPlugin = (ReportPlugin) childPlugins.get( parentPlugin.getKey() );

                    if ( ( childPlugin != null ) && !assembledPlugins.contains( childPlugin ) )
                    {
                        ReportPlugin assembledPlugin = childPlugin;

                        mergeReportPluginDefinitions( childPlugin, parentPlugin, handleAsInheritance );

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
                                                                        child.getPlugins() );

                child.setPlugins( results );

                child.flushReportPluginMap();
            }
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

        String parentInherited = parent.getInherited();

        boolean parentIsInherited = ( parentInherited == null ) || Boolean.valueOf( parentInherited ).booleanValue();

        // merge configuration just like with build plugins	
        if ( parentIsInherited )
        {
            Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
            Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

            childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

            child.setConfiguration( childConfiguration );
        }

        // from here to the end of the method is dealing with merging of the <executions/> section.
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

    public static Model cloneModel( Model src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Model result = new Model();
        
        cloneModelBaseFields( src, result );

        result.setArtifactId( src.getArtifactId() );
        result.setBuild( cloneBuild( src.getBuild() ) );
        result.setCiManagement( cloneCiManagement( src.getCiManagement() ) );
        
        result.setContributors( cloneList( src.getContributors(), CONTRIBUTOR_CLONER ) );
        
        result.setDescription( src.getDescription() );
        result.setDevelopers( cloneList( src.getDevelopers(), DEVELOPER_CLONER ) );
        
        result.setGroupId( src.getGroupId() );
        result.setInceptionYear( src.getInceptionYear() );
        result.setIssueManagement( cloneIssueManagement( src.getIssueManagement() ) );
        result.setLicenses( cloneList( src.getLicenses(), LICENSE_CLONER ) );
        
        result.setMailingLists( cloneList( src.getMailingLists(), MAILING_LIST_CLONER ) );
        result.setModelVersion( src.getModelVersion() );
        result.setName( src.getName() );
        result.setOrganization( cloneOrganization( src.getOrganization() ) );
        result.setPackaging( src.getPackaging() );
        result.setParent( cloneParent( src.getParent() ) );
        
        result.setPrerequisites( clonePrerequisites( src.getPrerequisites() ) );
        result.setProfiles( cloneList( src.getProfiles(), PROFILE_CLONER ) );
        
        result.setScm( cloneScm( src.getScm() ) );
        result.setUrl( src.getUrl() );
        result.setVersion( src.getVersion() );

        return result;
    }

    public static Scm cloneScm( Scm src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Scm result = new Scm();
        
        result.setConnection( src.getConnection() );
        result.setDeveloperConnection( src.getDeveloperConnection() );
        result.setTag( src.getTag() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static Prerequisites clonePrerequisites( Prerequisites src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Prerequisites result = new Prerequisites();
        
        result.setMaven( src.getMaven() );

        return result;
    }

    public static Organization cloneOrganization( Organization src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Organization result = new Organization();
        
        result.setName( src.getName() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static License cloneLicense( License src )
    {
        if ( src == null )
        {
            return null;
        }
        
        License result = new License();
        
        result.setComments( src.getComments() );
        result.setDistribution( src.getDistribution() );
        result.setName( src.getName() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static IssueManagement cloneIssueManagement( IssueManagement src )
    {
        if ( src == null )
        {
            return null;
        }
        
        IssueManagement result = new IssueManagement();
        
        result.setSystem( src.getSystem() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static DistributionManagement cloneDistributionManagement( DistributionManagement src )
    {
        if ( src == null )
        {
            return null;
        }
        
        DistributionManagement result = new DistributionManagement();
        
        result.setDownloadUrl( src.getDownloadUrl() );
        result.setRelocation( cloneRelocation( src.getRelocation() ) );
        result.setRepository( cloneDeploymentRepository( src.getRepository() ) );
        result.setSite( cloneSite( src.getSite() ) );
        result.setSnapshotRepository( cloneDeploymentRepository( src.getSnapshotRepository() ) );
        result.setStatus( src.getStatus() );
        
        return result;
    }

    public static Site cloneSite( Site src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Site result = new Site();
        
        result.setId( src.getId() );
        result.setName( src.getName() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static DeploymentRepository cloneDeploymentRepository( DeploymentRepository src )
    {
        if ( src == null )
        {
            return null;
        }
        
        DeploymentRepository result = new DeploymentRepository();
        
        result.setUniqueVersion( src.isUniqueVersion() );
        
        cloneRepositoryBaseFields( src, result );
        
        return result;
    }

    private static void cloneRepositoryBaseFields( RepositoryBase src, RepositoryBase result )
    {
        result.setId( src.getId() );
        result.setLayout( src.getLayout() );
        result.setName( src.getName() );
        result.setUrl( src.getUrl() );
    }

    public static Relocation cloneRelocation( Relocation src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Relocation result = new Relocation();
        
        result.setArtifactId( src.getArtifactId() );
        result.setGroupId( src.getGroupId() );
        result.setMessage( src.getMessage() );
        result.setVersion( src.getVersion() );
        
        return result;
    }

    public static DependencyManagement cloneDependencyManagement( DependencyManagement src )
    {
        if ( src == null )
        {
            return null;
        }
        
        DependencyManagement result = new DependencyManagement();
        result.setDependencies( cloneList( src.getDependencies(), DEPENDENCY_CLONER ) );
        
        return result;
    }

    private static List cloneList( List src, ModelPartCloner cloner )
    {
        List result = null;
        if ( src != null )
        {
            result = new ArrayList( src.size() );
            for ( Iterator it = src.iterator(); it.hasNext(); )
            {
                result.add( cloner.cloneModelPart( (Object) it.next() ) );
            }
        }
        
        return result;
    }

    public static Contributor cloneContributor( Contributor src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Contributor result = new Contributor();
        cloneContributorFields( src, result );
        
        return result;
    }
    
    public static Developer cloneDeveloper( Developer src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Developer result = new Developer();
        
        result.setId( src.getId() );
        
        cloneContributorFields( src, result );
        
        return result;
    }
    
    private static void cloneContributorFields( Contributor src, Contributor result )
    {
        result.setEmail( src.getEmail() );
        result.setName( src.getName() );
        result.setOrganization( src.getOrganization() );
        result.setOrganizationUrl( src.getOrganizationUrl() );
        result.setProperties( cloneProperties( src.getProperties() ) );
        result.setRoles( cloneListOfStrings( src.getRoles() ) );
        result.setTimezone( src.getTimezone() );
        result.setUrl( src.getUrl() );
    }

    public static CiManagement cloneCiManagement( CiManagement src )
    {
        if ( src == null )
        {
            return null;
        }
        
        CiManagement result = new CiManagement();
        
        List notifiers = null;
        if ( src.getNotifiers() != null )
        {
            notifiers = new ArrayList( src.getNotifiers().size() );
            for ( Iterator it = src.getNotifiers().iterator(); it.hasNext(); )
            {
                notifiers.add( cloneNotifier( (Notifier) it.next() ) );
            }
        }
        result.setNotifiers( cloneList( src.getNotifiers(), NOTIFIER_CLONER ) );
        
        result.setSystem( src.getSystem() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static Notifier cloneNotifier( Notifier src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Notifier result = new Notifier();
        result.setAddress( src.getAddress() );
        result.setConfiguration( cloneProperties( src.getConfiguration() ) );
        result.setSendOnError( src.isSendOnError() );
        result.setSendOnFailure( result.isSendOnFailure() );
        result.setSendOnSuccess( result.isSendOnSuccess() );
        result.setSendOnWarning( result.isSendOnWarning() );
        
        return result;
    }

    public static Properties cloneProperties( Properties src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Properties result = new Properties();
        for( Enumeration e = src.propertyNames(); e.hasMoreElements(); )
        {
            String key = (String) e.nextElement();
            result.setProperty( key, src.getProperty( key ) );
        }
        
        return result;
    }

    public static Build cloneBuild( Build src )
    {
        if ( src == null )
        {
            return null;
        }

        Build result = new Build();
        
        cloneBuildBaseFields( src, result );
        
        result.setExtensions( cloneList( src.getExtensions(), EXTENSION_CLONER ) );
        result.setOutputDirectory( src.getOutputDirectory() );
        
        result.setScriptSourceDirectory( src.getScriptSourceDirectory() );
        result.setSourceDirectory( src.getSourceDirectory() );
        result.setTestOutputDirectory( src.getTestOutputDirectory() );
        result.setTestSourceDirectory( src.getTestSourceDirectory() );
        
        return result;
    }

    public static void cloneBuildBaseFields( BuildBase src, BuildBase result )
    {
        result.setDefaultGoal( src.getDefaultGoal() );
        result.setDirectory( src.getDirectory() );
        
        result.setFilters( cloneListOfStrings( src.getFilters() ) );
        result.setFinalName( src.getFinalName() );
        
        result.setPluginManagement( clonePluginManagement( src.getPluginManagement() ) );
        result.setPlugins( cloneList( src.getPlugins(), PLUGIN_CLONER ) );
 
        result.setResources( cloneList( src.getResources(), RESOURCE_CLONER ) );
        
        result.setTestResources( cloneList( src.getTestResources(), RESOURCE_CLONER ) );
    }

    public static PluginManagement clonePluginManagement( PluginManagement src )
    {
        PluginManagement pMgmt = null;
        if ( src != null )
        {
            pMgmt = new PluginManagement();
            pMgmt.setPlugins( cloneList( src.getPlugins(), PLUGIN_CLONER ) );
        }
        
        return pMgmt;
    }

    public static Resource cloneResource( Resource src )
    {
        Resource result = null;
        if ( src != null )
        {
            result = new Resource();
            
            result.setDirectory( src.getDirectory() );
            result.setExcludes( cloneListOfStrings( src.getExcludes() ) );
            result.setFiltering( src.isFiltering() );
            result.setIncludes( cloneListOfStrings( src.getIncludes() ) );
            result.setMergeId( src.getMergeId() );
            result.setTargetPath( src.getTargetPath() );
        }
        
        return result;
    }

    public static Plugin clonePlugin( Plugin src )
    {
        Plugin result = null;
        if ( src != null )
        {
            result = new Plugin();
            result.setArtifactId( src.getArtifactId() );
            
            result.setConfiguration( cloneConfiguration( src.getConfiguration() ) );
            
            result.setDependencies( cloneList( src.getDependencies(), DEPENDENCY_CLONER ) );
            result.setExecutions( cloneList( src.getExecutions(), PLUGIN_EXECUTION_CLONER ) );
            
            result.setExtensions( src.isExtensions() );
            result.setGroupId( src.getGroupId() );
            result.setInherited( src.getInherited() );
            result.setVersion( src.getVersion() );
        }
        
        return result;
    }

    public static PluginExecution clonePluginExecution( PluginExecution src )
    {
        PluginExecution result = null;
        
        if ( src != null )
        {
            result = new PluginExecution();
            
            result.setId( src.getId() );
            result.setGoals( cloneListOfStrings( src.getGoals() ) );
            result.setConfiguration( cloneConfiguration( src.getConfiguration() ) );
            result.setInherited( src.getInherited() );
            result.setPhase( src.getPhase() );
        }
        
        return result;
    }

    // FIXME: We need something better than this for configurations!
    public static Object cloneConfiguration( Object configuration )
    {
        if ( configuration == null )
        {
            return null;
        }
        
        return new Xpp3Dom( (Xpp3Dom) configuration );
    }

    public static Dependency cloneDependency( Dependency src )
    {
        Dependency result = null;
        if ( src != null )
        {
            result = new Dependency();
            
            result.setArtifactId( src.getArtifactId() );
            result.setClassifier( src.getClassifier() );
            result.setExclusions( cloneList( src.getExclusions(), DEPENDENCY_EXCLUSION_CLONER ) );
            result.setGroupId( src.getGroupId() );
            result.setOptional( src.isOptional() );
            result.setScope( src.getScope() );
            result.setSystemPath( src.getSystemPath() );
            result.setType( src.getType() );
            result.setVersion( src.getVersion() );
        }
        
        return result;
    }

    public static Exclusion cloneExclusion( Exclusion src )
    {
        Exclusion result = null;
        if ( src != null )
        {
            result = new Exclusion();
            result.setArtifactId( src.getArtifactId() );
            result.setGroupId( src.getGroupId() );
        }
        
        return result;
    }

    public static List cloneListOfStrings( List src )
    {
        List result = null;
        if ( src != null )
        {
            result = new ArrayList( src.size() );
            for ( Iterator it = src.iterator(); it.hasNext(); )
            {
                String item = (String) it.next();
                result.add( item );
            }
        }
        
        return result;
    }

    public static Extension cloneExtension( Extension src )
    {
        Extension rExt = new Extension();
        rExt.setArtifactId( src.getArtifactId() );
        rExt.setGroupId( src.getGroupId() );
        rExt.setVersion( src.getVersion() );
        
        return rExt;
    }

    public static Exclusion cloneDependencyExclusion( Exclusion src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Exclusion result = new Exclusion();

        result.setArtifactId( src.getArtifactId() );
        result.setGroupId( src.getGroupId() );
        
        return result;
    }

    public static Parent cloneParent( Parent src )
    {
        if ( src == null )
        {
            return null;
        }

        Parent result = new Parent();
        result.setArtifactId( src.getArtifactId() );
        result.setGroupId( src.getGroupId() );
        result.setRelativePath( src.getRelativePath() );
        result.setVersion( src.getVersion() );
        
        return result;
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
        Map extMap = new LinkedHashMap();

        List ext = childBuild.getExtensions();

        if ( ext != null )
        {
            for ( Iterator it = ext.iterator(); it.hasNext(); )
            {
                Extension extension = (Extension) it.next();
                extMap.put( extension.getKey(), extension );
            }
        }

        ext = parentBuild.getExtensions();

        if ( ext != null )
        {
            for ( Iterator it = ext.iterator(); it.hasNext(); )
            {
                Extension extension = (Extension) it.next();
                if ( !extMap.containsKey( extension.getKey() ) )
                {
                    extMap.put( extension.getKey(), extension );
                }
            }
        }

        childBuild.setExtensions( new ArrayList( extMap.values() ) );
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
        Map depsMap = new LinkedHashMap();

        if ( child != null )
        {
            for ( Iterator it = child.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }

        if ( parent != null )
        {
            for ( Iterator it = parent.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                if ( !depsMap.containsKey( dependency.getManagementKey() ) )
                {
                    depsMap.put( dependency.getManagementKey(), dependency );
                }
            }
        }

        return new ArrayList( depsMap.values() );
    }
    
    public static interface ModelPartCloner
    {
        Object cloneModelPart( Object src );
    }
    

}
