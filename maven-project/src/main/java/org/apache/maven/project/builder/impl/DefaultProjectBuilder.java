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
import java.io.StringReader;
import java.util.*;

import org.apache.maven.MavenTools;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.mercury.PomProcessor;
import org.apache.maven.mercury.PomProcessorException;
import org.apache.maven.mercury.MavenDomainModel;
import org.apache.maven.mercury.MavenDomainModelFactory;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.mercury.builder.api.DependencyProcessorException;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.builder.*;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.profile.ProfileContext;
import org.apache.maven.project.builder.profile.ProfileUri;
import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.apache.maven.shared.model.ModelMarshaller;
import org.apache.commons.jxpath.JXPathContext;

/**
 * Default implementation of the project builder.
 */
@Component(role = ProjectBuilder.class)
public class DefaultProjectBuilder
    implements ProjectBuilder, Mixer, PomProcessor, LogEnabled
{
    @Requirement
    private ArtifactFactory artifactFactory;
    
    @Requirement
    private MavenTools mavenTools;

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
        return buildModel( pom, null, interpolatorProperties, null, resolver );
    }    
    
    private PomClassicDomainModel buildModel( File pom,
                                             List<Model> mixins,
                                             Collection<InterpolatorProperty> interpolatorProperties,
                                             Collection<String> activeProfileIds,
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

        if(activeProfileIds == null)
        {
            activeProfileIds = new ArrayList<String>();
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

        ProfileContext profileContext = new ProfileContext(new DefaultModelDataSource(domainModel.getModelProperties(),
                PomTransformer.MODEL_CONTAINER_FACTORIES), activeProfileIds, properties);

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

        File parentFile = null;
        int lineageCount = 0;
        if ( domainModel.getModel().getParent() != null )
        {
            List<DomainModel> mavenParents;
            if ( isParentLocal( domainModel.getModel().getParent(), pom.getParentFile() ) )
            {
                mavenParents = getDomainModelParentsFromLocalPath( domainModel, resolver, pom.getParentFile(), properties, activeProfileIds );
            }
            else
            {
                mavenParents = getDomainModelParentsFromRepository( domainModel, resolver, properties, activeProfileIds );
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
    
    public MavenProject buildFromLocalPath( File pom, 
                                            List<Model> mixins,
                                            Collection<InterpolatorProperty> interpolatorProperties,
                                            PomArtifactResolver resolver, 
                                            ProjectBuilderConfiguration projectBuilderConfiguration,
                                            MavenProjectBuilder mavenProjectBuilder)
        throws IOException
    {

       List<String> profileIds = (projectBuilderConfiguration != null &&
                projectBuilderConfiguration.getGlobalProfileManager() != null &&
                projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext() != null) ?
               projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext().getExplicitlyActiveProfileIds() : new ArrayList<String>();


        PomClassicDomainModel domainModel = buildModel( pom, 
                                                        mixins, 
                                                        interpolatorProperties,
                                                        profileIds,
                                                        resolver ); 
        
        try
        {
            MavenProject mavenProject = new MavenProject( domainModel.getModel(), 
                                                          artifactFactory, 
                                                          mavenTools, 
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
                                                                   PomArtifactResolver artifactResolver,
                                                                   List<InterpolatorProperty> properties,
                                                                   Collection<String> activeProfileIds)
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

         ProfileContext profileContext = new ProfileContext(new DefaultModelDataSource(parentDomainModel.getModelProperties(),
                PomTransformer.MODEL_CONTAINER_FACTORIES), activeProfileIds, properties);
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

        domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, artifactResolver, properties, activeProfileIds ) );
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
                                                                  Collection<String> activeProfileIds)
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
         ProfileContext profileContext = new ProfileContext(new DefaultModelDataSource(parentDomainModel.getModelProperties(),
                PomTransformer.MODEL_CONTAINER_FACTORIES), activeProfileIds, properties);
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

        if ( !parentDomainModel.matchesParent( domainModel.getModel().getParent() ) )
        {
            logger.debug( "Parent pom ids do not match: Parent File = " + parentFile.getAbsolutePath() + ", Parent ID = "
                    + parentDomainModel.getId() + ", Child ID = " + domainModel.getId() + ", Expected Parent ID = "
                    + domainModel.getModel().getParent().getId() );
            
            List<DomainModel> parentDomainModels = getDomainModelParentsFromRepository( domainModel, artifactResolver, properties, activeProfileIds );
            
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
                                                                         parentFile.getParentFile(), properties, activeProfileIds ) );
            }
            else
            {
                domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, artifactResolver, properties, activeProfileIds ) );
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

    public Model mixPlugin(Plugin plugin, Model model) throws IOException
    {
        //TODO - interpolation
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        domainModels.add( new PluginMixin(plugin) );
        domainModels.add( new PomClassicDomainModel(model) );

        PomClassicTransformer transformer = new PomClassicTransformer( new PomClassicDomainModelFactory() );

        ModelTransformerContext ctx = new ModelTransformerContext(PomTransformer.MODEL_CONTAINER_INFOS );

        PomClassicDomainModel transformedDomainModel = ( (PomClassicDomainModel) ctx.transform( domainModels,
                                                                                                transformer,
                                                                                                transformer,
                                                                                                Collections.EMPTY_LIST,
                                                                                                null,
                                                                                                listeners ) );
        return transformedDomainModel.getModel();
        
    }

    public PlexusConfiguration mixPluginAndReturnConfig(Plugin plugin, Model model) throws IOException
    {
        List<ModelProperty> mps = mixPluginAndReturnConfigAsProperties(plugin, model);
        return !mps.isEmpty() ?
            new XmlPlexusConfiguration(ModelMarshaller.unmarshalModelPropertiesToXml(mps, ProjectUri.Build.Plugins.Plugin.xUri)) : null;
    }

   public Object mixPluginAndReturnConfigAsDom(Plugin plugin, Model model) throws IOException, XmlPullParserException
   {
       List<ModelProperty> mps = mixPluginAndReturnConfigAsProperties(plugin, model);
       return  !mps.isEmpty() ? Xpp3DomBuilder.build(
               new StringReader(ModelMarshaller.unmarshalModelPropertiesToXml(mps, ProjectUri.Build.Plugins.Plugin.xUri) ) ) : null;
   }

   public Object mixPluginAndReturnConfigAsDom(Plugin plugin, Model model, String xpathExpression) throws IOException,
           XmlPullParserException
   {
       Object dom = mixPluginAndReturnConfigAsDom(plugin, model);
       if(dom == null)
       {
           return null;
       }
       return JXPathContext.newContext( dom ).getValue(xpathExpression);
   }

   private List<ModelProperty> mixPluginAndReturnConfigAsProperties(Plugin plugin, Model model) throws IOException
   {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        domainModels.add( new PluginMixin(plugin) );
        domainModels.add( new PomClassicDomainModel(model) );

        PomClassicTransformer transformer = new PomClassicTransformer( new PomClassicDomainModelFactory() );

        ModelTransformerContext ctx = new ModelTransformerContext(PomTransformer.MODEL_CONTAINER_INFOS );

        PomClassicDomainModel transformedDomainModel = ( (PomClassicDomainModel) ctx.transform( domainModels,
                                                                                                transformer,
                                                                                                transformer,
                                                                                                Collections.EMPTY_LIST,
                                                                                                null,
                                                                                                listeners ) );
        ModelDataSource source =
                new DefaultModelDataSource(transformedDomainModel.getModelProperties(), PomTransformer.MODEL_CONTAINER_FACTORIES);
        for(ModelContainer pluginContainer : source.queryFor(ProjectUri.Build.Plugins.Plugin.xUri))
        {
            if(matchesIdOfPlugin(pluginContainer, plugin))
            {
                List<ModelProperty> config = new ArrayList<ModelProperty>();
                for(ModelProperty mp : pluginContainer.getProperties())
                {
                    if(mp.getUri().startsWith(ProjectUri.Build.Plugins.Plugin.configuration))
                    {
                        config.add(mp);
                    }
                }
                return config;

            }
        }
        return new ArrayList<ModelProperty>();
   }

    private static boolean matchesIdOfPlugin(ModelContainer mc, Plugin plugin)
    {   
        List<ModelProperty> props = mc.getProperties();
        return getValueByUri(ProjectUri.Build.Plugins.Plugin.groupId, props).equals(plugin.getGroupId())
                && getValueByUri(ProjectUri.Build.Plugins.Plugin.artifactId, props).equals(plugin.getArtifactId())
                && getValueByUri(ProjectUri.Build.Plugins.Plugin.version, props).equals(plugin.getVersion());
    }

    private static String getValueByUri(String uri, List<ModelProperty> modelProperties)
    {
        for(ModelProperty mp : modelProperties)
        {
            if(mp.getUri().equals(uri))
            {
                return mp.getResolvedValue();
            }
        }
        return "";
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
