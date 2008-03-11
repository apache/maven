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
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Very simple stub of <code>MavenProject<code> object, going to take a lot of work to make it
 * useful as a stub though.
 *
 * @author jesse
 * @version $Id$
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

    private List collectedProjects;

    private List attachedArtifacts;

    private List compileSourceRoots;

    private List testCompileSourceRoots;

    private List scriptSourceRoots;

    private List pluginArtifactRepositories;

    private ArtifactRepository releaseArtifactRepository;

    private ArtifactRepository snapshotArtifactRepository;

    private List activeProfiles;

    private Set dependencyArtifacts;

    private Artifact artifact;

    private Map artifactMap;

    private Model originalModel;

    private Map pluginArtifactMap;

    private Map reportArtifactMap;

    private Map extensionArtifactMap;

    private Map projectReferences;

    private Build buildOverlay;

    private boolean executionRoot;

    private List compileArtifacts;

    private List compileDependencies;

    private List systemDependencies;

    private List testClasspathElements;

    private List testDependencies;

    private List systemClasspathElements;

    private List systemArtifacts;

    private List testArtifacts;

    private List runtimeArtifacts;

    private List runtimeDependencies;

    private List runtimeClasspathElements;

    private String modelVersion;

    private String packaging;

    private String inceptionYear;

    private String url;

    private String description;

    private String version;

    private String defaultGoal;

    private List licenses;

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
    public String getModulePathAdjustment( MavenProject mavenProject )
        throws IOException
    {
        return "";
    }

    /** {@inheritDoc} */
    public Artifact getArtifact()
    {
        return artifact;
    }

    /** {@inheritDoc} */
    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    /** {@inheritDoc} */
    public Model getModel()
    {
        return model;
    }

    /** {@inheritDoc} */
    public MavenProject getParent()
    {
        return parent;
    }

    /** {@inheritDoc} */
    public void setParent( MavenProject mavenProject )
    {
        this.parent = mavenProject;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setRemoteArtifactRepositories(java.util.List)
     */
    public void setRemoteArtifactRepositories( List list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getRemoteArtifactRepositories()
     */
    public List getRemoteArtifactRepositories()
    {
        return Collections.EMPTY_LIST;
    }

    /** {@inheritDoc} */
    public boolean hasParent()
    {
        if ( parent != null )
        {
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    public File getFile()
    {
        return file;
    }

    /** {@inheritDoc} */
    public void setFile( File file )
    {
        this.file = file;
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        return new File( PlexusTestCase.getBasedir() );
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setDependencies(java.util.List)
     */
    public void setDependencies( List list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDependencies()
     */
    public List getDependencies()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDependencyManagement()
     */
    public DependencyManagement getDependencyManagement()
    {
        return null;
    }

    /** {@inheritDoc} */
    public void addCompileSourceRoot( String string )
    {
        if ( compileSourceRoots == null )
        {
            compileSourceRoots = new ArrayList( Collections.singletonList( string ) );
        }
        else
        {
            compileSourceRoots.add( string );
        }
    }

    /** {@inheritDoc} */
    public void addScriptSourceRoot( String string )
    {
        if ( scriptSourceRoots == null )
        {
            scriptSourceRoots = new ArrayList( Collections.singletonList( string ) );
        }
        else
        {
            scriptSourceRoots.add( string );
        }
    }

    /** {@inheritDoc} */
    public void addTestCompileSourceRoot( String string )
    {
        if ( testCompileSourceRoots == null )
        {
            testCompileSourceRoots = new ArrayList( Collections.singletonList( string ) );
        }
        else
        {
            testCompileSourceRoots.add( string );
        }
    }

    /** {@inheritDoc} */
    public List getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    /** {@inheritDoc} */
    public List getScriptSourceRoots()
    {
        return scriptSourceRoots;
    }

    /** {@inheritDoc} */
    public List getTestCompileSourceRoots()
    {
        return testCompileSourceRoots;
    }

    /** {@inheritDoc} */
    public List getCompileClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return compileSourceRoots;
    }

    /**
     * @param compileArtifacts
     */
    public void setCompileArtifacts( List compileArtifacts )
    {
        this.compileArtifacts = compileArtifacts;
    }

    /** {@inheritDoc} */
    public List getCompileArtifacts()
    {
        return compileArtifacts;
    }

    /** {@inheritDoc} */
    public List getCompileDependencies()
    {
        return compileDependencies;
    }

    /** {@inheritDoc} */
    public List getTestClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return testClasspathElements;
    }

    /** {@inheritDoc} */
    public List getTestArtifacts()
    {
        return testArtifacts;
    }

    /** {@inheritDoc} */
    public List getTestDependencies()
    {
        return testDependencies;
    }

    /** {@inheritDoc} */
    public List getRuntimeClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return runtimeClasspathElements;
    }

    /** {@inheritDoc} */
    public List getRuntimeArtifacts()
    {
        return runtimeArtifacts;
    }

    /** {@inheritDoc} */
    public List getRuntimeDependencies()
    {
        return runtimeDependencies;
    }

    /** {@inheritDoc} */
    public List getSystemClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return systemClasspathElements;
    }

    /** {@inheritDoc} */
    public List getSystemArtifacts()
    {
        return systemArtifacts;
    }

    /**
     * @param runtimeClasspathElements
     */
    public void setRuntimeClasspathElements( List runtimeClasspathElements )
    {
        this.runtimeClasspathElements = runtimeClasspathElements;
    }

    /**
     * @param attachedArtifacts
     */
    public void setAttachedArtifacts( List attachedArtifacts )
    {
        this.attachedArtifacts = attachedArtifacts;
    }

    /**
     * @param compileSourceRoots
     */
    public void setCompileSourceRoots( List compileSourceRoots )
    {
        this.compileSourceRoots = compileSourceRoots;
    }

    /**
     * @param testCompileSourceRoots
     */
    public void setTestCompileSourceRoots( List testCompileSourceRoots )
    {
        this.testCompileSourceRoots = testCompileSourceRoots;
    }

    /**
     * @param scriptSourceRoots
     */
    public void setScriptSourceRoots( List scriptSourceRoots )
    {
        this.scriptSourceRoots = scriptSourceRoots;
    }

    /**
     * @param artifactMap
     */
    public void setArtifactMap( Map artifactMap )
    {
        this.artifactMap = artifactMap;
    }

    /**
     * @param pluginArtifactMap
     */
    public void setPluginArtifactMap( Map pluginArtifactMap )
    {
        this.pluginArtifactMap = pluginArtifactMap;
    }

    /**
     * @param reportArtifactMap
     */
    public void setReportArtifactMap( Map reportArtifactMap )
    {
        this.reportArtifactMap = reportArtifactMap;
    }

    /**
     * @param extensionArtifactMap
     */
    public void setExtensionArtifactMap( Map extensionArtifactMap )
    {
        this.extensionArtifactMap = extensionArtifactMap;
    }

    /**
     * @param projectReferences
     */
    public void setProjectReferences( Map projectReferences )
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
    public void setCompileDependencies( List compileDependencies )
    {
        this.compileDependencies = compileDependencies;
    }

    /**
     * @param systemDependencies
     */
    public void setSystemDependencies( List systemDependencies )
    {
        this.systemDependencies = systemDependencies;
    }

    /**
     * @param testClasspathElements
     */
    public void setTestClasspathElements( List testClasspathElements )
    {
        this.testClasspathElements = testClasspathElements;
    }

    /**
     * @param testDependencies
     */
    public void setTestDependencies( List testDependencies )
    {
        this.testDependencies = testDependencies;
    }

    /**
     * @param systemClasspathElements
     */
    public void setSystemClasspathElements( List systemClasspathElements )
    {
        this.systemClasspathElements = systemClasspathElements;
    }

    /**
     * @param systemArtifacts
     */
    public void setSystemArtifacts( List systemArtifacts )
    {
        this.systemArtifacts = systemArtifacts;
    }

    /**
     * @param testArtifacts
     */
    public void setTestArtifacts( List testArtifacts )
    {
        this.testArtifacts = testArtifacts;
    }

    /**
     * @param runtimeArtifacts
     */
    public void setRuntimeArtifacts( List runtimeArtifacts )
    {
        this.runtimeArtifacts = runtimeArtifacts;
    }

    /**
     * @param runtimeDependencies
     */
    public void setRuntimeDependencies( List runtimeDependencies )
    {
        this.runtimeDependencies = runtimeDependencies;
    }

    /**
     * @param model
     */
    public void setModel( Model model )
    {
        this.model = model;
    }

    /** {@inheritDoc} */
    public List getSystemDependencies()
    {
        return systemDependencies;
    }

    /** {@inheritDoc} */
    public void setModelVersion( String string )
    {
        this.modelVersion = string;
    }

    /** {@inheritDoc} */
    public String getModelVersion()
    {
        return modelVersion;
    }

    /**
     * By default, return an empty String.
     *
     * @see org.apache.maven.project.MavenProject#getId()
     */
    public String getId()
    {
        return "";
    }

    /** {@inheritDoc} */
    public void setGroupId( String string )
    {
        this.groupId = string;
    }

    /** {@inheritDoc} */
    public String getGroupId()
    {
        return groupId;
    }

    /** {@inheritDoc} */
    public void setArtifactId( String string )
    {
        this.artifactId = string;
    }

    /** {@inheritDoc} */
    public String getArtifactId()
    {
        return artifactId;
    }

    /** {@inheritDoc} */
    public void setName( String string )
    {
        this.name = string;
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return name;
    }

    /** {@inheritDoc} */
    public void setVersion( String string )
    {
        this.version = string;
    }

    /** {@inheritDoc} */
    public String getVersion()
    {
        return version;
    }

    /** {@inheritDoc} */
    public String getPackaging()
    {
        return packaging;
    }

    /** {@inheritDoc} */
    public void setPackaging( String string )
    {
        this.packaging = string;
    }

    /** {@inheritDoc} */
    public void setInceptionYear( String string )
    {
        this.inceptionYear = string;
    }

    /** {@inheritDoc} */
    public String getInceptionYear()
    {
        return inceptionYear;
    }

    /** {@inheritDoc} */
    public void setUrl( String string )
    {
        this.url = string;
    }

    /** {@inheritDoc} */
    public String getUrl()
    {
        return url;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPrerequisites()
     */
    public Prerequisites getPrerequisites()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setIssueManagement(org.apache.maven.model.IssueManagement)
     */
    public void setIssueManagement( IssueManagement issueManagement )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getCiManagement()
     */
    public CiManagement getCiManagement()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setCiManagement(org.apache.maven.model.CiManagement)
     */
    public void setCiManagement( CiManagement ciManagement )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getIssueManagement()
     */
    public IssueManagement getIssueManagement()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setDistributionManagement(org.apache.maven.model.DistributionManagement)
     */
    public void setDistributionManagement( DistributionManagement distributionManagement )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDistributionManagement()
     */
    public DistributionManagement getDistributionManagement()
    {
        return null;
    }

    /** {@inheritDoc} */
    public void setDescription( String string )
    {
        this.description = string;
    }

    /** {@inheritDoc} */
    public String getDescription()
    {
        return description;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setOrganization(org.apache.maven.model.Organization)
     */
    public void setOrganization( Organization organization )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getOrganization()
     */
    public Organization getOrganization()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setScm(org.apache.maven.model.Scm)
     */
    public void setScm( Scm scm )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getScm()
     */
    public Scm getScm()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setMailingLists(java.util.List)
     */
    public void setMailingLists( List list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getMailingLists()
     */
    public List getMailingLists()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addMailingList(org.apache.maven.model.MailingList)
     */
    public void addMailingList( MailingList mailingList )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setDevelopers(java.util.List)
     */
    public void setDevelopers( List list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDevelopers()
     */
    public List getDevelopers()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addDeveloper(org.apache.maven.model.Developer)
     */
    public void addDeveloper( Developer developer )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setContributors(java.util.List)
     */
    public void setContributors( List list )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getContributors()
     */
    public List getContributors()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addContributor(org.apache.maven.model.Contributor)
     */
    public void addContributor( Contributor contributor )
    {
        // nop
    }

    /** {@inheritDoc} */
    public void setBuild( Build build )
    {
        this.build = build;
    }

    /** {@inheritDoc} */
    public Build getBuild()
    {
        return build;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getResources()
     */
    public List getResources()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getTestResources()
     */
    public List getTestResources()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addResource(org.apache.maven.model.Resource)
     */
    public void addResource( Resource resource )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addTestResource(org.apache.maven.model.Resource)
     */
    public void addTestResource( Resource resource )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setReporting(org.apache.maven.model.Reporting)
     */
    public void setReporting( Reporting reporting )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReporting()
     */
    public Reporting getReporting()
    {
        return null;
    }

    /** {@inheritDoc} */
    public void setLicenses( List licenses )
    {
        this.licenses = licenses;
    }

    /** {@inheritDoc} */
    public List getLicenses()
    {
        return licenses;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addLicense(org.apache.maven.model.License)
     */
    public void addLicense( License license )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setArtifacts(java.util.Set)
     */
    public void setArtifacts( Set set )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#getArtifacts()
     */
    public Set getArtifacts()
    {
        return Collections.EMPTY_SET;
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getArtifactMap()
     */
    public Map getArtifactMap()
    {
        return Collections.EMPTY_MAP;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setPluginArtifacts(java.util.Set)
     */
    public void setPluginArtifacts( Set set )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPluginArtifacts()
     */
    public Set getPluginArtifacts()
    {
        return Collections.EMPTY_SET;
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPluginArtifactMap()
     */
    public Map getPluginArtifactMap()
    {
        return Collections.EMPTY_MAP;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setReportArtifacts(java.util.Set)
     */
    public void setReportArtifacts( Set set )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReportArtifacts()
     */
    public Set getReportArtifacts()
    {
        return Collections.EMPTY_SET;
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReportArtifactMap()
     */
    public Map getReportArtifactMap()
    {
        return Collections.EMPTY_MAP;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setExtensionArtifacts(java.util.Set)
     */
    public void setExtensionArtifacts( Set set )
    {
        // nop
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#getExtensionArtifacts()
     */
    public Set getExtensionArtifacts()
    {
        return Collections.EMPTY_SET;
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getExtensionArtifactMap()
     */
    public Map getExtensionArtifactMap()
    {
        return Collections.EMPTY_MAP;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setParentArtifact(org.apache.maven.artifact.Artifact)
     */
    public void setParentArtifact( Artifact artifact )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getParentArtifact()
     */
    public Artifact getParentArtifact()
    {
        return null;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getRepositories()
     */
    public List getRepositories()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReportPlugins()
     */
    public List getReportPlugins()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getBuildPlugins()
     */
    public List getBuildPlugins()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getModules()
     */
    public List getModules()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPluginManagement()
     */
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
    public List getCollectedProjects()
    {
        return collectedProjects;
    }

    /** {@inheritDoc} */
    public void setCollectedProjects( List list )
    {
        this.collectedProjects = list;
    }

    /** {@inheritDoc} */
    public void setPluginArtifactRepositories( List list )
    {
        this.pluginArtifactRepositories = list;
    }

    /** {@inheritDoc} */
    public List getPluginArtifactRepositories()
    {
        return pluginArtifactRepositories;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getDistributionManagementArtifactRepository()
     */
    public ArtifactRepository getDistributionManagementArtifactRepository()
    {
        return null;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getPluginRepositories()
     */
    public List getPluginRepositories()
    {
        return Collections.EMPTY_LIST;
    }

    /** {@inheritDoc} */
    public void setActiveProfiles( List list )
    {
        activeProfiles = list;
    }

    /** {@inheritDoc} */
    public List getActiveProfiles()
    {
        return activeProfiles;
    }

    /** {@inheritDoc} */
    public void addAttachedArtifact( Artifact artifact )
    {
        if ( attachedArtifacts == null )
        {
            this.attachedArtifacts = new ArrayList( Collections.singletonList( artifact ) );
        }
        else
        {
            attachedArtifacts.add( artifact );
        }
    }

    /** {@inheritDoc} */
    public List getAttachedArtifacts()
    {
        return attachedArtifacts;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getGoalConfiguration(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public Xpp3Dom getGoalConfiguration( String string, String string1, String string2, String string3 )
    {
        return null;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getReportConfiguration(java.lang.String, java.lang.String, java.lang.String)
     */
    public Xpp3Dom getReportConfiguration( String string, String string1, String string2 )
    {
        return null;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#getExecutionProject()
     */
    public MavenProject getExecutionProject()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#setExecutionProject(org.apache.maven.project.MavenProject)
     */
    public void setExecutionProject( MavenProject mavenProject )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#writeModel(java.io.Writer)
     */
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
    public void writeOriginalModel( Writer writer )
        throws IOException
    {
        // nop
    }

    /** {@inheritDoc} */
    public Set getDependencyArtifacts()
    {
        return dependencyArtifacts;
    }

    /** {@inheritDoc} */
    public void setDependencyArtifacts( Set set )
    {
        this.dependencyArtifacts = set;
    }

    /** {@inheritDoc} */
    public void setReleaseArtifactRepository( ArtifactRepository artifactRepository )
    {
        this.releaseArtifactRepository = artifactRepository;
    }

    /** {@inheritDoc} */
    public void setSnapshotArtifactRepository( ArtifactRepository artifactRepository )
    {
        this.snapshotArtifactRepository = artifactRepository;
    }

    /** {@inheritDoc} */
    public void setOriginalModel( Model model )
    {
        this.originalModel = model;
    }

    /** {@inheritDoc} */
    public Model getOriginalModel()
    {
        return originalModel;
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getBuildExtensions()
     */
    public List getBuildExtensions()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, return <code>Collections.EMPTY_SET</code>.
     *
     * @see org.apache.maven.project.MavenProject#createArtifacts(org.apache.maven.artifact.factory.ArtifactFactory, java.lang.String, org.apache.maven.artifact.resolver.filter.ArtifactFilter)
     */
    public Set createArtifacts( ArtifactFactory artifactFactory, String string, ArtifactFilter artifactFilter )
        throws InvalidDependencyVersionException
    {
        return Collections.EMPTY_SET;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#addProjectReference(org.apache.maven.project.MavenProject)
     */
    public void addProjectReference( MavenProject mavenProject )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.project.MavenProject#attachArtifact(java.lang.String, java.lang.String, java.io.File)
     */
    public void attachArtifact( String string, String string1, File file )
    {
        // nop
    }

    /**
     * By default, return a new instance of <code>Properties</code>.
     *
     * @see org.apache.maven.project.MavenProject#getProperties()
     */
    public Properties getProperties()
    {
        return new Properties();
    }

    /**
     * By default, return <code>Collections.EMPTY_LIST</code>.
     *
     * @see org.apache.maven.project.MavenProject#getFilters()
     */
    public List getFilters()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * By default, return <code>Collections.EMPTY_MAP</code>.
     *
     * @see org.apache.maven.project.MavenProject#getProjectReferences()
     */
    public Map getProjectReferences()
    {
        return Collections.EMPTY_MAP;
    }

    /** {@inheritDoc} */
    public boolean isExecutionRoot()
    {
        return executionRoot;
    }

    /** {@inheritDoc} */
    public void setExecutionRoot( boolean b )
    {
        this.executionRoot = b;
    }

    /** {@inheritDoc} */
    public String getDefaultGoal()
    {
        return defaultGoal;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.project.MavenProject#replaceWithActiveArtifact(org.apache.maven.artifact.Artifact)
     */
    public Artifact replaceWithActiveArtifact( Artifact artifact )
    {
        return null;
    }
}
