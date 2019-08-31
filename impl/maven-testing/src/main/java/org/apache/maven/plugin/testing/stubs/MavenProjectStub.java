package org.apache.maven.plugin.testing.stubs;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Build;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Extension;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Very simple stub of <code>MavenProject</code> object, going to take a lot of work to make it
 * useful as a stub though.
 *
 * @author jesse
 */
public class MavenProjectStub
    extends MavenProject
{
    private String groupId;

    private String artifactId;

    private String name;

    private Model model;

    private MavenProject parent;

    private File file;

    private List<MavenProject> collectedProjects;

    private List<Artifact> attachedArtifacts;

    private List<String> compileSourceRoots;

    private List<String> testCompileSourceRoots;

    private List<String> scriptSourceRoots;

    private List<ArtifactRepository> pluginArtifactRepositories;

    private ArtifactRepository releaseArtifactRepository;

    private ArtifactRepository snapshotArtifactRepository;

    private List<Profile> activeProfiles;

    private Set<Artifact> dependencyArtifacts;

    private Artifact artifact;

    private Map<String, Artifact> artifactMap;

    private Model originalModel;

    private Map<String, Artifact> pluginArtifactMap;

    private Map<String, Artifact> reportArtifactMap;

    private Map<String, Artifact> extensionArtifactMap;

    private Map<String, MavenProject> projectReferences;

    private Build buildOverlay;

    private boolean executionRoot;

    private List<Artifact> compileArtifacts;

    private List<Dependency> compileDependencies;

    private List<Dependency> systemDependencies;

    private List<String> testClasspathElements;

    private List<Dependency> testDependencies;

    private List<String> systemClasspathElements;

    private List<Artifact> systemArtifacts;

    private List<Artifact> testArtifacts;

    private List<Artifact> runtimeArtifacts;

    private List<Dependency> runtimeDependencies;

    private List<String> runtimeClasspathElements;

    private String modelVersion;

    private String packaging;

    private String inceptionYear;

    private String url;

    private String description;

    private String version;

    private String defaultGoal;

    private List<License> licenses;

    private Build build;

    /**
     * Default constructor
     */
    public MavenProjectStub()
    {
        this( new Model() );
    }

    /**
     * @param model the given model
     */
    public MavenProjectStub( Model model )
    {
        super( (Model) null );
        this.model = model;
    }

    /**
     * Loads the model for this stub from the specified POM. For convenience, any checked exception caused by I/O or
     * parser errors will be wrapped into an unchecked exception.
     * 
     * @param pomFile The path to the POM file to load, must not be <code>null</code>. If this path is relative, it
     *            is resolved against the return value of {@link #getBasedir()}.
     */
    protected void readModel( File pomFile )
    {
        if ( !pomFile.isAbsolute() )
        {
            pomFile = new File( getBasedir(), pomFile.getPath() );
        }
        try
        {
            setModel( new MavenXpp3Reader().read( ReaderFactory.newXmlReader( pomFile ) ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Failed to read POM file: " + pomFile, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RuntimeException( "Failed to parse POM file: " + pomFile, e );
        }
    }

    /**
     * No project model is associated
     *
     * @param project the given project
     */
    public MavenProjectStub( MavenProject project )
    {
        super( (Model) null );
    }

    /**
     * @param mavenProject
     * @return an empty String
     * @throws IOException if any
     */
    @Override
    public String getModulePathAdjustment( MavenProject mavenProject )
        throws IOException
    {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public Artifact getArtifact()
    {
        return artifact;
    }

    /** {@inheritDoc} */
    @Override
    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    /** {@inheritDoc} */
    @Override
    public Model getModel()
    {
        return model;
    }

    /** {@inheritDoc} */
    @Override
    public MavenProject getParent()
    {
        return parent;
    }

    /** {@inheritDoc} */
    @Override
    public void setParent( MavenProject mavenProject )
    {
        this.parent = mavenProject;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setRemoteArtifactRepositories(java.util.List)
     */
    @Override
    public void setRemoteArtifactRepositories( List<ArtifactRepository> list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getRemoteArtifactRepositories()
     */
    @Override
    public List<ArtifactRepository> getRemoteArtifactRepositories()
    {
        return Collections.<ArtifactRepository>emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasParent()
    {
        if ( parent != null )
        {
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public File getFile()
    {
        return file;
    }

    /** {@inheritDoc} */
    @Override
    public void setFile( File file )
    {
        this.file = file;
    }

    /** {@inheritDoc} */
    @Override
    public File getBasedir()
    {
        return new File( PlexusTestCase.getBasedir() );
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setDependencies(java.util.List)
     */
    @Override
    public void setDependencies( List<Dependency> list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDependencies()
     */
    @Override
    public List<Dependency> getDependencies()
    {
        return Collections.<Dependency>emptyList();
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDependencyManagement()
     */
    @Override
    public DependencyManagement getDependencyManagement()
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void addCompileSourceRoot( String string )
    {
        if ( compileSourceRoots == null )
        {
            compileSourceRoots = new ArrayList<>( Collections.singletonList( string ) );
        }
        else
        {
            compileSourceRoots.add( string );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addScriptSourceRoot( String string )
    {
        if ( scriptSourceRoots == null )
        {
            scriptSourceRoots = new ArrayList<>( Collections.singletonList( string ) );
        }
        else
        {
            scriptSourceRoots.add( string );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addTestCompileSourceRoot( String string )
    {
        if ( testCompileSourceRoots == null )
        {
            testCompileSourceRoots = new ArrayList<>( Collections.singletonList( string ) );
        }
        else
        {
            testCompileSourceRoots.add( string );
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getScriptSourceRoots()
    {
        return scriptSourceRoots;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getTestCompileSourceRoots()
    {
        return testCompileSourceRoots;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getCompileClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return compileSourceRoots;
    }

    /**
     * @param compileArtifacts
     */
    public void setCompileArtifacts( List<Artifact> compileArtifacts )
    {
        this.compileArtifacts = compileArtifacts;
    }

    /** {@inheritDoc} */
    @Override
    public List<Artifact> getCompileArtifacts()
    {
        return compileArtifacts;
    }

    /** {@inheritDoc} */
    @Override
    public List<Dependency> getCompileDependencies()
    {
        return compileDependencies;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getTestClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return testClasspathElements;
    }

    /** {@inheritDoc} */
    @Override
    public List<Artifact> getTestArtifacts()
    {
        return testArtifacts;
    }

    /** {@inheritDoc} */
    @Override
    public List<Dependency> getTestDependencies()
    {
        return testDependencies;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getRuntimeClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return runtimeClasspathElements;
    }

    /** {@inheritDoc} */
    @Override
    public List<Artifact> getRuntimeArtifacts()
    {
        return runtimeArtifacts;
    }

    /** {@inheritDoc} */
    @Override
    public List<Dependency> getRuntimeDependencies()
    {
        return runtimeDependencies;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getSystemClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return systemClasspathElements;
    }

    /** {@inheritDoc} */
    @Override
    public List<Artifact> getSystemArtifacts()
    {
        return systemArtifacts;
    }

    /**
     * @param runtimeClasspathElements
     */
    public void setRuntimeClasspathElements( List<String> runtimeClasspathElements )
    {
        this.runtimeClasspathElements = runtimeClasspathElements;
    }

    /**
     * @param attachedArtifacts
     */
    @Override
    public void setAttachedArtifacts( List<Artifact> attachedArtifacts )
    {
        this.attachedArtifacts = attachedArtifacts;
    }

    /**
     * @param compileSourceRoots
     */
    @Override
    public void setCompileSourceRoots( List<String> compileSourceRoots )
    {
        this.compileSourceRoots = compileSourceRoots;
    }

    /**
     * @param testCompileSourceRoots
     */
    @Override
    public void setTestCompileSourceRoots( List<String> testCompileSourceRoots )
    {
        this.testCompileSourceRoots = testCompileSourceRoots;
    }

    /**
     * @param scriptSourceRoots
     */
    @Override
    public void setScriptSourceRoots( List<String> scriptSourceRoots )
    {
        this.scriptSourceRoots = scriptSourceRoots;
    }

    /**
     * @param artifactMap
     */
    public void setArtifactMap( Map<String, Artifact> artifactMap )
    {
        this.artifactMap = artifactMap;
    }

    /**
     * @param pluginArtifactMap
     */
    public void setPluginArtifactMap( Map<String, Artifact> pluginArtifactMap )
    {
        this.pluginArtifactMap = pluginArtifactMap;
    }

    /**
     * @param reportArtifactMap
     */
    public void setReportArtifactMap( Map<String, Artifact> reportArtifactMap )
    {
        this.reportArtifactMap = reportArtifactMap;
    }

    /**
     * @param extensionArtifactMap
     */
    public void setExtensionArtifactMap( Map<String, Artifact> extensionArtifactMap )
    {
        this.extensionArtifactMap = extensionArtifactMap;
    }

    /**
     * @param projectReferences
     */
    public void setProjectReferences( Map<String, MavenProject> projectReferences )
    {
        this.projectReferences = projectReferences;
    }

    /**
     * @param buildOverlay
     */
    public void setBuildOverlay( Build buildOverlay )
    {
        this.buildOverlay = buildOverlay;
    }

    /**
     * @param compileDependencies
     */
    public void setCompileDependencies( List<Dependency> compileDependencies )
    {
        this.compileDependencies = compileDependencies;
    }

    /**
     * @param systemDependencies
     */
    public void setSystemDependencies( List<Dependency> systemDependencies )
    {
        this.systemDependencies = systemDependencies;
    }

    /**
     * @param testClasspathElements
     */
    public void setTestClasspathElements( List<String> testClasspathElements )
    {
        this.testClasspathElements = testClasspathElements;
    }

    /**
     * @param testDependencies
     */
    public void setTestDependencies( List<Dependency> testDependencies )
    {
        this.testDependencies = testDependencies;
    }

    /**
     * @param systemClasspathElements
     */
    public void setSystemClasspathElements( List<String> systemClasspathElements )
    {
        this.systemClasspathElements = systemClasspathElements;
    }

    /**
     * @param systemArtifacts
     */
    public void setSystemArtifacts( List<Artifact> systemArtifacts )
    {
        this.systemArtifacts = systemArtifacts;
    }

    /**
     * @param testArtifacts
     */
    public void setTestArtifacts( List<Artifact> testArtifacts )
    {
        this.testArtifacts = testArtifacts;
    }

    /**
     * @param runtimeArtifacts
     */
    public void setRuntimeArtifacts( List<Artifact> runtimeArtifacts )
    {
        this.runtimeArtifacts = runtimeArtifacts;
    }

    /**
     * @param runtimeDependencies
     */
    public void setRuntimeDependencies( List<Dependency> runtimeDependencies )
    {
        this.runtimeDependencies = runtimeDependencies;
    }

    /**
     * @param model
     */
    @Override
    public void setModel( Model model )
    {
        this.model = model;
    }

    /** {@inheritDoc} */
    @Override
    public List<Dependency> getSystemDependencies()
    {
        return systemDependencies;
    }

    /** {@inheritDoc} */
    @Override
    public void setModelVersion( String string )
    {
        this.modelVersion = string;
    }

    /** {@inheritDoc} */
    @Override
    public String getModelVersion()
    {
        return modelVersion;
    }

    /**
     * By default, return an empty String.
     *
     * @see org.apache.maven.project.MavenProject#getId()
     */
    @Override
    public String getId()
    {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public void setGroupId( String string )
    {
        this.groupId = string;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupId()
    {
        return groupId;
    }

    /** {@inheritDoc} */
    @Override
    public void setArtifactId( String string )
    {
        this.artifactId = string;
    }

    /** {@inheritDoc} */
    @Override
    public String getArtifactId()
    {
        return artifactId;
    }

    /** {@inheritDoc} */
    @Override
    public void setName( String string )
    {
        this.name = string;
    }

    /** {@inheritDoc} */
    @Override
    public String getName()
    {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public void setVersion( String string )
    {
        this.version = string;
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion()
    {
        return version;
    }

    /** {@inheritDoc} */
    @Override
    public String getPackaging()
    {
        return packaging;
    }

    /** {@inheritDoc} */
    @Override
    public void setPackaging( String string )
    {
        this.packaging = string;
    }

    /** {@inheritDoc} */
    @Override
    public void setInceptionYear( String string )
    {
        this.inceptionYear = string;
    }

    /** {@inheritDoc} */
    @Override
    public String getInceptionYear()
    {
        return inceptionYear;
    }

    /** {@inheritDoc} */
    @Override
    public void setUrl( String string )
    {
        this.url = string;
    }

    /** {@inheritDoc} */
    @Override
    public String getUrl()
    {
        return url;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPrerequisites()
     */
    @Override
    public Prerequisites getPrerequisites()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setIssueManagement(org.apache.maven.model.IssueManagement)
     */
    @Override
    public void setIssueManagement( IssueManagement issueManagement )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getCiManagement()
     */
    @Override
    public CiManagement getCiManagement()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setCiManagement(org.apache.maven.model.CiManagement)
     */
    @Override
    public void setCiManagement( CiManagement ciManagement )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getIssueManagement()
     */
    @Override
    public IssueManagement getIssueManagement()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setDistributionManagement(org.apache.maven.model.DistributionManagement)
     */
    @Override
    public void setDistributionManagement( DistributionManagement distributionManagement )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDistributionManagement()
     */
    @Override
    public DistributionManagement getDistributionManagement()
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setDescription( String string )
    {
        this.description = string;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription()
    {
        return description;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setOrganization(org.apache.maven.model.Organization)
     */
    @Override
    public void setOrganization( Organization organization )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getOrganization()
     */
    @Override
    public Organization getOrganization()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setScm(org.apache.maven.model.Scm)
     */
    @Override
    public void setScm( Scm scm )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getScm()
     */
    @Override
    public Scm getScm()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setMailingLists(java.util.List)
     */
    @Override
    public void setMailingLists( List<MailingList> list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getMailingLists()
     */
    @Override
    public List<MailingList> getMailingLists()
    {
        return Collections.<MailingList>emptyList();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addMailingList(org.apache.maven.model.MailingList)
     */
    @Override
    public void addMailingList( MailingList mailingList )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setDevelopers(java.util.List)
     */
    @Override
    public void setDevelopers( List<Developer> list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDevelopers()
     */
    @Override
    public List<Developer> getDevelopers()
    {
        return Collections.<Developer>emptyList();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addDeveloper(org.apache.maven.model.Developer)
     */
    @Override
    public void addDeveloper( Developer developer )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setContributors(java.util.List)
     */
    @Override
    public void setContributors( List<Contributor> list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getContributors()
     */
    @Override
    public List<Contributor> getContributors()
    {
        return Collections.<Contributor>emptyList();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addContributor(org.apache.maven.model.Contributor)
     */
    @Override
    public void addContributor( Contributor contributor )
    {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void setBuild( Build build )
    {
        this.build = build;
    }

    /** {@inheritDoc} */
    @Override
    public Build getBuild()
    {
        return build;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getResources()
     */
    @Override
    public List<Resource> getResources()
    {
        return Collections.<Resource>emptyList();
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getTestResources()
     */
    @Override
    public List<Resource> getTestResources()
    {
        return Collections.<Resource>emptyList();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addResource(org.apache.maven.model.Resource)
     */
    @Override
    public void addResource( Resource resource )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addTestResource(org.apache.maven.model.Resource)
     */
    @Override
    public void addTestResource( Resource resource )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setReporting(org.apache.maven.model.Reporting)
     */
    @Override
    public void setReporting( Reporting reporting )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReporting()
     */
    @Override
    public Reporting getReporting()
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setLicenses( List<License> licenses )
    {
        this.licenses = licenses;
    }

    /** {@inheritDoc} */
    @Override
    public List<License> getLicenses()
    {
        return licenses;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addLicense(org.apache.maven.model.License)
     */
    @Override
    public void addLicense( License license )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setArtifacts(java.util.Set)
     */
    @Override
    public void setArtifacts( Set<Artifact> set )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#getArtifacts()
     */
    @Override
    public Set<Artifact> getArtifacts()
    {
        return Collections.<Artifact>emptySet();
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getArtifactMap()
     */
    @Override
    public Map<String, Artifact> getArtifactMap()
    {
        return Collections.<String, Artifact>emptyMap();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setPluginArtifacts(java.util.Set)
     */
    @Override
    public void setPluginArtifacts( Set<Artifact> set )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPluginArtifacts()
     */
    @Override
    public Set<Artifact> getPluginArtifacts()
    {
        return Collections.<Artifact>emptySet();
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPluginArtifactMap()
     */
    @Override
    public Map<String, Artifact> getPluginArtifactMap()
    {
        return Collections.<String, Artifact>emptyMap();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setReportArtifacts(java.util.Set)
     */
    @Override
    public void setReportArtifacts( Set<Artifact> set )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReportArtifacts()
     */
    @Override
    public Set<Artifact> getReportArtifacts()
    {
        return Collections.<Artifact>emptySet();
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReportArtifactMap()
     */
    @Override
    public Map<String, Artifact> getReportArtifactMap()
    {
        return Collections.<String, Artifact>emptyMap();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setExtensionArtifacts(java.util.Set)
     */
    @Override
    public void setExtensionArtifacts( Set<Artifact> set )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#getExtensionArtifacts()
     */
    @Override
    public Set<Artifact> getExtensionArtifacts()
    {
        return Collections.<Artifact>emptySet();
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getExtensionArtifactMap()
     */
    @Override
    public Map<String, Artifact> getExtensionArtifactMap()
    {
        return Collections.<String, Artifact>emptyMap();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setParentArtifact(org.apache.maven.artifact.Artifact)
     */
    @Override
    public void setParentArtifact( Artifact artifact )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getParentArtifact()
     */
    @Override
    public Artifact getParentArtifact()
    {
        return null;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getRepositories()
     */
    @Override
    public List<Repository> getRepositories()
    {
        return Collections.<Repository>emptyList();
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReportPlugins()
     */
    @Override
    public List<ReportPlugin> getReportPlugins()
    {
        return Collections.<ReportPlugin>emptyList();
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getBuildPlugins()
     */
    @Override
    public List<Plugin> getBuildPlugins()
    {
        return Collections.<Plugin>emptyList();
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getModules()
     */
    @Override
    public List<String> getModules()
    {
        return Collections.<String>emptyList();
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPluginManagement()
     */
    @Override
    public PluginManagement getPluginManagement()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addPlugin(org.apache.maven.model.Plugin)
     */
    public void addPlugin( Plugin plugin )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @param plugin
     */
    public void injectPluginManagementInfo( Plugin plugin )
    {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public List<MavenProject> getCollectedProjects()
    {
        return collectedProjects;
    }

    /** {@inheritDoc} */
    @Override
    public void setCollectedProjects( List<MavenProject> list )
    {
        this.collectedProjects = list;
    }

    /** {@inheritDoc} */
    @Override
    public void setPluginArtifactRepositories( List<ArtifactRepository> list )
    {
        this.pluginArtifactRepositories = list;
    }

    /** {@inheritDoc} */
    @Override
    public List<ArtifactRepository> getPluginArtifactRepositories()
    {
        return pluginArtifactRepositories;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDistributionManagementArtifactRepository()
     */
    @Override
    public ArtifactRepository getDistributionManagementArtifactRepository()
    {
        return null;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPluginRepositories()
     */
    @Override
    public List<Repository> getPluginRepositories()
    {
        return Collections.<Repository>emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void setActiveProfiles( List<Profile> list )
    {
        activeProfiles = list;
    }

    /** {@inheritDoc} */
    @Override
    public List<Profile> getActiveProfiles()
    {
        return activeProfiles;
    }

    /** {@inheritDoc} */
    @Override
    public void addAttachedArtifact( Artifact artifact )
    {
        if ( attachedArtifacts == null )
        {
            this.attachedArtifacts = new ArrayList<>( Collections.singletonList( artifact ) );
        }
        else
        {
            attachedArtifacts.add( artifact );
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Artifact> getAttachedArtifacts()
    {
        return attachedArtifacts;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getGoalConfiguration(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Xpp3Dom getGoalConfiguration( String string, String string1, String string2, String string3 )
    {
        return null;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReportConfiguration(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Xpp3Dom getReportConfiguration( String string, String string1, String string2 )
    {
        return null;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getExecutionProject()
     */
    @Override
    public MavenProject getExecutionProject()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setExecutionProject(org.apache.maven.project.MavenProject)
     */
    @Override
    public void setExecutionProject( MavenProject mavenProject )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#writeModel(java.io.Writer)
     */
    @Override
    public void writeModel( Writer writer )
        throws IOException
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#writeOriginalModel(java.io.Writer)
     */
    @Override
    public void writeOriginalModel( Writer writer )
        throws IOException
    {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public Set<Artifact> getDependencyArtifacts()
    {
        return dependencyArtifacts;
    }

    /** {@inheritDoc} */
    @Override
    public void setDependencyArtifacts( Set<Artifact> set )
    {
        this.dependencyArtifacts = set;
    }

    /** {@inheritDoc} */
    @Override
    public void setReleaseArtifactRepository( ArtifactRepository artifactRepository )
    {
        this.releaseArtifactRepository = artifactRepository;
    }

    /** {@inheritDoc} */
    @Override
    public void setSnapshotArtifactRepository( ArtifactRepository artifactRepository )
    {
        this.snapshotArtifactRepository = artifactRepository;
    }

    /** {@inheritDoc} */
    @Override
    public void setOriginalModel( Model model )
    {
        this.originalModel = model;
    }

    /** {@inheritDoc} */
    @Override
    public Model getOriginalModel()
    {
        return originalModel;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getBuildExtensions()
     */
    @Override
    public List<Extension> getBuildExtensions()
    {
        return Collections.<Extension>emptyList();
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#createArtifacts(org.apache.maven.artifact.factory.ArtifactFactory, java.lang.String, org.apache.maven.artifact.resolver.filter.ArtifactFilter)
     */
    @Override
    public Set<Artifact> createArtifacts( ArtifactFactory artifactFactory, String string,
                                          ArtifactFilter artifactFilter )
    {
        return Collections.<Artifact>emptySet();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addProjectReference(org.apache.maven.project.MavenProject)
     */
    @Override
    public void addProjectReference( MavenProject mavenProject )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#attachArtifact(java.lang.String, java.lang.String, java.io.File)
     */
    @Override
    public void attachArtifact( String string, String string1, File file )
    {
        // nop
    }

    /**
     * By default, return a new instance of <code>Properties</code>.
     *
     * @see org.apache.maven.project.MavenProject#getProperties()
     */
    @Override
    public Properties getProperties()
    {
        return new Properties();
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getFilters()
     */
    @Override
    public List<String> getFilters()
    {
        return Collections.<String>emptyList();
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getProjectReferences()
     */
    @Override
    public Map<String, MavenProject> getProjectReferences()
    {
        return Collections.<String, MavenProject>emptyMap();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExecutionRoot()
    {
        return executionRoot;
    }

    /** {@inheritDoc} */
    @Override
    public void setExecutionRoot( boolean b )
    {
        this.executionRoot = b;
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultGoal()
    {
        return defaultGoal;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#replaceWithActiveArtifact(org.apache.maven.artifact.Artifact)
     */
    @Override
    public Artifact replaceWithActiveArtifact( Artifact artifact )
    {
        return null;
    }
}
