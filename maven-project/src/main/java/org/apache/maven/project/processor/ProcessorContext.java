package org.apache.maven.project.processor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
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
    public static PomClassicDomainModel build( List<DomainModel> domainModels )
        throws IOException
    {
        List<Processor> processors =
            Arrays.asList( (Processor) new BuildProcessor( new ArrayList<Processor>() ),
                           (Processor) new ModuleProcessor(), new PropertiesProcessor(), new ParentProcessor(),
                           new OrganizationProcessor(), new MailingListProcessor(), new IssueManagementProcessor(),
                           new CiManagementProcessor(), new ReportingProcessor(), new RepositoriesProcessor());

        ModelProcessor modelProcessor = new ModelProcessor( processors );

        List<Model> models = new ArrayList<Model>();
        
        PomClassicDomainModel child = null;
        for ( DomainModel domainModel : domainModels )
        {

            //TODO: Getting some null profiles - work around to skip for now
            boolean artifactId = false;
            for(ModelProperty mp : domainModel.getModelProperties())
            {
                if(mp.getUri().equals(ProjectUri.artifactId))
                {
                    artifactId = true;
                    break;
                }
            }
            
            if(!artifactId)
            {
                continue;
            }
            //TODO:END
            
            if(domainModel.isMostSpecialized())
            {
                child = (PomClassicDomainModel) domainModel;
            }
            
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

        Collections.reverse( models );

        int length = models.size();
        Model target = new Model();
        if(length == 1)
        {
            modelProcessor.process( null, models.get( 0 ), target, true );    
        }
        else if( length == 2)
        {
            modelProcessor.process( models.get( 0 ), models.get( 1 ), target, true );    
        }
        
        for ( int i = 1; i < length - 1; i++ )
        {
            if ( i < length - 2 )
            {
                modelProcessor.process( models.get( i ), models.get( i + 1 ), target, false );
            }
            else
            {
                modelProcessor.process( models.get( i ), models.get( i + 1 ), target, true );
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

        PomClassicDomainModel model = convertToDomainModel( target, false );
        interpolateModelProperties(model.getModelProperties(), new ArrayList(), child);
        return new PomClassicDomainModel(model.getModelProperties());
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
        // PomClassicDomainModel dm = (PomClassicDomainModel) domainModel;

        if ( !containsProjectVersion( interpolatorProperties ) )
        {
            aliases.put( "\\$\\{project.version\\}", "\\$\\{version\\}" );
        }
       // System.out.println(aliases);    
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

}
