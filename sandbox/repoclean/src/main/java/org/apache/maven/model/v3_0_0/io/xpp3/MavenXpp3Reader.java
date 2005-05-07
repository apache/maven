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
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;

import java.io.Reader;

/**
 * Class MavenXpp3Reader.
 * 
 * @version $Revision$ $Date$
 */
public class MavenXpp3Reader
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * If set the parser till be loaded with all single characters
     * from the XHTML specification.
     * The entities used:
     * <ul>
     * <li>http://www.w3.org/TR/xhtml1/DTD/xhtml-lat1.ent</li>
     * <li>http://www.w3.org/TR/xhtml1/DTD/xhtml-special.ent</li>
     * <li>http://www.w3.org/TR/xhtml1/DTD/xhtml-symbol.ent</li>
     * </ul>
     */
    private boolean addDefaultEntities = true;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getAddDefaultEntities
     */
    public boolean getAddDefaultEntities()
    {
        return addDefaultEntities;
    } //-- boolean getAddDefaultEntities() 

    /**
     * Method parseBranch
     * 
     * @param tagName
     * @param parser
     */
    private Branch parseBranch( String tagName, XmlPullParser parser )
        throws Exception
    {
        Branch branch = new Branch();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "tag" ) )
            {
                branch.setTag( parser.nextText().trim() );
            }
            else
            {
                parser.nextText();
            }
        }
        return branch;
    } //-- Branch parseBranch(String, XmlPullParser) 

    /**
     * Method parseBuild
     * 
     * @param tagName
     * @param parser
     */
    private Build parseBuild( String tagName, XmlPullParser parser )
        throws Exception
    {
        Build build = new Build();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "nagEmailAddress" ) )
            {
                build.setNagEmailAddress( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "sourceDirectory" ) )
            {
                build.setSourceDirectory( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "unitTestSourceDirectory" ) )
            {
                build.setUnitTestSourceDirectory( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "aspectSourceDirectory" ) )
            {
                build.setAspectSourceDirectory( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "integrationUnitTestSourceDirectory" ) )
            {
                build.setIntegrationUnitTestSourceDirectory( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "sourceModifications" ) )
            {
                java.util.List sourceModifications = new java.util.ArrayList();
                build.setSourceModifications( sourceModifications );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "sourceModification" ) )
                    {
                        sourceModifications.add( parseSourceModification( "sourceModification", parser ) );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "unitTest" ) )
            {
                build.setUnitTest( parseUnitTest( "unitTest", parser ) );
            }
            else if ( parser.getName().equals( "resources" ) )
            {
                java.util.List resources = new java.util.ArrayList();
                build.setResources( resources );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "resource" ) )
                    {
                        resources.add( parseResource( "resource", parser ) );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else
            {
                parser.nextText();
            }
        }
        return build;
    } //-- Build parseBuild(String, XmlPullParser) 

    /**
     * Method parseContributor
     * 
     * @param tagName
     * @param parser
     */
    private Contributor parseContributor( String tagName, XmlPullParser parser )
        throws Exception
    {
        Contributor contributor = new Contributor();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "name" ) )
            {
                contributor.setName( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "email" ) )
            {
                contributor.setEmail( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "url" ) )
            {
                contributor.setUrl( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "organization" ) )
            {
                contributor.setOrganization( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "roles" ) )
            {
                java.util.List roles = new java.util.ArrayList();
                contributor.setRoles( roles );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "role" ) )
                    {
                        roles.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "timezone" ) )
            {
                contributor.setTimezone( parser.nextText().trim() );
            }
            else
            {
                parser.nextText();
            }
        }
        return contributor;
    } //-- Contributor parseContributor(String, XmlPullParser) 

    /**
     * Method parseDependency
     * 
     * @param tagName
     * @param parser
     */
    private Dependency parseDependency( String tagName, XmlPullParser parser )
        throws Exception
    {
        Dependency dependency = new Dependency();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "id" ) )
            {
                dependency.setId( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "groupId" ) )
            {
                dependency.setGroupId( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "artifactId" ) )
            {
                dependency.setArtifactId( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "version" ) )
            {
                dependency.setVersion( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "url" ) )
            {
                dependency.setUrl( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "jar" ) )
            {
                dependency.setJar( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "type" ) )
            {
                dependency.setType( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "properties" ) )
            {
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    String key = parser.getName();
                    String value = parser.nextText().trim();
                    dependency.addProperty( key, value );
                }
            }
            else
            {
                parser.nextText();
            }
        }
        return dependency;
    } //-- Dependency parseDependency(String, XmlPullParser) 

    /**
     * Method parseDeveloper
     * 
     * @param tagName
     * @param parser
     */
    private Developer parseDeveloper( String tagName, XmlPullParser parser )
        throws Exception
    {
        Developer developer = new Developer();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "id" ) )
            {
                developer.setId( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "name" ) )
            {
                developer.setName( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "email" ) )
            {
                developer.setEmail( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "url" ) )
            {
                developer.setUrl( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "organization" ) )
            {
                developer.setOrganization( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "roles" ) )
            {
                java.util.List roles = new java.util.ArrayList();
                developer.setRoles( roles );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "role" ) )
                    {
                        roles.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "timezone" ) )
            {
                developer.setTimezone( parser.nextText().trim() );
            }
            else
            {
                parser.nextText();
            }
        }
        return developer;
    } //-- Developer parseDeveloper(String, XmlPullParser) 

    /**
     * Method parseFileSet
     * 
     * @param tagName
     * @param parser
     */
    private FileSet parseFileSet( String tagName, XmlPullParser parser )
        throws Exception
    {
        FileSet fileSet = new FileSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "directory" ) )
            {
                fileSet.setDirectory( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "includes" ) )
            {
                java.util.List includes = new java.util.ArrayList();
                fileSet.setIncludes( includes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "include" ) )
                    {
                        includes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "excludes" ) )
            {
                java.util.List excludes = new java.util.ArrayList();
                fileSet.setExcludes( excludes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "exclude" ) )
                    {
                        excludes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else
            {
                parser.nextText();
            }
        }
        return fileSet;
    } //-- FileSet parseFileSet(String, XmlPullParser) 

    /**
     * Method parseLicense
     * 
     * @param tagName
     * @param parser
     */
    private License parseLicense( String tagName, XmlPullParser parser )
        throws Exception
    {
        License license = new License();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "name" ) )
            {
                license.setName( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "url" ) )
            {
                license.setUrl( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "distribution" ) )
            {
                license.setDistribution( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "comments" ) )
            {
                license.setComments( parser.nextText().trim() );
            }
            else
            {
                parser.nextText();
            }
        }
        return license;
    } //-- License parseLicense(String, XmlPullParser) 

    /**
     * Method parseMailingList
     * 
     * @param tagName
     * @param parser
     */
    private MailingList parseMailingList( String tagName, XmlPullParser parser )
        throws Exception
    {
        MailingList mailingList = new MailingList();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "name" ) )
            {
                mailingList.setName( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "subscribe" ) )
            {
                mailingList.setSubscribe( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "unsubscribe" ) )
            {
                mailingList.setUnsubscribe( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "archive" ) )
            {
                mailingList.setArchive( parser.nextText().trim() );
            }
            else
            {
                parser.nextText();
            }
        }
        return mailingList;
    } //-- MailingList parseMailingList(String, XmlPullParser) 

    /**
     * Method parseModel
     * 
     * @param tagName
     * @param parser
     */
    private Model parseModel( String tagName, XmlPullParser parser )
        throws Exception
    {
        Model model = new Model();
        int eventType = parser.getEventType();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( parser.getName().equals( tagName ) )
                {
                }
                if ( parser.getName().equals( "extend" ) )
                {
                    model.setExtend( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "pomVersion" ) )
                {
                    model.setPomVersion( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "id" ) )
                {
                    model.setId( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "groupId" ) )
                {
                    model.setGroupId( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "artifactId" ) )
                {
                    model.setArtifactId( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "name" ) )
                {
                    model.setName( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "currentVersion" ) )
                {
                    model.setCurrentVersion( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "shortDescription" ) )
                {
                    model.setShortDescription( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "description" ) )
                {
                    model.setDescription( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "url" ) )
                {
                    model.setUrl( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "logo" ) )
                {
                    model.setLogo( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "issueTrackingUrl" ) )
                {
                    model.setIssueTrackingUrl( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "inceptionYear" ) )
                {
                    model.setInceptionYear( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "gumpRepositoryId" ) )
                {
                    model.setGumpRepositoryId( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "siteAddress" ) )
                {
                    model.setSiteAddress( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "siteDirectory" ) )
                {
                    model.setSiteDirectory( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "distributionSite" ) )
                {
                    model.setDistributionSite( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "distributionDirectory" ) )
                {
                    model.setDistributionDirectory( parser.nextText().trim() );
                }
                else if ( parser.getName().equals( "mailingLists" ) )
                {
                    java.util.List mailingLists = new java.util.ArrayList();
                    model.setMailingLists( mailingLists );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "mailingList" ) )
                        {
                            mailingLists.add( parseMailingList( "mailingList", parser ) );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "developers" ) )
                {
                    java.util.List developers = new java.util.ArrayList();
                    model.setDevelopers( developers );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "developer" ) )
                        {
                            developers.add( parseDeveloper( "developer", parser ) );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "contributors" ) )
                {
                    java.util.List contributors = new java.util.ArrayList();
                    model.setContributors( contributors );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "contributor" ) )
                        {
                            contributors.add( parseContributor( "contributor", parser ) );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "dependencies" ) )
                {
                    java.util.List dependencies = new java.util.ArrayList();
                    model.setDependencies( dependencies );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "dependency" ) )
                        {
                            dependencies.add( parseDependency( "dependency", parser ) );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "licenses" ) )
                {
                    java.util.List licenses = new java.util.ArrayList();
                    model.setLicenses( licenses );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "license" ) )
                        {
                            licenses.add( parseLicense( "license", parser ) );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "versions" ) )
                {
                    java.util.List versions = new java.util.ArrayList();
                    model.setVersions( versions );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "version" ) )
                        {
                            versions.add( parseVersion( "version", parser ) );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "branches" ) )
                {
                    java.util.List branches = new java.util.ArrayList();
                    model.setBranches( branches );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "branch" ) )
                        {
                            branches.add( parseBranch( "branch", parser ) );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "packageGroups" ) )
                {
                    java.util.List packageGroups = new java.util.ArrayList();
                    model.setPackageGroups( packageGroups );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "packageGroup" ) )
                        {
                            packageGroups.add( parsePackageGroup( "packageGroup", parser ) );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "reports" ) )
                {
                    java.util.List reports = new java.util.ArrayList();
                    model.setReports( reports );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "report" ) )
                        {
                            reports.add( parser.nextText().trim() );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "repository" ) )
                {
                    model.setRepository( parseRepository( "repository", parser ) );
                }
                else if ( parser.getName().equals( "build" ) )
                {
                    model.setBuild( parseBuild( "build", parser ) );
                }
                else if ( parser.getName().equals( "organization" ) )
                {
                    model.setOrganization( parseOrganization( "organization", parser ) );
                }
                else if ( parser.getName().equals( "properties" ) )
                {
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        String key = parser.getName();
                        String value = parser.nextText().trim();
                        model.addProperty( key, value );
                    }
                }
                else if ( parser.getName().equals( "package" ) )
                {
                    model.setPackageName( parser.nextText().trim() );
                }
            }
            eventType = parser.next();
        }
        return model;
    } //-- Model parseModel(String, XmlPullParser) 

    /**
     * Method parseOrganization
     * 
     * @param tagName
     * @param parser
     */
    private Organization parseOrganization( String tagName, XmlPullParser parser )
        throws Exception
    {
        Organization organization = new Organization();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "name" ) )
            {
                organization.setName( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "url" ) )
            {
                organization.setUrl( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "logo" ) )
            {
                organization.setLogo( parser.nextText().trim() );
            }
            else
            {
                parser.nextText();
            }
        }
        return organization;
    } //-- Organization parseOrganization(String, XmlPullParser) 

    /**
     * Method parsePackageGroup
     * 
     * @param tagName
     * @param parser
     */
    private PackageGroup parsePackageGroup( String tagName, XmlPullParser parser )
        throws Exception
    {
        PackageGroup packageGroup = new PackageGroup();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "title" ) )
            {
                packageGroup.setTitle( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "packages" ) )
            {
                packageGroup.setPackages( parser.nextText().trim() );
            }
            else
            {
                parser.nextText();
            }
        }
        return packageGroup;
    } //-- PackageGroup parsePackageGroup(String, XmlPullParser) 

    /**
     * Method parsePatternSet
     * 
     * @param tagName
     * @param parser
     */
    private PatternSet parsePatternSet( String tagName, XmlPullParser parser )
        throws Exception
    {
        PatternSet patternSet = new PatternSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "includes" ) )
            {
                java.util.List includes = new java.util.ArrayList();
                patternSet.setIncludes( includes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "include" ) )
                    {
                        includes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "excludes" ) )
            {
                java.util.List excludes = new java.util.ArrayList();
                patternSet.setExcludes( excludes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "exclude" ) )
                    {
                        excludes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else
            {
                parser.nextText();
            }
        }
        return patternSet;
    } //-- PatternSet parsePatternSet(String, XmlPullParser) 

    /**
     * Method parseRepository
     * 
     * @param tagName
     * @param parser
     */
    private Repository parseRepository( String tagName, XmlPullParser parser )
        throws Exception
    {
        Repository repository = new Repository();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "connection" ) )
            {
                repository.setConnection( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "developerConnection" ) )
            {
                repository.setDeveloperConnection( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "url" ) )
            {
                repository.setUrl( parser.nextText().trim() );
            }
            else
            {
                parser.nextText();
            }
        }
        return repository;
    } //-- Repository parseRepository(String, XmlPullParser) 

    /**
     * Method parseResource
     * 
     * @param tagName
     * @param parser
     */
    private Resource parseResource( String tagName, XmlPullParser parser )
        throws Exception
    {
        Resource resource = new Resource();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "targetPath" ) )
            {
                resource.setTargetPath( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "filtering" ) )
            {
                resource.setFiltering( ( new Boolean( parser.nextText().trim() ) ).booleanValue() );
            }
            else if ( parser.getName().equals( "directory" ) )
            {
                resource.setDirectory( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "includes" ) )
            {
                java.util.List includes = new java.util.ArrayList();
                resource.setIncludes( includes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "include" ) )
                    {
                        includes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "excludes" ) )
            {
                java.util.List excludes = new java.util.ArrayList();
                resource.setExcludes( excludes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "exclude" ) )
                    {
                        excludes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else
            {
                parser.nextText();
            }
        }
        return resource;
    } //-- Resource parseResource(String, XmlPullParser) 

    /**
     * Method parseSourceModification
     * 
     * @param tagName
     * @param parser
     */
    private SourceModification parseSourceModification( String tagName, XmlPullParser parser )
        throws Exception
    {
        SourceModification sourceModification = new SourceModification();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "className" ) )
            {
                sourceModification.setClassName( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "property" ) )
            {
                sourceModification.setProperty( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "directory" ) )
            {
                sourceModification.setDirectory( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "includes" ) )
            {
                java.util.List includes = new java.util.ArrayList();
                sourceModification.setIncludes( includes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "include" ) )
                    {
                        includes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "excludes" ) )
            {
                java.util.List excludes = new java.util.ArrayList();
                sourceModification.setExcludes( excludes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "exclude" ) )
                    {
                        excludes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else
            {
                parser.nextText();
            }
        }
        return sourceModification;
    } //-- SourceModification parseSourceModification(String, XmlPullParser) 

    /**
     * Method parseUnitTest
     * 
     * @param tagName
     * @param parser
     */
    private UnitTest parseUnitTest( String tagName, XmlPullParser parser )
        throws Exception
    {
        UnitTest unitTest = new UnitTest();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "resources" ) )
            {
                java.util.List resources = new java.util.ArrayList();
                unitTest.setResources( resources );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "resource" ) )
                    {
                        resources.add( parseResource( "resource", parser ) );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "includes" ) )
            {
                java.util.List includes = new java.util.ArrayList();
                unitTest.setIncludes( includes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "include" ) )
                    {
                        includes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "excludes" ) )
            {
                java.util.List excludes = new java.util.ArrayList();
                unitTest.setExcludes( excludes );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "exclude" ) )
                    {
                        excludes.add( parser.nextText().trim() );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else
            {
                parser.nextText();
            }
        }
        return unitTest;
    } //-- UnitTest parseUnitTest(String, XmlPullParser) 

    /**
     * Method parseVersion
     * 
     * @param tagName
     * @param parser
     */
    private Version parseVersion( String tagName, XmlPullParser parser )
        throws Exception
    {
        Version version = new Version();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( tagName ) )
            {
            }
            if ( parser.getName().equals( "name" ) )
            {
                version.setName( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "tag" ) )
            {
                version.setTag( parser.nextText().trim() );
            }
            else if ( parser.getName().equals( "id" ) )
            {
                version.setId( parser.nextText().trim() );
            }
            else
            {
                parser.nextText();
            }
        }
        return version;
    } //-- Version parseVersion(String, XmlPullParser) 

    /**
     * Method read
     * 
     * @param reader
     */
    public Model read( Reader reader )
        throws Exception
    {
        XmlPullParser parser = new MXParser();
        parser.setInput( reader );

        if ( addDefaultEntities )
        {
            // ----------------------------------------------------------------------
            // Latin 1 entities
            // ----------------------------------------------------------------------

            parser.defineEntityReplacementText( "nbsp", "\u00a0" );
            parser.defineEntityReplacementText( "iexcl", "\u00a1" );
            parser.defineEntityReplacementText( "cent", "\u00a2" );
            parser.defineEntityReplacementText( "pound", "\u00a3" );
            parser.defineEntityReplacementText( "curren", "\u00a4" );
            parser.defineEntityReplacementText( "yen", "\u00a5" );
            parser.defineEntityReplacementText( "brvbar", "\u00a6" );
            parser.defineEntityReplacementText( "sect", "\u00a7" );
            parser.defineEntityReplacementText( "uml", "\u00a8" );
            parser.defineEntityReplacementText( "copy", "\u00a9" );
            parser.defineEntityReplacementText( "ordf", "\u00aa" );
            parser.defineEntityReplacementText( "laquo", "\u00ab" );
            parser.defineEntityReplacementText( "not", "\u00ac" );
            parser.defineEntityReplacementText( "shy", "\u00ad" );
            parser.defineEntityReplacementText( "reg", "\u00ae" );
            parser.defineEntityReplacementText( "macr", "\u00af" );
            parser.defineEntityReplacementText( "deg", "\u00b0" );
            parser.defineEntityReplacementText( "plusmn", "\u00b1" );
            parser.defineEntityReplacementText( "sup2", "\u00b2" );
            parser.defineEntityReplacementText( "sup3", "\u00b3" );
            parser.defineEntityReplacementText( "acute", "\u00b4" );
            parser.defineEntityReplacementText( "micro", "\u00b5" );
            parser.defineEntityReplacementText( "para", "\u00b6" );
            parser.defineEntityReplacementText( "middot", "\u00b7" );
            parser.defineEntityReplacementText( "cedil", "\u00b8" );
            parser.defineEntityReplacementText( "sup1", "\u00b9" );
            parser.defineEntityReplacementText( "ordm", "\u00ba" );
            parser.defineEntityReplacementText( "raquo", "\u00bb" );
            parser.defineEntityReplacementText( "frac14", "\u00bc" );
            parser.defineEntityReplacementText( "frac12", "\u00bd" );
            parser.defineEntityReplacementText( "frac34", "\u00be" );
            parser.defineEntityReplacementText( "iquest", "\u00bf" );
            parser.defineEntityReplacementText( "Agrave", "\u00c0" );
            parser.defineEntityReplacementText( "Aacute", "\u00c1" );
            parser.defineEntityReplacementText( "Acirc", "\u00c2" );
            parser.defineEntityReplacementText( "Atilde", "\u00c3" );
            parser.defineEntityReplacementText( "Auml", "\u00c4" );
            parser.defineEntityReplacementText( "Aring", "\u00c5" );
            parser.defineEntityReplacementText( "AElig", "\u00c6" );
            parser.defineEntityReplacementText( "Ccedil", "\u00c7" );
            parser.defineEntityReplacementText( "Egrave", "\u00c8" );
            parser.defineEntityReplacementText( "Eacute", "\u00c9" );
            parser.defineEntityReplacementText( "Ecirc", "\u00ca" );
            parser.defineEntityReplacementText( "Euml", "\u00cb" );
            parser.defineEntityReplacementText( "Igrave", "\u00cc" );
            parser.defineEntityReplacementText( "Iacute", "\u00cd" );
            parser.defineEntityReplacementText( "Icirc", "\u00ce" );
            parser.defineEntityReplacementText( "Iuml", "\u00cf" );
            parser.defineEntityReplacementText( "ETH", "\u00d0" );
            parser.defineEntityReplacementText( "Ntilde", "\u00d1" );
            parser.defineEntityReplacementText( "Ograve", "\u00d2" );
            parser.defineEntityReplacementText( "Oacute", "\u00d3" );
            parser.defineEntityReplacementText( "Ocirc", "\u00d4" );
            parser.defineEntityReplacementText( "Otilde", "\u00d5" );
            parser.defineEntityReplacementText( "Ouml", "\u00d6" );
            parser.defineEntityReplacementText( "times", "\u00d7" );
            parser.defineEntityReplacementText( "Oslash", "\u00d8" );
            parser.defineEntityReplacementText( "Ugrave", "\u00d9" );
            parser.defineEntityReplacementText( "Uacute", "\u00da" );
            parser.defineEntityReplacementText( "Ucirc", "\u00db" );
            parser.defineEntityReplacementText( "Uuml", "\u00dc" );
            parser.defineEntityReplacementText( "Yacute", "\u00dd" );
            parser.defineEntityReplacementText( "THORN", "\u00de" );
            parser.defineEntityReplacementText( "szlig", "\u00df" );
            parser.defineEntityReplacementText( "agrave", "\u00e0" );
            parser.defineEntityReplacementText( "aacute", "\u00e1" );
            parser.defineEntityReplacementText( "acirc", "\u00e2" );
            parser.defineEntityReplacementText( "atilde", "\u00e3" );
            parser.defineEntityReplacementText( "auml", "\u00e4" );
            parser.defineEntityReplacementText( "aring", "\u00e5" );
            parser.defineEntityReplacementText( "aelig", "\u00e6" );
            parser.defineEntityReplacementText( "ccedil", "\u00e7" );
            parser.defineEntityReplacementText( "egrave", "\u00e8" );
            parser.defineEntityReplacementText( "eacute", "\u00e9" );
            parser.defineEntityReplacementText( "ecirc", "\u00ea" );
            parser.defineEntityReplacementText( "euml", "\u00eb" );
            parser.defineEntityReplacementText( "igrave", "\u00ec" );
            parser.defineEntityReplacementText( "iacute", "\u00ed" );
            parser.defineEntityReplacementText( "icirc", "\u00ee" );
            parser.defineEntityReplacementText( "iuml", "\u00ef" );
            parser.defineEntityReplacementText( "eth", "\u00f0" );
            parser.defineEntityReplacementText( "ntilde", "\u00f1" );
            parser.defineEntityReplacementText( "ograve", "\u00f2" );
            parser.defineEntityReplacementText( "oacute", "\u00f3" );
            parser.defineEntityReplacementText( "ocirc", "\u00f4" );
            parser.defineEntityReplacementText( "otilde", "\u00f5" );
            parser.defineEntityReplacementText( "ouml", "\u00f6" );
            parser.defineEntityReplacementText( "divide", "\u00f7" );
            parser.defineEntityReplacementText( "oslash", "\u00f8" );
            parser.defineEntityReplacementText( "ugrave", "\u00f9" );
            parser.defineEntityReplacementText( "uacute", "\u00fa" );
            parser.defineEntityReplacementText( "ucirc", "\u00fb" );
            parser.defineEntityReplacementText( "uuml", "\u00fc" );
            parser.defineEntityReplacementText( "yacute", "\u00fd" );
            parser.defineEntityReplacementText( "thorn", "\u00fe" );
            parser.defineEntityReplacementText( "yuml", "\u00ff" );

            // ----------------------------------------------------------------------
            // Special entities
            // ----------------------------------------------------------------------

            parser.defineEntityReplacementText( "OElig", "\u0152" );
            parser.defineEntityReplacementText( "oelig", "\u0153" );
            parser.defineEntityReplacementText( "Scaron", "\u0160" );
            parser.defineEntityReplacementText( "scaron", "\u0161" );
            parser.defineEntityReplacementText( "Yuml", "\u0178" );
            parser.defineEntityReplacementText( "circ", "\u02c6" );
            parser.defineEntityReplacementText( "tilde", "\u02dc" );
            parser.defineEntityReplacementText( "ensp", "\u2002" );
            parser.defineEntityReplacementText( "emsp", "\u2003" );
            parser.defineEntityReplacementText( "thinsp", "\u2009" );
            parser.defineEntityReplacementText( "zwnj", "\u200c" );
            parser.defineEntityReplacementText( "zwj", "\u200d" );
            parser.defineEntityReplacementText( "lrm", "\u200e" );
            parser.defineEntityReplacementText( "rlm", "\u200f" );
            parser.defineEntityReplacementText( "ndash", "\u2013" );
            parser.defineEntityReplacementText( "mdash", "\u2014" );
            parser.defineEntityReplacementText( "lsquo", "\u2018" );
            parser.defineEntityReplacementText( "rsquo", "\u2019" );
            parser.defineEntityReplacementText( "sbquo", "\u201a" );
            parser.defineEntityReplacementText( "ldquo", "\u201c" );
            parser.defineEntityReplacementText( "rdquo", "\u201d" );
            parser.defineEntityReplacementText( "bdquo", "\u201e" );
            parser.defineEntityReplacementText( "dagger", "\u2020" );
            parser.defineEntityReplacementText( "Dagger", "\u2021" );
            parser.defineEntityReplacementText( "permil", "\u2030" );
            parser.defineEntityReplacementText( "lsaquo", "\u2039" );
            parser.defineEntityReplacementText( "rsaquo", "\u203a" );
            parser.defineEntityReplacementText( "euro", "\u20ac" );

            // ----------------------------------------------------------------------
            // Symbol entities
            // ----------------------------------------------------------------------

            parser.defineEntityReplacementText( "fnof", "\u0192" );
            parser.defineEntityReplacementText( "Alpha", "\u0391" );
            parser.defineEntityReplacementText( "Beta", "\u0392" );
            parser.defineEntityReplacementText( "Gamma", "\u0393" );
            parser.defineEntityReplacementText( "Delta", "\u0394" );
            parser.defineEntityReplacementText( "Epsilon", "\u0395" );
            parser.defineEntityReplacementText( "Zeta", "\u0396" );
            parser.defineEntityReplacementText( "Eta", "\u0397" );
            parser.defineEntityReplacementText( "Theta", "\u0398" );
            parser.defineEntityReplacementText( "Iota", "\u0399" );
            parser.defineEntityReplacementText( "Kappa", "\u039a" );
            parser.defineEntityReplacementText( "Lambda", "\u039b" );
            parser.defineEntityReplacementText( "Mu", "\u039c" );
            parser.defineEntityReplacementText( "Nu", "\u039d" );
            parser.defineEntityReplacementText( "Xi", "\u039e" );
            parser.defineEntityReplacementText( "Omicron", "\u039f" );
            parser.defineEntityReplacementText( "Pi", "\u03a0" );
            parser.defineEntityReplacementText( "Rho", "\u03a1" );
            parser.defineEntityReplacementText( "Sigma", "\u03a3" );
            parser.defineEntityReplacementText( "Tau", "\u03a4" );
            parser.defineEntityReplacementText( "Upsilon", "\u03a5" );
            parser.defineEntityReplacementText( "Phi", "\u03a6" );
            parser.defineEntityReplacementText( "Chi", "\u03a7" );
            parser.defineEntityReplacementText( "Psi", "\u03a8" );
            parser.defineEntityReplacementText( "Omega", "\u03a9" );
            parser.defineEntityReplacementText( "alpha", "\u03b1" );
            parser.defineEntityReplacementText( "beta", "\u03b2" );
            parser.defineEntityReplacementText( "gamma", "\u03b3" );
            parser.defineEntityReplacementText( "delta", "\u03b4" );
            parser.defineEntityReplacementText( "epsilon", "\u03b5" );
            parser.defineEntityReplacementText( "zeta", "\u03b6" );
            parser.defineEntityReplacementText( "eta", "\u03b7" );
            parser.defineEntityReplacementText( "theta", "\u03b8" );
            parser.defineEntityReplacementText( "iota", "\u03b9" );
            parser.defineEntityReplacementText( "kappa", "\u03ba" );
            parser.defineEntityReplacementText( "lambda", "\u03bb" );
            parser.defineEntityReplacementText( "mu", "\u03bc" );
            parser.defineEntityReplacementText( "nu", "\u03bd" );
            parser.defineEntityReplacementText( "xi", "\u03be" );
            parser.defineEntityReplacementText( "omicron", "\u03bf" );
            parser.defineEntityReplacementText( "pi", "\u03c0" );
            parser.defineEntityReplacementText( "rho", "\u03c1" );
            parser.defineEntityReplacementText( "sigmaf", "\u03c2" );
            parser.defineEntityReplacementText( "sigma", "\u03c3" );
            parser.defineEntityReplacementText( "tau", "\u03c4" );
            parser.defineEntityReplacementText( "upsilon", "\u03c5" );
            parser.defineEntityReplacementText( "phi", "\u03c6" );
            parser.defineEntityReplacementText( "chi", "\u03c7" );
            parser.defineEntityReplacementText( "psi", "\u03c8" );
            parser.defineEntityReplacementText( "omega", "\u03c9" );
            parser.defineEntityReplacementText( "thetasym", "\u03d1" );
            parser.defineEntityReplacementText( "upsih", "\u03d2" );
            parser.defineEntityReplacementText( "piv", "\u03d6" );
            parser.defineEntityReplacementText( "bull", "\u2022" );
            parser.defineEntityReplacementText( "hellip", "\u2026" );
            parser.defineEntityReplacementText( "prime", "\u2032" );
            parser.defineEntityReplacementText( "Prime", "\u2033" );
            parser.defineEntityReplacementText( "oline", "\u203e" );
            parser.defineEntityReplacementText( "frasl", "\u2044" );
            parser.defineEntityReplacementText( "weierp", "\u2118" );
            parser.defineEntityReplacementText( "image", "\u2111" );
            parser.defineEntityReplacementText( "real", "\u211c" );
            parser.defineEntityReplacementText( "trade", "\u2122" );
            parser.defineEntityReplacementText( "alefsym", "\u2135" );
            parser.defineEntityReplacementText( "larr", "\u2190" );
            parser.defineEntityReplacementText( "uarr", "\u2191" );
            parser.defineEntityReplacementText( "rarr", "\u2192" );
            parser.defineEntityReplacementText( "darr", "\u2193" );
            parser.defineEntityReplacementText( "harr", "\u2194" );
            parser.defineEntityReplacementText( "crarr", "\u21b5" );
            parser.defineEntityReplacementText( "lArr", "\u21d0" );
            parser.defineEntityReplacementText( "uArr", "\u21d1" );
            parser.defineEntityReplacementText( "rArr", "\u21d2" );
            parser.defineEntityReplacementText( "dArr", "\u21d3" );
            parser.defineEntityReplacementText( "hArr", "\u21d4" );
            parser.defineEntityReplacementText( "forall", "\u2200" );
            parser.defineEntityReplacementText( "part", "\u2202" );
            parser.defineEntityReplacementText( "exist", "\u2203" );
            parser.defineEntityReplacementText( "empty", "\u2205" );
            parser.defineEntityReplacementText( "nabla", "\u2207" );
            parser.defineEntityReplacementText( "isin", "\u2208" );
            parser.defineEntityReplacementText( "notin", "\u2209" );
            parser.defineEntityReplacementText( "ni", "\u220b" );
            parser.defineEntityReplacementText( "prod", "\u220f" );
            parser.defineEntityReplacementText( "sum", "\u2211" );
            parser.defineEntityReplacementText( "minus", "\u2212" );
            parser.defineEntityReplacementText( "lowast", "\u2217" );
            parser.defineEntityReplacementText( "radic", "\u221a" );
            parser.defineEntityReplacementText( "prop", "\u221d" );
            parser.defineEntityReplacementText( "infin", "\u221e" );
            parser.defineEntityReplacementText( "ang", "\u2220" );
            parser.defineEntityReplacementText( "and", "\u2227" );
            parser.defineEntityReplacementText( "or", "\u2228" );
            parser.defineEntityReplacementText( "cap", "\u2229" );
            parser.defineEntityReplacementText( "cup", "\u222a" );
            parser.defineEntityReplacementText( "int", "\u222b" );
            parser.defineEntityReplacementText( "there4", "\u2234" );
            parser.defineEntityReplacementText( "sim", "\u223c" );
            parser.defineEntityReplacementText( "cong", "\u2245" );
            parser.defineEntityReplacementText( "asymp", "\u2248" );
            parser.defineEntityReplacementText( "ne", "\u2260" );
            parser.defineEntityReplacementText( "equiv", "\u2261" );
            parser.defineEntityReplacementText( "le", "\u2264" );
            parser.defineEntityReplacementText( "ge", "\u2265" );
            parser.defineEntityReplacementText( "sub", "\u2282" );
            parser.defineEntityReplacementText( "sup", "\u2283" );
            parser.defineEntityReplacementText( "nsub", "\u2284" );
            parser.defineEntityReplacementText( "sube", "\u2286" );
            parser.defineEntityReplacementText( "supe", "\u2287" );
            parser.defineEntityReplacementText( "oplus", "\u2295" );
            parser.defineEntityReplacementText( "otimes", "\u2297" );
            parser.defineEntityReplacementText( "perp", "\u22a5" );
            parser.defineEntityReplacementText( "sdot", "\u22c5" );
            parser.defineEntityReplacementText( "lceil", "\u2308" );
            parser.defineEntityReplacementText( "rceil", "\u2309" );
            parser.defineEntityReplacementText( "lfloor", "\u230a" );
            parser.defineEntityReplacementText( "rfloor", "\u230b" );
            parser.defineEntityReplacementText( "lang", "\u2329" );
            parser.defineEntityReplacementText( "rang", "\u232a" );
            parser.defineEntityReplacementText( "loz", "\u25ca" );
            parser.defineEntityReplacementText( "spades", "\u2660" );
            parser.defineEntityReplacementText( "clubs", "\u2663" );
            parser.defineEntityReplacementText( "hearts", "\u2665" );
            parser.defineEntityReplacementText( "diams", "\u2666" );

        }

        return parseModel( "model", parser );
    } //-- Model read(Reader) 

    /**
     * Returns the state of the "add default entities" flag.
     * 
     * @param addDefaultEntities
     */
    public void setAddDefaultEntities( boolean addDefaultEntities )
    {
        this.addDefaultEntities = addDefaultEntities;
    } //-- void setAddDefaultEntities(boolean) 

}