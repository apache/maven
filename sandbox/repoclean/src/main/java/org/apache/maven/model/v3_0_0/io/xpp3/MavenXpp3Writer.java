/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0.io.xpp3;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

import org.apache.maven.model.v3_0_0.Branch;
import org.apache.maven.model.v3_0_0.Build;
import org.apache.maven.model.v3_0_0.Contributor;
import org.apache.maven.model.v3_0_0.Dependency;
import org.apache.maven.model.v3_0_0.Developer;
import org.apache.maven.model.v3_0_0.FileSet;
import org.apache.maven.model.v3_0_0.License;
import org.apache.maven.model.v3_0_0.MailingList;
import org.apache.maven.model.v3_0_0.Model;
import org.apache.maven.model.v3_0_0.Organization;
import org.apache.maven.model.v3_0_0.PackageGroup;
import org.apache.maven.model.v3_0_0.PatternSet;
import org.apache.maven.model.v3_0_0.Repository;
import org.apache.maven.model.v3_0_0.Resource;
import org.apache.maven.model.v3_0_0.SourceModification;
import org.apache.maven.model.v3_0_0.UnitTest;
import org.apache.maven.model.v3_0_0.Version;
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
     * Method writeBranch
     * 
     * @param branch
     * @param serializer
     * @param tagName
     */
    private void writeBranch( Branch branch, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( branch != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( branch.getTag() != null )
            {
                serializer.startTag( NAMESPACE, "tag" ).text( branch.getTag() ).endTag( NAMESPACE, "tag" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeBranch(Branch, String, XmlSerializer) 

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
            if ( build.getNagEmailAddress() != null )
            {
                serializer.startTag( NAMESPACE, "nagEmailAddress" ).text( build.getNagEmailAddress() )
                          .endTag( NAMESPACE, "nagEmailAddress" );
            }
            if ( build.getSourceDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "sourceDirectory" ).text( build.getSourceDirectory() )
                          .endTag( NAMESPACE, "sourceDirectory" );
            }
            if ( build.getUnitTestSourceDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "unitTestSourceDirectory" ).text( build.getUnitTestSourceDirectory() )
                          .endTag( NAMESPACE, "unitTestSourceDirectory" );
            }
            if ( build.getAspectSourceDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "aspectSourceDirectory" ).text( build.getAspectSourceDirectory() )
                          .endTag( NAMESPACE, "aspectSourceDirectory" );
            }
            if ( build.getIntegrationUnitTestSourceDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "integrationUnitTestSourceDirectory" )
                          .text( build.getIntegrationUnitTestSourceDirectory() )
                          .endTag( NAMESPACE, "integrationUnitTestSourceDirectory" );
            }
            if ( build.getSourceModifications() != null && build.getSourceModifications().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "sourceModifications" );
                for ( Iterator iter = build.getSourceModifications().iterator(); iter.hasNext(); )
                {
                    SourceModification sourceModification = (SourceModification) iter.next();
                    writeSourceModification( sourceModification, "sourceModification", serializer );
                }
                serializer.endTag( NAMESPACE, "sourceModifications" );
            }
            if ( build.getUnitTest() != null )
            {
                writeUnitTest( build.getUnitTest(), "unitTest", serializer );
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
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeBuild(Build, String, XmlSerializer) 

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
            if ( dependency.getId() != null )
            {
                serializer.startTag( NAMESPACE, "id" ).text( dependency.getId() ).endTag( NAMESPACE, "id" );
            }
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
            if ( dependency.getUrl() != null )
            {
                serializer.startTag( NAMESPACE, "url" ).text( dependency.getUrl() ).endTag( NAMESPACE, "url" );
            }
            if ( dependency.getJar() != null )
            {
                serializer.startTag( NAMESPACE, "jar" ).text( dependency.getJar() ).endTag( NAMESPACE, "jar" );
            }
            if ( dependency.getType() != null )
            {
                serializer.startTag( NAMESPACE, "type" ).text( dependency.getType() ).endTag( NAMESPACE, "type" );
            }
            if ( dependency.getProperties() != null && dependency.getProperties().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "properties" );
                for ( Iterator iter = dependency.getProperties().keySet().iterator(); iter.hasNext(); )
                {
                    String key = (String) iter.next();
                    String value = (String) dependency.getProperties().get( key );
                    serializer.startTag( NAMESPACE, "" + key + "" ).text( value ).endTag( NAMESPACE, "" + key + "" );
                }
                serializer.endTag( NAMESPACE, "properties" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeDependency(Dependency, String, XmlSerializer) 

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
            if ( fileSet.getIncludes() != null && fileSet.getIncludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "includes" );
                for ( Iterator iter = fileSet.getIncludes().iterator(); iter.hasNext(); )
                {
                    String include = (String) iter.next();
                    serializer.startTag( NAMESPACE, "include" ).text( include ).endTag( NAMESPACE, "include" );
                }
                serializer.endTag( NAMESPACE, "includes" );
            }
            if ( fileSet.getExcludes() != null && fileSet.getExcludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "excludes" );
                for ( Iterator iter = fileSet.getExcludes().iterator(); iter.hasNext(); )
                {
                    String exclude = (String) iter.next();
                    serializer.startTag( NAMESPACE, "exclude" ).text( exclude ).endTag( NAMESPACE, "exclude" );
                }
                serializer.endTag( NAMESPACE, "excludes" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeFileSet(FileSet, String, XmlSerializer) 

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
            if ( license.getDistribution() != null )
            {
                serializer.startTag( NAMESPACE, "distribution" ).text( license.getDistribution() )
                          .endTag( NAMESPACE, "distribution" );
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
            if ( mailingList.getArchive() != null )
            {
                serializer.startTag( NAMESPACE, "archive" ).text( mailingList.getArchive() ).endTag( NAMESPACE,
                                                                                                     "archive" );
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
            if ( model.getPomVersion() != null )
            {
                serializer.startTag( NAMESPACE, "pomVersion" ).text( model.getPomVersion() ).endTag( NAMESPACE,
                                                                                                     "pomVersion" );
            }
            if ( model.getId() != null )
            {
                serializer.startTag( NAMESPACE, "id" ).text( model.getId() ).endTag( NAMESPACE, "id" );
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
            if ( model.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( model.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( model.getCurrentVersion() != null )
            {
                serializer.startTag( NAMESPACE, "currentVersion" ).text( model.getCurrentVersion() )
                          .endTag( NAMESPACE, "currentVersion" );
            }
            if ( model.getShortDescription() != null )
            {
                serializer.startTag( NAMESPACE, "shortDescription" ).text( model.getShortDescription() )
                          .endTag( NAMESPACE, "shortDescription" );
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
            if ( model.getLogo() != null )
            {
                serializer.startTag( NAMESPACE, "logo" ).text( model.getLogo() ).endTag( NAMESPACE, "logo" );
            }
            if ( model.getIssueTrackingUrl() != null )
            {
                serializer.startTag( NAMESPACE, "issueTrackingUrl" ).text( model.getIssueTrackingUrl() )
                          .endTag( NAMESPACE, "issueTrackingUrl" );
            }
            if ( model.getInceptionYear() != null )
            {
                serializer.startTag( NAMESPACE, "inceptionYear" ).text( model.getInceptionYear() )
                          .endTag( NAMESPACE, "inceptionYear" );
            }
            if ( model.getGumpRepositoryId() != null )
            {
                serializer.startTag( NAMESPACE, "gumpRepositoryId" ).text( model.getGumpRepositoryId() )
                          .endTag( NAMESPACE, "gumpRepositoryId" );
            }
            if ( model.getSiteAddress() != null )
            {
                serializer.startTag( NAMESPACE, "siteAddress" ).text( model.getSiteAddress() ).endTag( NAMESPACE,
                                                                                                       "siteAddress" );
            }
            if ( model.getSiteDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "siteDirectory" ).text( model.getSiteDirectory() )
                          .endTag( NAMESPACE, "siteDirectory" );
            }
            if ( model.getDistributionSite() != null )
            {
                serializer.startTag( NAMESPACE, "distributionSite" ).text( model.getDistributionSite() )
                          .endTag( NAMESPACE, "distributionSite" );
            }
            if ( model.getDistributionDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "distributionDirectory" ).text( model.getDistributionDirectory() )
                          .endTag( NAMESPACE, "distributionDirectory" );
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
            if ( model.getVersions() != null && model.getVersions().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "versions" );
                for ( Iterator iter = model.getVersions().iterator(); iter.hasNext(); )
                {
                    Version version = (Version) iter.next();
                    writeVersion( version, "version", serializer );
                }
                serializer.endTag( NAMESPACE, "versions" );
            }
            if ( model.getBranches() != null && model.getBranches().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "branches" );
                for ( Iterator iter = model.getBranches().iterator(); iter.hasNext(); )
                {
                    Branch branch = (Branch) iter.next();
                    writeBranch( branch, "branch", serializer );
                }
                serializer.endTag( NAMESPACE, "branches" );
            }
            if ( model.getPackageGroups() != null && model.getPackageGroups().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "packageGroups" );
                for ( Iterator iter = model.getPackageGroups().iterator(); iter.hasNext(); )
                {
                    PackageGroup packageGroup = (PackageGroup) iter.next();
                    writePackageGroup( packageGroup, "packageGroup", serializer );
                }
                serializer.endTag( NAMESPACE, "packageGroups" );
            }
            if ( model.getReports() != null && model.getReports().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "reports" );
                for ( Iterator iter = model.getReports().iterator(); iter.hasNext(); )
                {
                    String report = (String) iter.next();
                    serializer.startTag( NAMESPACE, "report" ).text( report ).endTag( NAMESPACE, "report" );
                }
                serializer.endTag( NAMESPACE, "reports" );
            }
            if ( model.getRepository() != null )
            {
                writeRepository( model.getRepository(), "repository", serializer );
            }
            if ( model.getBuild() != null )
            {
                writeBuild( model.getBuild(), "build", serializer );
            }
            if ( model.getOrganization() != null )
            {
                writeOrganization( model.getOrganization(), "organization", serializer );
            }
            if ( model.getProperties() != null && model.getProperties().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "properties" );
                for ( Iterator iter = model.getProperties().keySet().iterator(); iter.hasNext(); )
                {
                    String key = (String) iter.next();
                    String value = (String) model.getProperties().get( key );
                    serializer.startTag( NAMESPACE, "" + key + "" ).text( value ).endTag( NAMESPACE, "" + key + "" );
                }
                serializer.endTag( NAMESPACE, "properties" );
            }
            if ( model.getPackageName() != null )
            {
                serializer.startTag( NAMESPACE, "package" ).text( model.getPackageName() )
                          .endTag( NAMESPACE, "package" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeModel(Model, String, XmlSerializer) 

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
            if ( organization.getLogo() != null )
            {
                serializer.startTag( NAMESPACE, "logo" ).text( organization.getLogo() ).endTag( NAMESPACE, "logo" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeOrganization(Organization, String, XmlSerializer) 

    /**
     * Method writePackageGroup
     * 
     * @param packageGroup
     * @param serializer
     * @param tagName
     */
    private void writePackageGroup( PackageGroup packageGroup, String tagName, XmlSerializer serializer )
        throws Exception
    {
        if ( packageGroup != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( packageGroup.getTitle() != null )
            {
                serializer.startTag( NAMESPACE, "title" ).text( packageGroup.getTitle() ).endTag( NAMESPACE, "title" );
            }
            if ( packageGroup.getPackages() != null )
            {
                serializer.startTag( NAMESPACE, "packages" ).text( packageGroup.getPackages() ).endTag( NAMESPACE,
                                                                                                        "packages" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writePackageGroup(PackageGroup, String, XmlSerializer) 

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
            if ( patternSet.getIncludes() != null && patternSet.getIncludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "includes" );
                for ( Iterator iter = patternSet.getIncludes().iterator(); iter.hasNext(); )
                {
                    String include = (String) iter.next();
                    serializer.startTag( NAMESPACE, "include" ).text( include ).endTag( NAMESPACE, "include" );
                }
                serializer.endTag( NAMESPACE, "includes" );
            }
            if ( patternSet.getExcludes() != null && patternSet.getExcludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "excludes" );
                for ( Iterator iter = patternSet.getExcludes().iterator(); iter.hasNext(); )
                {
                    String exclude = (String) iter.next();
                    serializer.startTag( NAMESPACE, "exclude" ).text( exclude ).endTag( NAMESPACE, "exclude" );
                }
                serializer.endTag( NAMESPACE, "excludes" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writePatternSet(PatternSet, String, XmlSerializer) 

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
            if ( repository.getConnection() != null )
            {
                serializer.startTag( NAMESPACE, "connection" ).text( repository.getConnection() ).endTag( NAMESPACE,
                                                                                                          "connection" );
            }
            if ( repository.getDeveloperConnection() != null )
            {
                serializer.startTag( NAMESPACE, "developerConnection" ).text( repository.getDeveloperConnection() )
                          .endTag( NAMESPACE, "developerConnection" );
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
            if ( resource.isFiltering() != false )
            {
                serializer.startTag( NAMESPACE, "filtering" ).text( String.valueOf( resource.isFiltering() ) )
                          .endTag( NAMESPACE, "filtering" );
            }
            if ( resource.getDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "directory" ).text( resource.getDirectory() ).endTag( NAMESPACE,
                                                                                                      "directory" );
            }
            if ( resource.getIncludes() != null && resource.getIncludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "includes" );
                for ( Iterator iter = resource.getIncludes().iterator(); iter.hasNext(); )
                {
                    String include = (String) iter.next();
                    serializer.startTag( NAMESPACE, "include" ).text( include ).endTag( NAMESPACE, "include" );
                }
                serializer.endTag( NAMESPACE, "includes" );
            }
            if ( resource.getExcludes() != null && resource.getExcludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "excludes" );
                for ( Iterator iter = resource.getExcludes().iterator(); iter.hasNext(); )
                {
                    String exclude = (String) iter.next();
                    serializer.startTag( NAMESPACE, "exclude" ).text( exclude ).endTag( NAMESPACE, "exclude" );
                }
                serializer.endTag( NAMESPACE, "excludes" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeResource(Resource, String, XmlSerializer) 

    /**
     * Method writeSourceModification
     * 
     * @param sourceModification
     * @param serializer
     * @param tagName
     */
    private void writeSourceModification( SourceModification sourceModification, String tagName,
                                         XmlSerializer serializer ) throws Exception
    {
        if ( sourceModification != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( sourceModification.getClassName() != null )
            {
                serializer.startTag( NAMESPACE, "className" ).text( sourceModification.getClassName() )
                          .endTag( NAMESPACE, "className" );
            }
            if ( sourceModification.getProperty() != null )
            {
                serializer.startTag( NAMESPACE, "property" ).text( sourceModification.getProperty() )
                          .endTag( NAMESPACE, "property" );
            }
            if ( sourceModification.getDirectory() != null )
            {
                serializer.startTag( NAMESPACE, "directory" ).text( sourceModification.getDirectory() )
                          .endTag( NAMESPACE, "directory" );
            }
            if ( sourceModification.getIncludes() != null && sourceModification.getIncludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "includes" );
                for ( Iterator iter = sourceModification.getIncludes().iterator(); iter.hasNext(); )
                {
                    String include = (String) iter.next();
                    serializer.startTag( NAMESPACE, "include" ).text( include ).endTag( NAMESPACE, "include" );
                }
                serializer.endTag( NAMESPACE, "includes" );
            }
            if ( sourceModification.getExcludes() != null && sourceModification.getExcludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "excludes" );
                for ( Iterator iter = sourceModification.getExcludes().iterator(); iter.hasNext(); )
                {
                    String exclude = (String) iter.next();
                    serializer.startTag( NAMESPACE, "exclude" ).text( exclude ).endTag( NAMESPACE, "exclude" );
                }
                serializer.endTag( NAMESPACE, "excludes" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeSourceModification(SourceModification, String, XmlSerializer) 

    /**
     * Method writeUnitTest
     * 
     * @param unitTest
     * @param serializer
     * @param tagName
     */
    private void writeUnitTest( UnitTest unitTest, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( unitTest != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( unitTest.getResources() != null && unitTest.getResources().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "resources" );
                for ( Iterator iter = unitTest.getResources().iterator(); iter.hasNext(); )
                {
                    Resource resource = (Resource) iter.next();
                    writeResource( resource, "resource", serializer );
                }
                serializer.endTag( NAMESPACE, "resources" );
            }
            if ( unitTest.getIncludes() != null && unitTest.getIncludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "includes" );
                for ( Iterator iter = unitTest.getIncludes().iterator(); iter.hasNext(); )
                {
                    String include = (String) iter.next();
                    serializer.startTag( NAMESPACE, "include" ).text( include ).endTag( NAMESPACE, "include" );
                }
                serializer.endTag( NAMESPACE, "includes" );
            }
            if ( unitTest.getExcludes() != null && unitTest.getExcludes().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "excludes" );
                for ( Iterator iter = unitTest.getExcludes().iterator(); iter.hasNext(); )
                {
                    String exclude = (String) iter.next();
                    serializer.startTag( NAMESPACE, "exclude" ).text( exclude ).endTag( NAMESPACE, "exclude" );
                }
                serializer.endTag( NAMESPACE, "excludes" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeUnitTest(UnitTest, String, XmlSerializer) 

    /**
     * Method writeVersion
     * 
     * @param version
     * @param serializer
     * @param tagName
     */
    private void writeVersion( Version version, String tagName, XmlSerializer serializer ) throws Exception
    {
        if ( version != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( version.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( version.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( version.getTag() != null )
            {
                serializer.startTag( NAMESPACE, "tag" ).text( version.getTag() ).endTag( NAMESPACE, "tag" );
            }
            if ( version.getId() != null )
            {
                serializer.startTag( NAMESPACE, "id" ).text( version.getId() ).endTag( NAMESPACE, "id" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeVersion(Version, String, XmlSerializer) 

}