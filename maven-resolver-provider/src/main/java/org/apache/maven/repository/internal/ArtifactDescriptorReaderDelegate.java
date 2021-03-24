package org.apache.maven.repository.internal;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Repository;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * Populates Aether {@link ArtifactDescriptorResult} from Maven project {@link Model}.
 *
 * <strong>Note:</strong> This class is part of work in progress and can be changed or removed without notice.
 * @since 3.2.4
 */
public class ArtifactDescriptorReaderDelegate
{
    public void populateResult( RepositorySystemSession session, ArtifactDescriptorResult result, Model model )
    {
        ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

        for ( Repository r : model.getRepositories() )
        {
            result.addRepository( ArtifactDescriptorUtils.toRemoteRepository( r ) );
        }

        for ( org.apache.maven.model.Dependency dependency : model.getDependencies() )
        {
            result.addDependency( convert( dependency, stereotypes ) );
        }

        DependencyManagement mgmt = model.getDependencyManagement();
        if ( mgmt != null )
        {
            for ( org.apache.maven.model.Dependency dependency : mgmt.getDependencies() )
            {
                result.addManagedDependency( convert( dependency, stereotypes ) );
            }
        }

        Map<String, Object> properties = new LinkedHashMap<>();

        Prerequisites prerequisites = model.getPrerequisites();
        if ( prerequisites != null )
        {
            properties.put( "prerequisites.maven", prerequisites.getMaven() );
        }

        List<License> licenses = model.getLicenses();
        properties.put( "license.count", licenses.size() );
        for ( int i = 0; i < licenses.size(); i++ )
        {
            License license = licenses.get( i );
            properties.put( "license." + i + ".name", license.getName() );
            properties.put( "license." + i + ".url", license.getUrl() );
            properties.put( "license." + i + ".comments", license.getComments() );
            properties.put( "license." + i + ".distribution", license.getDistribution() );
        }

        result.setProperties( properties );

        setArtifactProperties( result, model );
    }

    private Dependency convert( org.apache.maven.model.Dependency dependency, ArtifactTypeRegistry stereotypes )
    {
        ArtifactType stereotype = stereotypes.get( dependency.getType() );
        if ( stereotype == null )
        {
            stereotype = new DefaultArtifactType( dependency.getType() );
        }

        boolean system = dependency.getSystemPath() != null && dependency.getSystemPath().length() > 0;

        Map<String, String> props = null;
        if ( system )
        {
            props = Collections.singletonMap( ArtifactProperties.LOCAL_PATH, dependency.getSystemPath() );
        }

        Artifact artifact =
            new DefaultArtifact( dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), null,
                                 dependency.getVersion(), props, stereotype );

        List<Exclusion> exclusions = new ArrayList<>( dependency.getExclusions().size() );
        for ( org.apache.maven.model.Exclusion exclusion : dependency.getExclusions() )
        {
            exclusions.add( convert( exclusion ) );
        }

        Dependency result = new Dependency( artifact, dependency.getScope(),
                                            dependency.getOptional() != null
                                                ? dependency.isOptional()
                                                : null,
                                            exclusions );

        return result;
    }

    private Exclusion convert( org.apache.maven.model.Exclusion exclusion )
    {
        return new Exclusion( exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*" );
    }

    private void setArtifactProperties( ArtifactDescriptorResult result, Model model )
    {
        String downloadUrl = null;
        DistributionManagement distMgmt = model.getDistributionManagement();
        if ( distMgmt != null )
        {
            downloadUrl = distMgmt.getDownloadUrl();
        }
        if ( downloadUrl != null && downloadUrl.length() > 0 )
        {
            Artifact artifact = result.getArtifact();
            Map<String, String> props = new HashMap<>( artifact.getProperties() );
            props.put( ArtifactProperties.DOWNLOAD_URL, downloadUrl );
            result.setArtifact( artifact.setProperties( props ) );
        }
    }
}
