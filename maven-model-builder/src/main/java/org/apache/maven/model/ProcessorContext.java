package org.apache.maven.model;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.processors.BuildProcessor;
import org.apache.maven.model.processors.CiManagementProcessor;
import org.apache.maven.model.processors.ContributorsProcessor;
import org.apache.maven.model.processors.DependencyManagementProcessor;
import org.apache.maven.model.processors.DevelopersProcessor;
import org.apache.maven.model.processors.DistributionManagementProcessor;
import org.apache.maven.model.processors.IssueManagementProcessor;
import org.apache.maven.model.processors.LicensesProcessor;
import org.apache.maven.model.processors.MailingListProcessor;
import org.apache.maven.model.processors.ModelProcessor;
import org.apache.maven.model.processors.ModuleProcessor;
import org.apache.maven.model.processors.OrganizationProcessor;
import org.apache.maven.model.processors.ParentProcessor;
import org.apache.maven.model.processors.PluginsManagementProcessor;
import org.apache.maven.model.processors.PrerequisitesProcessor;
import org.apache.maven.model.processors.ProfilePropertiesProcessor;
import org.apache.maven.model.processors.ProfilesModuleProcessor;
import org.apache.maven.model.processors.ProfilesProcessor;
import org.apache.maven.model.processors.PropertiesProcessor;
import org.apache.maven.model.processors.ReportingProcessor;
import org.apache.maven.model.processors.RepositoriesProcessor;
import org.apache.maven.model.processors.ScmProcessor;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class ProcessorContext
{

    /**
     * Parent domain models on bottom.
     * 
     * @param domainModels
     * @param listeners 
     * @return
     * @throws IOException
     */
    public static DomainModel build( List<DomainModel> domainModels, List<ModelEventListener> listeners )
        throws IOException
    {  
        DomainModel child = null;
        for ( DomainModel domainModel : domainModels )
        {   
            if(domainModel.isMostSpecialized())
            {
                child = (DomainModel) domainModel;
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
        Model target = processModelsForInheritance( convertDomainModelsToMavenModels( domainModels ), processors, false );
        if(listeners != null)
        {
        	for(ModelEventListener listener : listeners)
        	{
        		listener.fire(target);
        	}
        }       
        DomainModel domainModel = new DomainModel( target, child.isMostSpecialized() );
        domainModel.setProjectDirectory(child.getProjectDirectory());
        domainModel.setParentFile(child.getParentFile());

        return domainModel;
    }
    
    public static DomainModel mergeProfilesIntoModel(Collection<Profile> profiles, DomainModel domainModel) throws IOException
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
        
        Model target = processModelsForInheritance(profileModels, processors, true);

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
        
        DomainModel targetModel = new DomainModel( target, domainModel.isMostSpecialized());
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
            DomainModel dm = (DomainModel) domainModel;
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
    
    private static Model processModelsForInheritance(List<Model> models, List<Processor> processors, boolean isProfile)
    {
        ModelProcessor modelProcessor = new ModelProcessor( processors, isProfile );
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
 
        return target;
      
    }
    
    private static void addPlugin(Build build, String id)
    {	
    	Plugin p1 = new Plugin();
    	p1.setArtifactId(id);
    	build.addPlugin(p1);   	
    }
    
    public static Model processManagementNodes(Model target) 
    	throws IOException
    {
    //	Plugin plugin = new Plugin();
    //	plugin.setArtifactId("maven-compiler-plugin");
  //  	target.getBuild().addPlugin(plugin);
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
}
