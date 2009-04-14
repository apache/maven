package org.apache.maven.project.builder;

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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.project.builder.ProjectUri;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class ProcessorContext
{

    public static PomClassicDomainModel mergeProfilesIntoModel(Collection<Profile> profiles, PomClassicDomainModel domainModel) throws IOException
    {
        List<Model> profileModels = new ArrayList<Model>();
        List<Model> externalProfileModels = new ArrayList<Model>();
        
        for(Profile profile : profiles)
        {
        	if("pom".equals(profile.getSource()))
        	{
        		profileModels.add( attachProfileNodesToModel(profile) );	
        	}
        	else
        	{
        		externalProfileModels.add(attachProfileNodesToModel(profile));
        	}
        }
        profileModels.addAll(externalProfileModels);//external takes precedence
        
        Model model = domainModel.getModel();
        profileModels.add( 0, model );
        List<Processor> processors =
            Arrays.<Processor> asList( new BuildProcessor( new ArrayList<Processor>() ), new ProfilesModuleProcessor(),
                                       new ProfilePropertiesProcessor(), new ParentProcessor(),
                                       new OrganizationProcessor(), new MailingListProcessor(),
                                       new IssueManagementProcessor(), new CiManagementProcessor(),
                                       new ReportingProcessor(), new RepositoriesProcessor(),
                                       new DistributionManagementProcessor(), new LicensesProcessor(),
                                       new ScmProcessor(), new PrerequisitesProcessor(), new ContributorsProcessor(),
                                       new DevelopersProcessor(), new ProfilesProcessor() );
        
        //Remove the plugin management and dependency management so they aren't applied again with the profile processing
        PluginManagement mng = null;
        if( model.getBuild() != null)
        {
            mng = model.getBuild().getPluginManagement();
            model.getBuild().setPluginManagement( null );           
        }
     
        DependencyManagement depMng = model.getDependencyManagement();
        
        Model target = processModelsForInheritance(profileModels, processors);

        PluginsManagementProcessor pmp = new PluginsManagementProcessor();
        if( mng != null )
        {
        	if(target.getBuild().getPluginManagement() != null)
        	{
        		pmp.process(null, mng.getPlugins(), target.getBuild().getPluginManagement().getPlugins(), false);	
        	}
        	else
        	{
        		target.getBuild().setPluginManagement( mng );	
        	}  		
        }
        
        //TODO: Merge Dependency Management
        target.setDependencyManagement( depMng );
        
        PomClassicDomainModel targetModel = new PomClassicDomainModel( target, domainModel.isMostSpecialized());
        targetModel.setParentFile(domainModel.getParentFile());
        targetModel.setProjectDirectory(domainModel.getProjectDirectory());
        return targetModel;
    }
    
    private static Model attachProfileNodesToModel(Profile profile)
    {
        Profile p = copyOfProfile(profile);
        
        Model model = new Model();
        model.setModules( p.getModules() );
        model.setDependencies(p.getDependencies());
        model.setDependencyManagement( p.getDependencyManagement());
        model.setDistributionManagement( p.getDistributionManagement() );
        model.setProperties( p.getProperties() );  
        model.setModules( new ArrayList<String>(p.getModules() ) );
        model.setRepositories(p.getRepositories());
        model.setPluginRepositories(p.getPluginRepositories());
        model.setReporting(p.getReporting());
        BuildProcessor proc = new BuildProcessor( new ArrayList<Processor>());
        proc.processWithProfile( p.getBuild(), model);
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
            else 
            {
            	throw new IOException( "model: null" );              
            }
           
        }

        return models;
    }

    public static PomClassicDomainModel build( List<DomainModel> domainModels,
            List<InterpolatorProperty> interpolationProperties, List<ModelEventListener> listeners)
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
                                       new LicensesProcessor(), new ScmProcessor(), new PrerequisitesProcessor(),
                                       new ContributorsProcessor(), new DevelopersProcessor(), new ProfilesProcessor() );
        Model target = processModelsForInheritance( convertDomainModelsToMavenModels( domainModels ), processors );
        
        PomClassicDomainModel domainModel = new PomClassicDomainModel( target, child.isMostSpecialized() );
        domainModel.setProjectDirectory(child.getProjectDirectory());
        domainModel.setParentFile(child.getParentFile());
        return domainModel;
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
    	return build(domainModels, interpolationProperties, null);
    }
    
    private static Model processModelsForInheritance(List<Model> models, List<Processor> processors)
    {
        ModelProcessor modelProcessor = new ModelProcessor( processors );
        Collections.reverse( models );    

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
    private static void alignPaths( Model model, File basedir )
    {
    	Build build = model.getBuild();
    	if(build != null)
    	{
    		build.setDirectory(getAlignedPathFor(build.getDirectory(), basedir));
    		build.setOutputDirectory(getAlignedPathFor(build.getOutputDirectory(), basedir));	
    		build.setTestOutputDirectory(getAlignedPathFor(build.getTestOutputDirectory(), basedir));
    		build.setSourceDirectory(getAlignedPathFor(build.getSourceDirectory(), basedir));
    		build.setScriptSourceDirectory(getAlignedPathFor(build.getScriptSourceDirectory(), basedir));
    		
    		for( Resource r : build.getResources() )
    		{
    			r.setDirectory(getAlignedPathFor(r.getDirectory(), basedir));
    		}  	
    		
    		for( Resource r : build.getTestResources() )
    		{
    			r.setDirectory(getAlignedPathFor(r.getDirectory(), basedir));
    		}  	
    		
    		List<String> filters = new ArrayList<String>();
    		for( String f : build.getFilters() )
    		{
    			filters.add(getAlignedPathFor(f, basedir));
    		}  
    		build.setFilters(filters);
    	}
    	
    	Reporting reporting = model.getReporting();
    	if(reporting != null)
    	{
    		reporting.setOutputDirectory(getAlignedPathFor(reporting.getOutputDirectory(), basedir));	
    	}
          
    }
    
    private static String getAlignedPathFor(String path, File basedir)
    {
        if ( path != null )
        {
            File file = new File( path );
            if ( file.isAbsolute() )
            {
                // path was already absolute, just normalize file separator and we're done
                path = file.getPath();
            }
            else if ( file.getPath().startsWith( File.separator ) )
            {
                // drive-relative Windows path, don't align with project directory but with drive root
                path = file.getAbsolutePath();
            }
            else
            {
                // an ordinary relative path, align with project directory
                path = new File( new File( basedir, path ).toURI().normalize() ).getAbsolutePath();
            }
        }   
        return path;
    }

    public static Profile copyOfProfile(Profile profile)
    {  
        Profile p = new Profile();
        p.setModules( new ArrayList<String>(profile.getModules()) );
        p.setDependencies(new ArrayList<Dependency>(profile.getDependencies()));
        p.setDependencyManagement( profile.getDependencyManagement());
        p.setDistributionManagement( profile.getDistributionManagement() );
        p.setProperties( profile.getProperties() );  
        p.setBuild( copyBuild(profile.getBuild()) );
        p.setId( profile.getId() );
        p.setActivation( profile.getActivation() );
        p.setRepositories(profile.getRepositories());
        p.setPluginRepositories(profile.getPluginRepositories());
        p.setReporting(profile.getReporting());        
        return p;
    }
    
    private static BuildBase copyBuild(BuildBase base)
    {
        if(base == null)
        {
            return null;
        }
        
        BuildBase b = new BuildBase();
        b.setDefaultGoal( base.getDefaultGoal() );
        b.setDirectory( base.getDirectory() );
        b.setFilters( new ArrayList<String>(base.getFilters()) );
        b.setFinalName( base.getFinalName() );
        b.setPluginManagement( copyPluginManagement(base.getPluginManagement()) );
        b.setPlugins( copyPlugins(base.getPlugins()) );
        b.setResources( new ArrayList<Resource>(base.getResources()) );
        b.setTestResources( new ArrayList<Resource>(base.getTestResources()) );    
        return b;
    }
    
    private static PluginManagement copyPluginManagement(PluginManagement mng)
    {
    	if(mng == null)
    	{
    		return null;
    	}
    	
    	PluginManagement pm = new PluginManagement();
    	pm.setPlugins(copyPlugins(mng.getPlugins()));
    	return pm;
    }
    
    private static List<Plugin> copyPlugins(List<Plugin> plugins)
    {
        List<Plugin> ps = new ArrayList<Plugin>();
        for(Plugin p : plugins)
        {
            ps.add( copyPlugin(p) );
        }
        return ps;
    }
    
    private static Plugin copyPlugin(Plugin plugin)
    {
        Plugin p = new Plugin();
        p.setArtifactId( plugin.getArtifactId() );
        if(plugin.getConfiguration() != null) 
        {
            p.setConfiguration( new Xpp3Dom((Xpp3Dom) plugin.getConfiguration()) );           
        }

        p.setDependencies( new ArrayList<Dependency>(plugin.getDependencies()) );
        p.setExecutions( new ArrayList<PluginExecution>(plugin.getExecutions()) );
        p.setGoals( plugin.getGoals() );
        p.setGroupId( plugin.getGroupId() );
        p.setInherited( plugin.getInherited() );
        p.setVersion( plugin.getVersion() );
        return p;
        
    }
    
    public static PomClassicDomainModel interpolateDomainModel( PomClassicDomainModel dm, List<InterpolatorProperty> interpolatorProperties )
    	throws IOException {

		if (dm == null) {
			throw new IllegalArgumentException("dm: null");
		}
		if (!containsProjectVersion(interpolatorProperties)) {
			aliases.put("\\$\\{project.version\\}", "\\$\\{version\\}");
		}
		//TODO: Insert customized logic for parsing
		List<ModelProperty> modelProperties = getModelProperties(dm.getInputStream());

		if ("jar".equals(dm.getModel().getPackaging())) {
			modelProperties.add(new ModelProperty(ProjectUri.packaging, "jar"));
		}

		List<ModelProperty> firstPassModelProperties = new ArrayList<ModelProperty>();
		List<ModelProperty> secondPassModelProperties = new ArrayList<ModelProperty>();

		ModelProperty buildProperty = new ModelProperty(ProjectUri.Build.xUri,
				null);

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
        
			/*
			if ( !buildProperty.isParentOf(mp) && mp.getValue() != null && !mp.getUri().contains("#property")
					&& !mp.getUri().contains("#collection")) {
				if ((!mp.getUri().equals(
								ProjectUri.Reporting.outputDirectory) || mp
						.getUri().equals(ProjectUri.Build.finalName))) {
					firstPassModelProperties.add(mp);
				} else {
					secondPassModelProperties.add(mp);
				}
			}
			*/

		List<InterpolatorProperty> standardInterpolatorProperties = new ArrayList<InterpolatorProperty>();

		if (dm.isPomInBuild()) {
			String basedir = dm.getProjectDirectory().getAbsolutePath();
			standardInterpolatorProperties.add(new InterpolatorProperty(
					"${project.basedir}", basedir,
					PomInterpolatorTag.PROJECT_PROPERTIES.name()));
			standardInterpolatorProperties.add(new InterpolatorProperty(
					"${basedir}", basedir,
					PomInterpolatorTag.PROJECT_PROPERTIES.name()));
			standardInterpolatorProperties.add(new InterpolatorProperty(
					"${pom.basedir}", basedir,
					PomInterpolatorTag.PROJECT_PROPERTIES.name()));

			String baseuri = dm.getProjectDirectory().toURI().toString();
			standardInterpolatorProperties.add(new InterpolatorProperty(
					"${project.baseUri}", baseuri,
					PomInterpolatorTag.PROJECT_PROPERTIES.name()));
			standardInterpolatorProperties.add(new InterpolatorProperty(
					"${pom.baseUri}", baseuri,
					PomInterpolatorTag.PROJECT_PROPERTIES.name()));
		}

		for (ModelProperty mp : modelProperties) {
			if (mp.getUri().startsWith(ProjectUri.properties)
					&& mp.getValue() != null) {
				String uri = mp.getUri();
				standardInterpolatorProperties.add(new InterpolatorProperty(
						"${"
								+ uri.substring(uri.lastIndexOf("/") + 1, uri
										.length()) + "}", mp.getValue(),
						PomInterpolatorTag.PROJECT_PROPERTIES.name()));
			}
		}

		// FIRST PASS - Withhold using build directories as interpolator
		// properties
		List<InterpolatorProperty> ips1 = new ArrayList<InterpolatorProperty>(
				interpolatorProperties);
		ips1.addAll(standardInterpolatorProperties);
		ips1.addAll(createInterpolatorProperties(
				firstPassModelProperties, ProjectUri.baseUri, aliases,
				PomInterpolatorTag.PROJECT_PROPERTIES.name()));
		Collections.sort(ips1, new Comparator<InterpolatorProperty>() {
			public int compare(InterpolatorProperty o, InterpolatorProperty o1) {
				if (o.getTag() == null || o1.getTag() == null) {
					return 0;
				}
				return PomInterpolatorTag.valueOf(o.getTag()).compareTo(
						PomInterpolatorTag.valueOf(o1.getTag()));
			}
		});

		interpolateModelProperties(modelProperties, ips1);

		// SECOND PASS - Set absolute paths on build directories
		if (dm.isPomInBuild()) {
			String basedir = dm.getProjectDirectory().getAbsolutePath();
			Map<ModelProperty, ModelProperty> buildDirectories = new HashMap<ModelProperty, ModelProperty>();
			for (ModelProperty mp : secondPassModelProperties) {
				if (mp.getUri().startsWith(ProjectUri.Build.xUri)
						|| mp.getUri().equals(
								ProjectUri.Reporting.outputDirectory)) {
					File file = new File(mp.getResolvedValue());
					if (!file.isAbsolute()
							&& !mp.getResolvedValue().startsWith(
									"${project.build.")
							&& !mp.getResolvedValue().equals(
									"${project.basedir}")) {
						buildDirectories.put(mp, new ModelProperty(mp.getUri(),
								new File(basedir, file.getPath())
										.getAbsolutePath()));
					}
				}
			}
			for (Map.Entry<ModelProperty, ModelProperty> e : buildDirectories
					.entrySet()) {
				secondPassModelProperties.remove(e.getKey());
				secondPassModelProperties.add(e.getValue());
			}
		}

		// THIRD PASS - Use build directories as interpolator properties
		List<InterpolatorProperty> ips2 = new ArrayList<InterpolatorProperty>(
				interpolatorProperties);
		ips2.addAll(standardInterpolatorProperties);
		ips2.addAll(createInterpolatorProperties(
				secondPassModelProperties, ProjectUri.baseUri, aliases,
				PomInterpolatorTag.PROJECT_PROPERTIES.name()));
		ips2.addAll(interpolatorProperties);
		Collections.sort(ips2, new Comparator<InterpolatorProperty>() {
			public int compare(InterpolatorProperty o, InterpolatorProperty o1) {
				if (o.getTag() == null || o1.getTag() == null) {
					return 0;
				}

				return PomInterpolatorTag.valueOf(o.getTag()).compareTo(
						PomInterpolatorTag.valueOf(o1.getTag()));
			}
		});

		interpolateModelProperties(modelProperties, ips2);
		
        try
        {
            String xml = unmarshalModelPropertiesToXml( modelProperties, ProjectUri.baseUri );
            PomClassicDomainModel domainModel = new PomClassicDomainModel( new ByteArrayInputStream ( xml.getBytes( "UTF-8" )));
        	if ( dm.getProjectDirectory() != null )
        	{
        		alignPaths(domainModel.getModel(), dm.getProjectDirectory());
        	}
        	return domainModel;
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Unmarshalling of model properties failed", e );
        }
        
    	
    	
        /*
		for(ModelProperty mp : modelProperties)
		{
			if((mp.getValue() != null) && !mp.getValue().equals(mp.getResolvedValue()))
			{
				if(mp.getUri().equals(ProjectUri.version))
				{
					
				}
			}
		}
		*/
	}

    public static void interpolateModelProperties(List<ModelProperty> modelProperties, 
            List<InterpolatorProperty> interpolatorProperties )
    {
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
						break;
					}
				}
			}
		}
	}
    
    private static List<InterpolatorProperty> createInterpolatorProperties(List<ModelProperty> modelProperties,
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
 
    /**
     * Returns XML string unmarshalled from the specified list of model properties
     *
     * @param modelProperties the model properties to unmarshal. May not be null or empty
     * @param baseUri         the base uri of every model property. May not be null or empty.
     * @return XML string unmarshalled from the specified list of model properties
     * @throws IOException if there was a problem with unmarshalling
     */
    public static String unmarshalModelPropertiesToXml( List<ModelProperty> modelProperties, String baseUri )
        throws IOException
    {
        if ( modelProperties == null || modelProperties.isEmpty() )
        {
            throw new IllegalArgumentException( "modelProperties: null or empty" );
        }

        if ( baseUri == null || baseUri.trim().length() == 0 )
        {
            throw new IllegalArgumentException( "baseUri: null or empty" );
        }

        final int basePosition = baseUri.length();

        StringBuffer sb = new StringBuffer();
        List<String> lastUriTags = new ArrayList<String>();
        for ( ModelProperty mp : modelProperties )
        {
            String uri = mp.getUri();
            if ( uri.contains( "#property" ) )
            {
                continue;
            }

            //String val = (mp.getResolvedValue() != null) ? "\"" + mp.getResolvedValue() + "\"" : null;
            //   System.out.println("new ModelProperty(\"" + mp.getUri() +"\" , " + val +"),");
            if ( !uri.startsWith( baseUri ) )
            {
                throw new IllegalArgumentException(
                    "Passed in model property that does not match baseUri: Property URI = " + uri + ", Base URI = " +
                        baseUri );
            }

            List<String> tagNames = getTagNamesFromUri( basePosition, uri );

            for ( int i = lastUriTags.size() - 1; i >= 0 && i >= tagNames.size() - 1; i-- )
            {
                sb.append( toEndTag( lastUriTags.get( i ) ) );
            }

            String tag = tagNames.get( tagNames.size() - 1 );

            List<ModelProperty> attributes = new ArrayList<ModelProperty>();
            for(int peekIndex = modelProperties.indexOf( mp ) + 1; peekIndex < modelProperties.size(); peekIndex++)
            {
                if ( peekIndex <= modelProperties.size() - 1 )
                {
                    ModelProperty peekProperty = modelProperties.get( peekIndex );
                    if ( peekProperty.getUri().contains( "#property" ) )
                    {
                        attributes.add(peekProperty);
                    }
                    else
                    {
                        break;
                    }
                }
                else
                {
                    break;
                }
            }

            sb.append( toStartTag( tag, attributes ) );

            if ( mp.getResolvedValue() != null )
            {
                sb.append( mp.getResolvedValue() );
            }

            lastUriTags = tagNames;
        }

        for ( int i = lastUriTags.size() - 1; i >= 1; i-- )
        {
            sb.append( toEndTag( lastUriTags.get( i ) ) );
        }

        return sb.toString();
    }

    /**
     * Returns list of tag names parsed from the specified uri. All #collection parts of the tag are removed from the
     * tag names.
     *
     * @param basePosition the base position in the specified URI to start the parse
     * @param uri          the uri to parse for tag names
     * @return list of tag names parsed from the specified uri
     */
    private static List<String> getTagNamesFromUri( int basePosition, String uri )
    {
        return Arrays.asList( uri.substring( basePosition ).replaceAll( "#collection", "" )
                .replaceAll("#set", "").split( "/" ) );
    }

    /**
     * Returns the XML formatted start tag for the specified value and the specified attribute.
     *
     * @param value     the value to use for the start tag
     * @param attributes the attribute to use in constructing of start tag
     * @return the XML formatted start tag for the specified value and the specified attribute
     */
    private static String toStartTag( String value, List<ModelProperty> attributes )
    {
        StringBuffer sb = new StringBuffer(); //TODO: Support more than one attribute
        sb.append( "\r\n<" ).append( value );
        if ( attributes != null )
        {
            for(ModelProperty attribute : attributes)
            {
                sb.append( " " ).append(
                    attribute.getUri().substring( attribute.getUri().indexOf( "#property/" ) + 10 ) ).append( "=\"" )
                    .append( attribute.getResolvedValue() ).append( "\" " );
            }
        }
        sb.append( ">" );
        return sb.toString();
    }

    /**
     * Returns XML formatted end tag for the specified value.
     *
     * @param value the value to use for the end tag
     * @return xml formatted end tag for the specified value
     */
    private static String toEndTag( String value )
    {
        if ( value.trim().length() == 0 )
        {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append( "</" ).append( value ).append( ">" );
        return sb.toString();
    }    
    
    public static List<ModelProperty> getModelProperties(InputStream is) throws IOException
    {
            Set<String> s = new HashSet<String>();
            //TODO: Should add all collections from ProjectUri
            s.addAll(URIS);
            s.add(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.DependencyManagement.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.xURI);
            s.add(ProjectUri.Reporting.Plugins.Plugin.ReportSets.xUri);
            s.add(ProjectUri.Reporting.Plugins.Plugin.ReportSets.ReportSet.configuration);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.Execution.configuration);
            //TODO: More profile info
            s.add(ProjectUri.Profiles.Profile.Build.PluginManagement.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Profiles.Profile.DependencyManagement.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Profiles.Profile.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.Execution.Goals.xURI);
            s.add(ProjectUri.Profiles.Profile.Reporting.Plugins.Plugin.ReportSets.xUri);
            s.add(ProjectUri.Profiles.Profile.Reporting.Plugins.Plugin.ReportSets.ReportSet.configuration);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.Execution.configuration);
            s.add(ProjectUri.Profiles.Profile.properties);
            s.add(ProjectUri.Profiles.Profile.modules);
            s.add(ProjectUri.Profiles.Profile.Dependencies.xUri);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.configuration);
            
        return new ArrayList<ModelProperty>(marshallXmlToModelProperties(is, ProjectUri.baseUri, s ));
    }    
    private static final Set<String> URIS = Collections.unmodifiableSet(new HashSet<String>( Arrays.asList(  ProjectUri.Build.Extensions.xUri,
            ProjectUri.Build.PluginManagement.Plugins.xUri,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.configuration,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.xUri,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.xURI,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.Dependencies.xUri,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.Dependencies.Dependency.Exclusions.xUri,
            ProjectUri.Build.Plugins.xUri,
            ProjectUri.properties,
            ProjectUri.Build.Plugins.Plugin.configuration,
            ProjectUri.Reporting.Plugins.xUri,
            ProjectUri.Reporting.Plugins.Plugin.configuration,
            ProjectUri.Build.Plugins.Plugin.Dependencies.xUri,
            ProjectUri.Build.Resources.xUri,
            ProjectUri.Build.Resources.Resource.includes,
            ProjectUri.Build.Resources.Resource.excludes,
            ProjectUri.Build.TestResources.xUri,
            ProjectUri.Build.Filters.xUri,
            ProjectUri.CiManagement.Notifiers.xUri,
            ProjectUri.Contributors.xUri,
            ProjectUri.Dependencies.xUri,
            ProjectUri.DependencyManagement.Dependencies.xUri,
            ProjectUri.Developers.xUri,
            ProjectUri.Developers.Developer.roles,
            ProjectUri.Licenses.xUri,
            ProjectUri.MailingLists.xUri,
            ProjectUri.Modules.xUri,
            ProjectUri.PluginRepositories.xUri,
            ProjectUri.Profiles.xUri,
            ProjectUri.Profiles.Profile.Build.Plugins.xUri,
            ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Dependencies.xUri,
            ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.xUri,
            ProjectUri.Profiles.Profile.Build.Resources.xUri,
            ProjectUri.Profiles.Profile.Build.TestResources.xUri,
            ProjectUri.Profiles.Profile.Dependencies.xUri,
            ProjectUri.Profiles.Profile.DependencyManagement.Dependencies.xUri,
            ProjectUri.Profiles.Profile.PluginRepositories.xUri,
            ProjectUri.Profiles.Profile.Reporting.Plugins.xUri,
            ProjectUri.Profiles.Profile.Repositories.xUri,
            ProjectUri.Profiles.Profile.Build.PluginManagement.Plugins.xUri,
            ProjectUri.Profiles.Profile.Build.PluginManagement.Plugins.Plugin.Dependencies.xUri,
            ProjectUri.Reporting.Plugins.xUri,
            ProjectUri.Repositories.xUri) ));    
    
   /**
    * Returns list of model properties transformed from the specified input stream.
    *
    * @param inputStream input stream containing the xml document. May not be null.
    * @param baseUri     the base uri of every model property. May not be null or empty.
    * @param collections set of uris that are to be treated as a collection (multiple entries). May be null.
    * @return list of model properties transformed from the specified input stream.
    * @throws IOException if there was a problem doing the transform
    */
    public static List<ModelProperty> marshallXmlToModelProperties( InputStream inputStream, String baseUri,
            Set<String> collections )
			throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("inputStream: null");
		}

		if (baseUri == null || baseUri.trim().length() == 0) {
			throw new IllegalArgumentException("baseUri: null");
		}

		if (collections == null) {
			collections = Collections.emptySet();
		}

		List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
		XMLInputFactory xmlInputFactory = new com.ctc.wstx.stax.WstxInputFactory();
		xmlInputFactory.setProperty(
				XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,
				Boolean.FALSE);

		Uri uri = new Uri(baseUri);
		String tagName = baseUri;
		StringBuilder tagValue = new StringBuilder(256);

		int depth = 0;
		int depthOfTagValue = depth;
		XMLStreamReader xmlStreamReader = null;
		try {
			xmlStreamReader = xmlInputFactory
					.createXMLStreamReader(inputStream);

			Map<String, String> attributes = new HashMap<String, String>();
			for (;; xmlStreamReader.next()) {
				int type = xmlStreamReader.getEventType();
				switch (type) {

				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.CHARACTERS: {
					if (depth == depthOfTagValue) {
						tagValue.append(xmlStreamReader.getTextCharacters(),
								xmlStreamReader.getTextStart(), xmlStreamReader
										.getTextLength());
					}
					break;
				}

				case XMLStreamConstants.START_ELEMENT: {
					if (!tagName.equals(baseUri)) {
						String value = null;
						if (depth < depthOfTagValue) {
							value = tagValue.toString().trim();
						}
						modelProperties.add(new ModelProperty(tagName, value));
						if (!attributes.isEmpty()) {
							for (Map.Entry<String, String> e : attributes
									.entrySet()) {
								modelProperties.add(new ModelProperty(e
										.getKey(), e.getValue()));
							}
							attributes.clear();
						}
					}

					depth++;
					tagName = uri.getUriFor(xmlStreamReader.getName()
							.getLocalPart(), depth);
					if (collections.contains(tagName + "#collection")) {
						tagName = tagName + "#collection";
						uri.addTag(xmlStreamReader.getName().getLocalPart()
								+ "#collection");
					} else if (collections.contains(tagName + "#set")) {
						tagName = tagName + "#set";
						uri.addTag(xmlStreamReader.getName().getLocalPart()
								+ "#set");
					} else {
						uri.addTag(xmlStreamReader.getName().getLocalPart());
					}
					tagValue.setLength(0);
					depthOfTagValue = depth;
				}
				case XMLStreamConstants.ATTRIBUTE: {
					for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {

						attributes.put(tagName
								+ "#property/"
								+ xmlStreamReader.getAttributeName(i)
										.getLocalPart(), xmlStreamReader
								.getAttributeValue(i));
					}
					break;
				}
				case XMLStreamConstants.END_ELEMENT: {
					depth--;
					break;
				}
				case XMLStreamConstants.END_DOCUMENT: {
					modelProperties.add(new ModelProperty(tagName, tagValue
							.toString().trim()));
					if (!attributes.isEmpty()) {
						for (Map.Entry<String, String> e : attributes
								.entrySet()) {
							modelProperties.add(new ModelProperty(e.getKey(), e
									.getValue()));
						}
						attributes.clear();
					}
					return modelProperties;
				}
				}
			}
		} catch (XMLStreamException e) {
			throw new IOException(":" + e.toString());
		} finally {
			if (xmlStreamReader != null) {
				try {
					xmlStreamReader.close();
				} catch (XMLStreamException e) {
					e.printStackTrace();
				}
			}
			try {
				inputStream.close();
			} catch (IOException e) {

			}
		}
	}
   /**
    * Class for storing information about URIs.
    */
   private static class Uri
   {

       List<String> uris;

       Uri( String baseUri )
       {
           uris = new LinkedList<String>();
           uris.add( baseUri );
       }

       String getUriFor( String tag, int depth )
       {
           setUrisToDepth( depth );
           StringBuffer sb = new StringBuffer();
           for ( String tagName : uris )
           {
               sb.append( tagName ).append( "/" );
           }
           sb.append( tag );
           return sb.toString();
       }

       void addTag( String tag )
       {
           uris.add( tag );
       }

       void setUrisToDepth( int depth )
       {
           uris = new LinkedList<String>( uris.subList( 0, depth ) );
       }
   }    
}
