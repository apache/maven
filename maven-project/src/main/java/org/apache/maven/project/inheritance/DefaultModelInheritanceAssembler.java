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
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.apache.maven.project.ModelUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    public void assembleModelInheritance( Model child, Model parent )
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
                child.setUrl( appendPath( parent.getUrl(), child.getArtifactId() ) );
            }
            else
            {
                child.setUrl( parent.getUrl() );
            }
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
        assembleBuildInheritance( child, parent.getBuild() );
        
        assembleDependencyInheritance( child, parent );

        child.setRepositories( ModelUtils.mergeRepositoryLists( child.getRepositories(), parent.getRepositories() ) );
        child.setPluginRepositories( ModelUtils.mergeRepositoryLists( child.getPluginRepositories(), parent.getPluginRepositories() ) );
        
        assembleReportingInheritance( child, parent );
        
        assembleDependencyManagementInheritance( child, parent );
        
        assembleDistributionManagementInheritance( child, parent );
    }

    private void assembleDistributionManagementInheritance( Model child, Model parent )
    {
        DistributionManagement cDistMgmt = child.getDistributionManagement();
        DistributionManagement pDistMgmt = parent.getDistributionManagement();
        
        if ( cDistMgmt == null )
        {
            child.setDistributionManagement( pDistMgmt );
        }
        else if ( pDistMgmt != null )
        {
            if ( cDistMgmt.getRepository() == null )
            {
                cDistMgmt.setRepository( pDistMgmt.getRepository() );
            }
            
            if ( cDistMgmt.getSnapshotRepository() == null )
            {
                cDistMgmt.setSnapshotRepository( pDistMgmt.getSnapshotRepository() );
            }
            
            if ( StringUtils.isEmpty( cDistMgmt.getDownloadUrl() ) )
            {
                cDistMgmt.setDownloadUrl( pDistMgmt.getDownloadUrl() );
            }
            
            if ( cDistMgmt.getRelocation() == null )
            {
                cDistMgmt.setRelocation( pDistMgmt.getRelocation() );
            }
            
            if ( cDistMgmt.getSite() == null )
            {
                cDistMgmt.setSite( pDistMgmt.getSite() );
            }
            
            // NOTE: We SHOULD NOT be inheriting status, since this is an assessment of the POM quality.
        }
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

        if ( childReporting != null && parentReporting != null )
        {
            if ( StringUtils.isEmpty( childReporting.getOutputDirectory() ) )
            {
                childReporting.setOutputDirectory( parentReporting.getOutputDirectory() );
            }

            Map mergedReportPlugins = new HashMap();

            Map childReportersByKey = childReporting.getReportPluginsAsMap();

            List parentReportPlugins = parentReporting.getPlugins();

            if ( parentReportPlugins != null )
            {
                for ( Iterator it = parentReportPlugins.iterator(); it.hasNext(); )
                {
                    ReportPlugin parentReportPlugin = (ReportPlugin) it.next();

                    String inherited = parentReportPlugin.getInherited();

                    if ( StringUtils.isEmpty( inherited ) || Boolean.valueOf( inherited ).booleanValue() )
                    {
                        ReportPlugin childReportPlugin = (ReportPlugin) childReportersByKey.get(
                            parentReportPlugin.getKey() );

                        ReportPlugin mergedReportPlugin = parentReportPlugin;

                        if ( childReportPlugin != null )
                        {
                            mergedReportPlugin = childReportPlugin;

                            mergeReportPlugins( childReportPlugin, parentReportPlugin );
                        }
                        else if ( StringUtils.isEmpty( inherited ) )
                        {
                            mergedReportPlugin.unsetInheritanceApplied();
                        }

                        mergedReportPlugins.put( mergedReportPlugin.getKey(), mergedReportPlugin );
                    }
                }
            }

            for ( Iterator it = childReportersByKey.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();

                String key = (String) entry.getKey();

                if ( !mergedReportPlugins.containsKey( key ) )
                {
                    mergedReportPlugins.put( key, entry.getValue() );
                }
            }

            childReporting.setPlugins( new ArrayList( mergedReportPlugins.values() ) );

            childReporting.flushReportPluginMap();
        }
    }

    private void mergeReportPlugins( ReportPlugin childReportPlugin, ReportPlugin parentReportPlugin )
    {
          if ( StringUtils.isEmpty( childReportPlugin.getVersion() ) )
          {
              childReportPlugin.setVersion( parentReportPlugin.getVersion() );
          }
  
          Xpp3Dom childConfig = (Xpp3Dom) childReportPlugin.getConfiguration();
          Xpp3Dom parentConfig = (Xpp3Dom) parentReportPlugin.getConfiguration();
  
          childReportPlugin.setConfiguration( Xpp3Dom.mergeXpp3Dom( childConfig, parentConfig ) );
  
          Map mergedReportSets = new HashMap();
  
          Map childReportSetsById = childReportPlugin.getReportSetsAsMap();
  
          for ( Iterator it = parentReportPlugin.getReportSets().iterator(); it.hasNext(); )
          {
              ReportSet parentReportSet = (ReportSet) it.next();
  
              String inherited = parentReportSet.getInherited();
  
              if ( StringUtils.isEmpty( inherited ) || Boolean.valueOf( inherited ).booleanValue() )
              {
                  ReportSet childReportSet = (ReportSet) childReportSetsById.get( parentReportSet.getId() );
  
                  ReportSet merged = parentReportSet;
  
                  if ( childReportSet != null )
                  {
                      merged = childReportSet;
  
                      Xpp3Dom parentRSConfig = (Xpp3Dom) parentReportSet.getConfiguration();
                      Xpp3Dom mergedRSConfig = (Xpp3Dom) merged.getConfiguration();
  
                      merged.setConfiguration( Xpp3Dom.mergeXpp3Dom( mergedRSConfig, parentRSConfig ) );
  
                      List mergedReports = merged.getReports();
  
                      if ( mergedReports == null )
                      {
                          mergedReports = new ArrayList();
  
                          merged.setReports( mergedReports );
                      }
  
                      List parentRSReports = parentReportSet.getReports();
  
                      if ( parentRSReports != null )
                      {
                          for ( Iterator reportIterator = parentRSReports.iterator(); reportIterator.hasNext(); )
                          {
                              String report = (String) reportIterator.next();
  
                              if ( !mergedReports.contains( report ) )
                              {
                                  mergedReports.add( report );
                              }
                          }
                      }
                  }
                  else if ( StringUtils.isEmpty( inherited ) )
                  {
                      merged.unsetInheritanceApplied();
                  }
  
                  mergedReportSets.put( merged.getId(), merged );
              }
          }
  
          for ( Iterator rsIterator = childReportSetsById.entrySet().iterator(); rsIterator.hasNext(); )
          {
              Map.Entry entry = (Map.Entry) rsIterator.next();
  
              String key = (String) entry.getKey();
  
              if ( !mergedReportSets.containsKey( key ) )
              {
                  mergedReportSets.put( key, entry.getValue() );
              }
          }
  
          childReportPlugin.setReportSets( new ArrayList( mergedReportSets.values() ) );
  
          childReportPlugin.flushReportSetMap();
    }

    private void assembleDependencyInheritance( Model child, Model parent )
    {
        Map depsMap = new HashMap();
        
        List deps = parent.getDependencies();
        
        if ( deps != null )
        {
            for ( Iterator it = deps.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }
        
        deps = child.getDependencies();
        
        if ( deps != null )
        {
            for ( Iterator it = deps.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }
        
        child.setDependencies( new ArrayList( depsMap.values() ) );
    }

    private void assembleBuildInheritance( Model child, Build parentBuild )
    {
        Build childBuild = child.getBuild();

        if ( parentBuild != null )
        {
            if ( childBuild == null )
            {
                childBuild = new Build();
                child.setBuild( childBuild );
            }

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
            mergeExtensionLists( childBuild, parentBuild );

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

            // Plugins are aggregated if Plugin.inherit != false
            ModelUtils.mergePluginLists( childBuild, parentBuild, true );

            // Plugin management :: aggregate
            PluginManagement dominantPM = childBuild.getPluginManagement();
            PluginManagement recessivePM = parentBuild.getPluginManagement();

            if ( dominantPM == null && recessivePM != null )
            {
                childBuild.setPluginManagement( recessivePM );
            }
            else
            {
                ModelUtils.mergePluginLists( childBuild.getPluginManagement(), parentBuild.getPluginManagement(),
                                             false );
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
                childScm.setConnection( appendPath( parentScm.getConnection(), child.getArtifactId() ) );
            }

            if ( StringUtils.isEmpty( childScm.getDeveloperConnection() ) &&
                !StringUtils.isEmpty( parentScm.getDeveloperConnection() ) )
            {
                childScm
                    .setDeveloperConnection( appendPath( parentScm.getDeveloperConnection(), child.getArtifactId() ) );
            }

            if ( StringUtils.isEmpty( childScm.getUrl() ) && !StringUtils.isEmpty( parentScm.getUrl() ) )
            {
                childScm.setUrl( appendPath( parentScm.getUrl(), child.getArtifactId() ) );
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
                if ( parentDistMgmt.getSite() != null )
                {
                    Site site = new Site();

                    childDistMgmt.setSite( site );

                    site.setId( parentDistMgmt.getSite().getId() );

                    site.setName( parentDistMgmt.getSite().getName() );

                    site.setUrl( parentDistMgmt.getSite().getUrl() );

                    if ( site.getUrl() != null )
                    {
                        site.setUrl( appendPath( site.getUrl(), child.getArtifactId() ) );
                    }
                }
            }

            if ( childDistMgmt.getRepository() == null )
            {
                if ( parentDistMgmt.getRepository() != null )
                {
                    Repository repository = new Repository();

                    childDistMgmt.setRepository( repository );

                    repository.setId( parentDistMgmt.getRepository().getId() );

                    repository.setName( parentDistMgmt.getRepository().getName() );

                    repository.setUrl( parentDistMgmt.getRepository().getUrl() );
                }
            }

            if ( childDistMgmt.getSnapshotRepository() == null )
            {
                if ( parentDistMgmt.getSnapshotRepository() != null )
                {
                    Repository repository = new Repository();

                    childDistMgmt.setSnapshotRepository( repository );

                    repository.setId( parentDistMgmt.getSnapshotRepository().getId() );

                    repository.setName( parentDistMgmt.getSnapshotRepository().getName() );

                    repository.setUrl( parentDistMgmt.getSnapshotRepository().getUrl() );
                }
            }
        }
    }

    private String appendPath( String url, String path )
    {
        if ( url.endsWith( "/" ) )
        {
            return url + path;
        }
        else
        {
            return url + "/" + path;
        }
    }

    private void mergeExtensionLists( Build childBuild, Build parentBuild )
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
}
