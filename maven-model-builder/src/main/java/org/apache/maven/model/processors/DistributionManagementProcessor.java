package org.apache.maven.model.processors;

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

import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.Site;

public class DistributionManagementProcessor
    extends BaseProcessor
{

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );

        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;

        if ( t.getDependencyManagement() == null
            && ( p != null && p.getDistributionManagement() != null || c.getDistributionManagement() != null ) )
        {
            t.setDistributionManagement( new DistributionManagement() );
        }

        if ( c.getDistributionManagement() != null )
        {
            if ( p != null && p.getDistributionManagement() != null )
            {
                copy( p.getDistributionManagement(), t.getDistributionManagement(), false, c.getArtifactId(), p );
            }        	
            copy( c.getDistributionManagement(), t.getDistributionManagement(), isChildMostSpecialized,
                  c.getArtifactId(), p );
        }
        else if ( p != null && p.getDistributionManagement() != null )
        {
            copy( p.getDistributionManagement(), t.getDistributionManagement(), false, c.getArtifactId(), p );
        }
        else if(t.getDistributionManagement() != null &&  t.getDistributionManagement().getSite() != null)
        {
            copySite( t.getDistributionManagement().getSite(), t.getDistributionManagement().getSite(), false, c.getArtifactId(), p );
           // copy( t.getDistributionManagement(), t.getDistributionManagement(), isChildMostSpecialized, c.getArtifactId() );    
        }
    }

    private void copy( DistributionManagement source, DistributionManagement target, boolean isChild,
                              String artifactId, Model parent )
    {
        if ( source.getDownloadUrl() != null )
        {
            target.setDownloadUrl( source.getDownloadUrl() );
        }

        if ( isChild && source.getRelocation() != null )
        {
            Relocation sourceRelocation = source.getRelocation();
            Relocation r = new Relocation();
            r.setArtifactId( sourceRelocation.getArtifactId() );
            r.setGroupId( sourceRelocation.getGroupId() );
            r.setMessage( sourceRelocation.getMessage() );
            r.setVersion( sourceRelocation.getVersion() );
            target.setRelocation( r );
        }

        if ( source.getStatus() != null )
        {
            target.setStatus( source.getStatus() );
        }

        if ( source.getRepository() != null )
        {
            target.setRepository( new DeploymentRepository() );
            copyRepository( source.getRepository(), target.getRepository() );
        }

        if ( source.getSnapshotRepository() != null )
        {
            target.setSnapshotRepository( new DeploymentRepository() );
            copyRepository( source.getSnapshotRepository(), target.getSnapshotRepository() );
        }

        if (  source.getSite() != null )
        {
            target.setSite( new Site() );
            copySite( source.getSite(), target.getSite(), isChild, artifactId, parent );
        } 
    }

    private void copyRepository( DeploymentRepository source, DeploymentRepository target )
    {
        if ( source.getId() != null )
        {
            target.setId( source.getId() );
        }

        if ( source.getLayout() != null )
        {
            target.setLayout( source.getLayout() );
        }

        if ( source.getUrl() != null )
        {
        	target.setUrl( source.getUrl() );
        }

        if ( source.getName() != null )
        {
            target.setName( source.getName() );
        }

        target.setUniqueVersion( source.isUniqueVersion() );
    }

    private void copySite( Site source, Site target, boolean isChild, String artifactId, Model parent )
    {
        if ( source.getId() != null )
        {
            target.setId( source.getId() );
        }

        if ( source.getName() != null )
        {
            target.setName( source.getName() );
        }

        if ( target.getUrl() == null )
        {
            if ( isChild )
            {
                target.setUrl( source.getUrl() );
            }         
            else
            {          	
            	target.setUrl(normalizeUriWithRelativePath(source.getUrl(), artifactId, parent));
            }
        }
        else 
        {
            target.setUrl( target.getUrl() + (target.getUrl().endsWith("/")  ? "" : "/")+ artifactId );
        }
    }

}
