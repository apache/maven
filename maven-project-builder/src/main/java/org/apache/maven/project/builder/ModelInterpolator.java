package org.apache.maven.project.builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;

public class ModelInterpolator {

	private HashMap<String, String> values;
	
	public ModelInterpolator()
	{
		values = new HashMap<String, String>();
	}
	
	public static void interpolate(PomClassicDomainModel domainModel, List<InterpolatorProperty> interpolatorProperties ) throws IOException
	{
		List<ModelProperty> m = new ArrayList<ModelProperty>();
		
		Model model = domainModel.getModel();

		m.add(new ModelProperty(ProjectUri.groupId, model.getGroupId()));
		m.add(new ModelProperty(ProjectUri.artifactId, model.getArtifactId()));
		
		interpolateModelProperties(m, interpolatorProperties, domainModel);
		//Set model properties on model
		
		
		
		//InterpolatorProperty ip = new InterpolatorProperty();
		//ModelProperty mp;
		/*
		values.put("groupId", model.getGroupId());
		values.put("artifactId", model.getArtifactId());
		values.put("version", model.getVersion());
		values.put("packaging", model.getPackaging());
		values.put("name", model.getName());
		values.put("description", model.getDescription());
		
		//Collect uninterpolated values
		HashMap<String, String> hm = new HashMap<String, String>();
		for(Map.Entry<String, String> entry : hm.entrySet())
		{
			
		}
		*/
	}
	
	/*
	 *         addProjectAlias( "modelVersion", true );
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
	 */

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

   private static void interpolateModelProperties( List<ModelProperty> mps,
                                                  List<InterpolatorProperty> interpolatorProperties, 
                                                  PomClassicDomainModel dm )
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
       
       if("jar".equals( dm.getModel().getPackaging() ) )
       {
           mps.add( new ModelProperty(ProjectUri.packaging, "jar") );
       }  
       
       List<ModelProperty> firstPassModelProperties = new ArrayList<ModelProperty>();
       List<ModelProperty> secondPassModelProperties = new ArrayList<ModelProperty>();

       ModelProperty buildProperty = new ModelProperty( ProjectUri.Build.xUri, null );
       for ( ModelProperty mp : mps )
       {
           if ( mp.getValue() != null )
           {
        	   //!buildProperty.isParentOf( mp ) && 
               if ( ( !mp.getUri().equals( ProjectUri.Reporting.outputDirectory ) || mp.getUri().equals(
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
        
       for ( ModelProperty mp : mps )
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
       ips1.addAll(createInterpolatorProperties(firstPassModelProperties,
				ProjectUri.baseUri, aliases,
				PomInterpolatorTag.PROJECT_PROPERTIES.name()));
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

       interpolateModelProperties( mps, ips1 );

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
       ips2.addAll(createInterpolatorProperties(secondPassModelProperties,
				ProjectUri.baseUri, aliases,
				PomInterpolatorTag.PROJECT_PROPERTIES.name()));
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
       
       interpolateModelProperties( mps, ips2 );
   }
   
   private static void interpolateModelProperties(
			List<ModelProperty> modelProperties,
			List<InterpolatorProperty> interpolatorProperties) {
		if (modelProperties == null) {
			throw new IllegalArgumentException("modelProperties: null");
		}

		if (interpolatorProperties == null) {
			throw new IllegalArgumentException("interpolatorProperties: null");
		}

		List<ModelProperty> unresolvedProperties = new ArrayList<ModelProperty>();
		for (ModelProperty mp : modelProperties) {
			if (!mp.isResolved()) {
				unresolvedProperties.add(mp);
			}
		}

		LinkedHashSet<InterpolatorProperty> ips = new LinkedHashSet<InterpolatorProperty>();
		ips.addAll(interpolatorProperties);
		boolean continueInterpolation = true;
		while (continueInterpolation) {
			continueInterpolation = false;
			for (InterpolatorProperty ip : ips) {
				for (ModelProperty mp : unresolvedProperties) {
					if (mp.resolveWith(ip) && !continueInterpolation) {
						continueInterpolation = true;
					}
				}
			}
		}
	}
 
   public static List<InterpolatorProperty> createInterpolatorProperties(List<ModelProperty> modelProperties,
           String baseUriForModel,
           Map<String, String> aliases,
           String interpolatorTag)
{
		if (modelProperties == null) {
			throw new IllegalArgumentException("modelProperties: null");
		}

		if (baseUriForModel == null) {
			throw new IllegalArgumentException("baseUriForModel: null");
		}

		List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();

		for (ModelProperty mp : modelProperties) {
			InterpolatorProperty ip = mp
					.asInterpolatorProperty(baseUriForModel);
			if (ip != null) {
				ip.setTag(interpolatorTag);
				interpolatorProperties.add(ip);
				for (Map.Entry<String, String> a : aliases.entrySet()) {
					interpolatorProperties.add(new InterpolatorProperty(ip
							.getKey().replaceAll(a.getKey(), a.getValue()), ip
							.getValue().replaceAll(a.getKey(), a.getValue()),
							interpolatorTag));
				}
			}
		}

		List<InterpolatorProperty> ips = new ArrayList<InterpolatorProperty>();
		for (InterpolatorProperty ip : interpolatorProperties) {
			if (!ips.contains(ip)) {
				ips.add(ip);
			}
		}
		return ips;
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
