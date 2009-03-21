package org.apache.maven.project.processor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.project.builder.PomInterpolatorTag;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelTransformerContext;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class ProcessorContext
{

    /**
     * The URIs that denote file/directory paths and need their basedir alignment or normalization.
     */
    private static final Collection<String> PATH_URIS =
        Collections.unmodifiableSet( new HashSet<String>(
                                                          Arrays.asList(
                                                                         ProjectUri.Build.directory,
                                                                         ProjectUri.Build.outputDirectory,
                                                                         ProjectUri.Build.testOutputDirectory,
                                                                         ProjectUri.Build.sourceDirectory,
                                                                         ProjectUri.Build.testSourceDirectory,
                                                                         ProjectUri.Build.scriptSourceDirectory,
                                                                         ProjectUri.Build.Resources.Resource.directory,
                                                                         ProjectUri.Build.TestResources.TestResource.directory,
                                                                         ProjectUri.Build.Filters.filter,
                                                                         ProjectUri.Reporting.outputDirectory ) ) );   

    public static PomClassicDomainModel mergeProfileIntoModel(Collection<Profile> profiles, Model model, boolean isMostSpecialized) throws IOException
    {
        List<Model> profileModels = new ArrayList<Model>();
        profileModels.add( model );
        for(Profile profile : profiles)
        {
            profileModels.add( attachProfileNodesToModel(profile) );
        }
        
        List<Processor> processors =
            Arrays.asList( (Processor) new BuildProcessor( new ArrayList<Processor>() ),
                           (Processor) new ProfilesModuleProcessor(), new PropertiesProcessor(), new ParentProcessor(),
                           new OrganizationProcessor(), new MailingListProcessor(), new IssueManagementProcessor(),
                           new CiManagementProcessor(), new ReportingProcessor(), new RepositoriesProcessor(), 
                           new DistributionManagementProcessor());
        
        Model target = processModelsForInheritance(profileModels, processors, false);
        
        PomClassicDomainModel m = convertToDomainModel( target, true );
        interpolateModelProperties(m.getModelProperties(), new ArrayList<InterpolatorProperty>(), m); 
        
        return new PomClassicDomainModel(m.getModelProperties(), isMostSpecialized);  
    }
    
    private static Model attachProfileNodesToModel(Profile profile)
    {
        Model model = new Model();
        model.setModules( new ArrayList<String>(profile.getModules()) );
        model.setDependencies(profile.getDependencies());
        model.setDependencyManagement( profile.getDependencyManagement());
        model.setDistributionManagement( profile.getDistributionManagement() );
        model.setProperties( profile.getProperties() );  
        model.setModules( new ArrayList<String>(profile.getModules() ) );
        BuildProcessor proc = new BuildProcessor( new ArrayList<Processor>());
        proc.processWithProfile( profile.getBuild(), model);
        return model;
    }  

    private static List<Model> convertDomainModelsToMavenModels(List<DomainModel> domainModels) throws IOException
    {
        List<Model> models = new ArrayList<Model>();
        for(DomainModel domainModel : domainModels)
        {
            PomClassicDomainModel dm = (PomClassicDomainModel) domainModel;
            if(dm.getModel() != null)
            {
                if(dm.isMostSpecialized())
                {
                    models.add(0, dm.getModel() );
                }
                else
                {
                    models.add( dm.getModel() );  
                }
                
            }
            else {
                InputStream is = ( (PomClassicDomainModel) domainModel ).getInputStream();
                MavenXpp3Reader reader = new MavenXpp3Reader();
                try
                {
                    models.add( reader.read( is ) );
                }
                catch ( XmlPullParserException e )
                {
                    e.printStackTrace();
                    throw new IOException( e.getMessage() );
                }                  
            }
           
        }

        return models;
    }
    
    /**
     * Parent domain models on bottom.
     * 
     * @param domainModels
     * @return
     * @throws IOException
     */
    public static PomClassicDomainModel build( List<DomainModel> domainModels,
                                               List<InterpolatorProperty> interpolationProperties )
        throws IOException
    {  
        PomClassicDomainModel child = null;
        for ( DomainModel domainModel : domainModels )
        {   
            if(domainModel.isMostSpecialized())
            {
                child = (PomClassicDomainModel) domainModel;
            }
        }
        if(child == null)
        {
            throw new IOException("Could not find child model");
        }
        
        List<Processor> processors =
            Arrays.<Processor> asList( new BuildProcessor( new ArrayList<Processor>() ), new ModuleProcessor(),
                                       new PropertiesProcessor(), new ParentProcessor(), new OrganizationProcessor(),
                                       new MailingListProcessor(), new IssueManagementProcessor(),
                                       new CiManagementProcessor(), new ReportingProcessor(),
                                       new RepositoriesProcessor(), new DistributionManagementProcessor(),
                                       new LicensesProcessor(), new ScmProcessor(), new PrerequisitesProcessor() );
        Model target = processModelsForInheritance( convertDomainModelsToMavenModels( domainModels ), processors, true );
        
        PomClassicDomainModel model = convertToDomainModel( target, false );
        interpolateModelProperties( model.getModelProperties(), interpolationProperties, child );
        List<ModelProperty> modelProperties;
        if ( child.getProjectDirectory() != null )
        {
            modelProperties = alignPaths( model.getModelProperties(), child.getProjectDirectory() );
        }
        else
        {
            modelProperties = model.getModelProperties();
        }
        return new PomClassicDomainModel( modelProperties );
    }
    
    private static Model processModelsForInheritance(List<Model> models, List<Processor> processors, boolean reverse)
    {
        ModelProcessor modelProcessor = new ModelProcessor( processors );
       
      //  if(!reverse)
      //  {
            Collections.reverse( models );    
      //  }

        int length = models.size();
        Model target = new Model();
        if(length == 1)
        {
            modelProcessor.process( null, models.get( 0 ), target, true );    

        } else if( length == 2)
        {
            modelProcessor.process( models.get( 0 ), models.get( 1 ), target, true );    
        }
        else {
            for ( int i = 0; i < length - 1; i++ )
            {
                if(i == 0)
                {
                    modelProcessor.process( null, models.get( 0 ), target, false );    
                }
                else if ( i < length - 2 )
                {
                    modelProcessor.process( models.get( i ), models.get( i + 1 ), target, false );
                }
                else
                {
                    modelProcessor.process( models.get( i ), models.get( i + 1 ), target, true );
                }
            }           
        }



        // Dependency Management
        DependencyManagementProcessor depProc = new DependencyManagementProcessor();
        if ( target.getDependencyManagement() != null )
        {
            depProc.process( null, new ArrayList<Dependency>( target.getDependencyManagement().getDependencies() ),
                             target.getDependencies(), true );
        }
        
        // Plugin Management      
        PluginsManagementProcessor procMng = new PluginsManagementProcessor();
        if ( target.getBuild() != null && target.getBuild().getPluginManagement() != null)
        {
            procMng.process( null, new ArrayList<Plugin>( target.getBuild().getPluginManagement().getPlugins() ),
                              target.getBuild().getPlugins(), true );
        }
        return target;
      
    }

    private static PomClassicDomainModel convertToDomainModel( Model model, boolean isMostSpecialized )
        throws IOException
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
        return new PomClassicDomainModel( new ByteArrayInputStream( baos.toByteArray() ), isMostSpecialized );
    }

    private static final Map<String, String> aliases = new HashMap<String, String>();

    private static void addProjectAlias( String element, boolean leaf )
    {
        String suffix = leaf ? "\\}" : "\\.";
        aliases.put( "\\$\\{project\\." + element + suffix, "\\$\\{" + element + suffix );
    }

    static
    {
        aliases.put( "\\$\\{project\\.", "\\$\\{pom\\." );
        addProjectAlias( "modelVersion", true );
        addProjectAlias( "groupId", true );
        addProjectAlias( "artifactId", true );
        addProjectAlias( "version", true );
        addProjectAlias( "packaging", true );
        addProjectAlias( "name", true );
        addProjectAlias( "description", true );
        addProjectAlias( "inceptionYear", true );
        addProjectAlias( "url", true );
        addProjectAlias( "parent", false );
        addProjectAlias( "prerequisites", false );
        addProjectAlias( "organization", false );
        addProjectAlias( "build", false );
        addProjectAlias( "reporting", false );
        addProjectAlias( "scm", false );
        addProjectAlias( "distributionManagement", false );
        addProjectAlias( "issueManagement", false );
        addProjectAlias( "ciManagement", false );
    }

    public static void interpolateModelProperties( List<ModelProperty> modelProperties,
                                                   List<InterpolatorProperty> interpolatorProperties, PomClassicDomainModel dm )
        throws IOException
    {

        if(dm == null)
        {
            throw new IllegalArgumentException("dm: null");
        }
        if ( !containsProjectVersion( interpolatorProperties ) )
        {
            aliases.put( "\\$\\{project.version\\}", "\\$\\{version\\}" );
        }

        List<ModelProperty> firstPassModelProperties = new ArrayList<ModelProperty>();
        List<ModelProperty> secondPassModelProperties = new ArrayList<ModelProperty>();

        ModelProperty buildProperty = new ModelProperty( ProjectUri.Build.xUri, null );
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getValue() != null && !mp.getUri().contains( "#property" ) && !mp.getUri().contains( "#collection" ) )
            {
                if ( ( !buildProperty.isParentOf( mp ) && !mp.getUri().equals( ProjectUri.Reporting.outputDirectory ) || mp.getUri().equals(
                                                                                                                                             ProjectUri.Build.finalName ) ) )
                {
                    firstPassModelProperties.add( mp );
                }
                else
                {
                    secondPassModelProperties.add( mp );
                }
            }
        }

        List<InterpolatorProperty> standardInterpolatorProperties = new ArrayList<InterpolatorProperty>();
        
         if ( dm.isPomInBuild() )
        {
            String basedir = dm.getProjectDirectory().getAbsolutePath();
            standardInterpolatorProperties.add( new InterpolatorProperty( "${project.basedir}", basedir,
                                                                          PomInterpolatorTag.PROJECT_PROPERTIES.name() ) );
            standardInterpolatorProperties.add( new InterpolatorProperty( "${basedir}", basedir,
                                                                          PomInterpolatorTag.PROJECT_PROPERTIES.name() ) );
            standardInterpolatorProperties.add( new InterpolatorProperty( "${pom.basedir}", basedir,
                                                                          PomInterpolatorTag.PROJECT_PROPERTIES.name() ) );

            String baseuri = dm.getProjectDirectory().toURI().toString();
            standardInterpolatorProperties.add( new InterpolatorProperty( "${project.baseUri}", baseuri,
                                                                          PomInterpolatorTag.PROJECT_PROPERTIES.name() ) );
            standardInterpolatorProperties.add( new InterpolatorProperty( "${pom.baseUri}", baseuri,
                                                                          PomInterpolatorTag.PROJECT_PROPERTIES.name() ) );
        }
         
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.properties ) && mp.getValue() != null )
            {
                String uri = mp.getUri();
                standardInterpolatorProperties.add( new InterpolatorProperty(
                                                                              "${"
                                                                                  + uri.substring(
                                                                                                   uri.lastIndexOf( "/" ) + 1,
                                                                                                   uri.length() ) + "}",
                                                                              mp.getValue(),
                                                                              PomInterpolatorTag.PROJECT_PROPERTIES.name() ) );
            }
        }

        // FIRST PASS - Withhold using build directories as interpolator properties
        List<InterpolatorProperty> ips1 = new ArrayList<InterpolatorProperty>( interpolatorProperties );
        ips1.addAll( standardInterpolatorProperties );
        ips1.addAll( ModelTransformerContext.createInterpolatorProperties(
                                                                           firstPassModelProperties,
                                                                           ProjectUri.baseUri,
                                                                           aliases,
                                                                           PomInterpolatorTag.PROJECT_PROPERTIES.name(),
                                                                           false, false ) );
        Collections.sort( ips1, new Comparator<InterpolatorProperty>()
        {
            public int compare( InterpolatorProperty o, InterpolatorProperty o1 )
            {
                if ( o.getTag() == null || o1.getTag() == null )
                {
                    return 0;
                }
                return PomInterpolatorTag.valueOf( o.getTag() ).compareTo( PomInterpolatorTag.valueOf( o1.getTag() ) );
            }
        } );

        ModelTransformerContext.interpolateModelProperties( modelProperties, ips1 );

        // SECOND PASS - Set absolute paths on build directories

         if ( dm.isPomInBuild() )
        {
            String basedir = dm.getProjectDirectory().getAbsolutePath();
            Map<ModelProperty, ModelProperty> buildDirectories = new HashMap<ModelProperty, ModelProperty>();
            for ( ModelProperty mp : secondPassModelProperties )
            {
                if ( mp.getUri().startsWith( ProjectUri.Build.xUri )
                    || mp.getUri().equals( ProjectUri.Reporting.outputDirectory ) )
                {
                    File file = new File( mp.getResolvedValue() );
                    if ( !file.isAbsolute() && !mp.getResolvedValue().startsWith( "${project.build." )
                        && !mp.getResolvedValue().equals( "${project.basedir}" ) )
                    {
                        buildDirectories.put( mp,
                                              new ModelProperty( mp.getUri(),
                                                                 new File( basedir, file.getPath() ).getAbsolutePath() ) );
                    }
                }
            }
            for ( Map.Entry<ModelProperty, ModelProperty> e : buildDirectories.entrySet() )
            {
                secondPassModelProperties.remove( e.getKey() );
                secondPassModelProperties.add( e.getValue() );
            }
        }

        // THIRD PASS - Use build directories as interpolator properties
        List<InterpolatorProperty> ips2 = new ArrayList<InterpolatorProperty>( interpolatorProperties );
        ips2.addAll( standardInterpolatorProperties );
        ips2.addAll( ModelTransformerContext.createInterpolatorProperties(
                                                                           secondPassModelProperties,
                                                                           ProjectUri.baseUri,
                                                                           aliases,
                                                                           PomInterpolatorTag.PROJECT_PROPERTIES.name(),
                                                                           false, false ) );
        ips2.addAll( interpolatorProperties );
        Collections.sort( ips2, new Comparator<InterpolatorProperty>()
        {
            public int compare( InterpolatorProperty o, InterpolatorProperty o1 )
            {
                if ( o.getTag() == null || o1.getTag() == null )
                {
                    return 0;
                }

                return PomInterpolatorTag.valueOf( o.getTag() ).compareTo( PomInterpolatorTag.valueOf( o1.getTag() ) );
            }
        } );

        ModelTransformerContext.interpolateModelProperties( modelProperties, ips2 );
    }

    private static boolean containsProjectVersion( List<InterpolatorProperty> interpolatorProperties )
    {
        InterpolatorProperty versionInterpolatorProperty =
            new ModelProperty( ProjectUri.version, "" ).asInterpolatorProperty( ProjectUri.baseUri );
        for ( InterpolatorProperty ip : interpolatorProperties )
        {
            if ( ip.equals( versionInterpolatorProperty ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Post-processes the paths of build directories by aligning relative paths to the project directory and normalizing
     * file separators to the platform-specific separator.
     * 
     * @param modelProperties The model properties to process, must not be {@code null}.
     * @param basedir The project directory, must not be {@code null}.
     * @return The updated model properties, never {@code null}.
     */
    private static List<ModelProperty> alignPaths( Collection<ModelProperty> modelProperties, File basedir )
    {
        List<ModelProperty> mps = new ArrayList<ModelProperty>( modelProperties.size() );

        for ( ModelProperty mp : modelProperties )
        {
            String value = mp.getResolvedValue();
            if ( value != null && PATH_URIS.contains( mp.getUri() ) )
            {
                File file = new File( value );
                if ( file.isAbsolute() )
                {
                    // path was already absolute, just normalize file separator and we're done
                    value = file.getPath();
                }
                else if ( file.getPath().startsWith( File.separator ) )
                {
                    // drive-relative Windows path, don't align with project directory but with drive root
                    value = file.getAbsolutePath();
                }
                else
                {
                    // an ordinary relative path, align with project directory
                    value = new File( new File( basedir, value ).toURI().normalize() ).getAbsolutePath();
                }
                mp = new ModelProperty( mp.getUri(), value );
            }
            mps.add( mp );
        }

        return mps;
    }

}
