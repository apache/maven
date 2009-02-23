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

import java.io.*;
import java.util.*;

import org.apache.maven.mercury.PomProcessor;
import org.apache.maven.mercury.PomProcessorException;
import org.apache.maven.mercury.MavenDomainModel;
import org.apache.maven.mercury.MavenDomainModelFactory;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.builder.*;
import org.apache.maven.project.builder.profile.ProfileContext;
import org.apache.maven.repository.MavenRepositorySystem;
import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Default implementation of the project builder.
 */
@Component(role = ProjectBuilder.class)
public class DefaultProjectBuilder
    implements ProjectBuilder, PomProcessor, LogEnabled
{
    @Requirement
    private MavenRepositorySystem repositorySystem;

    @Requirement
    List<ModelEventListener> listeners;

    private Logger logger;

    public List<ModelProperty> getRawPom(ArtifactBasicMetadata bmd, MetadataReader mdReader, Map system, Map user)
            throws MetadataReaderException, PomProcessorException {
        if ( bmd == null )
        {
            throw new IllegalArgumentException( "bmd: null" );
        }

        if ( mdReader == null )
        {
            throw new IllegalArgumentException( "mdReader: null" );
        }

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        interpolatorProperties.add( new InterpolatorProperty( "${mavenVersion}", "3.0-SNAPSHOT",
                                                              PomInterpolatorTag.EXECUTION_PROPERTIES.name() ) );

        if ( system != null )
        {
            interpolatorProperties.addAll(
                InterpolatorProperty.toInterpolatorProperties( system, PomInterpolatorTag.EXECUTION_PROPERTIES.name() ) );
        }
        if ( user != null )
        {
            interpolatorProperties.addAll(
                InterpolatorProperty.toInterpolatorProperties( user, PomInterpolatorTag.USER_PROPERTIES.name() ) );
        }

       List<DomainModel> domainModels = new ArrayList<DomainModel>();
        try
        {
            // MavenDomainModel superPom =
            //     new MavenDomainModel(MavenDependencyProcessor.class.getResourceAsStream( "pom-4.0.0.xml" ));
            // domainModels.add(superPom);

            byte[] superBytes = mdReader.readMetadata( bmd );

            if ( superBytes == null || superBytes.length < 1 )
                throw new PomProcessorException( "cannot read metadata for " + bmd.getGAV() );

            MavenDomainModel domainModel = new MavenDomainModel( superBytes );
            domainModels.add( domainModel );

            Collection<ModelContainer> activeProfiles = domainModel.getActiveProfileContainers( interpolatorProperties );

            for ( ModelContainer mc : activeProfiles )
            {
                domainModels.add( new MavenDomainModel( transformProfiles( mc.getProperties() ) ) );
            }

            List<DomainModel> parentModels = getParentsOfDomainModel( domainModel, mdReader );

            if( parentModels == null )
                throw new PomProcessorException( "cannot read parent for " + bmd.getGAV() );

            domainModels.addAll( parentModels );
        }
        catch ( IOException e )
        {
            throw new MetadataReaderException( "Failed to create domain model. Message = " + e.getMessage() );
        }

        PomTransformer transformer = new PomTransformer( new MavenDomainModelFactory() );
        ModelTransformerContext ctx =
            new ModelTransformerContext( PomTransformer.MODEL_CONTAINER_INFOS );

        try
        {
            MavenDomainModel model =
                ( (MavenDomainModel) ctx.transform( domainModels, transformer, transformer, null,
                                                    interpolatorProperties, null ) );
            return model.getModelProperties();
        }
        catch ( IOException e )
        {
            throw new MetadataReaderException( "Unable to transform model" );
        }
    }

    public PomClassicDomainModel buildModel( File pom,
                                             Collection<InterpolatorProperty> interpolatorProperties,
                                             PomArtifactResolver resolver )
        throws IOException    
    {
        return buildModel( pom, interpolatorProperties, null, null, resolver );
    }    
    
    private PomClassicDomainModel buildModel(File pom,
                                             Collection<InterpolatorProperty> interpolatorProperties,
                                             Collection<String> activeProfileIds, Collection<String> inactiveProfileIds,
                                             PomArtifactResolver resolver)
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

        if(activeProfileIds == null)
        {
            activeProfileIds = new ArrayList<String>();
        }
        if ( inactiveProfileIds == null )
        {
            inactiveProfileIds = new ArrayList<String>();
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

        //Process Profile on most specialized child model
        ProfileContext profileContext = new ProfileContext(new DefaultModelDataSource(domainModel.getModelProperties(),
                PomTransformer.MODEL_CONTAINER_FACTORIES), activeProfileIds, inactiveProfileIds, properties);

        Collection<ModelContainer> profileContainers = profileContext.getActiveProfiles();

        for(ModelContainer mc : profileContainers)
        {
            List<ModelProperty> transformed = new ArrayList<ModelProperty>();
            //transformed.add(new ModelProperty(ProjectUri.xUri, null));
            for(ModelProperty mp : mc.getProperties())
            {
                if(mp.getUri().startsWith(ProjectUri.Profiles.Profile.xUri) && !mp.getUri().equals(ProjectUri.Profiles.Profile.id)
                        && !mp.getUri().startsWith(ProjectUri.Profiles.Profile.Activation.xUri) )
                {
                    transformed.add(new ModelProperty(mp.getUri().replace(ProjectUri.Profiles.Profile.xUri, ProjectUri.xUri),
                            mp.getResolvedValue()));
                }
            }
            domainModels.add(new PomClassicDomainModel(transformed));
        }

        File parentFile = null;
        int lineageCount = 0;
        if ( domainModel.getParentId() != null )
        {
            List<DomainModel> mavenParents;
            if ( isParentLocal( domainModel.getRelativePathOfParent(), pom.getParentFile() ) )
            {
                mavenParents =
                    getDomainModelParentsFromLocalPath( domainModel, resolver, pom.getParentFile(), properties,
                                                        activeProfileIds, inactiveProfileIds );
            }
            else
            {
                mavenParents =
                    getDomainModelParentsFromRepository( domainModel, resolver, properties, activeProfileIds,
                                                         inactiveProfileIds );
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

        domainModels.add( convertToDomainModel( getSuperModel() ) );

        PomTransformer transformer = new PomTransformer( new PomClassicDomainModelFactory() );
        
        ModelTransformerContext ctx = new ModelTransformerContext(PomTransformer.MODEL_CONTAINER_INFOS );
        
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

    private PomClassicDomainModel convertToDomainModel(Model model) throws IOException
    {
        if ( model == null )
        {
            throw new IllegalArgumentException( "model: null" );
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer out = null;
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try
        {
            out = WriterFactory.newXmlWriter( baos );
            writer.write( out, model );
        }
        finally
        {
            if ( out != null )
            {
                out.close();
            }
        }
        return new PomClassicDomainModel(new ByteArrayInputStream(baos.toByteArray()));
    }
    
    public MavenProject buildFromLocalPath(File pom,
                                           Collection<InterpolatorProperty> interpolatorProperties,
                                           PomArtifactResolver resolver,
                                           ProjectBuilderConfiguration projectBuilderConfiguration,
                                           MavenProjectBuilder mavenProjectBuilder)
        throws IOException
    {

       List<String> activeProfileIds = (projectBuilderConfiguration != null &&
                projectBuilderConfiguration.getGlobalProfileManager() != null &&
                projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext() != null) ?
               projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext().getExplicitlyActiveProfileIds() : new ArrayList<String>();

       List<String> inactiveProfileIds =
           ( projectBuilderConfiguration != null && projectBuilderConfiguration.getGlobalProfileManager() != null && 
                           projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext() != null ) ? 
                           projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext().getExplicitlyInactiveProfileIds() : new ArrayList<String>();

        PomClassicDomainModel domainModel = buildModel( pom,
                interpolatorProperties,
                                                        activeProfileIds, inactiveProfileIds,
                                                        resolver ); 
        
        try
        {
            MavenProject mavenProject = new MavenProject( convertFromInputStreamToModel(domainModel.getInputStream()),
                                                          repositorySystem, 
                                                          mavenProjectBuilder, 
                                                          projectBuilderConfiguration );
            
            mavenProject.setParentFile( domainModel.getParentFile() );
            
            return mavenProject;
        }
        catch ( InvalidRepositoryException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    private static Model convertFromInputStreamToModel(InputStream inputStream) throws IOException
    {

        try
        {
            return new MavenXpp3Reader().read( ReaderFactory.newXmlReader( inputStream ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new IOException( e.getMessage() );
        }

    }

    /**
     * Returns true if the relative path of the specified parent references a pom, otherwise returns false.
     *
     * @param relativePath         the parent model info
     * @param projectDirectory the project directory of the child pom
     * @return true if the relative path of the specified parent references a pom, otherwise returns fals
     */
    private boolean isParentLocal( String relativePath, File projectDirectory )
    {
        try
        {
            File f = new File( projectDirectory, relativePath ).getCanonicalFile();
            
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
                                                                   PomArtifactResolver artifactResolver,
                                                                   List<InterpolatorProperty> properties,
                                                                   Collection<String> activeProfileIds,
                                                                   Collection<String> inactiveProfileIds )
        throws IOException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        String parentId = domainModel.getParentId();

        if ( parentId == null )
        {
            return domainModels;
        }

        Artifact artifactParent = repositorySystem.createParentArtifact( domainModel.getParentGroupId(),
                domainModel.getParentArtifactId(), domainModel.getParentVersion() );
        
        artifactResolver.resolve( artifactParent );

        PomClassicDomainModel parentDomainModel = new PomClassicDomainModel( artifactParent.getFile() );

        if ( !parentDomainModel.matchesParentOf( domainModel ) )
        {
            logger.debug( "Parent pom ids do not match: Parent File = " + artifactParent.getFile().getAbsolutePath() +
                ": Child ID = " + domainModel.getId() );
            return domainModels;
        }

        domainModels.add( parentDomainModel );

        //Process Profiles
        ProfileContext profileContext = new ProfileContext(new DefaultModelDataSource(parentDomainModel.getModelProperties(),
                PomTransformer.MODEL_CONTAINER_FACTORIES), activeProfileIds, inactiveProfileIds, properties);
        Collection<ModelContainer> profileContainers = profileContext.getActiveProfiles();

        for(ModelContainer mc : profileContainers)
        {
            List<ModelProperty> transformed = new ArrayList<ModelProperty>();
            transformed.add(new ModelProperty(ProjectUri.xUri, null));
            for(ModelProperty mp : mc.getProperties())
            {
                if(mp.getUri().startsWith(ProjectUri.Profiles.Profile.xUri) && !mp.getUri().equals(ProjectUri.Profiles.Profile.id)
                        && !mp.getUri().startsWith(ProjectUri.Profiles.Profile.Activation.xUri) )
                {
                    transformed.add(new ModelProperty(mp.getUri().replace(ProjectUri.Profiles.Profile.xUri, ProjectUri.xUri),
                            mp.getResolvedValue()));
                }
            }

            domainModels.add(new PomClassicDomainModel(transformed));
        }        

        domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, artifactResolver, properties,
                                                                  activeProfileIds, inactiveProfileIds ) );
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
                                                                  File projectDirectory,
                                                                  List<InterpolatorProperty> properties,
                                                                  Collection<String> activeProfileIds,
                                                                  Collection<String> inactiveProfileIds )
        throws IOException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        String parentId = domainModel.getParentId();

        if ( parentId == null )
        {
            return domainModels;
        }

        File parentFile = new File( projectDirectory, domainModel.getRelativePathOfParent() ).getCanonicalFile();
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

        //Process Profiles
        ProfileContext profileContext = new ProfileContext(new DefaultModelDataSource(parentDomainModel.getModelProperties(),
                PomTransformer.MODEL_CONTAINER_FACTORIES), activeProfileIds, inactiveProfileIds, properties);
        Collection<ModelContainer> profileContainers = profileContext.getActiveProfiles();

        for(ModelContainer mc : profileContainers)
        {
            List<ModelProperty> transformed = new ArrayList<ModelProperty>();
            transformed.add(new ModelProperty(ProjectUri.xUri, null));
            for(ModelProperty mp : mc.getProperties())
            {
                if(mp.getUri().startsWith(ProjectUri.Profiles.Profile.xUri) && !mp.getUri().equals(ProjectUri.Profiles.Profile.id)
                    && !mp.getUri().startsWith(ProjectUri.Profiles.Profile.Activation.xUri))
                {
                    transformed.add(new ModelProperty(mp.getUri().replace(ProjectUri.Profiles.Profile.xUri, ProjectUri.xUri),
                            mp.getResolvedValue()));
                }
            }
            domainModels.add(new PomClassicDomainModel(transformed));
        }

        if ( !parentDomainModel.matchesParentOf( domainModel ) )
        {
            logger.info( "Parent pom ids do not match: Parent File = " + parentFile.getAbsolutePath() + ", Parent ID = "
                    + parentDomainModel.getId() + ", Child ID = " + domainModel.getId() + ", Expected Parent ID = "
                    + domainModel.getParentId() );
            
            List<DomainModel> parentDomainModels =
                getDomainModelParentsFromRepository( domainModel, artifactResolver, properties, activeProfileIds,
                                                     inactiveProfileIds );
            
            if(parentDomainModels.size() == 0)
            {
                throw new IOException("Unable to find parent pom on local path or repo: "
                        + domainModel.getParentId());
            }
            
            domainModels.addAll( parentDomainModels );
            return domainModels;
        }

        domainModels.add( parentDomainModel );
        if ( domainModel.getParentId() != null )
        {
            if ( isParentLocal(parentDomainModel.getRelativePathOfParent(), parentFile.getParentFile() ) )
            {
                domainModels.addAll( getDomainModelParentsFromLocalPath( parentDomainModel, artifactResolver,
                                                                         parentFile.getParentFile(), properties,
                                                                         activeProfileIds, inactiveProfileIds ) );
            }
            else
            {
                domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, artifactResolver,
                                                                          properties, activeProfileIds,
                                                                          inactiveProfileIds ) );
            }
        }

        return domainModels;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    private DomainModel superDomainModel;

    private DomainModel getSuperDomainModel()
        throws IOException
    {
        if( superDomainModel == null )
        {
            superDomainModel = convertToDomainModel( getSuperModel() );
        }
        return superDomainModel;
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

    private static List<DomainModel> getParentsOfDomainModel( MavenDomainModel domainModel, MetadataReader mdReader )
        throws IOException, MetadataReaderException, PomProcessorException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        if ( domainModel.hasParent() )
        {
            byte[] b = mdReader.readMetadata( domainModel.getParentMetadata() );

            if ( b == null || b.length < 1 )
                throw new PomProcessorException( "cannot read metadata for " + domainModel.getParentMetadata() );

            MavenDomainModel parentDomainModel =
                new MavenDomainModel( b );
            domainModels.add( parentDomainModel );
            domainModels.addAll( getParentsOfDomainModel( parentDomainModel, mdReader ) );
        }
        return domainModels;
    }

    private static List<ModelProperty> transformProfiles( List<ModelProperty> modelProperties )
    {
        List<ModelProperty> properties = new ArrayList<ModelProperty>();
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.Profiles.Profile.xUri )
                && !mp.getUri().equals( ProjectUri.Profiles.Profile.id )
                && !mp.getUri().startsWith( ProjectUri.Profiles.Profile.Activation.xUri ) )
            {
                properties.add( new ModelProperty( mp.getUri().replace( ProjectUri.Profiles.Profile.xUri,
                                                                        ProjectUri.xUri ), mp.getResolvedValue() ) );
            }
        }
        return properties;
    }
}
