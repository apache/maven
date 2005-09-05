package org.apache.maven.plugin.it;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @goal fork
 *
 * @execute phase="package"
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 */
public class ForkMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${session}"
     */
    private MavenSession session;

    /**
     * @parameter expression="${settings}"
     */
    private Settings settings;


    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.basedir}/src/it/"
     */
    private File projectsDir;


    private ArtifactRepository artifactRepo;

    private MavenProjectBuilder projectBuilder;


    public void execute()
        throws MojoExecutionException
    {
        initComponents();

        buildProjects( listITPoms() );
    }

    private void initComponents()
        throws MojoExecutionException
    {
        try
        {
            artifactRepo = createLocalRepository();

            projectBuilder = (MavenProjectBuilder)
                session.getContainer().lookup( MavenProjectBuilder.ROLE );

            if ( projectBuilder == null )
                throw new MojoExecutionException( "Lookup for MavenProjectBuilder returned null" );
        }
        catch (ComponentLookupException e)
        {
            throw new MojoExecutionException( "Cannot get a MavenProjectBuilder", e);
        }
    }

    private void buildProjects( List poms )
    {
        getLog().info( "Building " + poms.size() + " integration test projects.." );
        for ( Iterator i = poms.iterator(); i.hasNext(); )
        {
            try
            {
                MavenProject p = buildProject( (File) i.next() );

                getLog().info("Building " + p.getId() );
            }
            catch (ProjectBuildingException e)
            {
                getLog().error("Build Error", e);
            }
        }
    }

    private MavenProject buildProject( File pom )
        throws ProjectBuildingException
    {
        return projectBuilder.build( pom, artifactRepo, new DefaultProfileManager(
            session.getContainer() ) );
    }

    private List listITPoms()
    {
        List poms = new ArrayList();

        File [] children = projectsDir.listFiles();

        for ( int i = 0; i < children.length; i++ )
        {
            if ( children[i].isDirectory() )
            {
                File pomFile = new File( children[i], "pom.xml" );

                if ( pomFile.exists() && pomFile.isFile() )
                {
                    poms.add( pomFile );
                }

            }
        }

        return poms;
    }

    // Duplicate code from MavenCli, slightly modified.

    private ArtifactRepository createLocalRepository()
        throws ComponentLookupException
    {
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout)
            session.getContainer().lookup( ArtifactRepositoryLayout.ROLE, "default" );

        ArtifactRepositoryFactory artifactRepositoryFactory = (ArtifactRepositoryFactory)
            session.getContainer().lookup(
        ArtifactRepositoryFactory.ROLE );

        String url = "file://" + settings.getLocalRepository();
        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", url, repositoryLayout );

        boolean snapshotPolicySet = false;

        if ( settings.isOffline() )
        {
            artifactRepositoryFactory.setGlobalEnable( false );
            snapshotPolicySet = true;
        }

        /* can't do this here.. :(
        if ( !snapshotPolicySet && commandLine.hasOption( CLIManager.UPDATE_SNAPSHOTS ) )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        if ( commandLine.hasOption( CLIManager.CHECKSUM_FAILURE_POLICY ) )
        {
            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL );
        }
        else if ( commandLine.hasOption( CLIManager.CHECKSUM_WARNING_POLICY ) )
        {
            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
        }
        */

        return localRepository;
    }

}
