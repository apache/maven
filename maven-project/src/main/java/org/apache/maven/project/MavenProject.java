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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import org.apache.maven.model.Extension;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.artifact.ActiveProjectArtifact;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.codehaus.plexus.logging.Logger;
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
    implements Cloneable
{
    public static final String EMPTY_PROJECT_GROUP_ID = "unknown";

    public static final String EMPTY_PROJECT_ARTIFACT_ID = "empty-project";

    public static final String EMPTY_PROJECT_VERSION = "0";

    private Model model;

    private MavenProject parent;

    private File file;

    private Set<Artifact> artifacts;

    private Artifact parentArtifact;

    private Set<Artifact> pluginArtifacts;

    private List<ArtifactRepository> remoteArtifactRepositories;

    private List<MavenProject> collectedProjects = Collections.emptyList();

    private List<Artifact> attachedArtifacts;

    private MavenProject executionProject;

    private List<String> compileSourceRoots = new ArrayList<String>();

    private List<String> testCompileSourceRoots = new ArrayList<String>();

    private List<String> scriptSourceRoots = new ArrayList<String>();

    private List<ArtifactRepository> pluginArtifactRepositories;

    private ArtifactRepository releaseArtifactRepository;

    private ArtifactRepository snapshotArtifactRepository;

    private List<Profile> activeProfiles = new ArrayList<Profile>();

    private Set<Artifact> dependencyArtifacts;

    private Artifact artifact;

    // calculated.
    private Map<String, Artifact> artifactMap;

    private Model originalModel;

    private Map<String, Artifact> pluginArtifactMap;

    private Set<Artifact> reportArtifacts;

    private Map<String, Artifact> reportArtifactMap;

    private Set<Artifact> extensionArtifacts;

    private Map<String, Artifact> extensionArtifactMap;

    private Map<String, Artifact> managedVersionMap;

    private Map<String, MavenProject> projectReferences = new HashMap<String, MavenProject>();

    private boolean executionRoot;

    private Map<String, String> moduleAdjustments;

    private File basedir;

    private Logger logger;

    private ProjectBuilderConfiguration projectBuilderConfiguration;

    public MavenProject()
    {
        Model model = new Model();

        model.setGroupId( EMPTY_PROJECT_GROUP_ID );
        model.setArtifactId( EMPTY_PROJECT_ARTIFACT_ID );
        model.setVersion( EMPTY_PROJECT_VERSION );

        this.setModel( model );
    }

    public MavenProject( Model model )
    {
        this.setModel( model );
    }

    public MavenProject( Model model, Logger logger )
    {
        this.setModel( model );
        this.setLogger( logger );
    }

    /**
     * @deprecated use {@link #clone()} so subclasses can provide a copy of the same class
     */
    public MavenProject( MavenProject project )
    {
        deepCopy( project );
    }

    private void deepCopy( MavenProject project )
    {
        // disown the parent

        // copy fields
        setFile( project.getFile() );

        // don't need a deep copy, they don't get modified or added/removed to/from - but make them unmodifiable to be
        // sure!
        if ( project.getDependencyArtifacts() != null )
        {
            setDependencyArtifacts( Collections.unmodifiableSet( project.getDependencyArtifacts() ) );
        }

        if ( project.getArtifacts() != null )
        {
            setArtifacts( Collections.unmodifiableSet( project.getArtifacts() ) );
        }

        if ( project.getPluginArtifacts() != null )
        {
            setPluginArtifacts( Collections.unmodifiableSet( project.getPluginArtifacts() ) );
        }

        if ( project.getReportArtifacts() != null )
        {
            setReportArtifacts( Collections.unmodifiableSet( project.getReportArtifacts() ) );
        }

        if ( project.getExtensionArtifacts() != null )
        {
            setExtensionArtifacts( Collections.unmodifiableSet( project.getExtensionArtifacts() ) );
        }

        setParentArtifact( ( project.getParentArtifact() ) );

        if ( project.getRemoteArtifactRepositories() != null )
        {
            setRemoteArtifactRepositories( Collections.unmodifiableList( project.getRemoteArtifactRepositories() ) );
        }

        if ( project.getPluginArtifactRepositories() != null )
        {
            setPluginArtifactRepositories( ( Collections.unmodifiableList( project.getPluginArtifactRepositories() ) ) );
        }

        if ( project.getCollectedProjects() != null )
        {
            setCollectedProjects( ( Collections.unmodifiableList( project.getCollectedProjects() ) ) );
        }

        if ( project.getActiveProfiles() != null )
        {
            setActiveProfiles( ( Collections.unmodifiableList( project.getActiveProfiles() ) ) );
        }

        if ( project.getAttachedArtifacts() != null )
        {
            // clone properties modifyable by plugins in a forked lifecycle
            setAttachedArtifacts( new ArrayList<Artifact>( project.getAttachedArtifacts() ) );
        }

        if ( project.getCompileSourceRoots() != null )
        {
            // clone source roots
            setCompileSourceRoots( ( new ArrayList<String>( project.getCompileSourceRoots() ) ) );
        }

        if ( project.getTestCompileSourceRoots() != null )
        {
            setTestCompileSourceRoots( ( new ArrayList<String>( project.getTestCompileSourceRoots() ) ) );
        }

        if ( project.getScriptSourceRoots() != null )
        {
            setScriptSourceRoots( ( new ArrayList<String>( project.getScriptSourceRoots() ) ) );
        }

        setModel( ( ModelUtils.cloneModel( project.getModel() ) ) );

        if ( project.getOriginalModel() != null )
        {
            setOriginalModel( ( ModelUtils.cloneModel( project.getOriginalModel() ) ) );
        }

        setExecutionRoot( project.isExecutionRoot() );

        if ( project.getArtifact() != null )
        {
            setArtifact( ArtifactUtils.copyArtifact( project.getArtifact() ) );
        }

        if ( project.getManagedVersionMap() != null )
        {
            setManagedVersionMap( new ManagedVersionMap( project.getManagedVersionMap() ) );
        }

        if ( project.getReleaseArtifactRepository() != null )
        {
            setReleaseArtifactRepository( project.getReleaseArtifactRepository() );
        }

        if ( project.getSnapshotArtifactRepository() != null )
        {
            setSnapshotArtifactRepository( project.getSnapshotArtifactRepository() );
        }

        if ( project.isConcrete() )
        {
            setDynamicBuild( ModelUtils.cloneBuild( project.getDynamicBuild() ) );
            setOriginalInterpolatedBuild( ModelUtils.cloneBuild( project.getOriginalInterpolatedBuild() ) );

            List<String> dynamicRoots = project.getDynamicCompileSourceRoots();
            if ( dynamicRoots != null )
            {
                setDynamicCompileSourceRoots( new ArrayList<String>( dynamicRoots ) );
                setOriginalInterpolatedCompileSourceRoots( new ArrayList<String>( project.getOriginalInterpolatedCompileSourceRoots() ) );
            }

            dynamicRoots = project.getDynamicTestCompileSourceRoots();
            if ( dynamicRoots != null )
            {
                setDynamicTestCompileSourceRoots( new ArrayList<String>( dynamicRoots ) );
                setOriginalInterpolatedTestCompileSourceRoots( new ArrayList<String>( project.getOriginalInterpolatedTestCompileSourceRoots() ) );
            }

            dynamicRoots = project.getDynamicScriptSourceRoots();
            if ( dynamicRoots != null )
            {
                setDynamicScriptSourceRoots( new ArrayList<String>( dynamicRoots ) );
                setOriginalInterpolatedScriptSourceRoots( new ArrayList<String>( project.getOriginalInterpolatedScriptSourceRoots() ) );
            }
        }

        preservedProperties = project.preservedProperties;
        preservedBasedir = project.preservedBasedir;
        setConcrete( project.isConcrete() );
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
            moduleAdjustments = new HashMap<String, String>();

            List<String> modules = getModules();
            if ( modules != null )
            {
                for ( String modulePath : modules )
                {
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

    public void setRemoteArtifactRepositories( List<ArtifactRepository> remoteArtifactRepositories )
    {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
    }

    public List<ArtifactRepository> getRemoteArtifactRepositories()
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
        if ( file == null )
        {
            return;
        }

        if ( basedir == null )
        {
            basedir = file.getParentFile();
        }

        this.file = file;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public File getBasedir()
    {
        return basedir;
    }

    public void setDependencies( List<Dependency> dependencies )
    {
        getModel().setDependencies( dependencies );
    }

    public List<Dependency> getDependencies()
    {
        return getModel().getDependencies();
    }

    public DependencyManagement getDependencyManagement()
    {
        return getModel().getDependencyManagement();
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
                if ( !getCompileSourceRoots().contains( path ) )
                {
                    getCompileSourceRoots().add( path );
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
                if ( !getScriptSourceRoots().contains( path ) )
                {
                    getScriptSourceRoots().add( path );
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
                if ( !getTestCompileSourceRoots().contains( path ) )
                {
                    getTestCompileSourceRoots().add( path );
                }
            }
        }
    }

    public List<String> getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    public List<String> getScriptSourceRoots()
    {
        return scriptSourceRoots;
    }

    public List<String> getTestCompileSourceRoots()
    {
        return testCompileSourceRoots;
    }

    public List<String> getCompileClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<String>( getArtifacts().size() );

        list.add( getBuild().getOutputDirectory() );

        for ( Artifact a : getArtifacts() )
        {
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    addArtifactPath( a, list );
                }
            }
        }
        return list;
    }

    public List<Artifact> getCompileArtifacts()
    {
        List<Artifact> list = new ArrayList<Artifact>( getArtifacts().size() );

        for ( Artifact a : getArtifacts() )
        {
            // TODO: classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    list.add( a );
                }
            }
        }
        return list;
    }

    public List<Dependency> getCompileDependencies()
    {
        Set<Artifact> artifacts = getArtifacts();

        if ( artifacts == null || artifacts.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<Dependency>( artifacts.size() );

        for ( Artifact a : getArtifacts() )
        {
            // TODO: let the scope handler deal with this
            if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
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

    public List<String> getTestClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<String>( getArtifacts().size() + 1 );

        list.add( getBuild().getTestOutputDirectory() );

        list.add( getBuild().getOutputDirectory() );

        for ( Artifact a : getArtifacts() )
        {
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

    public List<Artifact> getTestArtifacts()
    {
        List<Artifact> list = new ArrayList<Artifact>( getArtifacts().size() );

        for ( Artifact a : getArtifacts() )
        {
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

    public List<Dependency> getTestDependencies()
    {
        Set<Artifact> artifacts = getArtifacts();

        if ( artifacts == null || artifacts.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<Dependency>( artifacts.size() );

        for ( Artifact a : getArtifacts() )
        {
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

    public List<String> getRuntimeClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<String>( getArtifacts().size() + 1 );

        list.add( getBuild().getOutputDirectory() );

        for ( Artifact a : getArtifacts() )
        {
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

    public List<Artifact> getRuntimeArtifacts()
    {
        List<Artifact> list = new ArrayList<Artifact>( getArtifacts().size() );

        for ( Artifact a : getArtifacts() )
        {
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

    public List<Dependency> getRuntimeDependencies()
    {
        Set<Artifact> artifacts = getArtifacts();

        if ( artifacts == null || artifacts.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<Dependency>( artifacts.size() );

        for ( Artifact a : artifacts )
        {
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

    public List<String> getSystemClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<String>( getArtifacts().size() );

        list.add( getBuild().getOutputDirectory() );

        for ( Artifact a : getArtifacts() )
        {
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

    public List<Artifact> getSystemArtifacts()
    {
        List<Artifact> list = new ArrayList<Artifact>( getArtifacts().size() );

        for ( Artifact a : getArtifacts() )
        {
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

    public List<Dependency> getSystemDependencies()
    {
        Set<Artifact> artifacts = getArtifacts();

        if ( artifacts == null || artifacts.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<Dependency>( artifacts.size() );

        for ( Artifact a : getArtifacts() )
        {
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
        getModel().setModelVersion( pomVersion );
    }

    public String getModelVersion()
    {
        return getModel().getModelVersion();
    }

    public String getId()
    {
        return getModel().getId();
    }

    public void setGroupId( String groupId )
    {
        getModel().setGroupId( groupId );
    }

    public String getGroupId()
    {
        String groupId = getModel().getGroupId();

        if ( ( groupId == null ) && ( getModel().getParent() != null ) )
        {
            groupId = getModel().getParent().getGroupId();
        }

        return groupId;
    }

    public void setArtifactId( String artifactId )
    {
        getModel().setArtifactId( artifactId );
    }

    public String getArtifactId()
    {
        return getModel().getArtifactId();
    }

    public void setName( String name )
    {
        getModel().setName( name );
    }

    public String getName()
    {
        // TODO: this should not be allowed to be null.
        if ( getModel().getName() != null )
        {
            return getModel().getName();
        }
        else
        {
            return "Unnamed - " + getId();
        }
    }

    public void setVersion( String version )
    {
        getModel().setVersion( version );
    }

    public String getVersion()
    {
        String version = getModel().getVersion();

        if ( ( version == null ) && ( getModel().getParent() != null ) )
        {
            version = getModel().getParent().getVersion();
        }

        return version;
    }

    public String getPackaging()
    {
        return getModel().getPackaging();
    }

    public void setPackaging( String packaging )
    {
        getModel().setPackaging( packaging );
    }

    public void setInceptionYear( String inceptionYear )
    {
        getModel().setInceptionYear( inceptionYear );
    }

    public String getInceptionYear()
    {
        return getModel().getInceptionYear();
    }

    public void setUrl( String url )
    {
        getModel().setUrl( url );
    }

    public String getUrl()
    {
        return getModel().getUrl();
    }

    public Prerequisites getPrerequisites()
    {
        return getModel().getPrerequisites();
    }

    public void setIssueManagement( IssueManagement issueManagement )
    {
        getModel().setIssueManagement( issueManagement );
    }

    public CiManagement getCiManagement()
    {
        return getModel().getCiManagement();
    }

    public void setCiManagement( CiManagement ciManagement )
    {
        getModel().setCiManagement( ciManagement );
    }

    public IssueManagement getIssueManagement()
    {
        return getModel().getIssueManagement();
    }

    public void setDistributionManagement( DistributionManagement distributionManagement )
    {
        getModel().setDistributionManagement( distributionManagement );
    }

    public DistributionManagement getDistributionManagement()
    {
        return getModel().getDistributionManagement();
    }

    public void setDescription( String description )
    {
        getModel().setDescription( description );
    }

    public String getDescription()
    {
        return getModel().getDescription();
    }

    public void setOrganization( Organization organization )
    {
        getModel().setOrganization( organization );
    }

    public Organization getOrganization()
    {
        return getModel().getOrganization();
    }

    public void setScm( Scm scm )
    {
        getModel().setScm( scm );
    }

    public Scm getScm()
    {
        return getModel().getScm();
    }

    public void setMailingLists( List<MailingList> mailingLists )
    {
        getModel().setMailingLists( mailingLists );
    }

    public List<MailingList> getMailingLists()
    {
        return getModel().getMailingLists();
    }

    public void addMailingList( MailingList mailingList )
    {
        getModel().addMailingList( mailingList );
    }

    public void setDevelopers( List<Developer> developers )
    {
        getModel().setDevelopers( developers );
    }

    public List<Developer> getDevelopers()
    {
        return getModel().getDevelopers();
    }

    public void addDeveloper( Developer developer )
    {
        getModel().addDeveloper( developer );
    }

    public void setContributors( List<Contributor> contributors )
    {
        getModel().setContributors( contributors );
    }

    public List<Contributor> getContributors()
    {
        return getModel().getContributors();
    }

    public void addContributor( Contributor contributor )
    {
        getModel().addContributor( contributor );
    }

    public void setBuild( Build build )
    {
        getModel().setBuild( build );
    }

    public Build getBuild()
    {
        return getModelBuild();
    }

    public List<Resource> getResources()
    {
        return getBuild().getResources();
    }

    public List<Resource> getTestResources()
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
        getModel().setReporting( reporting );
    }

    public Reporting getReporting()
    {
        return getModel().getReporting();
    }

    public void setLicenses( List<License> licenses )
    {
        getModel().setLicenses( licenses );
    }

    public List<License> getLicenses()
    {
        return getModel().getLicenses();
    }

    public void addLicense( License license )
    {
        getModel().addLicense( license );
    }

    public void setArtifacts( Set<Artifact> artifacts )
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
    public Set<Artifact> getArtifacts()
    {
        return artifacts == null ? Collections.<Artifact>emptySet() : artifacts;
    }

    public Map<String, Artifact> getArtifactMap()
    {
        if ( artifactMap == null )
        {
            artifactMap = ArtifactUtils.artifactMapByVersionlessId( getArtifacts() );
        }

        return artifactMap;
    }

    public void setPluginArtifacts( Set<Artifact> pluginArtifacts )
    {
        this.pluginArtifacts = pluginArtifacts;

        this.pluginArtifactMap = null;
    }

    public Set<Artifact> getPluginArtifacts()
    {
        return pluginArtifacts;
    }

    public Map<String, Artifact> getPluginArtifactMap()
    {
        if ( pluginArtifactMap == null )
        {
            pluginArtifactMap = ArtifactUtils.artifactMapByVersionlessId( getPluginArtifacts() );
        }

        return pluginArtifactMap;
    }

    public void setReportArtifacts( Set<Artifact> reportArtifacts )
    {
        this.reportArtifacts = reportArtifacts;

        this.reportArtifactMap = null;
    }

    public Set<Artifact> getReportArtifacts()
    {
        return reportArtifacts;
    }

    public Map<String, Artifact> getReportArtifactMap()
    {
        if ( reportArtifactMap == null )
        {
            reportArtifactMap = ArtifactUtils.artifactMapByVersionlessId( getReportArtifacts() );
        }

        return reportArtifactMap;
    }

    public void setExtensionArtifacts( Set<Artifact> extensionArtifacts )
    {
        this.extensionArtifacts = extensionArtifacts;

        this.extensionArtifactMap = null;
    }

    public Set<Artifact> getExtensionArtifacts()
    {
        return this.extensionArtifacts;
    }

    public Map<String, Artifact> getExtensionArtifactMap()
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

    public List<Repository> getRepositories()
    {
        return getModel().getRepositories();
    }

    // ----------------------------------------------------------------------
    // Plugins
    // ----------------------------------------------------------------------

    public List<ReportPlugin> getReportPlugins()
    {
        if ( getModel().getReporting() == null )
        {
            return null;
        }
        return getModel().getReporting().getPlugins();

    }

    public List<Plugin> getBuildPlugins()
    {
        if ( getModel().getBuild() == null )
        {
            return null;
        }
        return getModel().getBuild().getPlugins();
    }

    public List<String> getModules()
    {
        return getModel().getModules();
    }

    public PluginManagement getPluginManagement()
    {
        PluginManagement pluginMgmt = null;

        Build build = getModel().getBuild();
        if ( build != null )
        {
            pluginMgmt = build.getPluginManagement();
        }

        return pluginMgmt;
    }

    private Build getModelBuild()
    {
        Build build = getModel().getBuild();

        if ( build == null )
        {
            build = new Build();

            getModel().setBuild( build );
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
            Map<String, Plugin> pmByKey = pm.getPluginsAsMap();

            String pluginKey = plugin.getKey();

            if ( pmByKey != null && pmByKey.containsKey( pluginKey ) )
            {
                Plugin pmPlugin = pmByKey.get( pluginKey );

                ModelUtils.mergePluginDefinitions( plugin, pmPlugin, false );
            }
        }
    }

    public List<MavenProject> getCollectedProjects()
    {
        return collectedProjects;
    }

    public void setCollectedProjects( List<MavenProject> collectedProjects )
    {
        this.collectedProjects = collectedProjects;
    }

    public void setPluginArtifactRepositories( List<ArtifactRepository> pluginArtifactRepositories )
    {
        this.pluginArtifactRepositories = pluginArtifactRepositories;
    }

    /**
     * @return a list of ArtifactRepository objects constructed
     *         from the Repository objects returned by getPluginRepositories.
     */
    public List<ArtifactRepository> getPluginArtifactRepositories()
    {
        return pluginArtifactRepositories;
    }

    public ArtifactRepository getDistributionManagementArtifactRepository()
    {
        return getArtifact().isSnapshot() && ( getSnapshotArtifactRepository() != null ) ? getSnapshotArtifactRepository()
            : getReleaseArtifactRepository();
    }

    public List<Repository> getPluginRepositories()
    {
        return getModel().getPluginRepositories();
    }

    public void setActiveProfiles( List<Profile> activeProfiles )
    {
        this.activeProfiles.addAll( activeProfiles );
    }

    public List<Profile> getActiveProfiles()
    {
        return activeProfiles;
    }

    public void addAttachedArtifact( Artifact artifact )
    {
        getAttachedArtifacts().add( artifact );
    }

    public List<Artifact> getAttachedArtifacts()
    {
        if ( attachedArtifacts == null )
        {
            attachedArtifacts = new ArrayList<Artifact>();
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
            for ( Plugin plugin : getBuildPlugins() )
            {
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

//        PluginManagement pluginManagement = getBuild().getPluginManagement();
//        if ( pluginManagement != null && pluginManagement.getPlugins() != null )
//        {
//            for ( Iterator iterator = pluginManagement.getPlugins().iterator(); iterator.hasNext(); )
//            {
//                Plugin plugin = (Plugin) iterator.next();
//
//                if ( pluginGroupId.equals( plugin.getGroupId() ) && pluginArtifactId.equals( plugin.getArtifactId() ) )
//                {
//                    Xpp3Dom managedDom = (Xpp3Dom) plugin.getConfiguration();
//
//                    if ( executionId != null )
//                    {
//                        PluginExecution execution = (PluginExecution) plugin.getExecutionsAsMap().get( executionId );
//                        if ( execution != null )
//                        {
//                            Xpp3Dom executionConfiguration = (Xpp3Dom) execution.getConfiguration();
//                            if ( executionConfiguration != null )
//                            {
//                                Xpp3Dom newDom = new Xpp3Dom( executionConfiguration );
//                                managedDom = Xpp3Dom.mergeXpp3Dom( newDom, dom );
//                            }
//                        }
//                    }
//
//                    dom = Xpp3Dom.mergeXpp3Dom( dom, managedDom );
//                    break;
//                }
//            }
//        }

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
            for ( ReportPlugin plugin : getReportPlugins() )
            {
                if ( pluginGroupId.equals( plugin.getGroupId() ) && pluginArtifactId.equals( plugin.getArtifactId() ) )
                {
                    dom = (Xpp3Dom) plugin.getConfiguration();

                    if ( reportSetId != null )
                    {
                        ReportSet reportSet = plugin.getReportSetsAsMap().get( reportSetId );
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
    public Set<Artifact> getDependencyArtifacts()
    {
        return dependencyArtifacts;
    }

    public void setDependencyArtifacts( Set<Artifact> dependencyArtifacts )
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

    public void setManagedVersionMap( Map<String, Artifact> map )
    {
        this.managedVersionMap = map;
    }

    public Map<String, Artifact> getManagedVersionMap()
    {
        return this.managedVersionMap;
    }

    @Override
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

    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }

    public List<Extension> getBuildExtensions()
    {
        Build build = getBuild();
        if ( build == null || build.getExtensions() == null )
        {
            return Collections.emptyList();
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
    public Set<Artifact> createArtifacts( ArtifactFactory artifactFactory, String inheritedScope,
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

    public static String getProjectReferenceId( String groupId, String artifactId, String version )
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

    public List<String> getFilters()
    {
        return getBuild().getFilters();
    }

    public Map<String, MavenProject> getProjectReferences()
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


    protected void setModel( Model model )
    {
        this.model = model;
    }

    protected void setAttachedArtifacts( List<Artifact> attachedArtifacts )
    {
        this.attachedArtifacts = attachedArtifacts;
    }

    protected void setCompileSourceRoots( List<String> compileSourceRoots )
    {
        this.compileSourceRoots = compileSourceRoots;
    }

    protected void setTestCompileSourceRoots( List<String> testCompileSourceRoots )
    {
        this.testCompileSourceRoots = testCompileSourceRoots;
    }

    protected void setScriptSourceRoots( List<String> scriptSourceRoots )
    {
        this.scriptSourceRoots = scriptSourceRoots;
    }

    protected ArtifactRepository getReleaseArtifactRepository()
    {
        return releaseArtifactRepository;
    }

    protected ArtifactRepository getSnapshotArtifactRepository()
    {
        return snapshotArtifactRepository;
    }

    public void resolveActiveArtifacts()
    {
        Set<Artifact> depArtifacts = getDependencyArtifacts();
        if ( depArtifacts == null )
        {
            return;
        }

        Set<Artifact> updated = new LinkedHashSet<Artifact>( depArtifacts.size() );
        int updatedCount = 0;

        for ( Artifact depArtifact : depArtifacts )
        {
            Artifact replaced = replaceWithActiveArtifact( depArtifact );

            if ( depArtifact != replaced )
            {
                updatedCount++;
            }

            updated.add( replaced );
        }

        if ( updatedCount > 0 )
        {
            setDependencyArtifacts( updated );
        }
    }

    public Artifact replaceWithActiveArtifact( Artifact pluginArtifact )
    {
        if ( getProjectReferences() != null && !getProjectReferences().isEmpty() )
        {
            String refId = getProjectReferenceId( pluginArtifact.getGroupId(), pluginArtifact.getArtifactId(), pluginArtifact.getVersion() );
            MavenProject ref = (MavenProject) getProjectReferences().get( refId );
            if ( ref != null )
            {
                if ( ref.getArtifact() != null
                    && ref.getArtifact().getDependencyConflictId().equals( pluginArtifact.getDependencyConflictId() ) )
                {
                    // if the project artifact doesn't exist, don't use it. We haven't built that far.
                    if ( ref.getArtifact().getFile() != null && ref.getArtifact().getFile().exists() )
                    {
                        // FIXME: Why aren't we using project.getArtifact() for the second parameter here??
                        Artifact resultArtifact = new ActiveProjectArtifact( ref, pluginArtifact );
                        return resultArtifact;
                    }
                    else
                    {
                        logMissingSiblingProjectArtifact( pluginArtifact );
                    }
                }

                Artifact attached = findMatchingArtifact( ref.getAttachedArtifacts(), pluginArtifact );
                if ( attached != null )
                {
                    if ( attached.getFile() != null && attached.getFile().exists() )
                    {
                        Artifact resultArtifact = ArtifactUtils.copyArtifact( attached );
                        resultArtifact.setScope( pluginArtifact.getScope() );
                        return resultArtifact;
                    }
                    else
                    {
                        logMissingSiblingProjectArtifact( pluginArtifact );
                    }
                }
            }
        }
        return pluginArtifact;
    }

    /**
     * Tries to resolve the specified artifact from the given collection of attached project artifacts.
     *
     * @param artifacts The attached artifacts, may be <code>null</code>.
     * @param requestedArtifact The artifact to resolve, must not be <code>null</code>.
     * @return The matching artifact or <code>null</code> if not found.
     */
    private Artifact findMatchingArtifact( List<Artifact> artifacts, Artifact requestedArtifact )
    {
        if ( artifacts != null && !artifacts.isEmpty() )
        {
            // first try matching by dependency conflict id
            String requestedId = requestedArtifact.getDependencyConflictId();
            for ( Artifact artifact : artifacts )
            {
                if ( requestedId.equals( artifact.getDependencyConflictId() ) )
                {
                    return artifact;
                }
            }

            // next try matching by repository conflict id
            requestedId = getRepositoryConflictId( requestedArtifact );
            for ( Artifact artifact : artifacts )
            {
                if ( requestedId.equals( getRepositoryConflictId( artifact ) ) )
                {
                    return artifact;
                }
            }
        }

        return null;
    }

    /**
     * Gets the repository conflict id of the specified artifact. Unlike the dependency conflict id, the repository
     * conflict id uses the artifact file extension instead of the artifact type. Hence, the repository conflict id more
     * closely reflects the identity of artifacts as perceived by a repository.
     *
     * @param artifact The artifact, must not be <code>null</code>.
     * @return The repository conflict id, never <code>null</code>.
     */
    private String getRepositoryConflictId( Artifact artifact )
    {
        StringBuffer buffer = new StringBuffer( 128 );
        buffer.append( artifact.getGroupId() );
        buffer.append( ':' ).append( artifact.getArtifactId() );
        if ( artifact.getArtifactHandler() != null )
        {
            buffer.append( ':' ).append( artifact.getArtifactHandler().getExtension() );
        }
        else
        {
            buffer.append( ':' ).append( artifact.getType() );
        }
        if ( artifact.hasClassifier() )
        {
            buffer.append( ':' ).append( artifact.getClassifier() );
        }
        return buffer.toString();
    }

    private void logMissingSiblingProjectArtifact( Artifact artifact )
    {
        if ( logger == null || !logger.isDebugEnabled() )
        {
            return;
        }

        if ( logger.isDebugEnabled() )
        {
            StringBuffer message = new StringBuffer();
            message.append( "WARNING: A dependency of the current project (or of one the plugins used in its build) was found in the reactor, " );
            message.append( "\nbut had not been built at the time it was requested. It will be resolved from the repository instead." );
            message.append( "\n\nCurrent Project: " ).append( getName() );
            message.append( "\nRequested Dependency: " ).append( artifact.getId() );
            message.append( "\n\nNOTE: You may need to run this build to the 'compile' lifecycle phase, or farther, in order to build the dependency artifact." );
            message.append( "\n" );

            logger.debug( message.toString() );
        }
        else
        {
            logger.warn( "Requested project artifact: " + artifact.getId() + " is not available at this time. Resolving externally." );
        }
    }

    private void addArtifactPath( Artifact a, List<String> list )
        throws DependencyResolutionRequiredException
    {
        File file = a.getFile();
        if ( file == null )
        {
            throw new DependencyResolutionRequiredException( a );
        }
        list.add( file.getPath() );
    }

    /**
     * Default toString
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer( 30 );
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
        catch ( NullPointerException e )
        {
            //don't log it.
        }

        return sb.toString();
    }

    /**
     * @throws CloneNotSupportedException
     * @since 2.0.9
     */
    @Override
    public Object clone()
        throws CloneNotSupportedException
    {
        MavenProject clone = (MavenProject) super.clone();
        clone.deepCopy( this );
        return clone;
    }

// ----------------------------------------------------------------------------
// CODE BELOW IS USED TO PRESERVE DYNAMISM IN THE BUILD SECTION OF THE POM.
// ----------------------------------------------------------------------------

    private Build dynamicBuild;

    private Build originalInterpolatedBuild;

    private List<String> dynamicCompileSourceRoots;

    private List<String> originalInterpolatedCompileSourceRoots;

    private List<String> dynamicTestCompileSourceRoots;

    private List<String> originalInterpolatedTestCompileSourceRoots;

    private List<String> dynamicScriptSourceRoots;

    private List<String> originalInterpolatedScriptSourceRoots;

    private boolean isConcrete = false;

    public boolean isConcrete()
    {
        return isConcrete;
    }

    public void setConcrete( boolean concrete )
    {
        isConcrete = concrete;
    }

    public Build getDynamicBuild()
    {
        return dynamicBuild;
    }

    public Build getOriginalInterpolatedBuild()
    {
        return originalInterpolatedBuild;
    }

    public List<String> getDynamicCompileSourceRoots()
    {
        return dynamicCompileSourceRoots;
    }

    public List<String> getOriginalInterpolatedCompileSourceRoots()
    {
        return originalInterpolatedCompileSourceRoots;
    }

    public List<String> getDynamicTestCompileSourceRoots()
    {
        return dynamicTestCompileSourceRoots;
    }

    public List<String> getOriginalInterpolatedTestCompileSourceRoots()
    {
        return originalInterpolatedTestCompileSourceRoots;
    }

    public List<String> getDynamicScriptSourceRoots()
    {
        return dynamicScriptSourceRoots;
    }

    public List<String> getOriginalInterpolatedScriptSourceRoots()
    {
        return originalInterpolatedScriptSourceRoots;
    }

    public void clearRestorableRoots()
    {
        dynamicCompileSourceRoots = null;
        dynamicTestCompileSourceRoots = null;
        dynamicScriptSourceRoots = null;
        originalInterpolatedCompileSourceRoots = null;
        originalInterpolatedScriptSourceRoots = null;
        originalInterpolatedTestCompileSourceRoots = null;
    }

    public void clearRestorableBuild()
    {
        dynamicBuild = null;
        originalInterpolatedBuild = null;
    }

    public void preserveCompileSourceRoots( List<String> originalInterpolatedCompileSourceRoots )
    {
        dynamicCompileSourceRoots = getCompileSourceRoots();
        this.originalInterpolatedCompileSourceRoots = originalInterpolatedCompileSourceRoots;
    }

    public void preserveTestCompileSourceRoots( List<String> originalInterpolatedTestCompileSourceRoots )
    {
        dynamicTestCompileSourceRoots = getTestCompileSourceRoots();
        this.originalInterpolatedTestCompileSourceRoots = originalInterpolatedTestCompileSourceRoots;
    }

    public void preserveScriptSourceRoots( List<String> originalInterpolatedScriptSourceRoots )
    {
        dynamicScriptSourceRoots = getScriptSourceRoots();
        this.originalInterpolatedScriptSourceRoots = originalInterpolatedScriptSourceRoots;
    }

    public void preserveBuild( Build originalInterpolatedBuild )
    {
        dynamicBuild = getBuild();
        this.originalInterpolatedBuild = originalInterpolatedBuild;

        this.originalInterpolatedBuild.setPluginManagement( null );
        this.originalInterpolatedBuild.setPlugins( null );
    }

    protected void setDynamicBuild( Build dynamicBuild )
    {
        this.dynamicBuild = dynamicBuild;
    }

    protected void setOriginalInterpolatedBuild( Build originalInterpolatedBuild )
    {
        this.originalInterpolatedBuild = originalInterpolatedBuild;
    }

    protected void setDynamicCompileSourceRoots( List<String> dynamicCompileSourceRoots )
    {
        this.dynamicCompileSourceRoots = dynamicCompileSourceRoots;
    }

    protected void setOriginalInterpolatedCompileSourceRoots( List<String> originalInterpolatedCompileSourceRoots )
    {
        this.originalInterpolatedCompileSourceRoots = originalInterpolatedCompileSourceRoots;
    }

    protected void setDynamicTestCompileSourceRoots( List<String> dynamicTestCompileSourceRoots )
    {
        this.dynamicTestCompileSourceRoots = dynamicTestCompileSourceRoots;
    }

    protected void setOriginalInterpolatedTestCompileSourceRoots( List<String> originalInterpolatedTestCompileSourceRoots )
    {
        this.originalInterpolatedTestCompileSourceRoots = originalInterpolatedTestCompileSourceRoots;
    }

    protected void setDynamicScriptSourceRoots( List<String> dynamicScriptSourceRoots )
    {
        this.dynamicScriptSourceRoots = dynamicScriptSourceRoots;
    }

    protected void setOriginalInterpolatedScriptSourceRoots( List<String> originalInterpolatedScriptSourceRoots )
    {
        this.originalInterpolatedScriptSourceRoots = originalInterpolatedScriptSourceRoots;
    }

    private Properties preservedProperties;

    public Properties getPreservedProperties()
    {
        return preservedProperties;
    }

    public void preserveProperties()
    {
        Properties p = getProperties();
        if ( p != null )
        {
            preservedProperties = new Properties();
            for ( Enumeration<?> e = p.propertyNames(); e.hasMoreElements(); )
            {
                String key = (String) e.nextElement();
                preservedProperties.setProperty( key, p.getProperty( key ) );
            }
        }
    }

    private File preservedBasedir;

    public File getPreservedBasedir()
    {
        return preservedBasedir;
    }

    public void preserveBasedir()
    {
        this.preservedBasedir = getBasedir();
    }

    public void setLogger( Logger logger )
    {
        this.logger = logger;
    }

    /**
     * Retrieve the {@link ProjectBuilderConfiguration} instance used to construct this MavenProject instance.
     */
    public ProjectBuilderConfiguration getProjectBuilderConfiguration()
    {
        return projectBuilderConfiguration;
    }

    /**
     * Set the {@link ProjectBuilderConfiguration} instance used to construct this MavenProject instance.
     * @param projectBuilderConfiguration
     */
    public void setProjectBuilderConfiguration( ProjectBuilderConfiguration projectBuilderConfiguration )
    {
        this.projectBuilderConfiguration = projectBuilderConfiguration;
    }

}
