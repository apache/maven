package org.apache.maven.tools.converter;

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

import org.apache.maven.model.Goal;
import org.apache.maven.model.Notifier;
import org.apache.maven.model.Reports;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.v4_0_0.Build;
import org.apache.maven.model.v4_0_0.CiManagement;
import org.apache.maven.model.v4_0_0.Contributor;
import org.apache.maven.model.v4_0_0.Dependency;
import org.apache.maven.model.v4_0_0.DependencyManagement;
import org.apache.maven.model.v4_0_0.Developer;
import org.apache.maven.model.v4_0_0.DistributionManagement;
import org.apache.maven.model.v4_0_0.IssueManagement;
import org.apache.maven.model.v4_0_0.License;
import org.apache.maven.model.v4_0_0.MailingList;
import org.apache.maven.model.v4_0_0.Model;
import org.apache.maven.model.v4_0_0.Organization;
import org.apache.maven.model.v4_0_0.Parent;
import org.apache.maven.model.v4_0_0.Plugin;
import org.apache.maven.model.v4_0_0.PluginManagement;
import org.apache.maven.model.v4_0_0.Repository;
import org.apache.maven.model.v4_0_0.Scm;
import org.apache.maven.model.v4_0_0.Site;
import org.apache.maven.model.v4_0_0.UnitTest;
import org.apache.maven.model.v4_0_0.Resource;
import org.apache.maven.model.v4_0_0.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class Main
{
    public static void main( String[] args )
        throws Exception
    {
        boolean reverse = false;
        if ( args.length > 0 && args[0].equals( "-reverse" ) )
        {
            reverse = true;
        }

        List files = FileUtils.getFiles( new File( System.getProperty( "user.dir" ) ), "**/pom.xml", "" );
        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            File file = (File) i.next();
            System.out.println( "Processing file: " + file );

            File backup = new File( file.getParent(), file.getName() + "~" );

            if ( reverse )
            {
                if ( !backup.exists() )
                {
                    System.err.println( "skipping... backup does not exist" );
                    continue;
                }
                FileUtils.copyFile( backup, file );
                backup.delete();
            }
            else
            {
                MavenXpp3Reader reader = new MavenXpp3Reader();
                MavenXpp3Writer writer = new MavenXpp3Writer();

                try
                {
                    if ( backup.exists() )
                    {
                        System.err.println( "skipping... backup exists" );
                        continue;
                    }

                    FileReader fileReader = new FileReader( file );
                    Model model = reader.read( fileReader );
                    fileReader.close();

                    org.apache.maven.model.Model newModel = new org.apache.maven.model.Model();
                    newModel.setArtifactId( model.getArtifactId() );
                    newModel.setBuild( convertBuild( model.getBuild(), convertPlugins( model.getPlugins() ) ) );
                    newModel.setCiManagement( convertCiManagement( model.getCiManagement() ) );
                    newModel.setContributors( convertContributors( model.getContributors() ) );
                    newModel.setDependencies( convertDependencies( model.getDependencies() ) );
                    newModel.setDependencyManagement( convertDependencyManagement( model.getDependencyManagement() ) );
                    newModel.setDescription( model.getDescription() );
                    newModel.setDevelopers( convertDevelopers( model.getDevelopers() ) );
                    newModel.setDistributionManagement(
                        convertDistributionManagement( model.getDistributionManagement() ) );
                    newModel.setExtend( model.getExtend() );
                    newModel.setGroupId( model.getGroupId() );
                    newModel.setInceptionYear( model.getInceptionYear() );
                    newModel.setIssueManagement( convertIssueManagement( model.getIssueManagement() ) );
                    newModel.setLicenses( convertLicenses( model.getLicenses() ) );
                    newModel.setMailingLists( convertMailingLists( model.getMailingLists() ) );
                    newModel.setModelVersion( model.getModelVersion() );
                    newModel.setName( model.getName() );
                    newModel.setOrganization( convertOrganization( model.getOrganization() ) );
                    newModel.setParent( convertParent( model.getParent() ) );
                    newModel.setPluginManagement( convertPluginManagement( model.getPluginManagement() ) );
                    newModel.setPluginRepositories( convertRepositories( model.getPluginRepositories() ) );
                    newModel.setReports( convertReports( model.getReports() ) );
                    newModel.setRepositories( convertRepositories( model.getRepositories() ) );
                    newModel.setScm( convertScm( model.getScm() ) );
                    newModel.setUrl( model.getUrl() );
                    newModel.setVersion( model.getVersion() );

                    FileUtils.copyFile( file, backup );

                    FileWriter fileWriter = new FileWriter( file );
                    writer.write( fileWriter, newModel );
                    fileWriter.close();
                }
                catch ( Exception e )
                {
                    System.err.println( "Skipping " + file );
                    if ( args.length > 0 && args[0].equals( "-X" ) )
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static org.apache.maven.model.Scm convertScm( Scm scm )
    {
        if ( scm == null )
        {
            return null;
        }

        org.apache.maven.model.Scm newScm = new org.apache.maven.model.Scm();

        newScm.setConnection( scm.getConnection() );
        newScm.setDeveloperConnection( scm.getDeveloperConnection() );
        newScm.setUrl( scm.getUrl() );

        return newScm;
    }

    private static Reports convertReports( List reports )
    {
        if ( reports.isEmpty() )
        {
            return null;
        }

        Reports newReports = new Reports();
        // newReports.setOutputDirectory( ); -- nothing needed

        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();

            org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();

            plugin.setArtifactId( name );

            newReports.addPlugin( plugin );
        }

        return newReports;
    }

    private static List convertRepositories( List repositories )
    {
        List newRepositorys = new ArrayList();

        for ( Iterator i = repositories.iterator(); i.hasNext(); )
        {
            newRepositorys.add( convertRepository( (Repository) i.next() ) );
        }
        return newRepositorys;
    }

    private static org.apache.maven.model.Repository convertRepository( Repository repository )
    {
        if ( repository == null )
        {
            return null;
        }

        org.apache.maven.model.Repository newRepository = new org.apache.maven.model.Repository();
        newRepository.setName( repository.getName() );
        newRepository.setId( repository.getId() );
        newRepository.setUrl( repository.getUrl() );
        return newRepository;
    }

    private static org.apache.maven.model.Parent convertParent( Parent parent )
    {
        if ( parent == null )
        {
            return null;
        }

        org.apache.maven.model.Parent newParent = new org.apache.maven.model.Parent();
        newParent.setArtifactId( parent.getArtifactId() );
        newParent.setGroupId( parent.getGroupId() );
        newParent.setVersion( parent.getVersion() );
        return newParent;
    }

    private static org.apache.maven.model.Organization convertOrganization( Organization organization )
    {
        if ( organization == null )
        {
            return null;
        }

        org.apache.maven.model.Organization newOrganization = new org.apache.maven.model.Organization();

        newOrganization.setName( organization.getName() );
        newOrganization.setUrl( organization.getUrl() );

        return newOrganization;
    }

    private static List convertMailingLists( List mailingLists )
    {
        List newMailinglists = new ArrayList();

        for ( Iterator i = mailingLists.iterator(); i.hasNext(); )
        {
            MailingList mailinglist = (MailingList) i.next();

            org.apache.maven.model.MailingList newMailinglist = new org.apache.maven.model.MailingList();
            newMailinglist.setName( mailinglist.getName() );
            newMailinglist.setArchive( mailinglist.getArchive() );
            newMailinglist.setOtherArchives( mailinglist.getOtherArchives() );
            newMailinglist.setPost( mailinglist.getPost() );
            newMailinglist.setSubscribe( mailinglist.getSubscribe() );
            newMailinglist.setUnsubscribe( mailinglist.getUnsubscribe() );

            newMailinglists.add( newMailinglist );
        }
        return newMailinglists;
    }

    private static List convertLicenses( List licenses )
    {
        List newLicenses = new ArrayList();

        for ( Iterator i = licenses.iterator(); i.hasNext(); )
        {
            License license = (License) i.next();

            org.apache.maven.model.License newLicense = new org.apache.maven.model.License();
            newLicense.setComments( license.getComments() );
            newLicense.setName( license.getName() );
            newLicense.setUrl( license.getUrl() );

            newLicenses.add( newLicense );
        }
        return newLicenses;
    }

    private static org.apache.maven.model.IssueManagement convertIssueManagement( IssueManagement issueManagement )
    {
        if ( issueManagement == null )
        {
            return null;
        }

        org.apache.maven.model.IssueManagement mgmt = new org.apache.maven.model.IssueManagement();

        mgmt.setSystem( issueManagement.getSystem() );
        mgmt.setUrl( issueManagement.getUrl() );

        return mgmt;
    }

    private static org.apache.maven.model.DistributionManagement convertDistributionManagement(
        DistributionManagement distributionManagement )
    {
        if ( distributionManagement == null )
        {
            return null;
        }

        org.apache.maven.model.DistributionManagement mgmt = new org.apache.maven.model.DistributionManagement();

        mgmt.setRepository( convertRepository( distributionManagement.getRepository() ) );
        mgmt.setSite( convertSite( distributionManagement.getSite() ) );

        return mgmt;
    }

    private static org.apache.maven.model.Site convertSite( Site site )
    {
        if ( site == null )
        {
            return null;
        }

        org.apache.maven.model.Site newSite = new org.apache.maven.model.Site();

        newSite.setId( site.getId() );
        newSite.setName( site.getName() );
        newSite.setUrl( site.getUrl() );

        return newSite;
    }

    private static org.apache.maven.model.DependencyManagement convertDependencyManagement(
        DependencyManagement dependencyManagement )
    {
        if ( dependencyManagement == null )
        {
            return null;
        }

        org.apache.maven.model.DependencyManagement mgmt = new org.apache.maven.model.DependencyManagement();

        mgmt.setDependencies( convertDependencies( dependencyManagement.getDependencies() ) );

        return mgmt;
    }

    private static org.apache.maven.model.PluginManagement convertPluginManagement( PluginManagement pluginManagement )
    {
        if ( pluginManagement == null )
        {
            return null;
        }

        org.apache.maven.model.PluginManagement mgmt = new org.apache.maven.model.PluginManagement();

        mgmt.setPlugins( new ArrayList( convertPlugins( pluginManagement.getPlugins() ).values() ) );

        return mgmt;
    }

    private static List convertDependencies( List dependencies )
    {
        List newDependencys = new ArrayList();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency dependency = (Dependency) i.next();

            org.apache.maven.model.Dependency newDependency = new org.apache.maven.model.Dependency();
            newDependency.setArtifactId( dependency.getArtifactId() );
            newDependency.setGroupId( dependency.getGroupId() );
            newDependency.setScope( dependency.getScope() );
            newDependency.setType( dependency.getType() );
            newDependency.setVersion( dependency.getVersion() );

            newDependencys.add( newDependency );
        }
        return newDependencys;
    }

    private static List convertContributors( List contributors )
    {
        List newContributors = new ArrayList();

        for ( Iterator i = contributors.iterator(); i.hasNext(); )
        {
            Contributor contributor = (Contributor) i.next();

            org.apache.maven.model.Contributor newContributor = new org.apache.maven.model.Contributor();
            newContributor.setEmail( contributor.getEmail() );
            newContributor.setName( contributor.getName() );
            newContributor.setOrganization( contributor.getOrganization() );
            newContributor.setTimezone( contributor.getTimezone() );
            newContributor.setRoles( contributor.getRoles() );
            newContributor.setUrl( contributor.getUrl() );

            newContributors.add( newContributor );
        }
        return newContributors;
    }

    private static List convertDevelopers( List developers )
    {
        List newDevelopers = new ArrayList();

        for ( Iterator i = developers.iterator(); i.hasNext(); )
        {
            Developer developer = (Developer) i.next();

            org.apache.maven.model.Developer newDeveloper = new org.apache.maven.model.Developer();
            newDeveloper.setEmail( developer.getEmail() );
            newDeveloper.setName( developer.getName() );
            newDeveloper.setOrganization( developer.getOrganization() );
            newDeveloper.setTimezone( developer.getTimezone() );
            newDeveloper.setRoles( developer.getRoles() );
            newDeveloper.setUrl( developer.getUrl() );
            newDeveloper.setId( developer.getId() );

            newDevelopers.add( newDeveloper );
        }
        return newDevelopers;
    }

    private static org.apache.maven.model.CiManagement convertCiManagement( CiManagement ciManagement )
    {
        if ( ciManagement == null )
        {
            return null;
        }

        org.apache.maven.model.CiManagement newCiManagement = new org.apache.maven.model.CiManagement();

        newCiManagement.setSystem( ciManagement.getNagEmailAddress() );
        newCiManagement.setUrl( ciManagement.getUrl() );
        if ( ciManagement.getNagEmailAddress() != null )
        {
            Notifier notifier = new Notifier();
            notifier.setAddress( ciManagement.getNagEmailAddress() );
            notifier.setType( "email" );
            newCiManagement.addNotifier( notifier );
        }

        return newCiManagement;
    }

    private static Map convertPlugins( List plugins )
    {
        Map newPlugins = new HashMap();

        for ( Iterator i = plugins.iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            org.apache.maven.model.Plugin newPlugin = new org.apache.maven.model.Plugin();
            newPlugin.setArtifactId( "maven-" + plugin.getId() + "-plugin" );
            newPlugin.setConfiguration( plugin.getConfiguration() );
            newPlugin.setDisabled( plugin.isDisabled() );
            newPlugin.setGoals( plugin.getGoals() );
            // newPlugin.setGroupId( "maven" );  -- nothing needed

            newPlugins.put( newPlugin.getArtifactId(), newPlugin );
        }
        return newPlugins;
    }

    private static org.apache.maven.model.Build convertBuild( Build build, Map plugins )
    {
        if ( build == null )
        {
            return null;
        }

        org.apache.maven.model.Build newBuild = new org.apache.maven.model.Build();

        newBuild.setDirectory( build.getDirectory() );
        newBuild.setFinalName( build.getFinalName() );
        newBuild.setOutputDirectory( build.getOutput() );
        newBuild.setPlugins( new ArrayList( plugins.values() ) );
        newBuild.setSourceDirectory( build.getSourceDirectory() );
        newBuild.setTestOutputDirectory( build.getTestOutput() );
        newBuild.setTestSourceDirectory( build.getUnitTestSourceDirectory() );
        newBuild.setResources( convertResources( build.getResources() ) );

        if ( build.getUnitTest() != null )
        {
            UnitTest unitTest = build.getUnitTest();
            org.apache.maven.model.Plugin plugin = getPlugin( plugins, "maven-compiler-plugin" );
            Goal goal = getGoal( plugin, "testCompile" );

            goal.getConfiguration().setProperty( "includes", convertPatternSet( unitTest.getIncludes() ) );
            goal.getConfiguration().setProperty( "excludes", convertPatternSet( unitTest.getExcludes() ) );

            newBuild.setTestResources( convertResources( unitTest.getResources() ) );
        }

        return newBuild;
    }

    private static List convertResources( List resources )
    {
        List newResources = new ArrayList();

        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();

            org.apache.maven.model.Resource newResource = new org.apache.maven.model.Resource();
            newResource.setDirectory( resource.getDirectory() );
            newResource.setIncludes( convertPatternSet( resource.getIncludes() ) );
            newResource.setExcludes( convertPatternSet( resource.getExcludes() ) );

            newResources.add( newResource );
        }
        return newResources;
    }

    private static String convertPatternSet( List list )
    {
        StringBuffer b = new StringBuffer();
        for ( Iterator i = list.iterator(); i.hasNext(); )
        {
            String pattern = (String) i.next();
            b.append( pattern );
            if ( i.hasNext() )
            {
                b.append( "," );
            }
        }
        return b.toString();
    }

    private static org.apache.maven.model.Plugin getPlugin( Map plugins, String artifactId )
    {
        org.apache.maven.model.Plugin plugin = (org.apache.maven.model.Plugin) plugins.get( artifactId );
        if ( plugin == null )
        {
            plugin = new org.apache.maven.model.Plugin();
            plugin.setArtifactId( artifactId );
            plugins.put( plugin.getArtifactId(), plugin );
        }
        return plugin;
    }

    private static Goal getGoal( org.apache.maven.model.Plugin plugin, String name )
    {
        Goal goal = null;
        for ( Iterator i = plugin.getGoals().iterator(); i.hasNext() && goal == null; )
        {
            Goal g = (Goal) i.next();
            if ( g.getId().equals( name ) )
            {
                goal = g;
            }
        }

        if ( goal == null )
        {
            goal = new Goal();
            goal.setId( name );
            plugin.addGoal( goal );
        }
        return goal;
    }
}
