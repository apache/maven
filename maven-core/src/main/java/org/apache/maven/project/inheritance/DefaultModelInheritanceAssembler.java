package org.apache.maven.project.inheritance;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Goal;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Scm;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultModelInheritanceAssembler.java,v 1.4 2004/08/23 20:24:54
 *          jdcasey Exp $
 * @todo generate this with modello to keep it in sync with changes in the
 * model.
 */
public class DefaultModelInheritanceAssembler
    implements ModelInheritanceAssembler
{
    public void assembleModelInheritance( Model child, Model parent )
    {
        // Group id
        if ( child.getGroupId() == null )
        {
            child.setGroupId( parent.getGroupId() );
        }

        // currentVersion
        if ( child.getVersion() == null )
        {
            child.setVersion( parent.getVersion() );
        }

        // inceptionYear
        if ( child.getInceptionYear() == null )
        {
            child.setInceptionYear( parent.getInceptionYear() );
        }

        // url
        if ( child.getUrl() == null )
        {
            child.setUrl( parent.getUrl() );
        }

        // ----------------------------------------------------------------------
        // Distribution
        // ----------------------------------------------------------------------

        assembleDistributionInheritence( child, parent );

        // issueManagement
        if ( child.getIssueManagement() == null )
        {
            child.setIssueManagement( parent.getIssueManagement() );
        }

        // description
        if ( child.getDescription() == null )
        {
            child.setDescription( parent.getDescription() );
        }

        // Organization
        if ( child.getOrganization() == null )
        {
            child.setOrganization( parent.getOrganization() );
        }

        // Scm
        assembleScmInheritance( child, parent );

        // ciManagement
        if ( child.getCiManagement() == null )
        {
            child.setCiManagement( parent.getCiManagement() );
        }

        // developers
        if ( child.getDevelopers().size() == 0 )
        {
            child.setDevelopers( parent.getDevelopers() );
        }

        // developers
        if ( child.getContributors().size() == 0 )
        {
            child.setContributors( parent.getContributors() );
        }

        // mailingLists
        if ( child.getMailingLists().size() == 0 )
        {
            child.setMailingLists( parent.getMailingLists() );
        }

        // Build
        assembleBuildInheritance( child, parent );

        // Dependencies :: aggregate
        List dependencies = parent.getDependencies();

        for ( Iterator iterator = dependencies.iterator(); iterator.hasNext(); )
        {
            Dependency dependency = (Dependency) iterator.next();

            child.addDependency( dependency );

        }

        // Repositories :: aggregate
        List parentRepositories = parent.getRepositories();

        List childRepositories = child.getRepositories();

        for ( Iterator iterator = parentRepositories.iterator(); iterator.hasNext(); )
        {
            Repository repository = (Repository) iterator.next();

            if ( !childRepositories.contains( repository ) )
            {
                child.addRepository( repository );
            }
        }

        // Plugins :: aggregate
        if ( parent.getBuild() != null && child.getBuild() != null )
        {
            List parentPlugins = parent.getBuild().getPlugins();

            List childPlugins = child.getBuild().getPlugins();

            for ( Iterator iterator = parentPlugins.iterator(); iterator.hasNext(); )
            {
                Plugin plugin = (Plugin) iterator.next();

                if ( !childPlugins.contains( plugin ) )
                {
                    child.getBuild().addPlugin( plugin );
                }
            }
        }

        // Reports :: aggregate
        if ( child.getReports() != null && parent.getReports() != null )
        {
            if ( child.getReports().getOutputDirectory() == null )
            {
                child.getReports().setOutputDirectory( parent.getReports().getOutputDirectory() );
            }

            List parentReports = parent.getReports().getPlugins();

            List childReports = child.getReports().getPlugins();

            for ( Iterator iterator = parentReports.iterator(); iterator.hasNext(); )
            {
                Plugin plugin = (Plugin) iterator.next();

                if ( !childReports.contains( plugin ) )
                {
                    child.getReports().addPlugin( plugin );
                }
            }
        }

        assembleDependencyManagementInheritance( child, parent );

        assemblePluginManagementInheritance( child, parent );

    }

    private void assemblePluginManagementInheritance( Model child, Model parent )
    {
        Build parentBuild = parent.getBuild();
        Build childBuild = parent.getBuild();

        if ( childBuild == null )
        {
            if ( parentBuild != null )
            {
                child.setBuild( parentBuild );
            }
            else
            {
                childBuild = new Build();
            }
        }
        else
        {
            PluginManagement parentPluginMgmt = parentBuild.getPluginManagement();

            PluginManagement childPluginMgmt = childBuild.getPluginManagement();

            if ( parentPluginMgmt != null )
            {
                if ( childPluginMgmt == null )
                {
                    childBuild.setPluginManagement( parentPluginMgmt );
                }
                else
                {
                    List childPlugins = childPluginMgmt.getPlugins();

                    Map mappedChildPlugins = new TreeMap();
                    for ( Iterator it = childPlugins.iterator(); it.hasNext(); )
                    {
                        Plugin plugin = (Plugin) it.next();
                        mappedChildPlugins.put( constructPluginKey( plugin ), plugin );
                    }

                    for ( Iterator it = parentPluginMgmt.getPlugins().iterator(); it.hasNext(); )
                    {
                        Plugin plugin = (Plugin) it.next();
                        if ( !mappedChildPlugins.containsKey( constructPluginKey( plugin ) ) )
                        {
                            childPluginMgmt.addPlugin( plugin );
                        }
                        else
                        {
                            Plugin childPlugin = (Plugin) mappedChildPlugins.get( constructPluginKey( plugin ) );

                            Map mappedChildGoals = new TreeMap();
                            for ( Iterator itGoals = childPlugin.getGoals().iterator(); itGoals.hasNext(); )
                            {
                                Goal goal = (Goal) itGoals.next();
                                mappedChildGoals.put( goal.getId(), goal );
                            }

                            for ( Iterator itGoals = plugin.getGoals().iterator(); itGoals.hasNext(); )
                            {
                                Goal parentGoal = (Goal) itGoals.next();
                                Goal childGoal = (Goal) mappedChildGoals.get( parentGoal.getId() );

                                if ( childGoal == null )
                                {
                                    childPlugin.addGoal( parentGoal );
                                }
                                else
                                {
                                    // TODO: configuration not currently merged
                                    if ( childGoal.getConfiguration() == null )
                                    {
                                        childGoal.setConfiguration( parentGoal.getConfiguration() );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String constructPluginKey( Plugin plugin )
    {
        return plugin.getGroupId() + ":" + plugin.getArtifactId();
    }

    private void assembleDependencyManagementInheritance( Model child, Model parent )
    {
        DependencyManagement parentDepMgmt = parent.getDependencyManagement();

        DependencyManagement childDepMgmt = child.getDependencyManagement();

        if ( parentDepMgmt != null )
        {
            if ( childDepMgmt == null )
            {
                child.setDependencyManagement( parentDepMgmt );
            }
            else
            {
                List childDeps = childDepMgmt.getDependencies();

                Map mappedChildDeps = new TreeMap();
                for ( Iterator it = childDeps.iterator(); it.hasNext(); )
                {
                    Dependency dep = (Dependency) it.next();
                    mappedChildDeps.put( dep.getManagementKey(), dep );
                }

                for ( Iterator it = parentDepMgmt.getDependencies().iterator(); it.hasNext(); )
                {
                    Dependency dep = (Dependency) it.next();
                    if ( !mappedChildDeps.containsKey( dep.getManagementKey() ) )
                    {
                        childDepMgmt.addDependency( dep );
                    }
                }
            }
        }
    }

    private void assembleBuildInheritance( Model child, Model parent )
    {
        Build childBuild = child.getBuild();
        Build parentBuild = parent.getBuild();

        if ( childBuild == null )
        {
            child.setBuild( parentBuild );
        }
        else
        {
            // The build has been set but we want to step in here and fill in
            // values
            // that have not been set by the child.

            if ( childBuild.getDirectory() == null )
            {
                childBuild.setDirectory( parentBuild.getDirectory() );
            }

            if ( childBuild.getSourceDirectory() == null )
            {
                childBuild.setSourceDirectory( parentBuild.getSourceDirectory() );
            }

            if ( childBuild.getScriptSourceDirectory() == null )
            {
                childBuild.setScriptSourceDirectory( parentBuild.getScriptSourceDirectory() );
            }

            if ( childBuild.getTestSourceDirectory() == null )
            {
                childBuild.setTestSourceDirectory( parentBuild.getTestSourceDirectory() );
            }

            if ( childBuild.getOutputDirectory() == null )
            {
                childBuild.setOutputDirectory( parentBuild.getOutputDirectory() );
            }

            if ( childBuild.getTestOutputDirectory() == null )
            {
                childBuild.setTestOutputDirectory( parentBuild.getTestOutputDirectory() );
            }

            if ( childBuild.getFinalName() == null )
            {
                childBuild.setFinalName( parentBuild.getFinalName() );
            }

            List resources = childBuild.getResources();
            if ( resources == null || resources.isEmpty() )
            {
                childBuild.setResources( parentBuild.getResources() );
            }

            resources = childBuild.getTestResources();
            if ( resources == null || resources.isEmpty() )
            {
                childBuild.setTestResources( parentBuild.getTestResources() );
            }
        }
    }

    private void assembleScmInheritance( Model child, Model parent )
    {
        if ( parent.getScm() != null )
        {
            Scm parentScm = parent.getScm();

            Scm childScm = child.getScm();

            if ( childScm == null )
            {
                childScm = new Scm();

                child.setScm( childScm );
            }

            if ( StringUtils.isEmpty( childScm.getConnection() ) && !StringUtils.isEmpty( parentScm.getConnection() ) )
            {
                childScm.setConnection( parentScm.getConnection() + "/" + child.getArtifactId() );
            }

            if ( StringUtils.isEmpty( childScm.getDeveloperConnection() ) &&
                !StringUtils.isEmpty( parentScm.getDeveloperConnection() ) )
            {
                childScm.setDeveloperConnection( parentScm.getDeveloperConnection() + "/" + child.getArtifactId() );
            }

            if ( StringUtils.isEmpty( childScm.getUrl() ) )
            {
                childScm.setUrl( parentScm.getUrl() );
            }
        }
    }

    private void assembleDistributionInheritence( Model child, Model parent )
    {
        if ( parent.getDistributionManagement() != null )
        {
            DistributionManagement parentDistMgmt = parent.getDistributionManagement();

            DistributionManagement childDistMgmt = child.getDistributionManagement();

            if ( childDistMgmt == null )
            {
                childDistMgmt = new DistributionManagement();

                child.setDistributionManagement( childDistMgmt );
            }

            if ( childDistMgmt.getSite() == null )
            {
                childDistMgmt.setSite( parentDistMgmt.getSite() );
            }

            if ( childDistMgmt.getRepository() == null )
            {
                childDistMgmt.setRepository( parentDistMgmt.getRepository() );
            }
        }
    }


}