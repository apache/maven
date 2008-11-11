package org.apache.maven.project.builder.impl;

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
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.IdModelContainerFactory;
import org.apache.maven.project.builder.PomArtifactResolver;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.project.builder.PomClassicTransformer;
import org.apache.maven.project.builder.ProjectBuilder;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.ImportModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelTransformerContext;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of the project builder.
 */
public final class DefaultProjectBuilder
    implements ProjectBuilder, LogEnabled
{

    private ArtifactFactory artifactFactory;

    /**
     * Logger instance
     */
    private Logger logger;

    private ModelValidator validator;

    private MavenTools mavenTools;

    /**
     * Default constructor
     */
    public DefaultProjectBuilder()
    {
    }

    /**
     * Constructor
     *
     * @param artifactFactory the artifact factory
     */
    protected DefaultProjectBuilder( ArtifactFactory artifactFactory )
    {
        if ( artifactFactory == null )
        {
            throw new IllegalArgumentException( "artifactFactory: null" );
        }
        this.artifactFactory = artifactFactory;
    }

    /**
     * @see ProjectBuilder#buildFromLocalPath(java.io.InputStream, java.util.List, java.util.Collection, java.util.Collection, org.apache.maven.project.builder.PomArtifactResolver, java.io.File, org.apache.maven.project.ProjectBuilderConfiguration)
     */
    public MavenProject buildFromLocalPath( InputStream pom, List<Model> inheritedModels,
                                            Collection<ImportModel> importModels,
                                            Collection<InterpolatorProperty> interpolatorProperties,
                                            PomArtifactResolver resolver, File projectDirectory,
                                            ProjectBuilderConfiguration projectBuilderConfiguration )
        throws IOException
    {
        if ( pom == null )
        {
            throw new IllegalArgumentException( "pom: null" );
        }

        if ( resolver == null )
        {
            throw new IllegalArgumentException( "resolver: null" );
        }

        if ( projectDirectory == null )
        {
            throw new IllegalArgumentException( "projectDirectory: null" );
        }

        if ( inheritedModels == null )
        {
            inheritedModels = new ArrayList<Model>();
        }
        else
        {
            inheritedModels = new ArrayList<Model>( inheritedModels );
            Collections.reverse( inheritedModels );
        }

        List<InterpolatorProperty> properties;
        if ( interpolatorProperties == null )
        {
            properties = new ArrayList<InterpolatorProperty>();
        }
        else
        {
            properties = new ArrayList<InterpolatorProperty>( interpolatorProperties );
        }

        PomClassicDomainModel domainModel = new PomClassicDomainModel( pom );
        domainModel.setProjectDirectory( projectDirectory );

        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        domainModels.add( domainModel );

        File parentFile = null;
        if ( domainModel.getModel().getParent() != null )
        {
            List<DomainModel> mavenParents;
            if ( isParentLocal( domainModel.getModel().getParent(), projectDirectory ) )
            {
                mavenParents = getDomainModelParentsFromLocalPath( domainModel, resolver, projectDirectory );
            }
            else
            {
                mavenParents = getDomainModelParentsFromRepository( domainModel, resolver );
            }

            if ( mavenParents.size() > 0 )
            {
                PomClassicDomainModel dm = (PomClassicDomainModel) mavenParents.get( 0 );
                parentFile = dm.getFile();
                domainModel.setParentFile( parentFile );
            }

            domainModels.addAll( mavenParents );
        }

        for ( Model model : inheritedModels )
        {
            domainModels.add( new PomClassicDomainModel( model ) );
        }

        PomClassicTransformer transformer = new PomClassicTransformer( null );
        ModelTransformerContext ctx = new ModelTransformerContext(
            Arrays.asList( new ArtifactModelContainerFactory(), new IdModelContainerFactory() ) );

        PomClassicDomainModel transformedDomainModel = ( (PomClassicDomainModel) ctx.transform( domainModels,
                                                                                                transformer,
                                                                                                transformer,
                                                                                                importModels,
                                                                                                properties ) );
        try
        {
            MavenProject mavenProject = new MavenProject( transformedDomainModel.getModel(), artifactFactory,
                                                          mavenTools, null,
                                                          projectBuilderConfiguration );
            mavenProject.setParentFile( parentFile );
            return mavenProject;
        }
        catch ( InvalidRepositoryException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    /**
     * Returns true if the relative path of the specified parent references a pom, otherwise returns false.
     *
     * @param parent           the parent model info
     * @param projectDirectory the project directory of the child pom
     * @return true if the relative path of the specified parent references a pom, otherwise returns fals
     */
    private boolean isParentLocal( Parent parent, File projectDirectory )
    {
        try
        {
            File f = new File( projectDirectory, parent.getRelativePath() ).getCanonicalFile();
            if ( f.isDirectory() )
            {
                f = new File( f, "pom.xml" );
            }
            return f.exists();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            return false;
        }
    }

    private List<DomainModel> getDomainModelParentsFromRepository( PomClassicDomainModel domainModel,
                                                                   PomArtifactResolver artifactResolver )
        throws IOException
    {
        if ( artifactFactory == null )
        {
            throw new IllegalArgumentException( "artifactFactory: not initialized" );
        }

        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        Parent parent = domainModel.getModel().getParent();

        if ( parent == null )
        {
            return domainModels;
        }

        Artifact artifactParent =
            artifactFactory.createParentArtifact( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
        artifactResolver.resolve( artifactParent );

        PomClassicDomainModel parentDomainModel = new PomClassicDomainModel( artifactParent.getFile() );

        if ( !parentDomainModel.matchesParent( domainModel.getModel().getParent() ) )
        {
            logger.debug( "Parent pom ids do not match: Parent File = " + artifactParent.getFile().getAbsolutePath() +
                ": Child ID = " + domainModel.getModel().getId() );
            return domainModels;
        }

        domainModels.add( parentDomainModel );
        domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, artifactResolver ) );
        return domainModels;
    }

    /**
     * Returns list of domain model parents of the specified domain model. The parent domain models are part
     *
     * @param domainModel
     * @param artifactResolver
     * @param projectDirectory
     * @return
     * @throws IOException
     */
    private List<DomainModel> getDomainModelParentsFromLocalPath( PomClassicDomainModel domainModel,
                                                                  PomArtifactResolver artifactResolver,
                                                                  File projectDirectory )
        throws IOException
    {

        if ( artifactFactory == null )
        {
            throw new IllegalArgumentException( "artifactFactory: not initialized" );
        }

        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        Parent parent = domainModel.getModel().getParent();

        if ( parent == null )
        {
            return domainModels;
        }

        Model model = domainModel.getModel();

        File parentFile = new File( projectDirectory, model.getParent().getRelativePath() ).getCanonicalFile();
        if ( parentFile.isDirectory() )
        {
            parentFile = new File( parentFile.getAbsolutePath(), "pom.xml" );
        }

        if ( !parentFile.exists() )
        {
            throw new IOException( "File does not exist: File = " + parentFile.getAbsolutePath() );
        }

        PomClassicDomainModel parentDomainModel = new PomClassicDomainModel( parentFile );
        parentDomainModel.setProjectDirectory( parentFile.getParentFile() );

        if ( !parentDomainModel.matchesParent( domainModel.getModel().getParent() ) )
        {
            logger.debug( "Parent pom ids do not match: Parent File = " + parentFile.getAbsolutePath() + ", Parent ID = "
                    + parentDomainModel.getId() + ", Child ID = " + domainModel.getId() + ", Expected Parent ID = "
                    + domainModel.getModel().getParent().getId() );
            List<DomainModel> parentDomainModels = getDomainModelParentsFromRepository( domainModel, artifactResolver );
            if(parentDomainModels.size() == 0)
            {
                throw new IOException("Unable to find parent pom on local path or repo: "
                        + domainModel.getModel().getParent().getId());
            }
            //logger.info("Attempting to lookup from the repository: Found parents: " + parentDomainModels.size());
            domainModels.addAll( parentDomainModels );
            return domainModels;
        }

        domainModels.add( parentDomainModel );
        if ( parentDomainModel.getModel().getParent() != null )
        {
            if ( isParentLocal( parentDomainModel.getModel().getParent(), parentFile.getParentFile() ) )
            {
                domainModels.addAll( getDomainModelParentsFromLocalPath( parentDomainModel, artifactResolver,
                                                                         parentFile.getParentFile() ) );
            }
            else
            {
                domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, artifactResolver ) );
            }
        }

        return domainModels;
    }


    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    private void validateModel( Model model )
        throws IOException
    {
        ModelValidationResult validationResult = validator.validate( model );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new IOException( "Failed to validate: " + validationResult.toString() );
        }
    }
}
