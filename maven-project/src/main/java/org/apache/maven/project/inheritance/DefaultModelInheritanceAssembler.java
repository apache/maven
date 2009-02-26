package org.apache.maven.project.inheritance;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.apache.maven.project.ModelUtils;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultModelInheritanceAssembler.java,v 1.4 2004/08/23 20:24:54
 *          jdcasey Exp $
 * @todo generate this with modello to keep it in sync with changes in the model.
 */
public class DefaultModelInheritanceAssembler
    implements ModelInheritanceAssembler
{
    public void copyModel( Model dest, Model source )
    {
        assembleModelInheritance( dest, source, null, false );
    }

    public void assembleModelInheritance( Model child, Model parent, String childPathAdjustment )
    {
        assembleModelInheritance( child, parent, childPathAdjustment, true );
    }

    public void assembleModelInheritance( Model child, Model parent )
    {
        assembleModelInheritance( child, parent, null, true );
    }

    private void assembleModelInheritance( Model child, Model parent, String childPathAdjustment, boolean appendPaths )
    {
        // cannot inherit from null parent.
        if ( parent == null )
        {
            return;
        }

        // Group id
        if ( child.getGroupId() == null )
        {
            child.setGroupId( parent.getGroupId() );
        }

        // version
        if ( child.getVersion() == null )
        {
            // The parent version may have resolved to something different, so we take what we asked for...
            // instead of - child.setVersion( parent.getVersion() );

            if ( child.getParent() != null )
            {
                child.setVersion( child.getParent().getVersion() );
            }
        }

        // inceptionYear
        if ( child.getInceptionYear() == null )
        {
            child.setInceptionYear( parent.getInceptionYear() );
        }

        // url
        if ( child.getUrl() == null )
        {
            if ( parent.getUrl() != null )
            {
                child.setUrl( appendPath( parent.getUrl(), child.getArtifactId(), childPathAdjustment, appendPaths ) );
            }
            else
            {
                child.setUrl( parent.getUrl() );
            }
        }

        assembleDistributionInheritence( child, parent, childPathAdjustment, appendPaths );

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
        assembleScmInheritance( child, parent, childPathAdjustment, appendPaths );

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

        // licenses
        if ( child.getLicenses().size() == 0 )
        {
            child.setLicenses( parent.getLicenses() );
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

        assembleDependencyInheritance( child, parent );

        child.setRepositories( ModelUtils.mergeRepositoryLists( child.getRepositories(), parent.getRepositories() ) );
        child.setPluginRepositories(
            ModelUtils.mergeRepositoryLists( child.getPluginRepositories(), parent.getPluginRepositories() ) );

        assembleReportingInheritance( child, parent );

        assembleDependencyManagementInheritance( child, parent );

        Properties props = new Properties();
        props.putAll( parent.getProperties() );
        props.putAll( child.getProperties() );

        child.setProperties( props );
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

    private void assembleReportingInheritance( Model child, Model parent )
    {
        // Reports :: aggregate
        Reporting childReporting = child.getReporting();
        Reporting parentReporting = parent.getReporting();

        if ( parentReporting != null )
        {
            if ( childReporting == null )
            {
                childReporting = new Reporting();
                child.setReporting( childReporting );
            }

            if ( childReporting.isExcludeDefaultsValue() == null )
            {
                childReporting.setExcludeDefaultsValue( parentReporting.isExcludeDefaultsValue() );
            }

            if ( StringUtils.isEmpty( childReporting.getOutputDirectory() ) )
            {
                childReporting.setOutputDirectory( parentReporting.getOutputDirectory() );
            }

            ModelUtils.mergeReportPluginLists( childReporting, parentReporting, true );
        }
    }

    private void assembleDependencyInheritance( Model child, Model parent )
    {
        child.setDependencies( ModelUtils.mergeDependencyList( child.getDependencies(), parent.getDependencies() ) );
    }

    private void assembleBuildInheritance( Model child, Model parent )
    {
        Build childBuild = child.getBuild();
        Build parentBuild = parent.getBuild();

        if ( parentBuild != null )
        {
            if ( childBuild == null )
            {
                childBuild = new Build();
                child.setBuild( childBuild );
            }

            assembleBuildInheritance( childBuild, parentBuild, true );
        }
    }

    public void assembleBuildInheritance( Build childBuild,
                                           Build parentBuild,
                                           boolean handleAsInheritance )
    {
        // The build has been set but we want to step in here and fill in
        // values that have not been set by the child.

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

        // Extensions are accumlated
        ModelUtils.mergeExtensionLists( childBuild, parentBuild );

        if ( childBuild.getDirectory() == null )
        {
            childBuild.setDirectory( parentBuild.getDirectory() );
        }

        if ( childBuild.getDefaultGoal() == null )
        {
            childBuild.setDefaultGoal( parentBuild.getDefaultGoal() );
        }

        if ( childBuild.getFinalName() == null )
        {
            childBuild.setFinalName( parentBuild.getFinalName() );
        }

        ModelUtils.mergeFilterLists( childBuild.getFilters(), parentBuild.getFilters() );

        List resources = childBuild.getResources();
        if ( ( resources == null ) || resources.isEmpty() )
        {
            childBuild.setResources( parentBuild.getResources() );
        }

        resources = childBuild.getTestResources();
        if ( ( resources == null ) || resources.isEmpty() )
        {
            childBuild.setTestResources( parentBuild.getTestResources() );
        }

        // Plugins are aggregated if Plugin.inherit != false
        ModelUtils.mergePluginLists( childBuild, parentBuild, handleAsInheritance );

        // Plugin management :: aggregate
        PluginManagement dominantPM = childBuild.getPluginManagement();
        PluginManagement recessivePM = parentBuild.getPluginManagement();

        if ( ( dominantPM == null ) && ( recessivePM != null ) )
        {
            // FIXME: Filter out the inherited == false stuff!
            childBuild.setPluginManagement( recessivePM );
        }
        else
        {
            ModelUtils.mergePluginLists( childBuild.getPluginManagement(), parentBuild.getPluginManagement(),
                                         false );
        }
    }

    private void assembleScmInheritance( Model child, Model parent, String childPathAdjustment, boolean appendPaths )
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
                childScm.setConnection(
                    appendPath( parentScm.getConnection(), child.getArtifactId(), childPathAdjustment, appendPaths ) );
            }

            if ( StringUtils.isEmpty( childScm.getDeveloperConnection() ) &&
                !StringUtils.isEmpty( parentScm.getDeveloperConnection() ) )
            {
                childScm
                    .setDeveloperConnection( appendPath( parentScm.getDeveloperConnection(), child.getArtifactId(),
                                                         childPathAdjustment, appendPaths ) );
            }

            if ( StringUtils.isEmpty( childScm.getUrl() ) && !StringUtils.isEmpty( parentScm.getUrl() ) )
            {
                childScm.setUrl(
                    appendPath( parentScm.getUrl(), child.getArtifactId(), childPathAdjustment, appendPaths ) );
            }
        }
    }

    private void assembleDistributionInheritence( Model child, Model parent, String childPathAdjustment, boolean appendPaths )
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
                if ( parentDistMgmt.getSite() != null )
                {
                    Site site = new Site();

                    childDistMgmt.setSite( site );

                    site.setId( parentDistMgmt.getSite().getId() );

                    site.setName( parentDistMgmt.getSite().getName() );

                    site.setUrl( parentDistMgmt.getSite().getUrl() );

                    if ( site.getUrl() != null )
                    {
                        site.setUrl(
                            appendPath( site.getUrl(), child.getArtifactId(), childPathAdjustment, appendPaths ) );
                    }
                }
            }

            if ( childDistMgmt.getRepository() == null )
            {
                if ( parentDistMgmt.getRepository() != null )
                {
                    DeploymentRepository repository = copyDistributionRepository( parentDistMgmt.getRepository() );
                    childDistMgmt.setRepository( repository );
                }
            }

            if ( childDistMgmt.getSnapshotRepository() == null )
            {
                if ( parentDistMgmt.getSnapshotRepository() != null )
                {
                    DeploymentRepository repository =
                        copyDistributionRepository( parentDistMgmt.getSnapshotRepository() );
                    childDistMgmt.setSnapshotRepository( repository );
                }
            }

            if ( StringUtils.isEmpty( childDistMgmt.getDownloadUrl() ) )
            {
                childDistMgmt.setDownloadUrl( parentDistMgmt.getDownloadUrl() );
            }

            // NOTE: We SHOULD NOT be inheriting status, since this is an assessment of the POM quality.
            // NOTE: We SHOULD NOT be inheriting relocation, since this relates to a single POM
        }
    }

    private static DeploymentRepository copyDistributionRepository( DeploymentRepository parentRepository )
    {
        DeploymentRepository repository = new DeploymentRepository();

        repository.setId( parentRepository.getId() );

        repository.setName( parentRepository.getName() );

        repository.setUrl( parentRepository.getUrl() );

        repository.setLayout( parentRepository.getLayout() );

        repository.setUniqueVersion( parentRepository.isUniqueVersion() );

        return repository;
    }

    // TODO: This should eventually be migrated to DefaultPathTranslator.
    protected String appendPath( String parentPath, String childPath, String pathAdjustment, boolean appendPaths )
    {
        String uncleanPath = parentPath;

        if ( appendPaths )
        {
            if ( pathAdjustment != null )
            {
                uncleanPath += "/" + pathAdjustment;
            }

            if ( childPath != null )
            {
                uncleanPath += "/" + childPath;
            }
        }

        String cleanedPath = "";

        int protocolIdx = uncleanPath.indexOf( "://" );

        if ( protocolIdx > -1 )
        {
            cleanedPath = uncleanPath.substring( 0, protocolIdx + 3 );
            uncleanPath = uncleanPath.substring( protocolIdx + 3 );
        }

        if ( uncleanPath.startsWith( "//" ) )
        {
            // preserve leading double slash for UNC paths like "file:////host/pom.xml"
            cleanedPath += "//";
        }
        else if ( uncleanPath.startsWith( "/" ) )
        {
            cleanedPath += "/";
        }

        return cleanedPath + resolvePath( uncleanPath );
    }

    // TODO: Move this to plexus-utils' PathTool.
    private static String resolvePath( String uncleanPath )
    {
        LinkedList pathElements = new LinkedList();

        StringTokenizer tokenizer = new StringTokenizer( uncleanPath, "/" );

        while ( tokenizer.hasMoreTokens() )
        {
            String token = tokenizer.nextToken();

            if ( token.equals( "" ) )
            {
                // Empty path entry ("...//.."), remove.
            }
            else if ( token.equals( ".." ) )
            {
                if ( pathElements.isEmpty() )
                {
                    // FIXME: somehow report to the user
                    // that there are too many '..' elements.
                    // For now, ignore the extra '..'.
                }
                else
                {
                    pathElements.removeLast();
                }
            }
            else
            {
                pathElements.addLast( token );
            }
        }


        StringBuffer cleanedPath = new StringBuffer();

        while ( !pathElements.isEmpty() )
        {
            cleanedPath.append( pathElements.removeFirst() );
            if ( !pathElements.isEmpty() )
            {
                cleanedPath.append( '/' );
            }
        }

        return cleanedPath.toString();
    }

}
