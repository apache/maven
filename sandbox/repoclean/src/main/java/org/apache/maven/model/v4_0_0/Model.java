/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

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
     * Field parent
     */
    private Parent parent;

    /**
     * Field modelVersion
     */
    private String modelVersion;

    /**
     * Field groupId
     */
    private String groupId;

    /**
     * Field artifactId
     */
    private String artifactId;

    /**
     * Field packaging
     */
    private String packaging = "jar";

    /**
     * Field modules
     */
    private java.util.List modules;

    /**
     * Field name
     */
    private String name;

    /**
     * Field version
     */
    private String version;

    /**
     * Field description
     */
    private String description;

    /**
     * Field url
     */
    private String url;

    /**
     * Field issueManagement
     */
    private IssueManagement issueManagement;

    /**
     * Field ciManagement
     */
    private CiManagement ciManagement;

    /**
     * Field inceptionYear
     */
    private String inceptionYear;

    /**
     * Field repositories
     */
    private java.util.List repositories;

    /**
     * This may be removed or relocated in the near future. It is
     * undecided whether plugins really need a remote repository
     * set of their own.
     */
    private java.util.List pluginRepositories;

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
     * when transitive dependencies come into play.
     */
    private java.util.List dependencies;

    /**
     * Field licenses
     */
    private java.util.List licenses;

    /**
     * Field reports
     */
    private Reports reports;

    /**
     * Field scm
     */
    private Scm scm;

    /**
     * Field build
     */
    private Build build;

    /**
     * Field organization
     */
    private Organization organization;

    /**
     * Field distributionManagement
     */
    private DistributionManagement distributionManagement;

    /**
     * Field dependencyManagement
     */
    private DependencyManagement dependencyManagement;

    //-----------/
    //- Methods -/
    //-----------/

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
     * Method addModule
     * 
     * @param string
     */
    public void addModule( String string )
    {
        getModules().add( string );
    } //-- void addModule(String) 

    /**
     * Method addPluginRepository
     * 
     * @param repository
     */
    public void addPluginRepository( Repository repository )
    {
        getPluginRepositories().add( repository );
    } //-- void addPluginRepository(Repository) 

    /**
     * Method addRepository
     * 
     * @param repository
     */
    public void addRepository( Repository repository )
    {
        getRepositories().add( repository );
    } //-- void addRepository(Repository) 

    /**
     * Method getArtifactId
     */
    public String getArtifactId()
    {
        return this.artifactId;
    } //-- String getArtifactId() 

    /**
     * Method getBuild
     */
    public Build getBuild()
    {
        return this.build;
    } //-- Build getBuild() 

    /**
     * Method getCiManagement
     */
    public CiManagement getCiManagement()
    {
        return this.ciManagement;
    } //-- CiManagement getCiManagement() 

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
     * Method getDependencyManagement
     */
    public DependencyManagement getDependencyManagement()
    {
        return this.dependencyManagement;
    } //-- DependencyManagement getDependencyManagement() 

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
     * Method getDistributionManagement
     */
    public DistributionManagement getDistributionManagement()
    {
        return this.distributionManagement;
    } //-- DistributionManagement getDistributionManagement() 

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
     * Method getInceptionYear
     */
    public String getInceptionYear()
    {
        return this.inceptionYear;
    } //-- String getInceptionYear() 

    /**
     * Method getIssueManagement
     */
    public IssueManagement getIssueManagement()
    {
        return this.issueManagement;
    } //-- IssueManagement getIssueManagement() 

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
     * Method getModelVersion
     */
    public String getModelVersion()
    {
        return this.modelVersion;
    } //-- String getModelVersion() 

    /**
     * Method getModules
     */
    public java.util.List getModules()
    {
        if ( this.modules == null )
        {
            this.modules = new java.util.ArrayList();
        }

        return this.modules;
    } //-- java.util.List getModules() 

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
     * Method getPackaging
     */
    public String getPackaging()
    {
        return this.packaging;
    } //-- String getPackaging() 

    /**
     * Method getParent
     */
    public Parent getParent()
    {
        return this.parent;
    } //-- Parent getParent() 

    /**
     * Method getPluginRepositories
     */
    public java.util.List getPluginRepositories()
    {
        if ( this.pluginRepositories == null )
        {
            this.pluginRepositories = new java.util.ArrayList();
        }

        return this.pluginRepositories;
    } //-- java.util.List getPluginRepositories() 

    /**
     * Method getReports
     */
    public Reports getReports()
    {
        return this.reports;
    } //-- Reports getReports() 

    /**
     * Method getRepositories
     */
    public java.util.List getRepositories()
    {
        if ( this.repositories == null )
        {
            this.repositories = new java.util.ArrayList();
        }

        return this.repositories;
    } //-- java.util.List getRepositories() 

    /**
     * Method getScm
     */
    public Scm getScm()
    {
        return this.scm;
    } //-- Scm getScm() 

    /**
     * Method getUrl
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl() 

    /**
     * Method getVersion
     */
    public String getVersion()
    {
        return this.version;
    } //-- String getVersion() 

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
     * Method removeModule
     * 
     * @param string
     */
    public void removeModule( String string )
    {
        getModules().remove( string );
    } //-- void removeModule(String) 

    /**
     * Method removePluginRepository
     * 
     * @param repository
     */
    public void removePluginRepository( Repository repository )
    {
        getPluginRepositories().remove( repository );
    } //-- void removePluginRepository(Repository) 

    /**
     * Method removeRepository
     * 
     * @param repository
     */
    public void removeRepository( Repository repository )
    {
        getRepositories().remove( repository );
    } //-- void removeRepository(Repository) 

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
     * Method setBuild
     * 
     * @param build
     */
    public void setBuild( Build build )
    {
        this.build = build;
    } //-- void setBuild(Build) 

    /**
     * Method setCiManagement
     * 
     * @param ciManagement
     */
    public void setCiManagement( CiManagement ciManagement )
    {
        this.ciManagement = ciManagement;
    } //-- void setCiManagement(CiManagement) 

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
     * Method setDependencies
     * 
     * @param dependencies
     */
    public void setDependencies( java.util.List dependencies )
    {
        this.dependencies = dependencies;
    } //-- void setDependencies(java.util.List) 

    /**
     * Method setDependencyManagement
     * 
     * @param dependencyManagement
     */
    public void setDependencyManagement( DependencyManagement dependencyManagement )
    {
        this.dependencyManagement = dependencyManagement;
    } //-- void setDependencyManagement(DependencyManagement) 

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
     * Method setDistributionManagement
     * 
     * @param distributionManagement
     */
    public void setDistributionManagement( DistributionManagement distributionManagement )
    {
        this.distributionManagement = distributionManagement;
    } //-- void setDistributionManagement(DistributionManagement) 

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
     * Method setInceptionYear
     * 
     * @param inceptionYear
     */
    public void setInceptionYear( String inceptionYear )
    {
        this.inceptionYear = inceptionYear;
    } //-- void setInceptionYear(String) 

    /**
     * Method setIssueManagement
     * 
     * @param issueManagement
     */
    public void setIssueManagement( IssueManagement issueManagement )
    {
        this.issueManagement = issueManagement;
    } //-- void setIssueManagement(IssueManagement) 

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
     * Method setMailingLists
     * 
     * @param mailingLists
     */
    public void setMailingLists( java.util.List mailingLists )
    {
        this.mailingLists = mailingLists;
    } //-- void setMailingLists(java.util.List) 

    /**
     * Method setModelVersion
     * 
     * @param modelVersion
     */
    public void setModelVersion( String modelVersion )
    {
        this.modelVersion = modelVersion;
    } //-- void setModelVersion(String) 

    /**
     * Method setModules
     * 
     * @param modules
     */
    public void setModules( java.util.List modules )
    {
        this.modules = modules;
    } //-- void setModules(java.util.List) 

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
     * Method setPackaging
     * 
     * @param packaging
     */
    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    } //-- void setPackaging(String) 

    /**
     * Method setParent
     * 
     * @param parent
     */
    public void setParent( Parent parent )
    {
        this.parent = parent;
    } //-- void setParent(Parent) 

    /**
     * Method setPluginRepositories
     * 
     * @param pluginRepositories
     */
    public void setPluginRepositories( java.util.List pluginRepositories )
    {
        this.pluginRepositories = pluginRepositories;
    } //-- void setPluginRepositories(java.util.List) 

    /**
     * Method setReports
     * 
     * @param reports
     */
    public void setReports( Reports reports )
    {
        this.reports = reports;
    } //-- void setReports(Reports) 

    /**
     * Method setRepositories
     * 
     * @param repositories
     */
    public void setRepositories( java.util.List repositories )
    {
        this.repositories = repositories;
    } //-- void setRepositories(java.util.List) 

    /**
     * Method setScm
     * 
     * @param scm
     */
    public void setScm( Scm scm )
    {
        this.scm = scm;
    } //-- void setScm(Scm) 

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
     * Method setVersion
     * 
     * @param version
     */
    public void setVersion( String version )
    {
        this.version = version;
    } //-- void setVersion(String) 

    public String getId()
    {
        StringBuffer id = new StringBuffer();

        id.append( getGroupId() );
        id.append( ":" );
        id.append( getArtifactId() );
        id.append( ":" );
        id.append( getPackaging() );
        id.append( ":" );
        id.append( getVersion() );

        return id.toString();
    }
}