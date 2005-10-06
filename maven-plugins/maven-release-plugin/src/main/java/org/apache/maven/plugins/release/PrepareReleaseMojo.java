package org.apache.maven.plugins.release;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.version.PluginVersionManager;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugins.release.helpers.ProjectScmRewriter;
import org.apache.maven.plugins.release.helpers.ProjectVersionResolver;
import org.apache.maven.plugins.release.helpers.ReleaseProgressTracker;
import org.apache.maven.plugins.release.helpers.ScmHelper;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ModelUtils;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.inputhandler.InputHandler;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Prepare for a release in SCM
 *
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @aggregator
 * @goal prepare
 * @requiresDependencyResolution test
 * @todo check how this works with version ranges
 */
public class PrepareReleaseMojo
    extends AbstractReleaseMojo
{
    private static final String RELEASE_POM = "release-pom.xml";

    private static final String POM = "pom.xml";

    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private String basedir;

    /**
     * @parameter expression="${settings.interactiveMode}"
     * @required
     * @readonly
     */
    private boolean interactive;

    /**
     * @component role="org.apache.maven.artifact.metadata.ArtifactMetadataSource"
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component role="org.apache.maven.plugin.version.PluginVersionManager"
     */
    private PluginVersionManager pluginVersionManager;

    /**
     * @parameter expression="${component.org.codehaus.plexus.components.inputhandler.InputHandler}"
     * @required
     * @readonly
     */
    private InputHandler inputHandler;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * @parameter expression="${project.scm.developerConnection}"
     * @required
     * @readonly
     */
    private String urlScm;

    /**
     * @parameter expression="${username}"
     */
    private String username;

    /**
     * @parameter expression="${password}"
     */
    private String password;

    /**
     * @parameter expression="${tag}"
     */
    private String tag;

    /**
     * @parameter expression="${tagBase}" default-value="../tags"
     */
    private String tagBase;

    /**
     * @parameter expression="${resume}" default-value="true"
     */
    private boolean resume;

    /**
     * @component
     */
    private PathTranslator pathTranslator;

    private String userTag;

    private ReleaseProgressTracker releaseProgress;

    private ProjectVersionResolver versionResolver;

    private ProjectScmRewriter scmRewriter;

    protected void executeTask()
        throws MojoExecutionException
    {
        try
        {
            getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_INITIALIZED );
        }
        catch ( IOException e )
        {
            getLog().warn( "Error writing checkpoint.", e );
        }

        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_PREPARED_RELEASE ) )
        {
            checkForLocalModifications();

            if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_POM_TRANSFORMED_FOR_RELEASE ) )
            {
                for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
                {
                    MavenProject project = (MavenProject) it.next();

                    checkForPresenceOfSnapshots( project );

                    String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                    if ( !ArtifactUtils.isSnapshot( project.getVersion() ) )
                    {
                        throw new MojoExecutionException( "The project " + project.getGroupId() + ":" +
                            project.getArtifactId() + " isn't a snapshot (" + project.getVersion() + ")." );
                    }

                    getVersionResolver().resolveVersion( project.getOriginalModel(), projectId );

                    Model model = ModelUtils.cloneModel( project.getOriginalModel() );

                    transformPomToReleaseVersionPom( model, projectId, project.getFile(), project.getParentArtifact(),
                                                     project.getPluginArtifactRepositories() );
                }

                try
                {
                    getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_POM_TRANSFORMED_FOR_RELEASE );
                }
                catch ( IOException e )
                {
                    getLog().warn( "Error writing checkpoint.", e );
                }

            }

            generateReleasePoms();

            checkInRelease();

            tagRelease();

            if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_POM_TRANSORMED_FOR_DEVELOPMENT ) )
            {
                for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
                {
                    MavenProject project = (MavenProject) it.next();

                    Model model = ModelUtils.cloneModel( project.getOriginalModel() );

                    String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );
                    getVersionResolver().incrementVersion( model, projectId );

                    getScmRewriter().restoreScmInfo( model );

                    transformPomToSnapshotVersionPom( model, project.getFile() );
                }

                try
                {
                    getReleaseProgress().checkpoint( basedir,
                                                     ReleaseProgressTracker.CP_POM_TRANSORMED_FOR_DEVELOPMENT );
                }
                catch ( IOException e )
                {
                    getLog().warn( "Error writing checkpoint.", e );
                }
            }

            removeReleasePoms();

            checkInNextSnapshot();

            try
            {
                getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_PREPARED_RELEASE );
            }
            catch ( IOException e )
            {
                getLog().warn( "Error writing checkpoint.", e );
            }
        }
    }

    private void transformPomToSnapshotVersionPom( Model model, File file )
        throws MojoExecutionException
    {
        ProjectVersionResolver versionResolver = getVersionResolver();

        Parent parent = model.getParent();

        //Rewrite parent version
        if ( parent != null )
        {
            String incrementedVersion = versionResolver.getResolvedVersion( parent.getGroupId(), parent
                .getArtifactId() );

            if ( incrementedVersion != null )
            {
                parent.setVersion( incrementedVersion );
            }
        }

        //Rewrite dependencies section
        List dependencies = model.getDependencies();

        if ( dependencies != null )
        {
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dep = (Dependency) i.next();

                String version = versionResolver.getResolvedVersion( dep.getGroupId(), dep.getArtifactId() );

                if ( version != null )
                {
                    dep.setVersion( version );
                }
            }
        }

        //Rewrite plugins section
        Build build = model.getBuild();

        if ( build != null )
        {
            List plugins = build.getPlugins();

            if ( plugins != null )
            {
                for ( Iterator i = plugins.iterator(); i.hasNext(); )
                {
                    Plugin plugin = (Plugin) i.next();

                    String version = versionResolver.getResolvedVersion( plugin.getGroupId(), plugin
                        .getArtifactId() );

                    if ( version != null )
                    {
                        plugin.setVersion( version );
                    }
                }
            }

            //Rewrite extensions section
            List extensions = build.getExtensions();

            for ( Iterator i = extensions.iterator(); i.hasNext(); )
            {
                Extension ext = (Extension) i.next();

                String version = versionResolver.getResolvedVersion( ext.getGroupId(), ext.getArtifactId() );

                if ( version != null )
                {
                    ext.setVersion( version );
                }
            }
        }

        Reporting reporting = model.getReporting();

        if ( reporting != null )
        {
            //Rewrite reports section
            List reports = reporting.getPlugins();

            if ( reports != null )
            {
                for ( Iterator i = reports.iterator(); i.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) i.next();

                    String version = versionResolver.getResolvedVersion( plugin.getGroupId(), plugin
                        .getArtifactId() );

                    if ( version != null )
                    {
                        plugin.setVersion( version );
                    }
                }
            }
        }

        File pomFile = new File( file.getParentFile(), POM );
        Writer writer = null;

        try
        {
            writer = new FileWriter( pomFile );

            MavenXpp3Writer pomWriter = new MavenXpp3Writer();

            pomWriter.write( writer, model );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot write development version of pom to: " + pomFile, e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    protected ReleaseProgressTracker getReleaseProgress()
        throws MojoExecutionException
    {
        if ( releaseProgress == null )
        {
            try
            {
                releaseProgress = ReleaseProgressTracker.loadOrCreate( basedir );
            }
            catch ( IOException e )
            {
                getLog().warn( "Cannot read existing release progress file from directory: " + basedir + "." );
                getLog().debug( "Cause", e );

                releaseProgress = ReleaseProgressTracker.create();
            }

            if ( resume )
            {
                releaseProgress.setResumeAtCheckpoint( true );
            }

            if ( releaseProgress.getUsername() == null )
            {
                if ( username == null )
                {
                    username = System.getProperty( "user.name" );
                }
                releaseProgress.setUsername( username );
            }

            if ( releaseProgress.getPassword() == null && password != null )
            {
                releaseProgress.setPassword( password );
            }

            if ( releaseProgress.getScmTag() == null )
            {
                releaseProgress.setScmTag( getTagLabel() );
            }

            if ( releaseProgress.getScmTagBase() == null )
            {
                releaseProgress.setScmTagBase( tagBase );
            }

            if ( releaseProgress.getScmUrl() == null )
            {
                releaseProgress.setScmUrl( urlScm );
            }

            if ( releaseProgress.getUsername() == null || releaseProgress.getScmTag() == null ||
                releaseProgress.getScmTagBase() == null || releaseProgress.getScmUrl() == null )
            {
                throw new MojoExecutionException( "Missing release preparation information." );
            }
        }
        return releaseProgress;
    }

    protected ProjectVersionResolver getVersionResolver()
    {
        if ( versionResolver == null )
        {
            versionResolver = new ProjectVersionResolver( getLog(), inputHandler, interactive );
        }

        return versionResolver;
    }

    protected ProjectScmRewriter getScmRewriter()
        throws MojoExecutionException
    {
        if ( scmRewriter == null )
        {
            scmRewriter = new ProjectScmRewriter( getReleaseProgress() );
        }

        return scmRewriter;
    }

    private void checkForLocalModifications()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_LOCAL_MODIFICATIONS_CHECKED ) )
        {
            getLog().info( "Verifying there are no local modifications ..." );

            List changedFiles;

            try
            {
                ScmHelper scm = getScm( basedir );

                changedFiles = scm.getStatus();
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "An error is occurred in the status process.", e );
            }

            String releaseProgressFilename = ReleaseProgressTracker.getReleaseProgressFilename();

            for ( Iterator i = changedFiles.iterator(); i.hasNext(); )
            {
                ScmFile f = (ScmFile) i.next();
                if ( "pom.xml.backup".equals( f.getPath() ) || f.getPath().equals( releaseProgressFilename ) )
                {
                    i.remove();
                }
            }

            if ( !changedFiles.isEmpty() )
            {
                StringBuffer message = new StringBuffer();

                for ( Iterator i = changedFiles.iterator(); i.hasNext(); )
                {
                    ScmFile file = (ScmFile) i.next();

                    message.append( file.toString() );

                    message.append( "\n" );
                }

                throw new MojoExecutionException(
                    "Cannot prepare the release because you have local modifications : \n" + message );
            }

            try
            {
                getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_LOCAL_MODIFICATIONS_CHECKED );
            }
            catch ( IOException e )
            {
                getLog().warn( "Error writing checkpoint.", e );
            }
        }
    }

    /**
     * Check the POM in an attempt to remove all instances of SNAPSHOTs in preparation for a release. The goal
     * is to make the build reproducable so the removal of SNAPSHOTs is a necessary one.
     *
     * A check is made to ensure any parents in the lineage are released, that all the dependencies are
     * released and that any plugins utilized by this project are released.
     *
     * @throws MojoExecutionException
     */
    private void checkForPresenceOfSnapshots( MavenProject project )
        throws MojoExecutionException
    {
        getLog().info( "Checking lineage for snapshots ..." );

        MavenProject currentProject = project;

        while ( currentProject.hasParent() )
        {
            MavenProject parentProject = currentProject.getParent();

            String parentVersion;

            if ( ArtifactUtils.isSnapshot( parentProject.getVersion() ) )
            {
                parentVersion = getVersionResolver().getResolvedVersion( parentProject.getGroupId(),
                                                                         parentProject.getArtifactId() );

                if ( parentVersion == null )
                {
                    parentVersion = parentProject.getVersion();
                }

                if ( ArtifactUtils.isSnapshot( parentVersion ) )
                {
                    throw new MojoExecutionException( "Can't release project due to non released parent (" +
                        parentProject.getGroupId() + ":" + parentProject.getArtifactId() + parentVersion + "." );
                }
            }

            currentProject = parentProject;
        }

        getLog().info( "Checking dependencies for snapshots ..." );

        Set snapshotDependencies = new HashSet();

        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            String artifactVersion = getVersionResolver().getResolvedVersion( artifact.getGroupId(),
                                                                              artifact.getArtifactId() );

            if ( artifactVersion == null )
            {
                artifactVersion = artifact.getVersion();
            }

            if ( ArtifactUtils.isSnapshot( artifactVersion ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        getLog().info( "Checking plugins for snapshots ..." );

        for ( Iterator i = project.getPluginArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            String artifactVersion = getVersionResolver().getResolvedVersion( artifact.getGroupId(),
                                                                              artifact.getArtifactId() );

            if ( artifactVersion == null )
            {
                artifactVersion = artifact.getVersion();
            }

            if ( ArtifactUtils.isSnapshot( artifactVersion ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        if ( !snapshotDependencies.isEmpty() )
        {
            List snapshotsList = new ArrayList( snapshotDependencies );

            Collections.sort( snapshotsList );

            StringBuffer message = new StringBuffer();

            for ( Iterator i = snapshotsList.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                message.append( "    " );

                message.append( artifact.getId() );

                message.append( "\n" );
            }

            throw new MojoExecutionException( "Can't release project due to non released dependencies :\n" + message );
        }
    }

    private void transformPomToReleaseVersionPom( Model model, String projectId, File file, Artifact parentArtifact,
                                                  List pluginArtifactRepositories )
        throws MojoExecutionException
    {
        getScmRewriter().rewriteScmInfo( model, projectId, getTagLabel() );

        //Rewrite parent version
        if ( model.getParent() != null )
        {
            if ( ArtifactUtils.isSnapshot( parentArtifact.getBaseVersion() ) )
            {
                String version = resolveVersion( parentArtifact, "parent", pluginArtifactRepositories );

                model.getParent().setVersion( version );
            }
        }

        //Rewrite dependencies section
        List dependencies = model.getDependencies();

        if ( dependencies != null )
        {
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dep = (Dependency) i.next();

                // Avoid in dep mgmt
                if ( dep.getVersion() != null )
                {
                    String resolvedVersion = getVersionResolver().getResolvedVersion( dep.getGroupId(),
                                                                                      dep.getArtifactId() );

                    if ( resolvedVersion != null )
                    {
                        dep.setVersion( resolvedVersion );
                    }
                }
            }
        }

        DependencyManagement dependencyManagement = model.getDependencyManagement();
        dependencies = dependencyManagement != null ? dependencyManagement.getDependencies() : null;

        if ( dependencies != null )
        {
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dep = (Dependency) i.next();

                if ( dep.getVersion() != null )
                {
                    String resolvedVersion = getVersionResolver().getResolvedVersion( dep.getGroupId(),
                                                                                      dep.getArtifactId() );

                    if ( resolvedVersion != null )
                    {
                        dep.setVersion( resolvedVersion );
                    }
                }
            }
        }

        Build build = model.getBuild();

        if ( build != null )
        {
            //Rewrite plugins section
            List plugins = build.getPlugins();

            if ( plugins != null )
            {
                for ( Iterator i = plugins.iterator(); i.hasNext(); )
                {
                    Plugin plugin = (Plugin) i.next();

                    // Avoid in plugin mgmt
                    if ( plugin.getVersion() != null )
                    {
                        String resolvedVersion = getVersionResolver().getResolvedVersion( plugin.getGroupId(),
                                                                                          plugin.getArtifactId() );

                        if ( resolvedVersion != null )
                        {
                            plugin.setVersion( resolvedVersion );
                        }
                    }
                }
            }

            PluginManagement pluginManagement = build.getPluginManagement();
            plugins = pluginManagement != null ? pluginManagement.getPlugins() : null;

            if ( plugins != null )
            {
                for ( Iterator i = plugins.iterator(); i.hasNext(); )
                {
                    Plugin plugin = (Plugin) i.next();

                    if ( plugin.getVersion() != null )
                    {
                        String resolvedVersion = getVersionResolver().getResolvedVersion( plugin.getGroupId(),
                                                                                          plugin.getArtifactId() );

                        if ( resolvedVersion != null )
                        {
                            plugin.setVersion( resolvedVersion );
                        }
                    }
                }
            }

            //Rewrite extensions section
            List extensions = build.getExtensions();

            for ( Iterator i = extensions.iterator(); i.hasNext(); )
            {
                Extension ext = (Extension) i.next();

                String resolvedVersion = getVersionResolver().getResolvedVersion( ext.getGroupId(),
                                                                                  ext.getArtifactId() );

                if ( resolvedVersion != null )
                {
                    ext.setVersion( resolvedVersion );
                }
            }
        }

        Reporting reporting = model.getReporting();

        if ( reporting != null )
        {
            //Rewrite reports section
            List reports = reporting.getPlugins();

            for ( Iterator i = reports.iterator(); i.hasNext(); )
            {
                ReportPlugin plugin = (ReportPlugin) i.next();

                String resolvedVersion = getVersionResolver().getResolvedVersion( plugin.getGroupId(),
                                                                                  plugin.getArtifactId() );

                if ( resolvedVersion != null )
                {
                    plugin.setVersion( resolvedVersion );
                }
            }
        }

        Writer writer = null;

        File pomFile = new File( file.getParentFile(), POM );
        try
        {
            writer = new FileWriter( pomFile );

            MavenXpp3Writer pomWriter = new MavenXpp3Writer();

            pomWriter.write( writer, model );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot write released version of pom to: " + pomFile, e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private void generateReleasePoms()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_GENERATED_RELEASE_POM ) )
        {
            String canonicalBasedir;

            try
            {
                canonicalBasedir = trimPathForScmCalculation( new File( basedir ) );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot canonicalize basedir: " + basedir, e );
            }

            for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                MavenProject releaseProject = new MavenProject( project );
                Model releaseModel = releaseProject.getModel();
                fixNullValueInModel( releaseModel, project.getModel() );

                // the release POM should reflect bits of these which were injected at build time...
                // we don't need these polluting the POM.
                releaseModel.setProfiles( Collections.EMPTY_LIST );
                releaseModel.setDependencyManagement( null );
                releaseProject.getBuild().setPluginManagement( null );

                String projectVersion = releaseModel.getVersion();
                if ( ArtifactUtils.isSnapshot( projectVersion ) )
                {
                    String snapshotVersion = projectVersion;

                    projectVersion = getVersionResolver().getResolvedVersion( project.getGroupId(),
                                                                              project.getArtifactId() );

                    if ( ArtifactUtils.isSnapshot( projectVersion ) )
                    {
                        throw new MojoExecutionException(
                            "MAJOR PROBLEM!!! Cannot find resolved version to be used in releasing project: " +
                                releaseProject.getId() );
                    }

                    releaseModel.setVersion( projectVersion );

                    String finalName = releaseModel.getBuild().getFinalName();

                    if ( finalName.equals( releaseModel.getArtifactId() + "-" + snapshotVersion ) )
                    {
                        releaseModel.getBuild().setFinalName( null );
                    }
                    else if ( finalName.indexOf( "SNAPSHOT" ) > -1 )
                    {
                        throw new MojoExecutionException(
                            "Cannot reliably adjust the finalName of project: " + releaseProject.getId() );
                    }
                }

                releaseModel.setParent( null );

                Set artifacts = releaseProject.getArtifacts();

                if ( artifacts != null )
                {
                    //Rewrite dependencies section
                    List newdeps = new ArrayList();

                    for ( Iterator i = releaseProject.getArtifacts().iterator(); i.hasNext(); )
                    {
                        Artifact artifact = (Artifact) i.next();

                        Dependency newdep = new Dependency();

                        newdep.setArtifactId( artifact.getArtifactId() );
                        newdep.setGroupId( artifact.getGroupId() );

                        String version = artifact.getVersion();
                        if ( artifact.isSnapshot() )
                        {
                            version = getVersionResolver().getResolvedVersion( artifact.getGroupId(),
                                                                               artifact.getArtifactId() );

                            if ( ArtifactUtils.isSnapshot( version ) )
                            {
                                throw new MojoExecutionException( "Unresolved SNAPSHOT version of: " +
                                    artifact.getId() + ". Cannot proceed with release." );
                            }
                        }

                        newdep.setVersion( version );
                        newdep.setType( artifact.getType() );
                        newdep.setScope( artifact.getScope() );
                        newdep.setClassifier( artifact.getClassifier() );

                        newdeps.add( newdep );
                    }

                    releaseModel.setDependencies( newdeps );
                }

                // Use original - don't want the lifecycle introduced ones
                Build build = releaseProject.getOriginalModel().getBuild();
                List plugins = build != null ? build.getPlugins() : null;

                if ( plugins != null )
                {
                    //Rewrite plugins version
                    for ( Iterator i = plugins.iterator(); i.hasNext(); )
                    {
                        Plugin plugin = (Plugin) i.next();

                        String version;
                        try
                        {
                            version = pluginVersionManager.resolvePluginVersion( plugin.getGroupId(),
                                                                                 plugin.getArtifactId(), releaseProject,
                                                                                 settings, localRepository );
                        }
                        catch ( PluginVersionResolutionException e )
                        {
                            throw new MojoExecutionException( "Cannot resolve version for plugin: " + plugin, e );
                        }

                        if ( ArtifactUtils.isSnapshot( version ) )
                        {
                            throw new MojoExecutionException(
                                "Resolved version of plugin is a snapshot. Please release this plugin before releasing this project.\n\nGroupId: " +
                                    plugin.getGroupId() + "\nArtifactId: " + plugin.getArtifactId() +
                                    "\nResolved Version: " + version + "\n\n" );
                        }

                        plugin.setVersion( version );
                    }
                }

                Reporting reporting = releaseModel.getReporting();
                List reports = reporting != null ? reporting.getPlugins() : null;

                if ( reports != null )
                {
                    //Rewrite report version
                    for ( Iterator i = reports.iterator(); i.hasNext(); )
                    {
                        ReportPlugin plugin = (ReportPlugin) i.next();

                        String version;
                        try
                        {
                            version = pluginVersionManager.resolveReportPluginVersion( plugin.getGroupId(),
                                                                                       plugin.getArtifactId(),
                                                                                       releaseProject, settings,
                                                                                       localRepository );
                        }
                        catch ( PluginVersionResolutionException e )
                        {
                            throw new MojoExecutionException( "Cannot resolve version for report plugin: " + plugin,
                                                              e );
                        }

                        if ( ArtifactUtils.isSnapshot( version ) )
                        {
                            throw new MojoExecutionException(
                                "Resolved version of plugin is a snapshot. Please release this report plugin before releasing this project.\n\nGroupId: " +
                                    plugin.getGroupId() + "\nArtifactId: " + plugin.getArtifactId() +
                                    "\nResolved Version: " + version + "\n\n" );
                        }

                        plugin.setVersion( version );
                    }
                }

                List extensions = build != null ? build.getExtensions() : null;

                if ( extensions != null )
                {
                    //Rewrite extension version
                    Map extensionArtifacts = releaseProject.getExtensionArtifactMap();

                    for ( Iterator i = extensions.iterator(); i.hasNext(); )
                    {
                        Extension ext = (Extension) i.next();

                        String extensionId = ArtifactUtils.versionlessKey( ext.getGroupId(), ext.getArtifactId() );

                        Artifact artifact = (Artifact) extensionArtifacts.get( extensionId );

                        String version = resolveVersion( artifact, "extension",
                                                         releaseProject.getPluginArtifactRepositories() );

                        ext.setVersion( version );
                    }
                }

                pathTranslator.unalignFromBaseDirectory( releaseProject.getModel(), project.getFile().getParentFile() );

                File releasePomFile = new File( releaseProject.getFile().getParentFile(), RELEASE_POM );

                Writer writer = null;

                try
                {
                    writer = new FileWriter( releasePomFile );

                    releaseProject.writeModel( writer );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Cannot write release-pom to: " + releasePomFile, e );
                }
                finally
                {
                    IOUtil.close( writer );
                }

                try
                {
                    String releasePomPath = trimPathForScmCalculation( releasePomFile );

                    releasePomPath = releasePomPath.substring( canonicalBasedir.length() + 1 );

                    ScmHelper scm = getScm( basedir );

                    scm.add( releasePomPath );
                }
                catch ( ScmException e )
                {
                    throw new MojoExecutionException( "Error adding the release-pom.xml: " + releasePomFile, e );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Error adding the release-pom.xml: " + releasePomFile, e );
                }

                try
                {
                    getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_GENERATED_RELEASE_POM );
                }
                catch ( IOException e )
                {
                    getLog().warn( "Error writing checkpoint.", e );
                }
            }
        }
    }

    private void fixNullValueInModel( Model modelToFix, Model correctModel )
    {
        if ( modelToFix.getModelVersion() != null )
        {
            modelToFix.setModelVersion( correctModel.getModelVersion() );
        }

        if ( modelToFix.getName() != null )
        {
            modelToFix.setName( correctModel.getName() );
        }

        if ( modelToFix.getParent() != null )
        {
            modelToFix.setParent( cloneParent( correctModel.getParent() ) );
        }

        if ( modelToFix.getVersion() != null )
        {
            modelToFix.setVersion( correctModel.getVersion() );
        }

        if ( modelToFix.getArtifactId() != null )
        {
            modelToFix.setArtifactId( correctModel.getArtifactId() );
        }

        if ( modelToFix.getProperties() != null && modelToFix.getProperties().isEmpty() )
        {
            modelToFix.setProperties( new Properties( correctModel.getProperties() ) );
        }

        if ( modelToFix.getGroupId() != null )
        {
            modelToFix.setGroupId( correctModel.getGroupId() );
        }

        if ( modelToFix.getPackaging() != null )
        {
            modelToFix.setPackaging( correctModel.getPackaging() );
        }

        if ( modelToFix.getModules() != null && !modelToFix.getModules().isEmpty() )
        {
            modelToFix.setModules( cloneModules( correctModel.getModules() ) );
        }

        if ( modelToFix.getDistributionManagement() != null )
        {
            modelToFix.setDistributionManagement( correctModel.getDistributionManagement() );
        }
    }

    private static List cloneModules( List modules )
    {
        if ( modules == null )
        {
            return modules;
        }
        return new ArrayList( modules );
    }

    private static Parent cloneParent( Parent parent )
    {
        if ( parent == null )
        {
            return parent;
        }

        Parent newParent = new Parent();
        newParent.setArtifactId( parent.getArtifactId() );
        newParent.setGroupId( parent.getGroupId() );
        newParent.setRelativePath( parent.getRelativePath() );
        newParent.setVersion( parent.getVersion() );
        return newParent;
    }

    private String resolveVersion( Artifact artifact, String artifactUsage, List pluginArtifactRepositories )
        throws MojoExecutionException
    {
        String resolvedVersion = getVersionResolver().getResolvedVersion( artifact.getGroupId(),
                                                                          artifact.getArtifactId() );

        if ( resolvedVersion == null )
        {
            if ( artifact.getFile() == null )
            {
                try
                {
                    artifactMetadataSource.retrieve( artifact, localRepository, pluginArtifactRepositories );
                }
                catch ( ArtifactMetadataRetrievalException e )
                {
                    throw new MojoExecutionException( "Cannot resolve " + artifactUsage + ": " + artifact.getId(), e );
                }
            }

            resolvedVersion = artifact.getVersion();
        }

        return resolvedVersion;
    }

    /**
     * Check in the POM to SCM after it has been transformed where the version has been
     * set to the release version.
     *
     * @throws MojoExecutionException
     */
    private void checkInRelease()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_CHECKED_IN_RELEASE_VERSION ) )
        {
            getLog().info( "Checking in modified POMs" );

            checkIn( "[maven-release-plugin] prepare release " + getTagLabel() );

            try
            {
                getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_CHECKED_IN_RELEASE_VERSION );
            }
            catch ( IOException e )
            {
                getLog().warn( "Error writing checkpoint.", e );
            }
        }
    }

    private void removeReleasePoms()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_REMOVED_RELEASE_POM ) )
        {
            getLog().info( "Removing release POMs" );

            File currentReleasePomFile = null;

            try
            {
                String canonicalBasedir = trimPathForScmCalculation( new File( basedir ) );

                for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
                {
                    MavenProject project = (MavenProject) it.next();

                    currentReleasePomFile = new File( project.getFile().getParentFile(), RELEASE_POM );

                    String releasePomPath = trimPathForScmCalculation( currentReleasePomFile );

                    releasePomPath = releasePomPath.substring( canonicalBasedir.length() + 1 );

                    ScmHelper scm = getScm( basedir );

                    scm.remove( "Removing for next development iteration.", releasePomPath );

                    currentReleasePomFile.delete();
                }
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "Cannot remove " + currentReleasePomFile + " from development HEAD.",
                                                  e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot remove " + currentReleasePomFile + " from development HEAD.",
                                                  e );
            }

            try
            {
                getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_REMOVED_RELEASE_POM );
            }
            catch ( IOException e )
            {
                getLog().warn( "Error writing checkpoint.", e );
            }
        }
    }

    private String trimPathForScmCalculation( File file )
        throws IOException
    {
        String path = file.getCanonicalPath();

        path = path.replace( File.separatorChar, '/' );

        if ( path.endsWith( "/" ) )
        {
            path = path.substring( path.length() - 1 );
        }

        return path;
    }

    private void checkInNextSnapshot()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_CHECKED_IN_DEVELOPMENT_VERSION ) )
        {
            getLog().info( "Checking in development POMs" );

            checkIn( "[maven-release-plugin] prepare for next development iteration" );

            try
            {
                getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_CHECKED_IN_DEVELOPMENT_VERSION );
            }
            catch ( IOException e )
            {
                getLog().warn( "Error writing checkpoint.", e );
            }
        }
    }

    private void checkIn( String message )
        throws MojoExecutionException
    {
        ScmHelper scm = getScm( basedir );

        String tag = scm.getTag();

        // No tag here - we suppose user works on correct branch
        scm.setTag( null );

        try
        {
            scm.checkin( message );
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "An error is occurred in the checkin process.", e );
        }

        scm.setTag( tag );
    }

    private String getTagLabel()
        throws MojoExecutionException
    {
        if ( userTag == null )
        {
            try
            {
                if ( tag == null && interactive )
                {
                    getLog().info( "What tag name should be used? " );

                    String inputTag = inputHandler.readLine();

                    if ( !StringUtils.isEmpty( inputTag ) )
                    {
                        userTag = inputTag;
                    }
                }
                else
                {
                    userTag = tag;
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "An error is occurred in the tag process.", e );
            }
        }

        if ( userTag == null )
        {
            userTag = releaseProgress.getScmTag();
        }

        if ( userTag == null )
        {
            throw new MojoExecutionException( "A tag must be specified" );
        }

        return userTag;
    }

    /**
     * Tag the release in preparation for performing the release.
     *
     * We will provide the user with a default tag name based on the artifact id
     * and the version of the project being released.
     *
     * where artifactId is <code>plexus-action</code> and the version is <code>1.0-beta-4</code>, the
     * the suggested tag will be <code>PLEXUS_ACTION_1_0_BETA_4</code>.
     *
     * @throws MojoExecutionException
     */
    private void tagRelease()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_TAGGED_RELEASE ) )
        {
            String tag = getTagLabel();

            try
            {
                ScmHelper scm = getScm( basedir );

                scm.setTag( tag );

                getLog().info( "Tagging release with the label " + tag + "." );

                scm.tag();
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "An error is occurred in the tag process.", e );
            }

            try
            {
                getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_TAGGED_RELEASE );
            }
            catch ( IOException e )
            {
                getLog().warn( "Error writing checkpoint.", e );
            }
        }
    }

}
