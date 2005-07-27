package org.apache.maven.plugin.release;

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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.scm.ScmBean;
import org.apache.maven.plugin.transformer.PomTransformer;
import org.apache.maven.plugin.transformer.VersionTransformer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileStatus;
import org.codehaus.plexus.components.inputhandler.InputHandler;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Prepare for a release in SCM
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id: DoxiaMojo.java 169372 2005-05-09 22:47:34Z evenisse $
 * @goal prepare
 * @requiresDependencyResolution test
 * @todo check how this works with version ranges
 */
public class PrepareReleaseMojo
    extends AbstractReleaseMojo
{
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

    private static final String SNAPSHOT = "-SNAPSHOT";

    private static final String RELEASE_POM = "release-pom.xml";

    private String projectVersion;

    private Model model;

    private String userTag;

    private String currentTag;

    private String currentScmConnection;

    private String currentScmDeveloperConnection;

    protected void executeTask()
        throws MojoExecutionException
    {
        model = project.getModel();

        checkForLocalModifications();

        checkForPresenceOfSnapshots();

        transformPomToReleaseVersionPom();

        generateReleasePropertiesFile();

        generateReleasePom();

        checkInReleaseVersionPom();

        tagRelease();

        transformPomToSnapshotVersionPom();

        new File( basedir, RELEASE_POM ).delete();

        checkInSnapshotVersionPom();
    }

    private void generateReleasePropertiesFile()
        throws MojoExecutionException
    {
        try
        {
            Properties releaseProperties = new Properties();

            releaseProperties.setProperty( "version", projectVersion );

            releaseProperties.setProperty( USERNAME, username );

            releaseProperties.setProperty( TAG, getTagLabel() );

            releaseProperties.setProperty( SCM_URL, urlScm );

            FileOutputStream os = new FileOutputStream( new File( project.getFile().getParentFile(), RELEASE_PROPS ) );

            releaseProperties.store( os, "maven release plugin info" );

            os.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannote write release-version file.", e );
        }
    }

    private boolean isSnapshot( String version )
    {
        return version.endsWith( SNAPSHOT );
    }

    private void checkForLocalModifications()
        throws MojoExecutionException
    {
        getLog().info( "Verifying there are no local modifications ..." );

        List changedFiles;

        try
        {
            ScmBean scm = getScm();

            scm.setWorkingDirectory( basedir );

            changedFiles = scm.getStatus();
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "An error is occurred in the status process.", e );
        }

        for ( Iterator i = changedFiles.iterator(); i.hasNext(); )
        {
            ScmFile f = (ScmFile) i.next();
            if ( f.getPath().equals( "pom.xml.backup" ) || f.getPath().equals( RELEASE_PROPS ) )
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
    private void checkForPresenceOfSnapshots()
        throws MojoExecutionException
    {
        MavenProject currentProject = project;

        getLog().info( "Checking lineage for snapshots ..." );

        while ( currentProject.hasParent() )
        {
            Artifact parentArtifact = currentProject.getParentArtifact();

            if ( isSnapshot( parentArtifact.getVersion() ) )
            {
                throw new MojoExecutionException( "Can't release project due to non released parent." );
            }

            currentProject = currentProject.getParent();
        }

        getLog().info( "Checking dependencies for snapshots ..." );

        List snapshotDependencies = new ArrayList();

        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( isSnapshot( artifact.getVersion() ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        getLog().info( "Checking plugins for snapshots ..." );

        for ( Iterator i = project.getPluginArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( isSnapshot( artifact.getVersion() ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        if ( !snapshotDependencies.isEmpty() )
        {
            Collections.sort( snapshotDependencies );

            StringBuffer message = new StringBuffer();

            for ( Iterator i = snapshotDependencies.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                message.append( "    " );

                message.append( artifact.getGroupId() );

                message.append( ":" );

                message.append( artifact.getArtifactId() );

                message.append( ":" );

                message.append( artifact.getVersion() );

                message.append( "\n" );
            }

            throw new MojoExecutionException(
                "Can't release project due to non released dependencies :\n" + message.toString() );
        }
    }

    private void transformPomToReleaseVersionPom()
        throws MojoExecutionException
    {
        if ( !isSnapshot( model.getVersion() ) )
        {
            throw new MojoExecutionException( "This project isn't a snapshot (" + model.getVersion() + ")." );
        }

        //Rewrite project version
        projectVersion = model.getVersion().substring( 0, model.getVersion().length() - SNAPSHOT.length() );

        try
        {
            getLog().info( "What is the new version? [" + projectVersion + "]" );

            String inputVersion = inputHandler.readLine();

            if ( !StringUtils.isEmpty( inputVersion ) )
            {
                projectVersion = inputVersion;
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Can't read user input.", e );
        }

        model.setVersion( projectVersion );

        currentTag = model.getScm().getTag();

        currentScmConnection = model.getScm().getConnection();

        currentScmDeveloperConnection = model.getScm().getDeveloperConnection();

        if ( model.getScm() != null )
        {
            model.getScm().setTag( getTagLabel() );

            model.getScm().setConnection( rewriteScmConnection( model.getScm().getConnection(), getTagLabel() ) );

            model.getScm().setDeveloperConnection(
                rewriteScmConnection( model.getScm().getDeveloperConnection(), getTagLabel() ) );
        }

        //Rewrite parent version
        if ( project.hasParent() )
        {
            Artifact parentArtifact = project.getParentArtifact();

            if ( isSnapshot( parentArtifact.getBaseVersion() ) )
            {
                String version = resolveVersion( parentArtifact, "parent" );

                model.getParent().setVersion( version );
            }
        }

        //Rewrite dependencies section
        Map artifactMap = ArtifactUtils.artifactMapByArtifactId( project.getArtifacts() );

        for ( Iterator i = model.getDependencies().iterator(); i.hasNext(); )
        {
            Dependency dep = (Dependency) i.next();

            String conflictId = ArtifactUtils.artifactId( dep.getGroupId(), dep.getArtifactId(), dep.getType(),
                                                          dep.getClassifier(), dep.getVersion() );

            Artifact artifact = (Artifact) artifactMap.get( conflictId );

            dep.setVersion( artifact.getVersion() );
        }

        try
        {
            PomTransformer transformer = new VersionTransformer();

            transformer.setOutputFile( project.getFile() );

            transformer.setProject( project.getFile() );

            transformer.setUpdatedModel( model );

            transformer.transformNodes();

            transformer.write();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Can't transform pom to its release version form.", e );
        }
    }

    private void generateReleasePom()
        throws MojoExecutionException
    {
        MavenProject releaseProject = new MavenProject( project );
        Model releaseModel = releaseProject.getModel();

        //Rewrite parent version
        if ( project.hasParent() )
        {
            Artifact parentArtifact = project.getParentArtifact();

            if ( isSnapshot( parentArtifact.getBaseVersion() ) )
            {
                String version = resolveVersion( parentArtifact, "parent" );

                model.getParent().setVersion( version );
            }
        }

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

        //Rewrite plugins version
        for ( Iterator i = releaseProject.getPluginArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( isSnapshot( artifact.getBaseVersion() ) )
            {
                for ( Iterator j = releaseModel.getBuild().getPlugins().iterator(); j.hasNext(); )
                {
                    Plugin plugin = (Plugin) j.next();
                    if ( ArtifactUtils.versionlessKey( artifact ).equals( plugin.getKey() ) )
                    {
                        String version = resolveVersion( artifact, "plugin" );

                        plugin.setGroupId( artifact.getGroupId() );
                        plugin.setVersion( version );
                    }
                }
            }
        }

        //Rewrite report version
        for ( Iterator i = releaseProject.getReportArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( isSnapshot( artifact.getBaseVersion() ) )
            {
                List reportPlugins = releaseProject.getReportPlugins();

                if ( reportPlugins != null )
                {
                    for ( Iterator j = reportPlugins.iterator(); j.hasNext(); )
                    {
                        ReportPlugin plugin = (ReportPlugin) j.next();
                        if ( ArtifactUtils.versionlessKey( artifact ).equals( plugin.getKey() ) )
                        {
                            String version = resolveVersion( artifact, "report" );

                            plugin.setGroupId( artifact.getGroupId() );
                            plugin.setVersion( version );
                        }
                    }
                }
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
            ScmBean scm = getScm();

            scm.setWorkingDirectory( basedir );

            List scmChanges = scm.getStatus();

            for ( Iterator i = scmChanges.iterator(); i.hasNext(); )
            {
                ScmFile f = (ScmFile) i.next();

                if ( f.getPath().equals( "release-pom.xml" ) && f.getStatus() != ScmFileStatus.MODIFIED )
                {
                    getScm().add( RELEASE_POM );
                }
            }
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "Error updating the release-pom.xml.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error updating the release-pom.xml.", e );
        }
    }

    private String resolveVersion( Artifact artifact, String artifactUsage )
        throws MojoExecutionException
    {
        if ( artifact.getFile() == null )
        {
            try
            {
                artifactMetadataSource.retrieve( artifact, localRepository, project.getPluginArtifactRepositories() );
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                throw new MojoExecutionException( "Cannot resolve " + artifactUsage + ": " + artifact.getId(), e );
            }
        }

        return artifact.getVersion();
    }

    private void transformPomToSnapshotVersionPom()
        throws MojoExecutionException
    {
        // TODO: we will need to incorporate versioning strategies here because it is unlikely
        // that everyone will be able to agree on a standard. This is extremely limited right
        // now and really only works for the way maven is versioned.

        // releaseVersion = 1.0-beta-4
        // snapshotVersion = 1.0-beta-5-SNAPSHOT

        String nextVersionString = projectVersion.substring( projectVersion.lastIndexOf( "-" ) + 1 );

        try
        {
            nextVersionString = Integer.toString( Integer.parseInt( nextVersionString ) + 1 );

            projectVersion = projectVersion.substring( 0, projectVersion.lastIndexOf( "-" ) + 1 ) + nextVersionString +
                SNAPSHOT;
        }
        catch ( NumberFormatException e )
        {
            projectVersion = "";
        }

        try
        {
            getLog().info( "What is the new version? [" + projectVersion + "]" );

            String inputVersion = inputHandler.readLine();

            if ( !StringUtils.isEmpty( inputVersion ) )
            {
                projectVersion = inputVersion;
            }

            model.setVersion( projectVersion );

            if ( model.getScm() != null )
            {
                model.getScm().setTag( currentTag );

                model.getScm().setConnection( currentScmConnection );

                model.getScm().setDeveloperConnection( currentScmDeveloperConnection );
            }

            PomTransformer transformer = new VersionTransformer();

            transformer.setOutputFile( project.getFile() );

            transformer.setProject( project.getFile() );

            transformer.setUpdatedModel( model );

            transformer.transformNodes();

            transformer.write();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Can't transform pom to its snapshot version form.", e );
        }
    }


    /**
     * Check in the POM to SCM after it has been transformed where the version has been
     * set to the release version.
     *
     * @throws MojoExecutionException
     */
    private void checkInReleaseVersionPom()
        throws MojoExecutionException
    {
        checkIn( "pom.xml,release-pom.xml", "[maven-release-plugin] prepare release " + projectVersion );
    }

    private void checkInSnapshotVersionPom()
        throws MojoExecutionException
    {
        checkIn( "pom.xml", "[maven-release-plugin] prepare for development " + projectVersion );
    }

    private void checkIn( String includePattern, String message )
        throws MojoExecutionException
    {
        try
        {
            ScmBean scm = getScm();

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

    private String getDefaultTagLabel()
    {
        String tag = project.getArtifactId().toUpperCase() + "_" + projectVersion.toUpperCase();

        tag = tag.replace( '-', '_' );

        tag = tag.replace( '.', '_' );

        return tag;
    }

    private String getTagLabel()
        throws MojoExecutionException
    {
        if ( userTag == null )
        {
            userTag = getDefaultTagLabel();

            try
            {
                ScmBean scm = getScm();

                if ( scm.getTag() == null && interactive )
                {
                    getLog().info( "What tag name should be used? [ " + tag + " ]" );

                    String inputTag = inputHandler.readLine();

                    if ( !StringUtils.isEmpty( inputTag ) )
                    {
                        userTag = inputTag;
                    }
                }
                else
                {
                    userTag = scm.getTag();
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
        String tag = getTagLabel();

        try
        {
            ScmBean scm = getScm();

            scm.setWorkingDirectory( basedir );

            scm.setTag( tag );

            getLog().info( "Tagging release with the label " + tag + "." );

            scm.tag();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the tag process.", e );
        }
    }

    private String rewriteScmConnection( String scmConnection, String tag )
    {
        if ( scmConnection != null )
        {
            if ( scmConnection.startsWith( "svn" ) )
            {
                if ( scmConnection.endsWith( "trunk/" ) )
                {
                    scmConnection = scmConnection.substring( 0, scmConnection.length() - "trunk/".length() );
                }
                if ( scmConnection.endsWith( "branches/" ) )
                {
                    scmConnection = scmConnection.substring( 0, scmConnection.length() - "branches/".length() );
                }
                scmConnection += "tags/" + tag;
            }
        }

        return scmConnection;
    }
}
