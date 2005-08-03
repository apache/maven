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
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.release.helpers.ProjectScmRewriter;
import org.apache.maven.plugins.release.helpers.ProjectVersionResolver;
import org.apache.maven.plugins.release.helpers.ReleaseProgressTracker;
import org.apache.maven.plugins.release.helpers.ScmHelper;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
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
    private static final String SNAPSHOT = "-SNAPSHOT";

    private static final String RELEASE_POM = "release-pom.xml";

    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private String basedir;

    /**
     * @parameter expression="${settings.interactiveMode}"
     * @readonly
     */
    private boolean interactive = true;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.metadata.ArtifactMetadataSource}"
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

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
     * @parameter expression="${maven.username}"
     */
    private String username = System.getProperty( "user.name" );

    /**
     * @parameter expression="${password}"
     */
    private String password;

    /**
     * @parameter expression="${tag}"
     */
    private String tag;

    /**
     * @parameter expression="${tagBase}"
     */
    private String tagBase = "../tags";

    /**
     * @parameter expression="${resume}"
     */
    private boolean resume = false;

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

            for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                getVersionResolver().resolveVersion( project );

                getScmRewriter().rewriteScmInfo( project, getTagLabel() );
            }

            for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                checkForPresenceOfSnapshots( project );

                transformPomToReleaseVersionPom( project );

            }

            generateReleasePoms();
            
            checkInRelease();

            tagRelease();

            for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                getVersionResolver().incrementVersion( project );

                getScmRewriter().restoreScmInfo( project );
            }

            for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                transformPomToSnapshotVersionPom( project );
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

    private void transformPomToSnapshotVersionPom( MavenProject project )
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_POM_TRANSORMED_FOR_DEVELOPMENT ) )
        {
            if ( isSnapshot( project.getVersion() ) )
            {
                throw new MojoExecutionException( "This project is a snapshot (" + project.getVersion() +
                    "). It appears that the release version has not been committed." );
            }

            Model model = project.getOriginalModel();

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

            Writer writer = null;

            try
            {
                writer = new FileWriter( project.getFile() );

                project.writeOriginalModel( writer );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write development version of pom to: " + project.getFile(),
                                                  e );
            }
            finally
            {
                IOUtil.close( writer );
            }

            try
            {
                getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_POM_TRANSORMED_FOR_DEVELOPMENT );
            }
            catch ( IOException e )
            {
                getLog().warn( "Error writing checkpoint.", e );
            }
        }
    }

    protected ReleaseProgressTracker getReleaseProgress()
        throws MojoExecutionException
    {
        if ( releaseProgress == null )
        {
            if ( resume )
            {
                try
                {
                    releaseProgress = ReleaseProgressTracker.load( basedir );

                    releaseProgress.verifyResumeCapable();
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException(
                        "Cannot read existing release progress file from directory: " + basedir + ". Cannot resume." );
                }
            }
            else
            {
                try
                {
                    releaseProgress = ReleaseProgressTracker.loadOrCreate( basedir );
                }
                catch ( IOException e )
                {
                    getLog().warn( "Cannot read existing release progress file from directory: " + basedir +
                        ". Creating new instance." );
                }

                releaseProgress.setResumeAtCheckpoint( resume );

                releaseProgress.setUsername( username );

                if ( password != null )
                {
                    releaseProgress.setPassword( password );
                }

                releaseProgress.setScmTag( getTagLabel() );

                releaseProgress.setScmTagBase( tagBase );

                releaseProgress.setScmUrl( urlScm );
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
    {
        if ( scmRewriter == null )
        {
            scmRewriter = new ProjectScmRewriter();
        }

        return scmRewriter;
    }

    private boolean isSnapshot( String version )
    {
        return version.endsWith( SNAPSHOT );
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
                ScmHelper scm = getScm();

                scm.setWorkingDirectory( basedir );

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
                if ( f.getPath().equals( "pom.xml.backup" ) || f.getPath().equals( releaseProgressFilename ) )
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
                    "Cannot prepare the release because you have local modifications : \n" + message.toString() );
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
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_SNAPSHOTS_CHECKED ) )
        {
            getLog().info( "Checking lineage for snapshots ..." );

            MavenProject currentProject = project;

            while ( currentProject.hasParent() )
            {
                MavenProject parentProject = currentProject.getParent();

                String parentVersion = getVersionResolver().getResolvedVersion( parentProject.getGroupId(),
                                                                                parentProject.getArtifactId() );

                if ( isSnapshot( parentVersion ) )
                {
                    throw new MojoExecutionException( "Can't release project due to non released parent." );
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

                if ( isSnapshot( artifactVersion ) )
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

                if ( isSnapshot( artifactVersion ) )
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

                throw new MojoExecutionException(
                    "Can't release project due to non released dependencies :\n" + message.toString() );
            }

            try
            {
                getReleaseProgress().checkpoint( basedir, ReleaseProgressTracker.CP_SNAPSHOTS_CHECKED );
            }
            catch ( IOException e )
            {
                getLog().warn( "Error writing checkpoint.", e );
            }
        }
    }

    private void transformPomToReleaseVersionPom( MavenProject project )
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_POM_TRANSFORMED_FOR_RELEASE ) )
        {
            if ( !isSnapshot( project.getVersion() ) )
            {
                throw new MojoExecutionException( "This project isn't a snapshot (" + project.getVersion() + ")." );
            }

            Model model = project.getOriginalModel();

            //Rewrite parent version
            if ( project.hasParent() )
            {
                Artifact parentArtifact = project.getParentArtifact();

                if ( isSnapshot( parentArtifact.getBaseVersion() ) )
                {
                    String version = resolveVersion( parentArtifact, "parent", project );

                    model.getParent().setVersion( version );
                }
            }

            //Rewrite dependencies section
            Map artifactMap = project.getArtifactMap();

            List dependencies = model.getDependencies();

            if ( dependencies != null )
            {
                for ( Iterator i = dependencies.iterator(); i.hasNext(); )
                {
                    Dependency dep = (Dependency) i.next();

                    String conflictId = ArtifactUtils.artifactId( dep.getGroupId(), dep.getArtifactId(), dep.getType(),
                                                                  dep.getClassifier(), dep.getVersion() );

                    Artifact artifact = (Artifact) artifactMap.get( conflictId );

                    String version = resolveVersion( artifact, "dependency", project );

                    dep.setVersion( version );
                }
            }

            Build build = model.getBuild();

            if ( build != null )
            {
                //Rewrite plugins section
                Map pluginArtifactMap = project.getPluginArtifactMap();

                List plugins = build.getPlugins();

                for ( Iterator i = plugins.iterator(); i.hasNext(); )
                {
                    Plugin plugin = (Plugin) i.next();

                    String pluginId = plugin.getKey();

                    Artifact artifact = (Artifact) pluginArtifactMap.get( pluginId );

                    String version = resolveVersion( artifact, "plugin", project );

                    plugin.setVersion( version );
                }

                //Rewrite extensions section
                Map extensionArtifactMap = project.getExtensionArtifactMap();

                List extensions = build.getExtensions();

                for ( Iterator i = extensions.iterator(); i.hasNext(); )
                {
                    Extension ext = (Extension) i.next();

                    String pluginId = ArtifactUtils.versionlessKey( ext.getGroupId(), ext.getArtifactId() );

                    Artifact artifact = (Artifact) extensionArtifactMap.get( pluginId );

                    String version = resolveVersion( artifact, "extension", project );

                    ext.setVersion( version );
                }
            }

            Reporting reporting = model.getReporting();

            if ( reporting != null )
            {
                //Rewrite reports section
                Map reportArtifactMap = project.getReportArtifactMap();

                List reports = reporting.getPlugins();

                for ( Iterator i = reports.iterator(); i.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) i.next();

                    String pluginId = plugin.getKey();

                    Artifact artifact = (Artifact) reportArtifactMap.get( pluginId );

                    String version = resolveVersion( artifact, "report", project );

                    plugin.setVersion( version );
                }
            }

            Writer writer = null;

            try
            {
                writer = new FileWriter( project.getFile() );

                project.writeOriginalModel( writer );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write released version of pom to: " + project.getFile(), e );
            }
            finally
            {
                IOUtil.close( writer );
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

                //Rewrite parent version
                if ( project.hasParent() )
                {
                    Artifact parentArtifact = project.getParentArtifact();

                    if ( isSnapshot( parentArtifact.getBaseVersion() ) )
                    {
                        String version = resolveVersion( parentArtifact, "parent", releaseProject );

                        releaseModel.getParent().setVersion( version );
                    }
                }

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
                        newdep.setVersion( artifact.getVersion() );
                        newdep.setType( artifact.getType() );
                        newdep.setScope( artifact.getScope() );
                        newdep.setClassifier( artifact.getClassifier() );

                        newdeps.add( newdep );
                    }

                    releaseModel.setDependencies( newdeps );
                }

                List plugins = releaseProject.getBuildPlugins();

                if ( plugins != null )
                {
                    //Rewrite plugins version
                    Map pluginArtifacts = releaseProject.getPluginArtifactMap();

                    for ( Iterator i = plugins.iterator(); i.hasNext(); )
                    {
                        Plugin plugin = (Plugin) i.next();

                        Artifact artifact = (Artifact) pluginArtifacts.get( plugin.getKey() );

                        String version = resolveVersion( artifact, "plugin", releaseProject );

                        plugin.setVersion( version );
                    }
                }

                List reports = releaseProject.getReportPlugins();

                if ( reports != null )
                {
                    //Rewrite report version
                    Map reportArtifacts = releaseProject.getReportArtifactMap();

                    for ( Iterator i = reports.iterator(); i.hasNext(); )
                    {
                        ReportPlugin plugin = (ReportPlugin) i.next();

                        Artifact artifact = (Artifact) reportArtifacts.get( plugin.getKey() );

                        String version = resolveVersion( artifact, "report", releaseProject );

                        plugin.setVersion( version );
                    }
                }

                List extensions = releaseProject.getBuildExtensions();

                if ( extensions != null )
                {
                    //Rewrite extension version
                    Map extensionArtifacts = releaseProject.getExtensionArtifactMap();

                    for ( Iterator i = extensions.iterator(); i.hasNext(); )
                    {
                        Extension ext = (Extension) i.next();

                        String extensionId = ArtifactUtils.versionlessKey( ext.getGroupId(), ext.getArtifactId() );

                        Artifact artifact = (Artifact) extensionArtifacts.get( extensionId );

                        String version = resolveVersion( artifact, "extension", releaseProject );

                        ext.setVersion( version );
                    }
                }

                File releasePomFile = new File( basedir, RELEASE_POM );

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
                    
                    releasePomPath = releasePomPath.substring( canonicalBasedir.length() );
                    
                    ScmHelper scm = getScm();

                    scm.setWorkingDirectory( basedir );

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

    private String resolveVersion( Artifact artifact, String artifactUsage, MavenProject project )
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
                    artifactMetadataSource.retrieve( artifact, localRepository,
                                                     project.getPluginArtifactRepositories() );
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
            checkIn( "**/pom.xml,**/release-pom.xml", "[maven-release-plugin] prepare release" );

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
            File currentReleasePomFile = null;

            try
            {
                String canonicalBasedir = trimPathForScmCalculation( new File( basedir ) );

                for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
                {
                    MavenProject project = (MavenProject) it.next();

                    currentReleasePomFile = new File( project.getFile().getParentFile(), RELEASE_POM );

                    String releasePom = trimPathForScmCalculation( currentReleasePomFile );

                    releasePom = releasePom.substring( canonicalBasedir.length() );

                    getScm().remove( "Removing for next development iteration.", releasePom );

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

        path.replace( File.separatorChar, '/' );

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
            checkIn( "**/pom.xml", "[maven-release-plugin] prepare for next development iteration" );

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

    private void checkIn( String includePattern, String message )
        throws MojoExecutionException
    {
        try
        {
            ScmHelper scm = getScm();

            scm.setWorkingDirectory( basedir );

            String tag = scm.getTag();

            // No tag here - we suppose user works on correct branch
            scm.setTag( null );

            scm.checkin( message, includePattern, null );

            scm.setTag( tag );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the checkin process.", e );
        }
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
            catch ( Exception e )
            {
                throw new MojoExecutionException( "An error is occurred in the tag process.", e );
            }
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
                ScmHelper scm = getScm();

                scm.setWorkingDirectory( basedir );

                scm.setTag( tag );

                getLog().info( "Tagging release with the label " + tag + "." );

                scm.tag();
            }
            catch ( Exception e )
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
