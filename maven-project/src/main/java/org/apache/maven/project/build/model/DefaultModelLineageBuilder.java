package org.apache.maven.project.build.model;

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

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.workspace.ProjectWorkspace;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @see org.apache.maven.project.build.model.ModelLineageBuilder
 */
public class DefaultModelLineageBuilder
    implements ModelLineageBuilder, LogEnabled
{

    public static final String ROLE_HINT = "default";

    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private MavenTools mavenTools;

    private ProfileAdvisor profileAdvisor;

    private ProjectWorkspace projectWorkspace;

    private Logger logger;

    public DefaultModelLineageBuilder()
    {
    }

    public DefaultModelLineageBuilder( ArtifactResolver resolver,
                                       ArtifactFactory artifactFactory )
    {
        artifactResolver = resolver;
        this.artifactFactory = artifactFactory;
    }

    /**
     * @see org.apache.maven.project.build.model.ModelLineageBuilder#buildModelLineage(java.io.File, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List)
     */
    public ModelLineage buildModelLineage( File pom,
                                           ProjectBuilderConfiguration config,
                                           List remoteRepositories,
                                           boolean allowStubs,
                                           boolean isReactorProject )
        throws ProjectBuildingException
    {
        ModelLineage lineage = new DefaultModelLineage();

        List currentRemoteRepositories = remoteRepositories == null ? new ArrayList()
                        : new ArrayList( remoteRepositories );

        ModelAndFile current = projectWorkspace.getModelAndFile( pom );
        if ( current == null )
        {
            current = new ModelAndFile( readModel( pom ), pom, isReactorProject );
            projectWorkspace.storeModelAndFile( current );
        }

        do
        {
            currentRemoteRepositories = updateRepositorySet( current.getModel(),
                                                             currentRemoteRepositories,
                                                             current.getFile(),
                                                             config,
                                                             current.isValidProfilesXmlLocation() );
            if ( lineage.size() == 0 )
            {
                lineage.setOrigin( current.getModel(),
                                   current.getFile(),
                                   currentRemoteRepositories,
                                   current.isValidProfilesXmlLocation() );
            }
            else
            {
                lineage.addParent( current.getModel(),
                                   current.getFile(),
                                   currentRemoteRepositories,
                                   current.isValidProfilesXmlLocation() );
            }


            current = resolveParentPom( current,
                                        currentRemoteRepositories,
                                        config,
                                        allowStubs,
                                        isReactorProject );
        }
        while ( current != null );

        return lineage;
    }

    public void resumeBuildingModelLineage( ModelLineage lineage,
                                            ProjectBuilderConfiguration config,
                                            boolean allowStubs,
                                            boolean isReactorProject )
        throws ProjectBuildingException
    {
        if ( lineage.size() == 0 )
        {
            throw new ProjectBuildingException( "unknown",
                                                "Cannot resume a ModelLineage that doesn't contain at least one Model instance." );
        }

        List currentRemoteRepositories = lineage.getDeepestAncestorArtifactRepositoryList();

        if ( currentRemoteRepositories == null )
        {
            currentRemoteRepositories = new ArrayList();
        }

        ModelAndFile current = new ModelAndFile( lineage.getDeepestAncestorModel(),
                                                 lineage.getDeepestAncestorFile(),
                                                 lineage.isDeepestAncestorUsingProfilesXml() );

        // use the above information to re-bootstrap the resolution chain...
        current = resolveParentPom( current,
                                    currentRemoteRepositories,
                                    config,
                                    allowStubs,
                                    isReactorProject);

        while ( current != null )
        {
            lineage.addParent( current.getModel(),
                               current.getFile(),
                               currentRemoteRepositories,
                               current.isValidProfilesXmlLocation() );

            currentRemoteRepositories = updateRepositorySet( current.getModel(),
                                                             currentRemoteRepositories,
                                                             current.getFile(),
                                                             config,
                                                             current.isValidProfilesXmlLocation() );

            current = resolveParentPom( current,
                                        currentRemoteRepositories,
                                        config,
                                        allowStubs,
                                        isReactorProject );
        }
    }

    /**
     * Read the Model instance from the given POM file. Skip caching the Model on this call, since
     * it's meant for diagnostic purposes (to determine a parent match).
     */
    private Model readModel( File pomFile )
        throws ProjectBuildingException
    {
        if ( pomFile.isDirectory() )
        {
            pomFile = new File( pomFile, "pom.xml" );
        }

        Model model;
        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( pomFile );
            model = new MavenXpp3Reader().read( reader );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "unknown", "Failed to read model from: " + pomFile,
                                                pomFile, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ProjectBuildingException( "unknown",
                                                "Failed to parse model from: " + pomFile, pomFile,
                                                e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return model;
    }

    /**
     * Update the remote repository set used to resolve parent POMs, by adding those declared in
     * the given model to the HEAD of a new list, then appending the old remote repositories list.
     * The specified pomFile is used for error reporting.
     * @param profileManager
     */
    private List updateRepositorySet( Model model,
                                      List oldArtifactRepositories,
                                      File pomFile,
                                      ProjectBuilderConfiguration config,
                                      boolean useProfilesXml )
        throws ProjectBuildingException
    {
        List repositories = model.getRepositories();

        Set artifactRepositories = null;

        if ( repositories != null )
        {
            try
            {
                List lastRemoteRepos = oldArtifactRepositories;
                List remoteRepos = mavenTools.buildArtifactRepositories( repositories );

                loadActiveProfileRepositories( remoteRepos,
                                               model,
                                               config,
                                               pomFile,
                                               useProfilesXml );

                artifactRepositories = new LinkedHashSet( remoteRepos.size()
                                                          + oldArtifactRepositories.size() );

                artifactRepositories.addAll( remoteRepos );
                artifactRepositories.addAll( lastRemoteRepos );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new ProjectBuildingException( model.getId(),
                                                    "Failed to create ArtifactRepository list for: "
                                                                    + pomFile, pomFile, e );
            }
        }

        return new ArrayList( artifactRepositories );
    }

    private void loadActiveProfileRepositories( List repositories,
                                                Model model,
                                                ProjectBuilderConfiguration config,
                                                File pomFile,
                                                boolean useProfilesXml )
        throws ProjectBuildingException
    {
//        getLogger().debug( "Grabbing profile-injected repositories for: " + model.getId() );

        // FIXME: Find a way to pass in this context, so it's never null!
        ProfileActivationContext context;

        if ( config.getGlobalProfileManager() != null )
        {
            context = config.getGlobalProfileManager().getProfileActivationContext();
        }
        else
        {
            context = new DefaultProfileActivationContext( config.getExecutionProperties(), false );
        }

        LinkedHashSet profileRepos = profileAdvisor.getArtifactRepositoriesFromActiveProfiles( model,
                                                                                               pomFile,
                                                                                               config.getGlobalProfileManager() );

//        getLogger().debug( "Got external-profile repositories: " + profileRepos );

        LinkedHashSet pomProfileRepos = profileAdvisor.getArtifactRepositoriesFromActiveProfiles( model,
                                                                                       pomFile,
                                                                                       useProfilesXml,
                                                                                       context );

//        getLogger().debug( "Got pom-profile repositories: " + pomProfileRepos );

        profileRepos.addAll( pomProfileRepos );

        if ( !profileRepos.isEmpty() )
        {
            repositories.addAll( profileRepos );
        }
    }

    /**
     * Pull the parent specification out of the given model, construct an Artifact instance, and
     * resolve that artifact...then, return the resolved POM file for the parent.
     * @param projectBuildCache
     * @param allowStubs
     * @param childIsReactorProject
     */
    private ModelAndFile resolveParentPom( ModelAndFile child,
                                           List remoteRepositories,
                                           ProjectBuilderConfiguration config,
                                           boolean allowStubs,
                                           boolean childIsReactorProject )
        throws ProjectBuildingException
    {
        Model model = child.getModel();
        File modelPomFile = child.getFile();

        Parent modelParent = model.getParent();

        ModelAndFile result = null;

        if ( modelParent != null )
        {
            validateParentDeclaration( modelParent, model );

            String key = modelParent.getGroupId() + ":" + modelParent.getArtifactId() + ":" + modelParent.getVersion();

            File parentPomFile = null;

            if ( childIsReactorProject && modelPomFile != null )
            {
//                getLogger().debug( "Attempting to locate parent model using relativePath and local filesystem; child is a reactor project." );

                // if the child isn't a reactor project, don't resolve the parent from the local filesystem...use the repository.
                String relativePath = modelParent.getRelativePath();
                File modelDir = modelPomFile.getParentFile();

                parentPomFile = new File( modelDir, relativePath );

                if ( parentPomFile.isDirectory() )
                {
                    parentPomFile = new File( parentPomFile, "pom.xml" );
                }

//                getLogger().debug( "Checking cache for parent model-and-file instance: " + key + " using file: " + parentPomFile );

                result = projectWorkspace.getModelAndFile( parentPomFile );
                if ( result != null && !parentModelMatches( modelParent, result.getModel() ) )
                {
                    parentPomFile = null;
                    result = null;
                }
            }

            if ( result == null )
            {
//                getLogger().debug( "Checking cache for parent model-and-file instance: " + key + " using project groupId:artifactId:version." );

                result = projectWorkspace.getModelAndFile( modelParent.getGroupId(), modelParent.getArtifactId(), modelParent.getVersion() );
            }

            if ( result != null )
            {
//                getLogger().debug( "Returning cached instance." );
                return result;
            }

//            getLogger().debug( "Allowing parent-model resolution to proceed for: " + key + " (child is: " + model.getId() + ")" );

            if ( parentPomFile != null )
            {
                if ( parentPomFile.exists() )
                {
                    Model parentModel = readModel( parentPomFile );
                    if ( !parentModelMatches( modelParent, parentModel ) )
                    {
                        parentPomFile = null;
                    }
                }
                else
                {
                    parentPomFile = null;
                }
            }

            boolean isResolved = false;

            if ( parentPomFile == null )
            {
                try
                {
//                    getLogger().debug( "Attempting to resolve parent POM: " + modelParent.getId() + " using repositories:\n" + StringUtils.join( remoteRepositories.iterator(), "\n" ) );

                    parentPomFile = resolveParentFromRepositories( modelParent,
                                                                   config,
                                                                   remoteRepositories,
                                                                   model.getId(),
                                                                   modelPomFile );
                    isResolved = true;
                }
                catch ( ProjectBuildingException e )
                {
                    if ( allowStubs )
                    {
                        getLogger().warn( "An error was encountered while resolving artifact for: "
                                          + modelParent.getId() + "\n\nError was: "
                                          + e.getMessage() );

                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug( "Stack trace: ", e );
                        }

                        parentPomFile = null;
                    }
                    else
                    {
                        throw e;
                    }
                }
            }

            if ( parentPomFile == null )
            {
                if ( allowStubs )
                {
                    getLogger().warn( "Cannot find parent POM: " + modelParent.getId()
                                      + " for child: " + model.getId()
                                      + ".\n\nMaven is using a stub model instead for this build." );

                    Model parent = new Model();

                    parent.setGroupId( modelParent.getGroupId() );
                    parent.setArtifactId( modelParent.getArtifactId() );
                    parent.setVersion( modelParent.getVersion() );

                    // we act as if the POM was resolved from the repository,
                    // for the purposes of external profiles.xml files...
                    // that's what the last parameter is about.
                    result = new ModelAndFile( parent, parentPomFile, false );
                }
                else
                {
                    getLogger().error( "Cannot find parent POM: " + modelParent.getId() );
                }
            }
            else
            {
                Model parent = readModel( parentPomFile );
                result = new ModelAndFile( parent, parentPomFile, !isResolved );
            }
        }

        if ( result != null )
        {
//            getLogger().debug( "Caching parent model-and-file: " + result );
            projectWorkspace.storeModelAndFile( result );
        }

        return result;
    }

    private boolean parentModelMatches( Parent modelParent, Model parentModel )
    {

        boolean groupsMatch = ( parentModel.getGroupId() == null )
                              || parentModel.getGroupId().equals( modelParent.getGroupId() );
        boolean versionsMatch = ( parentModel.getVersion() == null )
                                || parentModel.getVersion().equals( modelParent.getVersion() );

        if ( groupsMatch && versionsMatch
             && parentModel.getArtifactId().equals( modelParent.getArtifactId() ) )
        {
            return true;
        }

        return false;
    }

    private void validateParentDeclaration( Parent modelParent,
                                            Model model )
        throws ProjectBuildingException
    {
        if ( StringUtils.isEmpty( modelParent.getGroupId() ) )
        {
            throw new ProjectBuildingException( model.getId(),
                                                "Missing groupId element from parent element" );
        }
        else if ( StringUtils.isEmpty( modelParent.getArtifactId() ) )
        {
            throw new ProjectBuildingException( model.getId(),
                                                "Missing artifactId element from parent element" );
        }
        else if ( modelParent.getGroupId().equals( model.getGroupId() )
                  && modelParent.getArtifactId().equals( model.getArtifactId() ) )
        {
            throw new ProjectBuildingException( model.getId(), "Parent element is a duplicate of "
                                                               + "the current project " );
        }
        else if ( StringUtils.isEmpty( modelParent.getVersion() ) )
        {
            throw new ProjectBuildingException( model.getId(),
                                                "Missing version element from parent element" );
        }
    }

    private File resolveParentFromRepositories( Parent modelParent,
                                                ProjectBuilderConfiguration config,
                                                List remoteRepositories,
                                                String childId,
                                                File childPomFile )
        throws ProjectBuildingException
    {
        Artifact parentPomArtifact = artifactFactory.createBuildArtifact( modelParent.getGroupId(),
                                                                          modelParent.getArtifactId(),
                                                                          modelParent.getVersion(),
                                                                          "pom" );

        try
        {
            artifactResolver.resolve( parentPomArtifact, remoteRepositories, config.getLocalRepository() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( childId, "Failed to resolve parent POM: "
                                                         + modelParent.getId(), childPomFile, e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ProjectBuildingException( childId, "Cannot find artifact for parent POM: "
                                                         + modelParent.getId(), childPomFile, e );
        }

        if ( parentPomArtifact.isResolved() )
        {
            return parentPomArtifact.getFile();
        }
        else
        {
            return null;
        }
    }

    private Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "DefaultModelLineageBuilder:internal" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
