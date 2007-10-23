package org.apache.maven.project;

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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.versioning.ManagedVersionMap;
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
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.artifact.ActiveProjectArtifact;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.project.overlay.BuildOverlay;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * The concern of the project is provide runtime values based on the model. <p/>
 * The values in the model remain untouched but during the process of building a
 * project notions like inheritance and interpolation can be added. This allows
 * to have an entity which is useful in a runtime while preserving the model so
 * that it can be marshalled and unmarshalled without being tainted by runtime
 * requirements. <p/>We need to leave the model intact because we don't want
 * the following:
 * <ol>
 * <li>We don't want interpolated values being written back into the model.
 * <li>We don't want inherited values being written back into the model.
 * </ol>
 */
public class MavenProject
{
    public static final String EMPTY_PROJECT_GROUP_ID = "unknown";
    
    public static final String EMPTY_PROJECT_ARTIFACT_ID = "empty-project";
    
    public static final String EMPTY_PROJECT_VERSION = "0";
    
    private Model model;

    private MavenProject parent;

    private File file;

    private Set artifacts;

    private Artifact parentArtifact;

    private Set pluginArtifacts;

    private List remoteArtifactRepositories;

    private List collectedProjects = Collections.EMPTY_LIST;

    private List attachedArtifacts;

    private MavenProject executionProject;

    private List compileSourceRoots = new ArrayList();

    private List testCompileSourceRoots = new ArrayList();

    private List scriptSourceRoots = new ArrayList();

    private List pluginArtifactRepositories;

    private ArtifactRepository releaseArtifactRepository;

    private ArtifactRepository snapshotArtifactRepository;

    private List activeProfiles = new ArrayList();

    private Set dependencyArtifacts;

    private Artifact artifact;

    // calculated.
    private Map artifactMap;

    private Model originalModel;

    private Map pluginArtifactMap;

    private Set reportArtifacts;

    private Map reportArtifactMap;

    private Set extensionArtifacts;

    private Map extensionArtifactMap;

    private Map managedVersionMap;

    private Map projectReferences = new HashMap();

    private Build buildOverlay;

    private boolean executionRoot;
    
    private Map moduleAdjustments;

    public MavenProject()
    {
        Model model = new Model();
        
        model.setGroupId( EMPTY_PROJECT_GROUP_ID );
        model.setArtifactId( EMPTY_PROJECT_ARTIFACT_ID );
        model.setVersion( EMPTY_PROJECT_VERSION );
        
        this.model = model;
    }

    public MavenProject( Model model )
    {
        this.model = model;
    }

    public MavenProject( MavenProject project )
    {
        // disown the parent

        // copy fields
        this.file = project.file;

        // don't need a deep copy, they don't get modified or added/removed to/from - but make them unmodifiable to be sure!
        if ( project.dependencyArtifacts != null )
        {
            this.dependencyArtifacts = Collections.unmodifiableSet( project.dependencyArtifacts );
        }
        
        if ( project.artifacts != null )
        {
            this.artifacts = Collections.unmodifiableSet( project.artifacts );
        }
        
        if ( project.pluginArtifacts != null )
        {
            this.pluginArtifacts = Collections.unmodifiableSet( project.pluginArtifacts );
        }
        
        if ( project.reportArtifacts != null )
        {
            this.reportArtifacts = Collections.unmodifiableSet( project.reportArtifacts );
        }        
        
        if ( project.extensionArtifacts != null )
        {
            this.extensionArtifacts = Collections.unmodifiableSet( project.extensionArtifacts );
        }        
        
        this.parentArtifact = project.parentArtifact;

        if ( project.remoteArtifactRepositories != null )
        {
            this.remoteArtifactRepositories = Collections.unmodifiableList( project.remoteArtifactRepositories );
        }        
        
        if ( project.pluginArtifactRepositories != null )
        {
            this.pluginArtifactRepositories = Collections.unmodifiableList( project.pluginArtifactRepositories );
        }        
        
        if ( project.collectedProjects != null )
        {
            this.collectedProjects = Collections.unmodifiableList( project.collectedProjects );
        }        
        
        if ( project.activeProfiles != null )
        {
            this.activeProfiles = Collections.unmodifiableList( project.activeProfiles );
        }        
        
        if ( project.getAttachedArtifacts() != null )
        {
            // clone properties modifyable by plugins in a forked lifecycle
            this.attachedArtifacts = new ArrayList( project.getAttachedArtifacts() );
        }        
        
        if ( project.compileSourceRoots != null )
        {
            // clone source roots
            this.compileSourceRoots = new ArrayList( project.compileSourceRoots );
        }        
        
        if ( project.testCompileSourceRoots != null )
        {
            this.testCompileSourceRoots = new ArrayList( project.testCompileSourceRoots );
        }        
        
        if ( project.scriptSourceRoots != null )
        {
            this.scriptSourceRoots = new ArrayList( project.scriptSourceRoots );
        }        
        
        this.model = ModelUtils.cloneModel( project.model );

        if ( project.originalModel != null )
        {
            this.originalModel = ModelUtils.cloneModel( project.originalModel );
        }

        this.executionRoot = project.executionRoot;

        if ( project.artifact != null )
        {
            this.artifact = ArtifactUtils.copyArtifact( project.artifact );
        }

        if ( project.getManagedVersionMap() != null )
        {
            setManagedVersionMap( new ManagedVersionMap( project.getManagedVersionMap() ) );
        }
        
        if ( project.releaseArtifactRepository != null )
        {
            releaseArtifactRepository = project.releaseArtifactRepository;
        }
        
        if ( project.snapshotArtifactRepository != null )
        {
            snapshotArtifactRepository = project.snapshotArtifactRepository;
        }
    }
    
