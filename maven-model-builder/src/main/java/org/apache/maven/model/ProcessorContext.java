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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.inheritance.DefaultInheritanceAssembler;
import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.management.DefaultManagementInjector;
import org.apache.maven.model.management.ManagementInjector;
import org.apache.maven.model.profile.DefaultProfileInjector;
import org.apache.maven.model.profile.ProfileInjector;

/*
 *  TODO: Get rid of this class and go back to an inheritance assembler, profile injector and default injector, all
 *  orchestrated by the model builder. The processors will also by replaced by the merger.
 */

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
                child = domainModel;
            }
        }
        if(child == null)
        {
            throw new IOException("Could not find child model");
        }
        
        Model target = processModelsForInheritance( convertDomainModelsToMavenModels( domainModels ) );
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

    private static ProfileInjector profileInjector = new DefaultProfileInjector();

    public static DomainModel mergeProfilesIntoModel(Collection<Profile> profiles, DomainModel domainModel) throws IOException
    {
        Model model = domainModel.getModel();

        for ( Profile profile : profiles )
        {
            profileInjector.injectProfile( model, profile );
        }

        return domainModel;
    }

    private static List<Model> convertDomainModelsToMavenModels(List<DomainModel> domainModels) throws IOException
    {
        List<Model> models = new ArrayList<Model>();
        for(DomainModel domainModel : domainModels)
        {
            DomainModel dm = domainModel;
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

    private static InheritanceAssembler inheritanceAssembler = new DefaultInheritanceAssembler();

    private static Model processModelsForInheritance(List<Model> models)
    {
        Collections.reverse( models );    

        Model previousModel = null;

        for ( Model currentModel : models )
        {
            inheritanceAssembler.assembleModelInheritance( currentModel, previousModel );
            previousModel = currentModel;
        }

        return previousModel;
    }

    private static ManagementInjector managementInjector = new DefaultManagementInjector();
    
    public static Model processManagementNodes(Model target) 
    	throws IOException
    {
        managementInjector.injectManagement( target );
        return target;
    }

}
