/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0.io.xpp3;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

import org.apache.maven.model.v4_0_0.Build;
import org.apache.maven.model.v4_0_0.CiManagement;
import org.apache.maven.model.v4_0_0.Contributor;
import org.apache.maven.model.v4_0_0.Dependency;
import org.apache.maven.model.v4_0_0.DependencyManagement;
import org.apache.maven.model.v4_0_0.Developer;
import org.apache.maven.model.v4_0_0.DistributionManagement;
import org.apache.maven.model.v4_0_0.FileSet;
import org.apache.maven.model.v4_0_0.Goal;
import org.apache.maven.model.v4_0_0.IssueManagement;
import org.apache.maven.model.v4_0_0.License;
import org.apache.maven.model.v4_0_0.MailingList;
import org.apache.maven.model.v4_0_0.Model;
import org.apache.maven.model.v4_0_0.Notifier;
import org.apache.maven.model.v4_0_0.Organization;
import org.apache.maven.model.v4_0_0.Parent;
import org.apache.maven.model.v4_0_0.PatternSet;
import org.apache.maven.model.v4_0_0.Plugin;
import org.apache.maven.model.v4_0_0.PluginManagement;
import org.apache.maven.model.v4_0_0.Reports;
import org.apache.maven.model.v4_0_0.Repository;
import org.apache.maven.model.v4_0_0.Resource;
import org.apache.maven.model.v4_0_0.Scm;
import org.apache.maven.model.v4_0_0.Site;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

import java.io.Writer;
import java.util.Iterator;

/**
 * Class MavenXpp3Writer.
 * 
 * @version $Revision$ $Date$
 */
