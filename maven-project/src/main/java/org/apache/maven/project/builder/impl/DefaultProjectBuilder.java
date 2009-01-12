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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.IdModelContainerFactory;
import org.apache.maven.project.builder.PomArtifactResolver;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.project.builder.PomClassicDomainModelFactory;
import org.apache.maven.project.builder.PomClassicTransformer;
import org.apache.maven.project.builder.PomTransformer;
import org.apache.maven.project.builder.ProjectBuilder;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelEventListener;
import org.apache.maven.shared.model.ModelTransformerContext;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * Default implementation of the project builder.
 */
@Component(role = ProjectBuilder.class)
public class DefaultProjectBuilder
    implements ProjectBuilder, LogEnabled
{
    @Requirement
    private ArtifactFactory artifactFactory;
    
    @Requirement
    private MavenTools mavenTools;
       
    @Requirement
    List<ModelEventListener> listeners;

    private Logger logger;

    public PomClassicDomainModel buildModel( File pom, 
                                             Collection<InterpolatorProperty> interpolatorProperties,
                                             PomArtifactResolver resolver )
        throws IOException    
    {
        return buildModel( pom, null, interpolatorProperties, resolver );        
    }    
    
    public PomClassicDomainModel buildModel( File pom, 
                                             List<Model> mixins,
                                             Collection<InterpolatorProperty> interpolatorProperties,
                                             PomArtifactResolver resolver ) 
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

        if ( mixins == null )
        {
            mixins = new ArrayList<Model>();
            mixins.add( getSuperModel() );            
        }
        else
        {
            mixins = new ArrayList<Model>( mixins );
            Collections.reverse( mixins );
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
        domainModel.setProjectDirectory( pom.getParentFile() );

        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        domainModels.add( domainModel );

        File parentFile = null;
        int lineageCount = 0;
        if ( domainModel.getModel().getParent() != null )
        {
            List<DomainModel> mavenParents;
            if ( isParentLocal( domainModel.getModel().getParent(), pom.getParentFile() ) )
            {
                mavenParents = getDomainModelParentsFromLocalPath( domainModel, resolver, pom.getParentFile() );
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
                lineageCount = mavenParents.size();
            }
            
            domainModels.addAll( mavenParents );
        }

        for ( Model model : mixins )
        {
            domainModels.add( new PomClassicDomainModel( model ) );
        }
        
        PomClassicTransformer transformer = new PomClassicTransformer( new PomClassicDomainModelFactory() );
        
        ModelTransformerContext ctx = new ModelTransformerContext(PomTransformer.MODEL_CONTAINER_FACTORIES );
        
        PomClassicDomainModel transformedDomainModel = ( (PomClassicDomainModel) ctx.transform( domainModels,
                                                                                                transformer,
                                                                                                transformer,
                                                                                                Collections.EMPTY_LIST,
                                                                                                properties,                                                                 
                                                                                                listeners ) );
        // Lineage count is inclusive to add the POM read in itself.
        transformedDomainModel.setLineageCount( lineageCount + 1 );
        transformedDomainModel.setParentFile( parentFile );
        
        return transformedDomainModel;
    }
    
    public MavenProject buildFromLocalPath( File pom, 
                                            List<Model> mixins,
                                            Collection<InterpolatorProperty> interpolatorProperties,
                                            PomArtifactResolver resolver, 
                                            ProjectBuilderConfiguration projectBuilderConfiguration )
        throws IOException
    {
        PomClassicDomainModel domainModel = buildModel( pom, 
                                                        mixins, 
                                                        interpolatorProperties, 
                                                        resolver ); 
        
        try
        {
            MavenProject mavenProject = new MavenProject( domainModel.getModel(), 
                                                          artifactFactory, 
                                                          mavenTools, 
                                                          null, 
                                                          projectBuilderConfiguration );
            
            mavenProject.setParentFile( domainModel.getParentFile() );
            
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
            
            return f.isFile();
        }
        catch ( IOException e )
        {
            return false;
        }
    }

    private List<DomainModel> getDomainModelParentsFromRepository( PomClassicDomainModel domainModel,
                                                                   PomArtifactResolver artifactResolver )
        throws IOException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        Parent parent = domainModel.getModel().getParent();

        if ( parent == null )
        {
            return domainModels;
        }

        Artifact artifactParent = artifactFactory.createParentArtifact( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
        
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

        if ( !parentFile.isFile() )
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

    // Super Model Handling
    
    private static final String MAVEN_MODEL_VERSION = "4.0.0";

    private MavenXpp3Reader modelReader = new MavenXpp3Reader();

    private Model superModel;

    public Model getSuperModel()
    {
        if ( superModel != null )
        {
            return superModel;
        }

        Reader reader = null;
        
        try
        {
            reader = ReaderFactory.newXmlReader( getClass().getClassLoader().getResource( "org/apache/maven/project/pom-" + MAVEN_MODEL_VERSION + ".xml" ) );
                        
            superModel = modelReader.read( reader, true );                  
        }
        catch ( Exception e )
        {
            // Not going to happen we're reading the super pom embedded in the JAR
        }
        finally
        {
            IOUtil.close( reader );            
        }
        
        return superModel;        
    }        
}
