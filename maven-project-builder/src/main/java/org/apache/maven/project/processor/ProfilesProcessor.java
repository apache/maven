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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Resource;


public class ProfilesProcessor extends BaseProcessor
{
    private static List<Processor> processors =
        Arrays.<Processor> asList( new BuildProcessor( new ArrayList<Processor>() ), new ModuleProcessor(),
                                   new PropertiesProcessor(), new ParentProcessor(), new OrganizationProcessor(),
                                   new MailingListProcessor(), new IssueManagementProcessor(),
                                   new CiManagementProcessor(), new ReportingProcessor(),
                                   new RepositoriesProcessor(), new DistributionManagementProcessor(),
                                   new LicensesProcessor(), new ScmProcessor(), new PrerequisitesProcessor(),
                                   new ContributorsProcessor(), new DevelopersProcessor());
    
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        List<Profile> profiles = ((Model) child).getProfiles();
        List<Profile> copies = new ArrayList<Profile>();
        for(Profile p : profiles)
        {
            copies.add( ProcessorContext.copyOfProfile(p) );
        }
        t.setProfiles( copies );
   
        //TODO - copy
    }  
    

    
    private static Model attachProfileNodesToModel(Profile profile)
    {
        Model model = new Model();
        model.setModules( new ArrayList<String>(profile.getModules()) );
        model.setDependencies(new ArrayList<Dependency>(profile.getDependencies()));
        model.setDependencyManagement( profile.getDependencyManagement());
        model.setDistributionManagement( profile.getDistributionManagement() );
        model.setProperties( profile.getProperties() );  
        model.setModules( new ArrayList<String>(profile.getModules() ) );
        BuildProcessor proc = new BuildProcessor( new ArrayList<Processor>());
        proc.processWithProfile( profile.getBuild(), model);
        return model;
    }     
}
