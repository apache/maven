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
            copy( c.getDistributionManagement(), t.getDistributionManagement(), isChildMostSpecialized,
                  c.getArtifactId() );
            if ( p != null && p.getDistributionManagement() != null )
            {
                copy( p.getDistributionManagement(), t.getDistributionManagement(), false, c.getArtifactId() );
            }
        }
        else if ( p != null && p.getDistributionManagement() != null )
        {
            copy( p.getDistributionManagement(), t.getDistributionManagement(), false, c.getArtifactId() );
        }
    }

    private static void copy( DistributionManagement source, DistributionManagement target, boolean isChild,
                              String artifactId )
    {
        if ( target.getDownloadUrl() == null )
        {
            target.setDownloadUrl( source.getDownloadUrl() );
        }

        if ( target.getRelocation() == null && isChild && source.getRelocation() != null )
        {
            Relocation sourceRelocation = source.getRelocation();
            Relocation r = new Relocation();
            r.setArtifactId( sourceRelocation.getArtifactId() );
            r.setGroupId( sourceRelocation.getGroupId() );
            r.setMessage( sourceRelocation.getMessage() );
            r.setVersion( sourceRelocation.getVersion() );
            target.setRelocation( r );
        }

        if ( target.getStatus() == null )
        {
            target.setStatus( source.getStatus() );
        }

        if ( target.getRepository() == null && source.getRepository() != null )
        {
            target.setRepository( new DeploymentRepository() );
            copyRepository( source.getRepository(), target.getRepository() );
        }
        if ( target.getSnapshotRepository() == null && source.getSnapshotRepository() != null )
        {
            target.setSnapshotRepository( new DeploymentRepository() );
            copyRepository( source.getSnapshotRepository(), target.getSnapshotRepository() );
        }

        if ( target.getSite() == null && source.getSite() != null )
        {
            target.setSite( new Site() );
            copySite( source.getSite(), target.getSite(), isChild, artifactId );
        }
    }

    private static void copyRepository( DeploymentRepository source, DeploymentRepository target )
    {
        if ( target.getId() == null )
        {
            target.setId( source.getId() );
        }

        if ( target.getLayout() == null )
        {
            target.setLayout( source.getLayout() );
        }

        if ( target.getName() == null )
        {
            target.setUrl( source.getUrl() );
        }
    }

    private static void copySite( Site source, Site target, boolean isChild, String artifactId )
    {
        if ( target.getId() == null )
        {
            target.setId( source.getId() );
        }

        if ( target.getName() == null )
        {
            target.setName( source.getName() );
        }

        if ( target.getUrl() == null )
        {
            target.setUrl( source.getUrl() + "/" + artifactId );
        }
    }

}
