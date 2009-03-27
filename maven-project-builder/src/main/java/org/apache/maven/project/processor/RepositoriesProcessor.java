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

import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;

public class RepositoriesProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        
        Model t = (Model) target, c = (Model) child, p = (Model) parent;
        if(p != null)
        {
            copy( p.getRepositories(), t.getRepositories() );   
            copy( p.getPluginRepositories(), t.getPluginRepositories() );  
        }    
        copy(c.getPluginRepositories(), t.getPluginRepositories());    
        copy( c.getRepositories(), t.getRepositories() );
    }
    
    private static void copy(List<Repository> sources, List<Repository> targets)
    {
        for(Repository repository : sources)
        {
            Repository match = matches(repository, targets);
            Repository r = null;
            if(match != null)
            {
            	r = match;
            }
            else
            {
            	r = new Repository();	
            }
            
            r.setId( repository.getId() );
            r.setLayout( repository.getLayout() );
            r.setName( repository.getName() );
            r.setUrl( repository.getUrl() );
            if(repository.getReleases() != null)
            {
                r.setReleases( copy(repository.getReleases()) );
            }
            if(repository.getSnapshots() != null)       
            {
                r.setSnapshots( copy(repository.getSnapshots()) );
            }  
            if (match == null)
            {
            	targets.add( r );	
            }          
        }
    }
    
    private static Repository matches(Repository repository, List<Repository> targets)
    {
    	for(Repository r : targets)
    	{
    		if(r.getId() != null && r.getId().equals(repository.getId()))
    		{
    			return r;
    		}
    	}
    	return null;
    }
    
    private static RepositoryPolicy copy(RepositoryPolicy policy)
    {
        RepositoryPolicy p = new RepositoryPolicy();
        p.setChecksumPolicy( policy.getChecksumPolicy() );
        p.setEnabled( policy.isEnabled() );
        p.setUpdatePolicy( policy.getUpdatePolicy() );
        return p;
    }
}
