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


import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;

public abstract class BaseProcessor implements Processor
{

    Object parent;

    Object child;

    Collection<Processor> processors;
    
    private List<Model> parentModels;


    public BaseProcessor( Collection<Processor> processors )
    {
        if ( processors == null )
        {
            throw new IllegalArgumentException( "processors: null" );
        }

        this.processors = processors;
        parentModels = new ArrayList<Model>();
    }
    
    /**
     * Ordered from least specialized to most specialized.
     */
    public List<Model> getParentModels()
    {
    	return parentModels;
    }

    public BaseProcessor()
    {
        this(new ArrayList<Processor>());
    }

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        if ( target == null )
        {
            throw new IllegalArgumentException( "target: null" );
        }

        this.parent = parent;
        this.child = child;
        if(parent instanceof Model)
        {
        	parentModels.add( (Model) parent );
        }
        for ( Processor processor : processors )
        {
            processor.process( parent, child, target, isChildMostSpecialized );
        }

    }

    public Object getChild()
    {
        return child;
    }

    public Object getParent()
    {
        return parent;
    }
    
    protected String normalizeUriWithRelativePath(String u, String artifactId, Model parent)
    {
    	if(u == null)
    	{
    		return null;
    	}
		try 
		{
			String slashes = getSlashes(new URI(u).getRawSchemeSpecificPart());
			URI uri = new URI(u + "/"
					+ getModulePathAdjustment(parent, artifactId));

			String normalized = uri.normalize().toASCIIString();
			if("file".equals(uri.getScheme()))//UNC Paths
			{
				normalized = normalized.replaceFirst("/", slashes);
			}
			return normalized;   
		} 
		catch (URISyntaxException e) {

		}  
		return null;
    }
    
    private static String getSlashes(String uri)
    {
    	StringBuilder sb = new StringBuilder();
    	for(byte b : uri.getBytes())
    	{
    		if(b == 47)
    		{
    			sb.append("/");
    		}
        	else
        	{
        		break;
        	}
    	}
    	return sb.toString();
    }    
    
    private String getModulePathAdjustment(Model moduleProject,
			String artifactId) {

		Map<String, String> moduleAdjustments = new HashMap<String, String>();
		List<String> modules = moduleProject.getModules();
		if (modules != null) {
			for (Iterator<String> it = modules.iterator(); it.hasNext();) {
				String modulePath = it.next();
				String moduleName = modulePath;

				if (moduleName.endsWith("/") || moduleName.endsWith("\\")) {
					moduleName = moduleName.substring(0,
							moduleName.length() - 1);
				}

				int lastSlash = moduleName.lastIndexOf('/');

				if (lastSlash < 0) {
					lastSlash = moduleName.lastIndexOf('\\');
				}

				String adjustment = null;

				if (lastSlash > -1) {
					moduleName = moduleName.substring(lastSlash + 1);
					adjustment = modulePath.substring(0, lastSlash);
				}

				moduleAdjustments.put(moduleName, adjustment);
			}
		}
		String adjust = moduleAdjustments.get(artifactId);
		return (adjust != null) ? adjust + "/" + artifactId :  "/" + artifactId;
	}      
}