    public String getModulePathAdjustment( MavenProject moduleProject ) throws IOException
    {
        // FIXME: This is hacky. What if module directory doesn't match artifactid, and parent
        // is coming from the repository??
        
        // FIXME: If there is a hierarchy of three projects, with the url specified at the top, 
        // and the top two projects are referenced from copies that are in the repository, the
        // middle-level POM doesn't have a File associated with it (or the file's directory is
        // of an unexpected name), and module path adjustments fail.
        String module = moduleProject.getArtifactId();
        
        File moduleFile = moduleProject.getFile();
        
        if ( moduleFile != null )
        {
            File moduleDir = moduleFile.getCanonicalFile().getParentFile();
            
            module = moduleDir.getName();
        }
        
        if ( moduleAdjustments == null )
        {
            moduleAdjustments = new HashMap();
            
            List modules = getModules();
            if ( modules != null )
            {
                for ( Iterator it = modules.iterator(); it.hasNext(); )
                {
                    String modulePath = (String) it.next();
                    String moduleName = modulePath;
                    
                    if ( moduleName.endsWith( "/" ) || moduleName.endsWith( "\\" ) )
                    {
                        moduleName = moduleName.substring( 0, moduleName.length() - 1 );
                    }
                    
                    int lastSlash = moduleName.lastIndexOf( '/' );
                    
                    if ( lastSlash < 0 )
                    {
                        lastSlash = moduleName.lastIndexOf( '\\' );
                    }
                    
                    String adjustment = null;
                    
                    if ( lastSlash > -1 )
                    {
                        moduleName = moduleName.substring( lastSlash + 1 );
                        adjustment = modulePath.substring( 0, lastSlash );
                    }

                    moduleAdjustments.put( moduleName, adjustment );
                }
            }
        }
        
        return (String) moduleAdjustments.get( module );
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public Artifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    //@todo I would like to get rid of this. jvz.
    public Model getModel()
    {
        return model;
    }

    public MavenProject getParent()
    {
        return parent;
    }

    public void setParent( MavenProject parent )
    {
        this.parent = parent;
    }

    public void setRemoteArtifactRepositories( List remoteArtifactRepositories )
    {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
    }

    public List getRemoteArtifactRepositories()
    {
        return remoteArtifactRepositories;
    }

    public boolean hasParent()
    {
        return getParent() != null;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public File getBasedir()
    {
        if ( getFile() != null )
        {
            return getFile().getParentFile();
        }
        else
        {
            // repository based POM
            return null;
        }
    }

    public void setDependencies( List dependencies )
    {
        model.setDependencies( dependencies );
    }

    public List getDependencies()
    {
        return model.getDependencies();
    }

    public DependencyManagement getDependencyManagement()
    {
        return model.getDependencyManagement();
    }

    // ----------------------------------------------------------------------
    // Test and compile sourceroots.
    // ----------------------------------------------------------------------

    public void addCompileSourceRoot( String path )
    {
        if ( path != null )
        {
            path = path.trim();
            if ( path.length() != 0 )
            {
                if ( !compileSourceRoots.contains( path ) )
                {
                    compileSourceRoots.add( path );
                }
            }
        }
    }

    public void addScriptSourceRoot( String path )
    {
        if ( path != null )
        {
            path = path.trim();
            if ( path.length() != 0 )
            {
                if ( !scriptSourceRoots.contains( path ) )
                {
                    scriptSourceRoots.add( path );
                }
            }
        }
    }

    public void addTestCompileSourceRoot( String path )
    {
        if ( path != null )
        {
            path = path.trim();
            if ( path.length() != 0 )
            {
                if ( !testCompileSourceRoots.contains( path ) )
                {
                    testCompileSourceRoots.add( path );
                }
            }
        }
    }

    public List getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    public List getScriptSourceRoots()
    {
        return scriptSourceRoots;
    }

    public List getTestCompileSourceRoots()
    {
        return testCompileSourceRoots;
    }

    public List getCompileClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List list = new ArrayList( getArtifacts().size() );

        list.add( getBuild().getOutputDirectory() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() ) ||
                    Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    addArtifactPath( a, list );
                }
            }
        }
        return list;
    }

    public List getCompileArtifacts()
    {
        List list = new ArrayList( getArtifacts().size() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() ) ||
                    Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    list.add( a );
                }
            }
        }
        return list;
    }

    public List getCompileDependencies()
    {
        Set artifacts = getArtifacts();

        if ( artifacts == null || artifacts.isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List list = new ArrayList( artifacts.size() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: let the scope handler deal with this
            if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() ) ||
                Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
            {
                Dependency dependency = new Dependency();

                dependency.setArtifactId( a.getArtifactId() );
                dependency.setGroupId( a.getGroupId() );
                dependency.setVersion( a.getVersion() );
                dependency.setScope( a.getScope() );
                dependency.setType( a.getType() );
                dependency.setClassifier( a.getClassifier() );

                list.add( dependency );
            }
        }
        return list;
    }

    public List getTestClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List list = new ArrayList( getArtifacts().size() + 1 );

        list.add( getBuild().getTestOutputDirectory() );

        list.add( getBuild().getOutputDirectory() );
        
        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                // NOTE: [jc] scope == 'test' is the widest possible scope, so we don't really need to perform
                // this check...
                // if ( Artifact.SCOPE_TEST.equals( a.getScope() ) || Artifact.SCOPE_COMPILE.equals( a.getScope() ) ||
                //     Artifact.SCOPE_RUNTIME.equals( a.getScope() ) )
                // {
                // }
                File file = a.getFile();
                if ( file == null )
                {
                    throw new DependencyResolutionRequiredException( a );
                }
                list.add( file.getPath() );
            }
        }
        return list;
    }

    public List getTestArtifacts()
    {
        List list = new ArrayList( getArtifacts().size() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                // NOTE: [jc] scope == 'test' is the widest possible scope, so we don't really need to perform
                // this check...
                // if ( Artifact.SCOPE_TEST.equals( a.getScope() ) || Artifact.SCOPE_COMPILE.equals( a.getScope() ) ||
                //      Artifact.SCOPE_RUNTIME.equals( a.getScope() ) )
                // {
                //     list.add( a );
                // }

                list.add( a );
            }
        }
        return list;
    }

    public List getTestDependencies()
    {
        Set artifacts = getArtifacts();

        if ( artifacts == null || artifacts.isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List list = new ArrayList( artifacts.size() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: let the scope handler deal with this
            // NOTE: [jc] scope == 'test' is the widest possible scope, so we don't really need to perform
            // this check...
            // if ( Artifact.SCOPE_TEST.equals( a.getScope() ) || Artifact.SCOPE_COMPILE.equals( a.getScope() ) ||
            //     Artifact.SCOPE_RUNTIME.equals( a.getScope() ) )
            // {
            // }

            Dependency dependency = new Dependency();

            dependency.setArtifactId( a.getArtifactId() );
            dependency.setGroupId( a.getGroupId() );
            dependency.setVersion( a.getVersion() );
            dependency.setScope( a.getScope() );
            dependency.setType( a.getType() );
            dependency.setClassifier( a.getClassifier() );

            list.add( dependency );
        }
        return list;
    }

    public List getRuntimeClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List list = new ArrayList( getArtifacts().size() + 1 );

        list.add( getBuild().getOutputDirectory() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_RUNTIME.equals( a.getScope() ) )
                {
                    File file = a.getFile();
                    if ( file == null )
                    {
                        throw new DependencyResolutionRequiredException( a );
                    }
                    list.add( file.getPath() );
                }
            }
        }
        return list;
    }

    public List getRuntimeArtifacts()
    {
        List list = new ArrayList( getArtifacts().size() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_RUNTIME.equals( a.getScope() ) )
                {
                    list.add( a );
                }
            }
        }
        return list;
    }

    public List getRuntimeDependencies()
    {
        Set artifacts = getArtifacts();

        if ( artifacts == null || artifacts.isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List list = new ArrayList( artifacts.size() );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: let the scope handler deal with this
            if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_RUNTIME.equals( a.getScope() ) )
            {
                Dependency dependency = new Dependency();

                dependency.setArtifactId( a.getArtifactId() );
                dependency.setGroupId( a.getGroupId() );
                dependency.setVersion( a.getVersion() );
                dependency.setScope( a.getScope() );
                dependency.setType( a.getType() );
                dependency.setClassifier( a.getClassifier() );

                list.add( dependency );
            }
        }
        return list;
    }

    public List getSystemClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List list = new ArrayList( getArtifacts().size() );

        list.add( getBuild().getOutputDirectory() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    addArtifactPath( a, list );
                }
            }
        }
        return list;
    }

    public List getSystemArtifacts()
    {
        List list = new ArrayList( getArtifacts().size() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    list.add( a );
                }
            }
        }
        return list;
    }

    public List getSystemDependencies()
    {
        Set artifacts = getArtifacts();

        if ( artifacts == null || artifacts.isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List list = new ArrayList( artifacts.size() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: let the scope handler deal with this
            if ( Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
            {
                Dependency dependency = new Dependency();

                dependency.setArtifactId( a.getArtifactId() );
                dependency.setGroupId( a.getGroupId() );
                dependency.setVersion( a.getVersion() );
                dependency.setScope( a.getScope() );
                dependency.setType( a.getType() );
                dependency.setClassifier( a.getClassifier() );

                list.add( dependency );
            }
        }
        return list;
    }

    // ----------------------------------------------------------------------
    // Delegate to the model
    // ----------------------------------------------------------------------

    public void setModelVersion( String pomVersion )
    {
        model.setModelVersion( pomVersion );
    }

    public String getModelVersion()
    {
        return model.getModelVersion();
    }

    public String getId()
    {
        return model.getId();
    }

    public void setGroupId( String groupId )
    {
        model.setGroupId( groupId );
    }

    public String getGroupId()
    {
        String groupId = model.getGroupId();
        
        if ( groupId == null && model.getParent() != null )
        {
            groupId = model.getParent().getGroupId();
        }
        
        return groupId;
    }

    public void setArtifactId( String artifactId )
    {
        model.setArtifactId( artifactId );
    }

    public String getArtifactId()
    {
        return model.getArtifactId();
    }

    public void setName( String name )
    {
        model.setName( name );
    }

    public String getName()
    {
        // TODO: this should not be allowed to be null.
        if ( model.getName() != null )
        {
            return model.getName();
        }
        else
        {
            return "Unnamed - " + getId();
        }
    }

    public void setVersion( String version )
    {
        model.setVersion( version );
    }

    public String getVersion()
    {
        String version = model.getVersion();
        
        if ( version == null && model.getParent() != null )
        {
            version = model.getParent().getVersion();
        }
        
        return version;
    }

    public String getPackaging()
    {
        return model.getPackaging();
    }

    public void setPackaging( String packaging )
    {
        model.setPackaging( packaging );
    }

    public void setInceptionYear( String inceptionYear )
    {
        model.setInceptionYear( inceptionYear );
    }

    public String getInceptionYear()
    {
        return model.getInceptionYear();
    }

    public void setUrl( String url )
    {
        model.setUrl( url );
    }

    public String getUrl()
    {
        return model.getUrl();
    }

    public Prerequisites getPrerequisites()
    {
        return model.getPrerequisites();
    }

    public void setIssueManagement( IssueManagement issueManagement )
    {
        model.setIssueManagement( issueManagement );
    }

    public CiManagement getCiManagement()
    {
        return model.getCiManagement();
    }

    public void setCiManagement( CiManagement ciManagement )
    {
        model.setCiManagement( ciManagement );
    }

    public IssueManagement getIssueManagement()
    {
        return model.getIssueManagement();
    }

    public void setDistributionManagement( DistributionManagement distributionManagement )
    {
        model.setDistributionManagement( distributionManagement );
    }

    public DistributionManagement getDistributionManagement()
    {
        return model.getDistributionManagement();
    }

    public void setDescription( String description )
    {
        model.setDescription( description );
    }

    public String getDescription()
    {
        return model.getDescription();
    }

    public void setOrganization( Organization organization )
    {
        model.setOrganization( organization );
    }

    public Organization getOrganization()
    {
        return model.getOrganization();
    }

    public void setScm( Scm scm )
    {
        model.setScm( scm );
    }

    public Scm getScm()
    {
        return model.getScm();
    }

    public void setMailingLists( List mailingLists )
    {
        model.setMailingLists( mailingLists );
    }

    public List getMailingLists()
    {
        return model.getMailingLists();
    }

    public void addMailingList( MailingList mailingList )
    {
        model.addMailingList( mailingList );
    }

    public void setDevelopers( List developers )
    {
        model.setDevelopers( developers );
    }

    public List getDevelopers()
    {
        return model.getDevelopers();
    }

    public void addDeveloper( Developer developer )
    {
        model.addDeveloper( developer );
    }

    public void setContributors( List contributors )
    {
        model.setContributors( contributors );
    }

    public List getContributors()
    {
        return model.getContributors();
    }

    public void addContributor( Contributor contributor )
    {
        model.addContributor( contributor );
    }

    public void setBuild( Build build )
    {
        this.buildOverlay = new BuildOverlay( build );

        model.setBuild( build );
    }

    public Build getBuild()
    {
        if ( buildOverlay == null )
        {
            buildOverlay = new BuildOverlay( getModelBuild() );
        }

        return buildOverlay;
    }

    public List getResources()
    {
        return getBuild().getResources();
    }

    public List getTestResources()
    {
        return getBuild().getTestResources();
    }

    public void addResource( Resource resource )
    {
        getBuild().addResource( resource );
    }

    public void addTestResource( Resource testResource )
    {
        getBuild().addTestResource( testResource );
    }

    public void setReporting( Reporting reporting )
    {
        model.setReporting( reporting );
    }

    public Reporting getReporting()
    {
        return model.getReporting();
    }

    public void setLicenses( List licenses )
    {
        model.setLicenses( licenses );
    }

    public List getLicenses()
    {
        return model.getLicenses();
    }

    public void addLicense( License license )
    {
        model.addLicense( license );
    }

    public void setArtifacts( Set artifacts )
    {
        this.artifacts = artifacts;

        // flush the calculated artifactMap
        this.artifactMap = null;
    }

    /**
     * All dependencies that this project has, including transitive ones.
     * Contents are lazily populated, so depending on what phases have run dependencies in some scopes won't be included.
     * eg. if only compile phase has run, dependencies with scope test won't be included. 
     * @return {@link Set} &lt; {@link Artifact} >
     * @see #getDependencyArtifacts() to get only direct dependencies
     */
    public Set getArtifacts()
    {
        return artifacts == null ? Collections.EMPTY_SET : artifacts;
    }

    public Map getArtifactMap()
    {
        if ( artifactMap == null )
        {
            artifactMap = ArtifactUtils.artifactMapByVersionlessId( getArtifacts() );
        }

        return artifactMap;
    }

    public void setPluginArtifacts( Set pluginArtifacts )
    {
        this.pluginArtifacts = pluginArtifacts;

        this.pluginArtifactMap = null;
    }

    public Set getPluginArtifacts()
    {
        return pluginArtifacts;
    }

    public Map getPluginArtifactMap()
    {
        if ( pluginArtifactMap == null )
        {
            pluginArtifactMap = ArtifactUtils.artifactMapByVersionlessId( getPluginArtifacts() );
        }

        return pluginArtifactMap;
    }

    public void setReportArtifacts( Set reportArtifacts )
    {
        this.reportArtifacts = reportArtifacts;

        this.reportArtifactMap = null;
    }

    public Set getReportArtifacts()
    {
        return reportArtifacts;
    }

    public Map getReportArtifactMap()
    {
        if ( reportArtifactMap == null )
        {
            reportArtifactMap = ArtifactUtils.artifactMapByVersionlessId( getReportArtifacts() );
        }

        return reportArtifactMap;
    }

    public void setExtensionArtifacts( Set extensionArtifacts )
    {
        this.extensionArtifacts = extensionArtifacts;

        this.extensionArtifactMap = null;
    }

    public Set getExtensionArtifacts()
    {
        return this.extensionArtifacts;
    }

    public Map getExtensionArtifactMap()
    {
        if ( extensionArtifactMap == null )
        {
            extensionArtifactMap = ArtifactUtils.artifactMapByVersionlessId( getExtensionArtifacts() );
        }

        return extensionArtifactMap;
    }

    public void setParentArtifact( Artifact parentArtifact )
    {
        this.parentArtifact = parentArtifact;
    }

    public Artifact getParentArtifact()
    {
        return parentArtifact;
    }

    public List getRepositories()
    {
        return model.getRepositories();
    }

    // ----------------------------------------------------------------------
    // Plugins
    // ----------------------------------------------------------------------

    public List getReportPlugins()
    {
        if ( model.getReporting() == null )
        {
            return null;
        }
        return model.getReporting().getPlugins();

    }

    public List getBuildPlugins()
    {
        if ( model.getBuild() == null )
        {
            return null;
        }
        return model.getBuild().getPlugins();
    }

    public List getModules()
    {
        return model.getModules();
    }

    public PluginManagement getPluginManagement()
    {
        PluginManagement pluginMgmt = null;

        Build build = model.getBuild();
        if ( build != null )
        {
            pluginMgmt = build.getPluginManagement();
        }

        return pluginMgmt;
    }
    
    private Build getModelBuild()
    {
        Build build = model.getBuild();

        if ( build == null )
        {
            build = new Build();

            model.setBuild( build );
        }
        
        return build;
    }

    public void addPlugin( Plugin plugin )
    {
        Build build = getModelBuild();

        if ( !build.getPluginsAsMap().containsKey( plugin.getKey() ) )
        {
            injectPluginManagementInfo( plugin );

            build.addPlugin( plugin );
            build.flushPluginMap();
        }
    }
    
    public void injectPluginManagementInfo( Plugin plugin )
    {
        PluginManagement pm = getModelBuild().getPluginManagement();

        if ( pm != null )
        {
            Map pmByKey = pm.getPluginsAsMap();

            String pluginKey = plugin.getKey();

            if ( pmByKey != null && pmByKey.containsKey( pluginKey ) )
            {
                Plugin pmPlugin = (Plugin) pmByKey.get( pluginKey );

                ModelUtils.mergePluginDefinitions( plugin, pmPlugin, false );
            }
        }
    }

    public List getCollectedProjects()
    {
        return collectedProjects;
    }

    public void setCollectedProjects( List collectedProjects )
    {
        this.collectedProjects = collectedProjects;
    }

    public void setPluginArtifactRepositories( List pluginArtifactRepositories )
    {
        this.pluginArtifactRepositories = pluginArtifactRepositories;
    }

    /**
     * @return a list of ArtifactRepository objects constructed
     *         from the Repository objects returned by getPluginRepositories.
     */
    public List getPluginArtifactRepositories()
    {
        return pluginArtifactRepositories;
    }

    public ArtifactRepository getDistributionManagementArtifactRepository()
    {
        return getArtifact().isSnapshot() && snapshotArtifactRepository != null ? snapshotArtifactRepository
            : releaseArtifactRepository;
    }

    public List getPluginRepositories()
    {
        return model.getPluginRepositories();
    }

    public void setActiveProfiles( List activeProfiles )
    {
        this.activeProfiles.addAll( activeProfiles );
    }

    public List getActiveProfiles()
    {
        return activeProfiles;
    }

    public void addAttachedArtifact( Artifact artifact )
    {
        getAttachedArtifacts().add( artifact );
    }

    public List getAttachedArtifacts()
    {
        if ( attachedArtifacts == null )
        {
            attachedArtifacts = new ArrayList();
        }
        return attachedArtifacts;
    }

    public Xpp3Dom getGoalConfiguration( String pluginGroupId, String pluginArtifactId, String executionId,
                                         String goalId )
    {
        Xpp3Dom dom = null;

        // ----------------------------------------------------------------------
        // I would like to be able to lookup the Mojo object using a key but
        // we have a limitation in modello that will be remedied shortly. So
        // for now I have to iterate through and see what we have.
        // ----------------------------------------------------------------------

        if ( getBuildPlugins() != null )
        {
            for ( Iterator iterator = getBuildPlugins().iterator(); iterator.hasNext(); )
            {
                Plugin plugin = (Plugin) iterator.next();

                if ( pluginGroupId.equals( plugin.getGroupId() ) && pluginArtifactId.equals( plugin.getArtifactId() ) )
                {
                    dom = (Xpp3Dom) plugin.getConfiguration();

                    if ( executionId != null )
                    {
                        PluginExecution execution = (PluginExecution) plugin.getExecutionsAsMap().get( executionId );
                        if ( execution != null )
                        {
                            Xpp3Dom executionConfiguration = (Xpp3Dom) execution.getConfiguration();
                            if ( executionConfiguration != null )
                            {
                                Xpp3Dom newDom = new Xpp3Dom( executionConfiguration );
                                dom = Xpp3Dom.mergeXpp3Dom( newDom, dom );
                            }
                        }
                    }
                    break;
                }
            }
        }

        if ( dom != null )
        {
            // make a copy so the original in the POM doesn't get messed with
            dom = new Xpp3Dom( dom );
        }

        return dom;
    }

    public Xpp3Dom getReportConfiguration( String pluginGroupId, String pluginArtifactId, String reportSetId )
    {
        Xpp3Dom dom = null;

        // ----------------------------------------------------------------------
        // I would like to be able to lookup the Mojo object using a key but
        // we have a limitation in modello that will be remedied shortly. So
        // for now I have to iterate through and see what we have.
        // ----------------------------------------------------------------------

        if ( getReportPlugins() != null )
        {
            for ( Iterator iterator = getReportPlugins().iterator(); iterator.hasNext(); )
            {
                ReportPlugin plugin = (ReportPlugin) iterator.next();

                if ( pluginGroupId.equals( plugin.getGroupId() ) && pluginArtifactId.equals( plugin.getArtifactId() ) )
                {
                    dom = (Xpp3Dom) plugin.getConfiguration();

                    if ( reportSetId != null )
                    {
                        ReportSet reportSet = (ReportSet) plugin.getReportSetsAsMap().get( reportSetId );
                        if ( reportSet != null )
                        {
                            Xpp3Dom executionConfiguration = (Xpp3Dom) reportSet.getConfiguration();
                            if ( executionConfiguration != null )
                            {
                                Xpp3Dom newDom = new Xpp3Dom( executionConfiguration );
                                dom = Xpp3Dom.mergeXpp3Dom( newDom, dom );
                            }
                        }
                    }
                    break;
                }
            }
        }

        if ( dom != null )
        {
            // make a copy so the original in the POM doesn't get messed with
            dom = new Xpp3Dom( dom );
        }

        return dom;
    }

    public MavenProject getExecutionProject()
    {
        return executionProject;
    }

    public void setExecutionProject( MavenProject executionProject )
    {
        this.executionProject = executionProject;
    }

    public void writeModel( Writer writer )
        throws IOException
    {
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();

        pomWriter.write( writer, getModel() );
    }

    public void writeOriginalModel( Writer writer )
        throws IOException
    {
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();

        pomWriter.write( writer, getOriginalModel() );
    }

    /**
     * Direct dependencies that this project has.
     * @return {@link Set} &lt; {@link Artifact} >
     * @see #getArtifacts() to get all transitive dependencies
     */
    public Set getDependencyArtifacts()
    {
        return dependencyArtifacts;
    }

    public void setDependencyArtifacts( Set dependencyArtifacts )
    {
        this.dependencyArtifacts = dependencyArtifacts;
    }

    public void setReleaseArtifactRepository( ArtifactRepository releaseArtifactRepository )
    {
        this.releaseArtifactRepository = releaseArtifactRepository;
    }

    public void setSnapshotArtifactRepository( ArtifactRepository snapshotArtifactRepository )
    {
        this.snapshotArtifactRepository = snapshotArtifactRepository;
    }

    public void setOriginalModel( Model originalModel )
    {
        this.originalModel = originalModel;
    }

    public Model getOriginalModel()
    {
        return originalModel;
    }

    public void setManagedVersionMap( Map map )
    {
        this.managedVersionMap = map;
    }

    public Map getManagedVersionMap()
    {
        return this.managedVersionMap;
    }

    public boolean equals( Object other )
    {
        if ( other == this )
        {
            return true;
        }
        else if ( !( other instanceof MavenProject ) )
        {
            return false;
        }
        else
        {
            MavenProject otherProject = (MavenProject) other;

            return getId().equals( otherProject.getId() );
        }
    }

    public int hashCode()
    {
        return getId().hashCode();
    }

    public List getBuildExtensions()
    {
        Build build = getBuild();
        if ( build == null || build.getExtensions() == null )
        {
            return Collections.EMPTY_LIST;
        }
        else
        {
            return build.getExtensions();
        }
    }

    /**
     * @todo the lazy initialisation of this makes me uneasy.
     * @return {@link Set} &lt; {@link Artifact} >
     */
    public Set createArtifacts( ArtifactFactory artifactFactory, String inheritedScope,
                                ArtifactFilter dependencyFilter )
        throws InvalidDependencyVersionException
    {
        return MavenMetadataSource.createArtifacts( artifactFactory, getDependencies(), inheritedScope,
                                                    dependencyFilter, this );
    }

    public void addProjectReference( MavenProject project )
    {
        projectReferences.put( getProjectReferenceId( project.getGroupId(), project.getArtifactId(), project.getVersion() ), project );
    }

    private static String getProjectReferenceId( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * @deprecated Use MavenProjectHelper.attachArtifact(..) instead.
     */
    public void attachArtifact( String type, String classifier, File file )
    {
    }

    public Properties getProperties()
    {
        return getModel().getProperties();
    }

    public List getFilters()
    {
        return getBuild().getFilters();
    }

    public Map getProjectReferences()
    {
        return projectReferences;
    }

    public boolean isExecutionRoot()
    {
        return executionRoot;
    }

    public void setExecutionRoot( boolean executionRoot )
    {
        this.executionRoot = executionRoot;
    }

    public String getDefaultGoal()
    {
        return getBuild() != null ? getBuild().getDefaultGoal() : null;
    }

    public Artifact replaceWithActiveArtifact( Artifact pluginArtifact )
    {
        if ( getProjectReferences() != null && !getProjectReferences().isEmpty() )
        {
            String refId = getProjectReferenceId( pluginArtifact.getGroupId(), pluginArtifact.getArtifactId(), pluginArtifact.getVersion() );
            MavenProject ref = (MavenProject) getProjectReferences().get( refId );
            if ( ref != null && ref.getArtifact() != null )
            {
                // TODO: if not matching, we should get the correct artifact from that project (attached)
                if ( ref.getArtifact().getDependencyConflictId().equals( pluginArtifact.getDependencyConflictId() ) )
                {
                    // if the project artifact doesn't exist, don't use it. We haven't built that far.
                    if ( ref.getArtifact().getFile() != null && ref.getArtifact().getFile().exists() )
                    {
                        pluginArtifact = new ActiveProjectArtifact( ref, pluginArtifact );
                        return pluginArtifact;
                    }
                    else
                    {
/* TODO...
                        logger.warn( "Artifact found in the reactor has not been built when it's use was " +
                            "attempted - resolving from the repository instead" );
*/
                    }
                }

                Iterator itr = ref.getAttachedArtifacts().iterator();
                while(itr.hasNext()) {
                    Artifact attached = (Artifact) itr.next();
                    if( attached.getDependencyConflictId().equals(pluginArtifact.getDependencyConflictId()) ) {
                        /* TODO: if I use the original, I get an exception below:
                            java.lang.UnsupportedOperationException: Cannot change the download information for an attached artifact. It is derived from the main artifact.
                            at org.apache.maven.project.artifact.AttachedArtifact.setDownloadUrl(AttachedArtifact.java:89)
                            at org.apache.maven.project.artifact.MavenMetadataSource.retrieve(MavenMetadataSource.java:205)
                            at org.apache.maven.artifact.resolver.DefaultArtifactCollector.recurse(DefaultArtifactCollector.java:275)
                            at org.apache.maven.artifact.resolver.DefaultArtifactCollector.collect(DefaultArtifactCollector.java:67)
                            at org.apache.maven.artifact.resolver.DefaultArtifactResolver.resolveTransitively(DefaultArtifactResolver.java:223)
                            at org.apache.maven.artifact.resolver.DefaultArtifactResolver.resolveTransitively(DefaultArtifactResolver.java:211)
                            at org.apache.maven.artifact.resolver.DefaultArtifactResolver.resolveTransitively(DefaultArtifactResolver.java:182)
                            at org.apache.maven.plugin.DefaultPluginManager.resolveTransitiveDependencies(DefaultPluginManager.java:1117)
                            at org.apache.maven.plugin.DefaultPluginManager.executeMojo(DefaultPluginManager.java:366)
                            at org.apache.maven.lifecycle.DefaultLifecycleExecutor.executeGoals(DefaultLifecycleExecutor.java:534)
                            at org.apache.maven.lifecycle.DefaultLifecycleExecutor.executeGoalWithLifecycle(DefaultLifecycleExecutor.java:475)
                            at org.apache.maven.lifecycle.DefaultLifecycleExecutor.executeGoal(DefaultLifecycleExecutor.java:454)
                            at org.apache.maven.lifecycle.DefaultLifecycleExecutor.executeGoalAndHandleFailures(DefaultLifecycleExecutor.java:306)
                            at org.apache.maven.lifecycle.DefaultLifecycleExecutor.executeTaskSegments(DefaultLifecycleExecutor.java:273)
                            at org.apache.maven.lifecycle.DefaultLifecycleExecutor.execute(DefaultLifecycleExecutor.java:140)
                            at org.apache.maven.DefaultMaven.doExecute(DefaultMaven.java:322)
                            at org.apache.maven.DefaultMaven.execute(DefaultMaven.java:115)
                            at org.apache.maven.cli.MavenCli.main(MavenCli.java:256)
                        */
                        Artifact resultArtifact=ArtifactUtils.copyArtifact(attached);
                        resultArtifact.setScope(pluginArtifact.getScope());
                        return resultArtifact;
                    }
                }
            }
        }
        return pluginArtifact;
    }
    
    private void addArtifactPath(Artifact a, List list) throws DependencyResolutionRequiredException
    {
        String refId = getProjectReferenceId( a.getGroupId(), a.getArtifactId(), a.getVersion() );
        MavenProject project = (MavenProject) projectReferences.get( refId );
        
        boolean projectDirFound = false;
        if ( project != null )
        {
            if (a.getType().equals("test-jar"))
            {
                File testOutputDir = new File( project.getBuild().getTestOutputDirectory() );
                if ( testOutputDir.exists() )
                {
                    list.add( testOutputDir.getAbsolutePath() );
                    projectDirFound = true;
                }
            }
            else
            {
                list.add( project.getBuild().getOutputDirectory() );
                projectDirFound = true;
            }
        }
        if ( ! projectDirFound )
        {
            File file = a.getFile();
            if ( file == null )
            {
                throw new DependencyResolutionRequiredException( a );
            }
            list.add( file.getPath() );
        }
    }
    
    /**
     * Default toString
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer(30);
        sb.append( "MavenProject: " );
        sb.append( this.getGroupId() );
        sb.append( ":" );
        sb.append( this.getArtifactId() );
        sb.append( ":" );
        sb.append( this.getVersion() );
        sb.append( " @ " );
        
        try 
        {
            sb.append( this.getFile().getPath() );
        }
        catch (NullPointerException e)
        {
            //don't log it.
        }
        
        return sb.toString();        
    }
    
}