public class MavenXpp3Writer
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field serializer
     */
    private org.codehaus.plexus.util.xml.pull.XmlSerializer serializer;

    /**
     * Field NAMESPACE
     */
    private String NAMESPACE;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method write
     * 
     * @param writer
     * @param model
     */
    public void write( Writer writer, Model model ) throws Exception
    {
        serializer = new MXSerializer();
        serializer.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  " );
        serializer.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n" );
        serializer.setOutput( writer );
        writeModel( model, "model", serializer );
    } //-- void write(Writer, Model) 

    /**
     * Method writeBuild
     * 
     * @param build
     * @param serializer
     * @param tagName
     */
    private void writeBuild( Build build, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( build != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( build.getSourceDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "sourceDirectory" ).text( build.getSourceDirectory() )
                          .endTag( NAMESPACE, "sourceDirectory" );
            }
            if ( build.getScriptSourceDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "scriptSourceDirectory" ).text( build.getScriptSourceDirectory() )
                          .endTag( NAMESPACE, "scriptSourceDirectory" );
            }
            if ( build.getTestSourceDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "testSourceDirectory" ).text( build.getTestSourceDirectory() )
                          .endTag( NAMESPACE, "testSourceDirectory" );
            }
            if ( build.getResources() != null && build.getResources().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "resources" );
                for ( Iterator iter = build.getResources().iterator(); iter.hasNext(); )
                {
                    Resource resource = (Resource) iter.next();
                    writeResource( resource, "resource", serializer );
                }
                serializer.endTag( NAMESPACE, "resources" );
            }
            if ( build.getTestResources() != null && build.getTestResources().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "testResources" );
                for ( Iterator iter = build.getTestResources().iterator(); iter.hasNext(); )
                {
                    Resource resource = (Resource) iter.next();
                    writeResource( resource, "testResource", serializer );
                }
                serializer.endTag( NAMESPACE, "testResources" );
            }
            if ( build.getDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "directory" ).text( build.getDirectory() ).endTag( NAMESPACE,
                                                                                                   "directory" );
            }
            if ( build.getOutputDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "outputDirectory" ).text( build.getOutputDirectory() )
                          .endTag( NAMESPACE, "outputDirectory" );
            }
            if ( build.getFinalName() != null )
            {
                serializer.startTag( NAMESPACE, "finalName" ).text( build.getFinalName() ).endTag( NAMESPACE,
                                                                                                   "finalName" );
            }
            if ( build.getTestOutputDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "testOutputDirectory" ).text( build.getTestOutputDirectory() )
                          .endTag( NAMESPACE, "testOutputDirectory" );
            }
            if ( build.getPlugins() != null && build.getPlugins().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "plugins" );
                for ( Iterator iter = build.getPlugins().iterator(); iter.hasNext(); )
                {
                    Plugin plugin = (Plugin) iter.next();
                    writePlugin( plugin, "plugin", serializer );
                }
                serializer.endTag( NAMESPACE, "plugins" );
            }
            if ( build.getPluginManagement() != null )
            {
                writePluginManagement( build.getPluginManagement(), "pluginManagement", serializer );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeBuild(Build, String, XmlSerializer) 

    /**
     * Method writeCiManagement
     * 
     * @param ciManagement
     * @param serializer
     * @param tagName
     */
    private void writeCiManagement( CiManagement ciManagement, String tagName, XmlSerializer serializer )
        throws Exception
    {
        if ( ciManagement != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( ciManagement.getSystem() != null )
            {
                serializer.startTag( NAMESPACE, "system" ).text( ciManagement.getSystem() )
                          .endTag( NAMESPACE, "system" );
            }
            if ( ciManagement.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( ciManagement.getUrl() ).endTag( NAMESPACE, "url" );
            }
            if ( ciManagement.getNotifiers() != null && ciManagement.getNotifiers().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "notifiers" );
                for ( Iterator iter = ciManagement.getNotifiers().iterator(); iter.hasNext(); )
                {
                    Notifier notifier = (Notifier) iter.next();
                    writeNotifier( notifier, "notifier", serializer );
                }
                serializer.endTag( NAMESPACE, "notifiers" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeCiManagement(CiManagement, String, XmlSerializer) 

    /**
     * Method writeContributor
     * 
     * @param contributor
     * @param serializer
     * @param tagName
     */
    private void writeContributor( Contributor contributor, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( contributor != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( contributor.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( contributor.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( contributor.getEmail() != null )
            {
                serializer.startTag( NAMESPACE, "email" ).text( contributor.getEmail() ).endTag( NAMESPACE, "email" );
            }
            if ( contributor.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( contributor.getUrl() ).endTag( NAMESPACE, "url" );
            }
            if ( contributor.getOrganization() != null )
            {
                serializer.startTag( NAMESPACE, "organization" ).text( contributor.getOrganization() )
                          .endTag( NAMESPACE, "organization" );
            }
            if ( contributor.getRoles() != null && contributor.getRoles().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "roles" );
                for ( Iterator iter = contributor.getRoles().iterator(); iter.hasNext(); )
                {
                    String role = (String) iter.next();
                    serializer.startTag( NAMESPACE, "role" ).text( role ).endTag( NAMESPACE, "role" );
                }
                serializer.endTag( NAMESPACE, "roles" );
            }
            if ( contributor.getTimezone() != null )
            {
                serializer.startTag( NAMESPACE, "timezone" ).text( contributor.getTimezone() ).endTag( NAMESPACE,
                                                                                                       "timezone" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeContributor(Contributor, String, XmlSerializer) 

    /**
     * Method writeDependency
     * 
     * @param dependency
     * @param serializer
     * @param tagName
     */
    private void writeDependency( Dependency dependency, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( dependency != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( dependency.getGroupId() != null )
            {
                serializer.startTag( NAMESPACE, "groupId" ).text( dependency.getGroupId() ).endTag( NAMESPACE,
                                                                                                    "groupId" );
            }
            if ( dependency.getArtifactId() != null )
            {
                serializer.startTag( NAMESPACE, "artifactId" ).text( dependency.getArtifactId() ).endTag( NAMESPACE,
                                                                                                          "artifactId" );
            }
            if ( dependency.getVersion() != null )
            {
                serializer.startTag( NAMESPACE, "version" ).text( dependency.getVersion() ).endTag( NAMESPACE,
                                                                                                    "version" );
            }
            if ( dependency.getType() != null )
            {
                serializer.startTag( NAMESPACE, "type" ).text( dependency.getType() ).endTag( NAMESPACE, "type" );
            }
            if ( dependency.getScope() != null )
            {
                serializer.startTag( NAMESPACE, "scope" ).text( dependency.getScope() ).endTag( NAMESPACE, "scope" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeDependency(Dependency, String, XmlSerializer) 

    /**
     * Method writeDependencyManagement
     * 
     * @param dependencyManagement
     * @param serializer
     * @param tagName
     */
    private void writeDependencyManagement( DependencyManagement dependencyManagement, String tagName,
                                           XmlSerializer serializer ) throws Exception
    {
        if ( dependencyManagement != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( dependencyManagement.getDependencies() != null && dependencyManagement.getDependencies().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "dependencies" );
                for ( Iterator iter = dependencyManagement.getDependencies().iterator(); iter.hasNext(); )
                {
                    Dependency dependency = (Dependency) iter.next();
                    writeDependency( dependency, "dependency", serializer );
                }
                serializer.endTag( NAMESPACE, "dependencies" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeDependencyManagement(DependencyManagement, String, XmlSerializer) 

    /**
     * Method writeDeveloper
     * 
     * @param developer
     * @param serializer
     * @param tagName
     */
    private void writeDeveloper( Developer developer, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( developer != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( developer.getId() != null )
            {
                serializer.startTag( NAMESPACE, "id" ).text( developer.getId() ).endTag( NAMESPACE, "id" );
            }
            if ( developer.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( developer.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( developer.getEmail() != null )
            {
                serializer.startTag( NAMESPACE, "email" ).text( developer.getEmail() ).endTag( NAMESPACE, "email" );
            }
            if ( developer.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( developer.getUrl() ).endTag( NAMESPACE, "url" );
            }
            if ( developer.getOrganization() != null )
            {
                serializer.startTag( NAMESPACE, "organization" ).text( developer.getOrganization() )
                          .endTag( NAMESPACE, "organization" );
            }
            if ( developer.getRoles() != null && developer.getRoles().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "roles" );
                for ( Iterator iter = developer.getRoles().iterator(); iter.hasNext(); )
                {
                    String role = (String) iter.next();
                    serializer.startTag( NAMESPACE, "role" ).text( role ).endTag( NAMESPACE, "role" );
                }
                serializer.endTag( NAMESPACE, "roles" );
            }
            if ( developer.getTimezone() != null )
            {
                serializer.startTag( NAMESPACE, "timezone" ).text( developer.getTimezone() ).endTag( NAMESPACE,
                                                                                                     "timezone" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeDeveloper(Developer, String, XmlSerializer) 

    /**
     * Method writeDistributionManagement
     * 
     * @param distributionManagement
     * @param serializer
     * @param tagName
     */
    private void writeDistributionManagement( DistributionManagement distributionManagement, String tagName,
                                             XmlSerializer serializer ) throws Exception
    {
        if ( distributionManagement != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( distributionManagement.getRepository() != null )
            {
                writeRepository( distributionManagement.getRepository(), "repository", serializer );
            }
            if ( distributionManagement.getSite() != null )
            {
                writeSite( distributionManagement.getSite(), "site", serializer );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeDistributionManagement(DistributionManagement, String, XmlSerializer) 

    /**
     * Method writeFileSet
     * 
     * @param fileSet
     * @param serializer
     * @param tagName
     */
    private void writeFileSet( FileSet fileSet, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( fileSet != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( fileSet.getDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "directory" ).text( fileSet.getDirectory() ).endTag( NAMESPACE,
                                                                                                     "directory" );
            }
            if ( fileSet.getIncludes() != null )
            {
                serializer.startTag( NAMESPACE, "includes" ).text( fileSet.getIncludes() ).endTag( NAMESPACE,
                                                                                                   "includes" );
            }
            if ( fileSet.getExcludes() != null )
            {
                serializer.startTag( NAMESPACE, "excludes" ).text( fileSet.getExcludes() ).endTag( NAMESPACE,
                                                                                                   "excludes" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeFileSet(FileSet, String, XmlSerializer) 

    /**
     * Method writeGoal
     * 
     * @param goal
     * @param serializer
     * @param tagName
     */
    private void writeGoal( Goal goal, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( goal != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( goal.getId() != null )
            {
                serializer.startTag( NAMESPACE, "id" ).text( goal.getId() ).endTag( NAMESPACE, "id" );
            }
            if ( goal.isDisabled() != null )
            {
                serializer.startTag( NAMESPACE, "disabled" ).text( String.valueOf( goal.isDisabled() ) )
                          .endTag( NAMESPACE, "disabled" );
            }
            if ( goal.getConfiguration() != null && goal.getConfiguration().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "configuration" );
                for ( Iterator iter = goal.getConfiguration().keySet().iterator(); iter.hasNext(); )
                {
                    String key = (String) iter.next();
                    String value = (String) goal.getConfiguration().get( key );
                    serializer.startTag( NAMESPACE, "" + key + "" ).text( value ).endTag( NAMESPACE, "" + key + "" );
                }
                serializer.endTag( NAMESPACE, "configuration" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeGoal(Goal, String, XmlSerializer) 

    /**
     * Method writeIssueManagement
     * 
     * @param issueManagement
     * @param serializer
     * @param tagName
     */
    private void writeIssueManagement( IssueManagement issueManagement, String tagName, XmlSerializer serializer )
        throws Exception
    {
        if ( issueManagement != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( issueManagement.getSystem() != null )
            {
                serializer.startTag( NAMESPACE, "system" ).text( issueManagement.getSystem() ).endTag( NAMESPACE,
                                                                                                       "system" );
            }
            if ( issueManagement.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( issueManagement.getUrl() ).endTag( NAMESPACE, "url" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeIssueManagement(IssueManagement, String, XmlSerializer) 

    /**
     * Method writeLicense
     * 
     * @param license
     * @param serializer
     * @param tagName
     */
    private void writeLicense( License license, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( license != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( license.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( license.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( license.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( license.getUrl() ).endTag( NAMESPACE, "url" );
            }
            if ( license.getComments() != null )
            {
                serializer.startTag( NAMESPACE, "comments" ).text( license.getComments() ).endTag( NAMESPACE,
                                                                                                   "comments" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeLicense(License, String, XmlSerializer) 

    /**
     * Method writeMailingList
     * 
     * @param mailingList
     * @param serializer
     * @param tagName
     */
    private void writeMailingList( MailingList mailingList, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( mailingList != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( mailingList.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( mailingList.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( mailingList.getSubscribe() != null )
            {
                serializer.startTag( NAMESPACE, "subscribe" ).text( mailingList.getSubscribe() ).endTag( NAMESPACE,
                                                                                                         "subscribe" );
            }
            if ( mailingList.getUnsubscribe() != null )
            {
                serializer.startTag( NAMESPACE, "unsubscribe" ).text( mailingList.getUnsubscribe() )
                          .endTag( NAMESPACE, "unsubscribe" );
            }
            if ( mailingList.getPost() != null )
            {
                serializer.startTag( NAMESPACE, "post" ).text( mailingList.getPost() ).endTag( NAMESPACE, "post" );
            }
            if ( mailingList.getArchive() != null )
            {
                serializer.startTag( NAMESPACE, "archive" ).text( mailingList.getArchive() ).endTag( NAMESPACE,
                                                                                                     "archive" );
            }
            if ( mailingList.getOtherArchives() != null && mailingList.getOtherArchives().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "otherArchives" );
                for ( Iterator iter = mailingList.getOtherArchives().iterator(); iter.hasNext(); )
                {
                    String otherArchive = (String) iter.next();
                    serializer.startTag( NAMESPACE, "otherArchive" ).text( otherArchive ).endTag( NAMESPACE,
                                                                                                  "otherArchive" );
                }
                serializer.endTag( NAMESPACE, "otherArchives" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeMailingList(MailingList, String, XmlSerializer) 

    /**
     * Method writeModel
     * 
     * @param model
     * @param serializer
     * @param tagName
     */
    private void writeModel( Model model, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( model != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( model.getExtend() != null )
            {
                serializer.startTag( NAMESPACE, "extend" ).text( model.getExtend() ).endTag( NAMESPACE, "extend" );
            }
            if ( model.getParent() != null )
            {
                writeParent( model.getParent(), "parent", serializer );
            }
            if ( model.getModelVersion() != null )
            {
                serializer.startTag( NAMESPACE, "modelVersion" ).text( model.getModelVersion() )
                          .endTag( NAMESPACE, "modelVersion" );
            }
            if ( model.getGroupId() != null )
            {
                serializer.startTag( NAMESPACE, "groupId" ).text( model.getGroupId() ).endTag( NAMESPACE, "groupId" );
            }
            if ( model.getArtifactId() != null )
            {
                serializer.startTag( NAMESPACE, "artifactId" ).text( model.getArtifactId() ).endTag( NAMESPACE,
                                                                                                     "artifactId" );
            }
            if ( model.getPackaging() != null )
            {
                serializer.startTag( NAMESPACE, "packaging" ).text( model.getPackaging() ).endTag( NAMESPACE,
                                                                                                   "packaging" );
            }
            if ( model.getModules() != null && model.getModules().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "modules" );
                for ( Iterator iter = model.getModules().iterator(); iter.hasNext(); )
                {
                    String module = (String) iter.next();
                    serializer.startTag( NAMESPACE, "module" ).text( module ).endTag( NAMESPACE, "module" );
                }
                serializer.endTag( NAMESPACE, "modules" );
            }
            if ( model.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( model.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( model.getVersion() != null )
            {
                serializer.startTag( NAMESPACE, "version" ).text( model.getVersion() ).endTag( NAMESPACE, "version" );
            }
            if ( model.getDescription() != null )
            {
                serializer.startTag( NAMESPACE, "description" ).text( model.getDescription() ).endTag( NAMESPACE,
                                                                                                       "description" );
            }
            if ( model.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( model.getUrl() ).endTag( NAMESPACE, "url" );
            }
            if ( model.getIssueManagement() != null )
            {
                writeIssueManagement( model.getIssueManagement(), "issueManagement", serializer );
            }
            if ( model.getCiManagement() != null )
            {
                writeCiManagement( model.getCiManagement(), "ciManagement", serializer );
            }
            if ( model.getInceptionYear() != null )
            {
                serializer.startTag( NAMESPACE, "inceptionYear" ).text( model.getInceptionYear() )
                          .endTag( NAMESPACE, "inceptionYear" );
            }
            if ( model.getRepositories() != null && model.getRepositories().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "repositories" );
                for ( Iterator iter = model.getRepositories().iterator(); iter.hasNext(); )
                {
                    Repository repository = (Repository) iter.next();
                    writeRepository( repository, "repository", serializer );
                }
                serializer.endTag( NAMESPACE, "repositories" );
            }
            if ( model.getPluginRepositories() != null && model.getPluginRepositories().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "pluginRepositories" );
                for ( Iterator iter = model.getPluginRepositories().iterator(); iter.hasNext(); )
                {
                    Repository repository = (Repository) iter.next();
                    writeRepository( repository, "pluginRepository", serializer );
                }
                serializer.endTag( NAMESPACE, "pluginRepositories" );
            }
            if ( model.getMailingLists() != null && model.getMailingLists().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "mailingLists" );
                for ( Iterator iter = model.getMailingLists().iterator(); iter.hasNext(); )
                {
                    MailingList mailingList = (MailingList) iter.next();
                    writeMailingList( mailingList, "mailingList", serializer );
                }
                serializer.endTag( NAMESPACE, "mailingLists" );
            }
            if ( model.getDevelopers() != null && model.getDevelopers().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "developers" );
                for ( Iterator iter = model.getDevelopers().iterator(); iter.hasNext(); )
                {
                    Developer developer = (Developer) iter.next();
                    writeDeveloper( developer, "developer", serializer );
                }
                serializer.endTag( NAMESPACE, "developers" );
            }
            if ( model.getContributors() != null && model.getContributors().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "contributors" );
                for ( Iterator iter = model.getContributors().iterator(); iter.hasNext(); )
                {
                    Contributor contributor = (Contributor) iter.next();
                    writeContributor( contributor, "contributor", serializer );
                }
                serializer.endTag( NAMESPACE, "contributors" );
            }
            if ( model.getDependencies() != null && model.getDependencies().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "dependencies" );
                for ( Iterator iter = model.getDependencies().iterator(); iter.hasNext(); )
                {
                    Dependency dependency = (Dependency) iter.next();
                    writeDependency( dependency, "dependency", serializer );
                }
                serializer.endTag( NAMESPACE, "dependencies" );
            }
            if ( model.getLicenses() != null && model.getLicenses().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "licenses" );
                for ( Iterator iter = model.getLicenses().iterator(); iter.hasNext(); )
                {
                    License license = (License) iter.next();
                    writeLicense( license, "license", serializer );
                }
                serializer.endTag( NAMESPACE, "licenses" );
            }
            if ( model.getReports() != null )
            {
                writeReports( model.getReports(), "reports", serializer );
            }
            if ( model.getScm() != null )
            {
                writeScm( model.getScm(), "scm", serializer );
            }
            if ( model.getBuild() != null )
            {
                writeBuild( model.getBuild(), "build", serializer );
            }
            if ( model.getOrganization() != null )
            {
                writeOrganization( model.getOrganization(), "organization", serializer );
            }
            if ( model.getDistributionManagement() != null )
            {
                writeDistributionManagement( model.getDistributionManagement(), "distributionManagement", serializer );
            }
            if ( model.getDependencyManagement() != null )
            {
                writeDependencyManagement( model.getDependencyManagement(), "dependencyManagement", serializer );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeModel(Model, String, XmlSerializer) 

    /**
     * Method writeNotifier
     * 
     * @param notifier
     * @param serializer
     * @param tagName
     */
    private void writeNotifier( Notifier notifier, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( notifier != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( notifier.getType() != null )
            {
                serializer.startTag( NAMESPACE, "type" ).text( notifier.getType() ).endTag( NAMESPACE, "type" );
            }
            if ( notifier.getAddress() != null )
            {
                serializer.startTag( NAMESPACE, "address" ).text( notifier.getAddress() ).endTag( NAMESPACE, "address" );
            }
            if ( notifier.getConfiguration() != null && notifier.getConfiguration().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "configuration" );
                for ( Iterator iter = notifier.getConfiguration().keySet().iterator(); iter.hasNext(); )
                {
                    String key = (String) iter.next();
                    String value = (String) notifier.getConfiguration().get( key );
                    serializer.startTag( NAMESPACE, "" + key + "" ).text( value ).endTag( NAMESPACE, "" + key + "" );
                }
                serializer.endTag( NAMESPACE, "configuration" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeNotifier(Notifier, String, XmlSerializer) 

    /**
     * Method writeOrganization
     * 
     * @param organization
     * @param serializer
     * @param tagName
     */
    private void writeOrganization( Organization organization, String tagName, XmlSerializer serializer )
        throws Exception
    {
        if ( organization != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( organization.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( organization.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( organization.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( organization.getUrl() ).endTag( NAMESPACE, "url" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeOrganization(Organization, String, XmlSerializer) 

    /**
     * Method writeParent
     * 
     * @param parent
     * @param serializer
     * @param tagName
     */
    private void writeParent( Parent parent, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( parent != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( parent.getArtifactId() != null )
            {
                serializer.startTag( NAMESPACE, "artifactId" ).text( parent.getArtifactId() ).endTag( NAMESPACE,
                                                                                                      "artifactId" );
            }
            if ( parent.getGroupId() != null )
            {
                serializer.startTag( NAMESPACE, "groupId" ).text( parent.getGroupId() ).endTag( NAMESPACE, "groupId" );
            }
            if ( parent.getVersion() != null )
            {
                serializer.startTag( NAMESPACE, "version" ).text( parent.getVersion() ).endTag( NAMESPACE, "version" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeParent(Parent, String, XmlSerializer) 

    /**
     * Method writePatternSet
     * 
     * @param patternSet
     * @param serializer
     * @param tagName
     */
    private void writePatternSet( PatternSet patternSet, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( patternSet != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( patternSet.getIncludes() != null )
            {
                serializer.startTag( NAMESPACE, "includes" ).text( patternSet.getIncludes() ).endTag( NAMESPACE,
                                                                                                      "includes" );
            }
            if ( patternSet.getExcludes() != null )
            {
                serializer.startTag( NAMESPACE, "excludes" ).text( patternSet.getExcludes() ).endTag( NAMESPACE,
                                                                                                      "excludes" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writePatternSet(PatternSet, String, XmlSerializer) 

    /**
     * Method writePlugin
     * 
     * @param plugin
     * @param serializer
     * @param tagName
     */
    private void writePlugin( Plugin plugin, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( plugin != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( plugin.getGroupId() != null )
            {
                serializer.startTag( NAMESPACE, "groupId" ).text( plugin.getGroupId() ).endTag( NAMESPACE, "groupId" );
            }
            if ( plugin.getArtifactId() != null )
            {
                serializer.startTag( NAMESPACE, "artifactId" ).text( plugin.getArtifactId() ).endTag( NAMESPACE,
                                                                                                      "artifactId" );
            }
            if ( plugin.getVersion() != null )
            {
                serializer.startTag( NAMESPACE, "version" ).text( plugin.getVersion() ).endTag( NAMESPACE, "version" );
            }
            if ( plugin.isDisabled() != null )
            {
                serializer.startTag( NAMESPACE, "disabled" ).text( String.valueOf( plugin.isDisabled() ) )
                          .endTag( NAMESPACE, "disabled" );
            }
            if ( plugin.getConfiguration() != null && plugin.getConfiguration().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "configuration" );
                for ( Iterator iter = plugin.getConfiguration().keySet().iterator(); iter.hasNext(); )
                {
                    String key = (String) iter.next();
                    String value = (String) plugin.getConfiguration().get( key );
                    serializer.startTag( NAMESPACE, "" + key + "" ).text( value ).endTag( NAMESPACE, "" + key + "" );
                }
                serializer.endTag( NAMESPACE, "configuration" );
            }
            if ( plugin.getGoals() != null && plugin.getGoals().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "goals" );
                for ( Iterator iter = plugin.getGoals().iterator(); iter.hasNext(); )
                {
                    Goal goal = (Goal) iter.next();
                    writeGoal( goal, "goal", serializer );
                }
                serializer.endTag( NAMESPACE, "goals" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writePlugin(Plugin, String, XmlSerializer) 

    /**
     * Method writePluginManagement
     * 
     * @param pluginManagement
     * @param serializer
     * @param tagName
     */
    private void writePluginManagement( PluginManagement pluginManagement, String tagName, XmlSerializer serializer )
        throws Exception
    {
        if ( pluginManagement != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( pluginManagement.getPlugins() != null && pluginManagement.getPlugins().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "plugins" );
                for ( Iterator iter = pluginManagement.getPlugins().iterator(); iter.hasNext(); )
                {
                    Plugin plugin = (Plugin) iter.next();
                    writePlugin( plugin, "plugin", serializer );
                }
                serializer.endTag( NAMESPACE, "plugins" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writePluginManagement(PluginManagement, String, XmlSerializer) 

    /**
     * Method writeReports
     * 
     * @param reports
     * @param serializer
     * @param tagName
     */
    private void writeReports( Reports reports, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( reports != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( reports.getOutputDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "outputDirectory" ).text( reports.getOutputDirectory() )
                          .endTag( NAMESPACE, "outputDirectory" );
            }
            if ( reports.getPlugins() != null && reports.getPlugins().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "plugins" );
                for ( Iterator iter = reports.getPlugins().iterator(); iter.hasNext(); )
                {
                    Plugin plugin = (Plugin) iter.next();
                    writePlugin( plugin, "plugin", serializer );
                }
                serializer.endTag( NAMESPACE, "plugins" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeReports(Reports, String, XmlSerializer) 

    /**
     * Method writeRepository
     * 
     * @param repository
     * @param serializer
     * @param tagName
     */
    private void writeRepository( Repository repository, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( repository != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( repository.getId() != null )
            {
                serializer.startTag( NAMESPACE, "id" ).text( repository.getId() ).endTag( NAMESPACE, "id" );
            }
            if ( repository.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( repository.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( repository.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( repository.getUrl() ).endTag( NAMESPACE, "url" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeRepository(Repository, String, XmlSerializer) 

    /**
     * Method writeResource
     * 
     * @param resource
     * @param serializer
     * @param tagName
     */
    private void writeResource( Resource resource, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( resource != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( resource.getTargetPath() != null )
            {
                serializer.startTag( NAMESPACE, "targetPath" ).text( resource.getTargetPath() ).endTag( NAMESPACE,
                                                                                                        "targetPath" );
            }
            if ( resource.getDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "directory" ).text( resource.getDirectory() ).endTag( NAMESPACE,
                                                                                                      "directory" );
            }
            if ( resource.getIncludes() != null )
            {
                serializer.startTag( NAMESPACE, "includes" ).text( resource.getIncludes() ).endTag( NAMESPACE,
                                                                                                    "includes" );
            }
            if ( resource.getExcludes() != null )
            {
                serializer.startTag( NAMESPACE, "excludes" ).text( resource.getExcludes() ).endTag( NAMESPACE,
                                                                                                    "excludes" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeResource(Resource, String, XmlSerializer) 

    /**
     * Method writeScm
     * 
     * @param scm
     * @param serializer
     * @param tagName
     */
    private void writeScm( Scm scm, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( scm != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( scm.getConnection() != null )
            {
                serializer.startTag( NAMESPACE, "connection" ).text( scm.getConnection() ).endTag( NAMESPACE,
                                                                                                   "connection" );
            }
            if ( scm.getDeveloperConnection() != null )
            {
                serializer.startTag( NAMESPACE, "developerConnection" ).text( scm.getDeveloperConnection() )
                          .endTag( NAMESPACE, "developerConnection" );
            }
            if ( scm.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( scm.getUrl() ).endTag( NAMESPACE, "url" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeScm(Scm, String, XmlSerializer) 

    /**
     * Method writeSite
     * 
     * @param site
     * @param serializer
     * @param tagName
     */
    private void writeSite( Site site, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( site != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( site.getId() != null )
            {
                serializer.startTag( NAMESPACE, "id" ).text( site.getId() ).endTag( NAMESPACE, "id" );
            }
            if ( site.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( site.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( site.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( site.getUrl() ).endTag( NAMESPACE, "url" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeSite(Site, String, XmlSerializer) 

}