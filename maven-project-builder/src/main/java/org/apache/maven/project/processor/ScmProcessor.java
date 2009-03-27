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

import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;

public class ScmProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;
        if(t.getScm() == null)
        {
            t.setScm( new Scm() );    
        }
        
        copyUrl( ((p != null) ? p.getScm() : null), c.getScm(), t.getScm(), c.getArtifactId(), p);
        copyConnection( ((p != null) ? p.getScm() : null), c.getScm(), t.getScm(), c.getArtifactId(), p);
        copyDeveloperConnection( ((p != null) ? p.getScm() : null), c.getScm(), t.getScm(), c.getArtifactId(), p);
        copyTag( ( ( p != null ) ? p.getScm() : null ), c.getScm(), t.getScm() );
    }
    
    private void copyUrl(Scm p, Scm c, Scm t, String artifactId, Model parent )
    {
        if(c != null && c.getUrl() != null)
        {
            t.setUrl(c.getUrl() );                       
        }   
        else if(p != null && p.getUrl() != null)
        {
        	t.setUrl( normalizeUri(p.getUrl(), artifactId, parent));
        }      
        else if(t.getUrl() != null) {
            t.setUrl( t.getUrl() + "/" + artifactId );
        }
    }
    
    private void copyConnection(Scm p, Scm c, Scm t, String artifactId, Model parent )
    {
        if(c!= null && c.getConnection() != null)
        {
            t.setConnection(c.getConnection());         
        }       
        else if(p != null && p.getConnection() != null)
        {       	
            t.setConnection(  normalizeUri(p.getConnection(), artifactId, parent));
        } 
        else if(t.getConnection() != null) {
            t.setConnection( t.getConnection() + "/" + artifactId );
        }        
    }
    
    private void copyDeveloperConnection(Scm p, Scm c, Scm t, String artifactId, Model parent )
    {
        if(c!= null && c.getDeveloperConnection() != null)
        {
            t.setDeveloperConnection(c.getDeveloperConnection());         
        }       
        else if(p != null && p.getDeveloperConnection() != null)
        {     	
            t.setDeveloperConnection( normalizeUri(p.getDeveloperConnection(), artifactId, parent) );
        }    
        else if(t.getDeveloperConnection() != null){
            t.setDeveloperConnection( t.getDeveloperConnection() + "/" + artifactId );
        }           
    }    
    
    private static void copyTag( Scm p, Scm c, Scm t )
    {
        if ( c != null && c.getTag() != null )
        {
            t.setTag( c.getTag() );
        }
        else if ( p != null && p.getTag() != null )
        {
            t.setTag( p.getTag() );
        }
    }

}
