/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class Model.
 * 
 * @version $Revision$ $Date$
 */
public class Model
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field extend
     */
    private String extend;

    /**
     * Field pomVersion
     */
    private String pomVersion;

    /**
     * Field id
     */
    private String id;

    /**
     * Field groupId
     */
    private String groupId;

    /**
     * Field artifactId
     */
    private String artifactId;

    /**
     * Field name
     */
    private String name;

    /**
     * Field currentVersion
     */
    private String currentVersion;

    /**
     * Field shortDescription
     */
    private String shortDescription;

    /**
     * Field description
     */
    private String description;

    /**
     * Field url
     */
    private String url;

    /**
     * Field logo
     */
    private String logo;

    /**
     * Field issueTrackingUrl
     */
    private String issueTrackingUrl;

    /**
     * Field inceptionYear
     */
    private String inceptionYear;

    /**
     * Field gumpRepositoryId
     */
    private String gumpRepositoryId;

    /**
     * Field siteAddress
     */
    private String siteAddress;

    /**
     * Field siteDirectory
     */
    private String siteDirectory;

    /**
     * Field distributionSite
     */
    private String distributionSite;

    /**
     * Field distributionDirectory
     */
    private String distributionDirectory;

    /**
     * Field mailingLists
     */
    private java.util.List mailingLists;

    /**
     * Field developers
     */
    private java.util.List developers;

    /**
     * Field contributors
     */
    private java.util.List contributors;

    /**
     * These should ultimately only be compile time dependencies
     * when transitive 
     *             dependencies come into play.
     */
    private java.util.List dependencies;

    /**
     * Field licenses
     */
    private java.util.List licenses;

    /**
     * Field versions
     */
    private java.util.List versions;

    /**
     * Field branches
     */
    private java.util.List branches;

    /**
     * Field packageGroups
     */
    private java.util.List packageGroups;

    /**
     * Field reports
     */
    private java.util.List reports;

    /**
     * Field repository
     */
    private Repository repository;

    /**
     * Field build
     */
    private Build build;

    /**
     * Field organization
     */
    private Organization organization;

    /**
     * Field properties
     */
    private java.util.Properties properties;

    /**
     * Field packageName
     */
    private String packageName;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addBranch
     * 
     * @param branch
     */
    public void addBranch( Branch branch )
    {
        getBranches().add( branch );
    } //-- void addBranch(Branch) 

    /**
     * Method addContributor
     * 
     * @param contributor
     */
    public void addContributor( Contributor contributor )
    {
        getContributors().add( contributor );
    } //-- void addContributor(Contributor) 

    /**
     * Method addDependency
     * 
     * @param dependency
     */
    public void addDependency( Dependency dependency )
    {
        getDependencies().add( dependency );
    } //-- void addDependency(Dependency) 

    /**
     * Method addDeveloper
     * 
     * @param developer
     */
    public void addDeveloper( Developer developer )
    {
        getDevelopers().add( developer );
    } //-- void addDeveloper(Developer) 

    /**
     * Method addLicense
     * 
     * @param license
     */
    public void addLicense( License license )
    {
        getLicenses().add( license );
    } //-- void addLicense(License) 

    /**
     * Method addMailingList
     * 
     * @param mailingList
     */
    public void addMailingList( MailingList mailingList )
    {
        getMailingLists().add( mailingList );
    } //-- void addMailingList(MailingList) 

    /**
     * Method addPackageGroup
     * 
     * @param packageGroup
     */
    public void addPackageGroup( PackageGroup packageGroup )
    {
        getPackageGroups().add( packageGroup );
    } //-- void addPackageGroup(PackageGroup) 

    /**
     * Method addProperty
     * 
     * @param key
     * @param value
     */
    public void addProperty( String key, String value )
    {
        getProperties().put( key, value );
    } //-- void addProperty(String, String) 

    /**
     * Method addReport
     * 
     * @param string
     */
    public void addReport( String string )
    {
        getReports().add( string );
    } //-- void addReport(String) 

    /**
     * Method addVersion
     * 
     * @param version
     */
    public void addVersion( Version version )
    {
        getVersions().add( version );
    } //-- void addVersion(Version) 

    /**
     * Method getArtifactId
     */
    public String getArtifactId()
    {
        return this.artifactId;
    } //-- String getArtifactId() 

    /**
     * Method getBranches
     */
    public java.util.List getBranches()
    {
        if ( this.branches == null )
        {
            this.branches = new java.util.ArrayList();
        }

        return this.branches;
    } //-- java.util.List getBranches() 

    /**
     * Method getBuild
     */
    public Build getBuild()
    {
        return this.build;
    } //-- Build getBuild() 

    /**
     * Method getContributors
     */
    public java.util.List getContributors()
    {
        if ( this.contributors == null )
        {
            this.contributors = new java.util.ArrayList();
        }

        return this.contributors;
    } //-- java.util.List getContributors() 

    /**
     * Method getCurrentVersion
     */
    public String getCurrentVersion()
    {
        return this.currentVersion;
    } //-- String getCurrentVersion() 

    /**
     * Method getDependencies
     */
    public java.util.List getDependencies()
    {
        if ( this.dependencies == null )
        {
            this.dependencies = new java.util.ArrayList();
        }

        return this.dependencies;
    } //-- java.util.List getDependencies() 

    /**
     * Method getDescription
     */
    public String getDescription()
    {
        return this.description;
    } //-- String getDescription() 

    /**
     * Method getDevelopers
     */
    public java.util.List getDevelopers()
    {
        if ( this.developers == null )
        {
            this.developers = new java.util.ArrayList();
        }

        return this.developers;
    } //-- java.util.List getDevelopers() 

    /**
     * Method getDistributionDirectory
     */
    public String getDistributionDirectory()
    {
        return this.distributionDirectory;
    } //-- String getDistributionDirectory() 

    /**
     * Method getDistributionSite
     */
    public String getDistributionSite()
    {
        return this.distributionSite;
    } //-- String getDistributionSite() 

    /**
     * Method getExtend
     */
    public String getExtend()
    {
        return this.extend;
    } //-- String getExtend() 

    /**
     * Method getGroupId
     */
    public String getGroupId()
    {
        return this.groupId;
    } //-- String getGroupId() 

    /**
     * Method getGumpRepositoryId
     */
    public String getGumpRepositoryId()
    {
        return this.gumpRepositoryId;
    } //-- String getGumpRepositoryId() 

    /**
     * Method getId
     */
    public String getId()
    {
        return this.id;
    } //-- String getId() 

    /**
     * Method getInceptionYear
     */
    public String getInceptionYear()
    {
        return this.inceptionYear;
    } //-- String getInceptionYear() 

    /**
     * Method getIssueTrackingUrl
     */
    public String getIssueTrackingUrl()
    {
        return this.issueTrackingUrl;
    } //-- String getIssueTrackingUrl() 

    /**
     * Method getLicenses
     */
    public java.util.List getLicenses()
    {
        if ( this.licenses == null )
        {
            this.licenses = new java.util.ArrayList();
        }

        return this.licenses;
    } //-- java.util.List getLicenses() 

    /**
     * Method getLogo
     */
    public String getLogo()
    {
        return this.logo;
    } //-- String getLogo() 

    /**
     * Method getMailingLists
     */
    public java.util.List getMailingLists()
    {
        if ( this.mailingLists == null )
        {
            this.mailingLists = new java.util.ArrayList();
        }

        return this.mailingLists;
    } //-- java.util.List getMailingLists() 

    /**
     * Method getName
     */
    public String getName()
    {
        return this.name;
    } //-- String getName() 

    /**
     * Method getOrganization
     */
    public Organization getOrganization()
    {
        return this.organization;
    } //-- Organization getOrganization() 

    /**
     * Method getPackageGroups
     */
    public java.util.List getPackageGroups()
    {
        if ( this.packageGroups == null )
        {
            this.packageGroups = new java.util.ArrayList();
        }

        return this.packageGroups;
    } //-- java.util.List getPackageGroups() 

    /**
     * Method getPackageName
     */
    public String getPackageName()
    {
        return this.packageName;
    } //-- String getPackageName() 

    /**
     * Method getPomVersion
     */
    public String getPomVersion()
    {
        return this.pomVersion;
    } //-- String getPomVersion() 

    /**
     * Method getProperties
     */
    public java.util.Properties getProperties()
    {
        if ( this.properties == null )
        {
            this.properties = new java.util.Properties();
        }

        return this.properties;
    } //-- java.util.Properties getProperties() 

    /**
     * Method getReports
     */
    public java.util.List getReports()
    {
        if ( this.reports == null )
        {
            this.reports = new java.util.ArrayList();
        }

        return this.reports;
    } //-- java.util.List getReports() 

    /**
     * Method getRepository
     */
    public Repository getRepository()
    {
        return this.repository;
    } //-- Repository getRepository() 

    /**
     * Method getShortDescription
     */
    public String getShortDescription()
    {
        return this.shortDescription;
    } //-- String getShortDescription() 

    /**
     * Method getSiteAddress
     */
    public String getSiteAddress()
    {
        return this.siteAddress;
    } //-- String getSiteAddress() 

    /**
     * Method getSiteDirectory
     */
    public String getSiteDirectory()
    {
        return this.siteDirectory;
    } //-- String getSiteDirectory() 

    /**
     * Method getUrl
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl() 

    /**
     * Method getVersions
     */
    public java.util.List getVersions()
    {
        if ( this.versions == null )
        {
            this.versions = new java.util.ArrayList();
        }

        return this.versions;
    } //-- java.util.List getVersions() 

    /**
     * Method removeBranch
     * 
     * @param branch
     */
    public void removeBranch( Branch branch )
    {
        getBranches().remove( branch );
    } //-- void removeBranch(Branch) 

    /**
     * Method removeContributor
     * 
     * @param contributor
     */
    public void removeContributor( Contributor contributor )
    {
        getContributors().remove( contributor );
    } //-- void removeContributor(Contributor) 

    /**
     * Method removeDependency
     * 
     * @param dependency
     */
    public void removeDependency( Dependency dependency )
    {
        getDependencies().remove( dependency );
    } //-- void removeDependency(Dependency) 

    /**
     * Method removeDeveloper
     * 
     * @param developer
     */
    public void removeDeveloper( Developer developer )
    {
        getDevelopers().remove( developer );
    } //-- void removeDeveloper(Developer) 

    /**
     * Method removeLicense
     * 
     * @param license
     */
    public void removeLicense( License license )
    {
        getLicenses().remove( license );
    } //-- void removeLicense(License) 

    /**
     * Method removeMailingList
     * 
     * @param mailingList
     */
    public void removeMailingList( MailingList mailingList )
    {
        getMailingLists().remove( mailingList );
    } //-- void removeMailingList(MailingList) 

    /**
     * Method removePackageGroup
     * 
     * @param packageGroup
     */
    public void removePackageGroup( PackageGroup packageGroup )
    {
        getPackageGroups().remove( packageGroup );
    } //-- void removePackageGroup(PackageGroup) 

    /**
     * Method removeReport
     * 
     * @param string
     */
    public void removeReport( String string )
    {
        getReports().remove( string );
    } //-- void removeReport(String) 

    /**
     * Method removeVersion
     * 
     * @param version
     */
    public void removeVersion( Version version )
    {
        getVersions().remove( version );
    } //-- void removeVersion(Version) 

    /**
     * Method setArtifactId
     * 
     * @param artifactId
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    } //-- void setArtifactId(String) 

    /**
     * Method setBranches
     * 
     * @param branches
     */
    public void setBranches( java.util.List branches )
    {
        this.branches = branches;
    } //-- void setBranches(java.util.List) 

    /**
     * Method setBuild
     * 
     * @param build
     */
    public void setBuild( Build build )
    {
        this.build = build;
    } //-- void setBuild(Build) 

    /**
     * Method setContributors
     * 
     * @param contributors
     */
    public void setContributors( java.util.List contributors )
    {
        this.contributors = contributors;
    } //-- void setContributors(java.util.List) 

    /**
     * Method setCurrentVersion
     * 
     * @param currentVersion
     */
    public void setCurrentVersion( String currentVersion )
    {
        this.currentVersion = currentVersion;
    } //-- void setCurrentVersion(String) 

    /**
     * Method setDependencies
     * 
     * @param dependencies
     */
    public void setDependencies( java.util.List dependencies )
    {
        this.dependencies = dependencies;
    } //-- void setDependencies(java.util.List) 

    /**
     * Method setDescription
     * 
     * @param description
     */
    public void setDescription( String description )
    {
        this.description = description;
    } //-- void setDescription(String) 

    /**
     * Method setDevelopers
     * 
     * @param developers
     */
    public void setDevelopers( java.util.List developers )
    {
        this.developers = developers;
    } //-- void setDevelopers(java.util.List) 

    /**
     * Method setDistributionDirectory
     * 
     * @param distributionDirectory
     */
    public void setDistributionDirectory( String distributionDirectory )
    {
        this.distributionDirectory = distributionDirectory;
    } //-- void setDistributionDirectory(String) 

    /**
     * Method setDistributionSite
     * 
     * @param distributionSite
     */
    public void setDistributionSite( String distributionSite )
    {
        this.distributionSite = distributionSite;
    } //-- void setDistributionSite(String) 

    /**
     * Method setExtend
     * 
     * @param extend
     */
    public void setExtend( String extend )
    {
        this.extend = extend;
    } //-- void setExtend(String) 

    /**
     * Method setGroupId
     * 
     * @param groupId
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    } //-- void setGroupId(String) 

    /**
     * Method setGumpRepositoryId
     * 
     * @param gumpRepositoryId
     */
    public void setGumpRepositoryId( String gumpRepositoryId )
    {
        this.gumpRepositoryId = gumpRepositoryId;
    } //-- void setGumpRepositoryId(String) 

    /**
     * Method setId
     * 
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    } //-- void setId(String) 

    /**
     * Method setInceptionYear
     * 
     * @param inceptionYear
     */
    public void setInceptionYear( String inceptionYear )
    {
        this.inceptionYear = inceptionYear;
    } //-- void setInceptionYear(String) 

    /**
     * Method setIssueTrackingUrl
     * 
     * @param issueTrackingUrl
     */
    public void setIssueTrackingUrl( String issueTrackingUrl )
    {
        this.issueTrackingUrl = issueTrackingUrl;
    } //-- void setIssueTrackingUrl(String) 

    /**
     * Method setLicenses
     * 
     * @param licenses
     */
    public void setLicenses( java.util.List licenses )
    {
        this.licenses = licenses;
    } //-- void setLicenses(java.util.List) 

    /**
     * Method setLogo
     * 
     * @param logo
     */
    public void setLogo( String logo )
    {
        this.logo = logo;
    } //-- void setLogo(String) 

    /**
     * Method setMailingLists
     * 
     * @param mailingLists
     */
    public void setMailingLists( java.util.List mailingLists )
    {
        this.mailingLists = mailingLists;
    } //-- void setMailingLists(java.util.List) 

    /**
     * Method setName
     * 
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    } //-- void setName(String) 

    /**
     * Method setOrganization
     * 
     * @param organization
     */
    public void setOrganization( Organization organization )
    {
        this.organization = organization;
    } //-- void setOrganization(Organization) 

    /**
     * Method setPackageGroups
     * 
     * @param packageGroups
     */
    public void setPackageGroups( java.util.List packageGroups )
    {
        this.packageGroups = packageGroups;
    } //-- void setPackageGroups(java.util.List) 

    /**
     * Method setPackageName
     * 
     * @param packageName
     */
    public void setPackageName( String packageName )
    {
        this.packageName = packageName;
    } //-- void setPackageName(String) 

    /**
     * Method setPomVersion
     * 
     * @param pomVersion
     */
    public void setPomVersion( String pomVersion )
    {
        this.pomVersion = pomVersion;
    } //-- void setPomVersion(String) 

    /**
     * Method setProperties
     * 
     * @param properties
     */
    public void setProperties( java.util.Properties properties )
    {
        this.properties = properties;
    } //-- void setProperties(java.util.Properties) 

    /**
     * Method setReports
     * 
     * @param reports
     */
    public void setReports( java.util.List reports )
    {
        this.reports = reports;
    } //-- void setReports(java.util.List) 

    /**
     * Method setRepository
     * 
     * @param repository
     */
    public void setRepository( Repository repository )
    {
        this.repository = repository;
    } //-- void setRepository(Repository) 

    /**
     * Method setShortDescription
     * 
     * @param shortDescription
     */
    public void setShortDescription( String shortDescription )
    {
        this.shortDescription = shortDescription;
    } //-- void setShortDescription(String) 

    /**
     * Method setSiteAddress
     * 
     * @param siteAddress
     */
    public void setSiteAddress( String siteAddress )
    {
        this.siteAddress = siteAddress;
    } //-- void setSiteAddress(String) 

    /**
     * Method setSiteDirectory
     * 
     * @param siteDirectory
     */
    public void setSiteDirectory( String siteDirectory )
    {
        this.siteDirectory = siteDirectory;
    } //-- void setSiteDirectory(String) 

    /**
     * Method setUrl
     * 
     * @param url
     */
    public void setUrl( String url )
    {
        this.url = url;
    } //-- void setUrl(String) 

    /**
     * Method setVersions
     * 
     * @param versions
     */
    public void setVersions( java.util.List versions )
    {
        this.versions = versions;
    } //-- void setVersions(java.util.List) 

    public void setVersion( String version )
    {
        this.currentVersion = version;
    }

    public String getVersion()
    {
        return currentVersion;
    }

    // We need this because we can't use package as a field name. 
    public void setPackage( String packageName )
    {
        this.packageName = packageName;
    }

    public String getPackage()
    {
        return packageName;
    }
}